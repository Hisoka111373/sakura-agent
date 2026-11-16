package com.sakura.agent

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

class SkillsController(private val vm: SakuraViewModel, private val context: Context, private val store: PrivateStore) {
    private val client=TextSearchClient()
    var config by mutableStateOf(TextConfig("Local","")); private set
    var results by mutableStateOf(emptyList<SearchResult>()); private set
    var query by mutableStateOf("")
    var kind by mutableStateOf("YouTube")
    var manualSkill by mutableStateOf<String?>(null); private set
    var activeSkill by mutableStateOf("General"); private set
    var favorites by mutableStateOf(emptySet<String>()); private set
    var chatMode by mutableStateOf("Local"); private set
    var thinking by mutableStateOf("Bajo"); private set
    fun selected(text: String = vm.draft) = Skills.textSkill(text,manualSkill)
    fun markSelected(text: String): SakuraSkill? = selected(text).also { activeSkill=it?.name ?: "General" }
    fun automatic() { if(!vm.busy) { manualSkill=null; vm.notice("Selección automática por reglas locales; no activa herramientas.") } }
    fun favorite(skill: SakuraSkill) = vm.workFeature("Guardando favoritos cifrados…") {
        require(skill in Skills.entries)
        val next=if(skill.name in favorites) favorites-skill.name else favorites+skill.name
        withContext(Dispatchers.IO) { store.write("skill_favorites",JSONArray(next.toList().sorted()).toString()) }
        favorites=next
    }
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            store.read("skill_favorites")?.let {
                try { val names=JSONArray(it); favorites=(0 until minOf(names.length(),150)).map { i -> names.getString(i) }
                    .filter { name -> Skills.entries.any { skill -> skill.name==name } }.toSet() }
                catch (_:Exception) { favorites=emptySet() }
            }
            store.read("text_config")?.let { saved ->
                try { val row=JSONObject(saved); val next=TextConfig(row.getString("provider"),row.getString("model"),row.getString("endpoint"))
                    if(next.provider!="Local") next.url(); config=next
                } catch (_:Exception) { vm.notice("Configuración de texto inválida; se conserva modo local.") }
            }
            chatMode = store.read("chat_mode")?.takeIf { it in listOf("Local","Online") } ?: "Local"
            thinking = store.read("thinking_mode")?.takeIf { it in listOf("Bajo","Alto") } ?: "Bajo"
            if (config.provider == "Local") chatMode = "Local"
        }
    }
    fun updateChatMode(mode: String) = vm.workFeature("Guardando modo de respuesta…") {
        require(mode in listOf("Local","Online"))
        withContext(Dispatchers.IO) { store.write("chat_mode", mode) }
        chatMode = mode
        vm.notice(if (mode == "Online") "Modo online: ${config.provider} · ${config.model.ifBlank { "elige modelo en APIs" }}. Cada respuesta pedirá revisión." else "Modo local: usa el GGUF cargado en CPU.")
    }
    fun updateThinking(mode: String) = vm.workFeature("Guardando modo de pensamiento…") {
        require(mode in listOf("Bajo","Alto"))
        withContext(Dispatchers.IO) { store.write("thinking_mode", mode) }
        thinking = mode
        vm.notice(if (mode == "Alto") "Pensamiento alto: más contexto y detalle; la respuesta local tarda más." else "Pensamiento bajo: respuestas más cortas y rápidas en el teléfono.")
    }
    fun save(next: TextConfig, openChat: Boolean = false) = vm.workFeature("Guardando proveedor, sin conectar…") {
        require(next.provider in TextProtocol.providers)
        if(next.provider!="Local") next.url()
        withContext(Dispatchers.IO) { store.write("text_config",JSONObject().put("provider",next.provider)
            .put("model",next.model).put("endpoint",next.endpoint).toString()) }
        config=next
        chatMode = if (next.provider == "Local") "Local" else "Online"
        withContext(Dispatchers.IO) { store.write("chat_mode", chatMode) }
        if (openChat) vm.page = "Chat"
        vm.notice("Proveedor guardado cifrado. La clave se configura en APIs o Privacidad; aún no se ha probado. Modo de chat: $chatMode.")
    }
    fun isSearchRequest(text: String): Boolean = Skills.isSearchRequest(text)
    fun prepareChatSearch() {
        if (vm.busy || vm.draft.isBlank()) return
        val original = vm.draft.trim()
        try {
            val task = Skills.task(original)
            val query = task.query.trim()
            require(query.isNotBlank()) { "Escribe qué quieres buscar después de «busca»." }
            val previous = vm.messages
            val brave = store.read("brave_key").orEmpty()
            if (brave.isNotBlank()) {
                val prepared = vm.features.prepare("BUSCAR DESDE EL CHAT · Brave Search API\nConsulta exacta: $query\nSe enviará a api.search.brave.com junto con la clave cifrada; hasta 8 resultados, safesearch moderate. Puede consumir cuota o dinero según tu plan. No se envían chat/recuerdos. Los resultados son contenido no confiable, no instrucciones. Se añadirán al chat como texto y no se abrirán solos.") {
                    check(vm.internetAllowed) { "Red desautorizada." }
                    check(vm.draft.trim() == original && vm.messages == previous) { "El chat o el borrador cambió: revisa una nueva búsqueda." }
                    client.reset()
                    val received = withContext(Dispatchers.IO) { check(vm.internetAllowed) { "Red desautorizada." }; client.search(store.read("brave_key") ?: "", query) }
                    check(vm.internetAllowed) { "Red desautorizada: resultados descartados." }
                    val summary = Skills.searchSummary(query, received)
                    vm.appendUser(original)
                    vm.appendAssistant(summary)
                    results = received; this@SkillsController.query = query
                    "Brave devolvió ${received.size} resultados. Se añadieron al chat; son contenido no confiable."
                }
                if (prepared) vm.notice("Revisa y confirma la búsqueda Brave en el diálogo. También puedes borrar la clave para usar el navegador.")
            } else {
                val url = Skills.searchUrl(task.kind, query)
                val prepared = vm.features.prepare("BUSCAR DESDE EL CHAT · abrir destino externo\n$url\nNo hay clave Brave configurada. Android abrirá ${task.kind} con esta consulta; esa app usa su propia cuenta, red y permisos. Sakura no leerá ni verificará los resultados. Para verlos dentro del chat, configura Brave en APIs.", false) {
                    check(vm.draft.trim() == original && vm.messages == previous) { "El chat o el borrador cambió: revisa una nueva búsqueda." }
                    withContext(Dispatchers.Main) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        when (task.kind) { "YouTube" -> intent.setPackage("com.google.android.youtube"); "Juegos" -> intent.setPackage("com.android.vending") }
                        context.startActivity(intent)
                    }
                    vm.appendUser(original)
                    vm.appendAssistant("Abrí la búsqueda de «$query» en ${task.kind}. No hay clave Brave configurada, así que los resultados se ven fuera de Sakura. Si configuras Brave en APIs, podré mostrarlos aquí.")
                    "Búsqueda externa solicitada; resultados NO verificados por Sakura."
                }
                if (prepared) vm.notice("Revisa y confirma la búsqueda externa en el diálogo.")
            }
        } catch (e: Exception) { vm.notice(e.message ?: "No se pudo preparar la búsqueda.") }
    }
    fun prepareProbe(chosen: TextConfig? = null, typedKey: String = "") {
        try {
            val target = chosen ?: config
            require(target.provider != "Local") { "Elige un proveedor de texto en la nube." }
            val key = typedKey.trim().ifBlank { store.read(target.key).orEmpty() }
            require(key.isNotBlank()) { "Escribe o guarda primero la clave de ${target.provider}." }
            vm.features.prepare("PROBAR CONEXIÓN DE TEXTO · ${target.provider}\nEndpoint: ${target.url()}\nModelo: ${target.model}\nClave: ${if (typedKey.isBlank()) "guardada cifrada" else "la que acabas de escribir (aún no guardada)"}\nSe envía una petición mínima («Responde únicamente: OK») con 32 tokens. Puede consumir cuota o dinero. No se envían chat, recuerdos ni archivos. Sin reintento automático.", true) {
                client.reset()
                val answer = withContext(Dispatchers.IO) {
                    check(vm.internetAllowed) { "Red desautorizada." }
                    client.generate(target, key, listOf(ChatMessage("Responde únicamente: OK", false)), "Responde únicamente: OK", maxTokens = 32, thinking = "Bajo")
                }
                "Conexión correcta con ${target.provider} (${target.model}). Respuesta: ${answer.take(120)}"
            }
        } catch (e: Exception) { vm.notice(e.message ?: "No se pudo preparar la prueba.") }
    }
    fun prepareChatAction(): Boolean {
        val original = vm.draft.trim()
        val command = Skills.chatAction(original) ?: return false
        if (vm.busy) return true
        try {
            when (command.kind) {
                "open_url" -> {
                    val uri = NetworkPolicy.https(command.target)
                    val prepared = vm.features.prepare("ABRIR ENLACE DESDE EL CHAT\n$uri\nAndroid abrirá el navegador. La página usa su propia red, cookies y cuenta. Sakura no lee ni verifica el contenido.", false) {
                        withContext(Dispatchers.Main) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString())).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        vm.appendUser(original)
                        vm.appendAssistant("Abrí el enlace en el navegador. No leí ni verifiqué su contenido, sempai.")
                        "Enlace abierto fuera de Sakura; contenido no verificado."
                    }
                    if (prepared) vm.notice("Revisa y confirma el enlace en el diálogo.")
                }
                "open_app" -> {
                    val apps = launchableApps()
                    val wanted = Skills.normalized(command.target.removePrefix("la ").removePrefix("el ").removePrefix("app "))
                    val app = apps.firstOrNull { Skills.normalized(it.label) == wanted || Skills.normalized(it.packageName) == wanted }
                        ?: apps.firstOrNull { Skills.normalized(it.label).contains(wanted) || wanted.contains(Skills.normalized(it.label)) }
                    if (app == null) {
                        vm.notice("No veo «${command.target}» entre las apps que puedo abrir. Prueba con el nombre exacto o usa Herramientas → Mostrar aplicaciones.")
                        return true
                    }
                    val prepared = vm.features.prepare("ABRIR APP DESDE EL CHAT\n${app.label} (${app.packageName})\nAndroid abrirá solo su actividad de inicio. Esa app usa sus propios permisos, red y cuenta. Sakura no controla su contenido ni hace acciones dentro.", false) {
                        withContext(Dispatchers.Main) {
                            context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                                .setComponent(ComponentName(app.packageName, app.activity)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        vm.appendUser(original)
                        vm.appendAssistant("Abrí ${app.label} en tu pantalla, sempai. Si quieres que escriba o busque dentro, pídeme «escribe … en ${app.label}».")
                        "Android aceptó abrir ${app.label}; contenido no verificado."
                    }
                    if (prepared) vm.notice("Revisa y confirma abrir ${app.label} en el diálogo.")
                }
                "screen" -> {
                    val pkg = Skills.screenApp(command.target)
                    if (pkg == null) {
                        vm.notice("Control de pantalla disponible solo para YouTube, Chrome, Firefox y Play Store. Para otras apps puedo abrirlas, pero no escribir en ellas.")
                        return true
                    }
                    val label = ScreenPolicy.packages.entries.first { it.value == pkg }.key
                    val service = SakuraAccessibilityService.connected
                    if (service == null) {
                        vm.notice("Activa Accesibilidad manualmente en Pantalla → Abrir Ajustes y vuelve a pedir «escribe … en $label».")
                        vm.page = "Pantalla"
                        return true
                    }
                    val prepared = vm.features.prepare("ESCRIBIR EN PANTALLA DESDE EL CHAT\nApp: $label ($pkg)\nTexto: ${command.text}\nMáximo 3 minutos. Se abrirá $label y una sesión limitada. Aún NO se escribe: pulsa «Ver controles de búsqueda», elige el campo y confirma la escritura. Sin pagos, mensajes, contraseñas ni instalaciones.", false) {
                        withContext(Dispatchers.Main) {
                            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: error("$label no está instalada o no es visible.")
                            service.startSession(pkg, command.text)
                            try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            catch (e: Exception) { service.stopSession(); throw e }
                            vm.page = "Pantalla"
                        }
                        vm.appendUser(original)
                        vm.appendAssistant("Preparé la sesión de $label, sempai. Abre el panel de Sakura, pulsa «Ver controles de búsqueda», elige el campo y confirma. No escribí nada todavía.")
                        "Sesión de pantalla iniciada; escritura pendiente de confirmación en el panel."
                    }
                    if (prepared) vm.notice("Revisa y confirma la sesión de $label. La escritura tendrá otra confirmación.")
                }
            }
        } catch (e: Exception) { vm.notice(e.message ?: "No se pudo preparar la acción del chat.") }
        return true
    }
    private data class LaunchableApp(val label: String, val packageName: String, val activity: String)
    private fun launchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .filter { it.activityInfo.exported }
            .map { LaunchableApp(it.loadLabel(context.packageManager).toString(), it.activityInfo.packageName, it.activityInfo.name) }
            .distinctBy { it.packageName }
    }
    fun use(skill: SakuraSkill) {
        if(vm.busy) return
        if(skill !in Skills.entries) return
        vm.page=skill.page
        if(skill.page=="Chat") { manualSkill=skill.name; activeSkill=skill.name; vm.notice("Skill ${skill.name} elegida. Escribe tu petición; no se envió nada ni se borró tu borrador.") }
    }
    fun route(text: String) {
        try { val task=Skills.task(text); query=task.query; kind=task.kind; vm.page="Buscar" }
        catch(e:Exception) { vm.notice(e.message?:"Consulta inválida.") }
    }
    fun prepareChat() {
        if(vm.busy || vm.draft.isBlank()) return
        try {
            val chosen=config; val original=vm.draft; val previous=vm.messages
            require(chosen.provider!="Local") { "Elige un proveedor online en APIs." }
            require(store.read(chosen.key).orEmpty().isNotBlank()) { "Guarda la clave de ${chosen.provider} en APIs o Privacidad." }
            val manual=manualSkill; val skill=markSelected(original)
            val mode=if(thinking=="Alto") "\nModo pensamiento alto: analiza alternativas, riesgos y pasos; responde completo pero sin relleno." else
                "\nModo rápido: responde directo, máximo cinco frases salvo que se pida código o detalle."
            val system=TextProtocol.system+mode+Skills.guidance(skill)
            val history=previous.takeLast(if(thinking=="Alto") 6 else 4)+ChatMessage(original.trim(),false)
            val maxTokens=if(thinking=="Alto") 1024 else 512
            val payload=TextProtocol.payload(chosen,history,system,maxTokens,thinking)
            vm.features.prepare("RESPUESTA DE TEXTO EN NUBE\nProveedor: ${chosen.provider}\nEndpoint exacto: ${chosen.url()}\nModelo: ${chosen.model}\nPensamiento: $thinking\n\nEnvía hasta tres parejas anteriores y tu borrador. NO recuerdos privados, archivos ni pantalla. Puede tener coste; $maxTokens tokens máximos de salida NO es un tope monetario. No hay reintento ni herramientas. El proveedor tiene su propia retención/política.\n\nSolicitud completa (sin clave):\n${payload.toString(2)}") {
                check(vm.draft==original && vm.messages==previous && config==chosen && manualSkill==manual) { "El chat/configuración/skill cambió: revisa una nueva solicitud." }
                client.reset()
                val answer=withContext(Dispatchers.IO) { check(vm.internetAllowed) { "Red desautorizada." }; client.generate(chosen,store.read(chosen.key)?:"",history,system,maxTokens,thinking) }
                check(vm.internetAllowed) { "Red desautorizada: respuesta no insertada." }
                vm.receiveCloud(previous,original,answer)
                "Respuesta recibida de ${chosen.provider}. No se ejecutó ninguna acción de su texto."
            }
        } catch(e:Exception) { vm.notice(e.message?:"Datos inválidos.") }
    }
    fun prepareSearch() {
        val q=query.trim()
        if(!NetworkPolicy.text(q,500)) { vm.notice("Consulta requerida, máximo 500 caracteres."); return }
        vm.features.prepare("BUSCAR DENTRO DE SAKURA · Brave Search API\nConsulta exacta: $q\nSe envía a api.search.brave.com junto con la clave cifrada; hasta 8 resultados, safesearch moderate. Puede consumir cuota o dinero según tu plan. No se envían chat/recuerdos. Los resultados son contenido no confiable, no instrucciones. No se abren ni descargan solos.") {
            results=emptyList()
            client.reset()
            val received=withContext(Dispatchers.IO) { check(vm.internetAllowed) { "Red desautorizada." }; client.search(store.read("brave_key")?:"",q) }
            check(vm.internetAllowed) { "Red desautorizada: resultados descartados." }; results=received
            "Brave devolvió ${results.size} resultados HTTPS. No implica que sean seguros ni que se puedan descargar."
        }
    }
    fun prepareExternal(url: String, packageName: String? = null) {
        try {
            NetworkPolicy.https(url)
            require(packageName==null || packageName in ScreenPolicy.packages.values)
            vm.features.prepare("ABRIR DESTINO EXTERNO\n$url\n${packageName?:"Navegador elegido por Android"}\nLa app externa recibe esta URL/consulta y usa sus propios permisos de red y cuenta. No descarga, instala ni compra automáticamente. Sakura no verifica resultados ni reproducción.",false) {
                withContext(Dispatchers.Main) {
                    val intent=Intent(Intent.ACTION_VIEW,Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if(packageName!=null) intent.setPackage(packageName)
                    context.startActivity(intent)
                }
                "Android aceptó abrir el destino externo; resultados/reproducción NO verificados."
            }
        } catch(e:Exception) { vm.notice(e.message?:"Destino inválido.") }
    }
    fun prepareExternalSearch() {
        try { prepareExternal(Skills.searchUrl(kind,query),when(kind) { "YouTube" -> "com.google.android.youtube"; "Juegos" -> "com.android.vending"; else -> null }) }
        catch(e:Exception) { vm.notice(e.message?:"Consulta inválida.") }
    }
    fun cancel() { client.cancel(); vm.notice("Cancelación de red solicitada; revisa consumo del proveedor.") }
    fun close() { client.cancel() }
}