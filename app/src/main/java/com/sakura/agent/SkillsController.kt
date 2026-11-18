package com.sakura.agent

import android.content.Context
import android.content.Intent
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
    var onlineConfig by mutableStateOf(TextConfig("Gemini","gemini-3.1-flash-lite")); private set
    var connectionStatus by mutableStateOf("Aún no probado desde este teléfono."); private set
    var results by mutableStateOf(emptyList<SearchResult>()); private set
    var query by mutableStateOf("")
    var kind by mutableStateOf("YouTube")
    var manualSkill by mutableStateOf<String?>(null); private set
    var activeSkill by mutableStateOf("General"); private set
    var favorites by mutableStateOf(emptySet<String>()); private set
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
            store.read("online_config")?.let { saved ->
                try { val row=JSONObject(saved); val next=TextConfig(row.getString("provider"),row.getString("model"),row.getString("endpoint"),row.optString("effort","Bajo"))
                    require(next.provider != "Local" && next.effort in ThinkingPolicy.levels); next.url(); onlineConfig=next
                } catch(_: Exception) { }
            }
            store.read("skill_favorites")?.let {
                try { val names=JSONArray(it); favorites=(0 until minOf(names.length(),150)).map { i -> names.getString(i) }
                    .filter { name -> Skills.entries.any { skill -> skill.name==name } }.toSet() }
                catch (_:Exception) { favorites=emptySet() }
            }
            store.read("text_config")?.let { saved ->
                try { val row=JSONObject(saved); val next=TextConfig(row.getString("provider"),row.getString("model"),row.getString("endpoint"),row.optString("effort","Bajo"))
                    require(next.effort in ThinkingPolicy.levels)
                    if(next.provider!="Local") { next.url(); onlineConfig=next }; config=next
                } catch (_:Exception) { vm.notice("Configuración de texto inválida; se conserva modo local.") }
            }
        }
    }
    fun save(next: TextConfig) = vm.workFeature("Guardando proveedor, sin conectar…") {
        require(next.provider in TextProtocol.providers)
        require(next.effort in ThinkingPolicy.levels)
        if(next.provider!="Local") next.url()
        val online = if(next.provider != "Local") next else onlineConfig.copy(effort=next.effort)
        withContext(Dispatchers.IO) { store.write("text_config",JSONObject().put("provider",next.provider)
            .put("model",next.model).put("endpoint",next.endpoint).put("effort",next.effort).toString())
            store.write("online_config",JSONObject().put("provider",online.provider).put("model",online.model).put("endpoint",online.endpoint).put("effort",online.effort).toString())
        }
        config=next; onlineConfig=online; invalidateConnection(); vm.notice("Motor ${next.provider} · esfuerzo ${next.effort} guardado. No se envió ninguna petición. Clave en Privacidad.")
    }
    fun selectEngine(local: Boolean) { if(!vm.busy) save(if(local) TextConfig("Local","",effort=config.effort) else onlineConfig.copy(effort=config.effort)) }
    fun effort(level: String) { if(!vm.busy && level in ThinkingPolicy.levels) save(config.copy(effort=level)) }
    fun invalidateConnection() { connectionStatus="Configuración cambiada: prueba pendiente desde este teléfono." }
    fun testGemini() {
        if(vm.busy || config.provider != "Gemini") return
        try {
            val chosen=config
            val history=listOf(ChatMessage("Prueba de conexión de Sakura: responde solo Sakura conectada.",false))
            val payload=TextProtocol.payload(chosen,history)
            val ready=vm.features.prepare("COMPROBAR GEMINI CON UNA PETICIÓN REAL\nModelo: ${chosen.model}\nEndpoint: ${chosen.url()}\nNo usa chat, recuerdos, archivos ni pantalla. Puede consumir cuota/dinero; límite ${ThinkingPolicy.cloudTokens(chosen)} tokens no es un tope monetario. No se reintenta.\nSolicitud completa sin clave:\n${payload.toString(2)}") {
                check(config==chosen) { "La configuración cambió; vuelve a preparar." }
                client.reset(); val start=android.os.SystemClock.elapsedRealtime()
                val answer=withContext(Dispatchers.IO) { check(vm.internetAllowed); client.generate(chosen,store.read("gemini_key").orEmpty(),history) }
                check(vm.internetAllowed) { "Red desautorizada; prueba descartada." }
                "Gemini respondió realmente en ${(android.os.SystemClock.elapsedRealtime()-start)/1000.0} s:\n${answer.take(500)}\nModelo: ${chosen.model}. No verifica velocidad futura ni todas las herramientas."
            }
            if(ready) vm.features.observePending("gemini-check") { result -> connectionStatus=result.getOrElse { "Prueba NO confirmada: ${it.message}" } }
        } catch(e: Exception) { vm.notice(e.message ?: "Configuración Gemini inválida.") }
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
    fun prepareProbe() {
        try {
            val chosen = config
            require(chosen.provider != "Local") { "Elige un proveedor de texto en la nube." }
            require(store.read(chosen.key).orEmpty().isNotBlank()) { "Guarda primero la clave de ${chosen.provider}." }
            val history=listOf(ChatMessage("Responde solo: OK", false))
            val instruction="Responde solo: OK"
            val payload=TextProtocol.payload(chosen,history,instruction)
            vm.features.prepare("PROBAR CONEXIÓN DE TEXTO · ${chosen.provider}\nEndpoint: ${chosen.url()}\nModelo: ${chosen.model}\nPetición completa sin clave:\n${payload.toString(2)}\nPuede consumir cuota o dinero. Sin chat/recuerdos/archivos ni reintento.", true) {
                check(config==chosen) { "Configuración cambiada: revisa de nuevo." }
                client.reset()
                val answer = withContext(Dispatchers.IO) {
                    check(vm.internetAllowed) { "Red desautorizada." }
                    client.generate(chosen, store.read(chosen.key) ?: "", listOf(ChatMessage("Responde solo: OK", false)), "Responde solo: OK")
                }
                "Conexión correcta con ${chosen.provider}. Respuesta: ${answer.take(120)}"
            }
        } catch (e: Exception) { vm.notice(e.message ?: "No se pudo preparar la prueba.") }
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
            val manual=manualSkill; val skill=markSelected(original)
            val system=TextProtocol.system+Skills.guidance(skill)+ThinkingPolicy.guidance(chosen.effort)+(if(vm.kawaii) ThinkingPolicy.kawaii else "")
            val history=ThinkingPolicy.boundedHistory(previous,chosen.effort)+ChatMessage(original.trim(),false)
            val payload=TextProtocol.payload(chosen,history,system)
            vm.features.prepare("RESPUESTA DE TEXTO EN NUBE\nProveedor: ${chosen.provider}\nEndpoint exacto: ${chosen.url()}\nModelo: ${chosen.model} · esfuerzo ${chosen.effort}\n\nEnvía hasta ${ThinkingPolicy.chatHistory(chosen.effort)/2} parejas anteriores y tu borrador. NO recuerdos privados, archivos ni pantalla. Puede tener coste; ${ThinkingPolicy.cloudTokens(chosen)} tokens máximos de salida (incluye pensamiento en Gemini) NO es un tope monetario. No hay reintento ni herramientas. El proveedor tiene su propia retención/política.\n\nSolicitud completa (sin clave):\n${payload.toString(2)}") {
                check(vm.draft==original && vm.messages==previous && config==chosen && manualSkill==manual) { "El chat/configuración/skill cambió: revisa una nueva solicitud." }
                client.reset()
                vm.reactCompanion("Pensando")
                val answer=withContext(Dispatchers.IO) { check(vm.internetAllowed) { "Red desautorizada." }; client.generate(chosen,store.read(chosen.key)?:"",history,system) }
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