package com.sakura.agent

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Event-driven supervised agent. No loop, blanket consent, polling or automatic continuation. */
class AgentController(private val vm: SakuraViewModel, private val context: Context, private val store: PrivateStore) {
    private val text = TextSearchClient()
    private val web = WebClient()
    private val github = GitHubClient()
    private var revision = 0
    private var objective = ""
    private var observations = emptyList<String>()
    var goal by mutableStateOf("")
    var proposal by mutableStateOf("")
    var steps by mutableStateOf(0); private set
    var active by mutableStateOf(false); private set
    var awaiting by mutableStateOf(false); private set
    var events by mutableStateOf(emptyList<String>()); private set
    var final by mutableStateOf(""); private set
    val canNext get() = active && !awaiting && proposal.isEmpty() && steps < AgentTools.MAX_STEPS

    fun start() {
        if(vm.busy || awaiting) return
        if(!NetworkPolicy.text(goal.trim(),1500)) { vm.notice("Objetivo requerido, máximo 1500 caracteres; no incluyas secretos."); return }
        revision++; objective = goal.trim(); observations = emptyList(); events = emptyList()
        proposal = ""; final = ""; steps = 0; active = true
        next()
    }
    fun next() {
        if(vm.busy || !canNext) return
        val id = revision
        val chosen = vm.skills.config
        val system = AgentTools.prompt+ThinkingPolicy.guidance(chosen.effort)
        val history = listOf(ChatMessage(AgentTools.context(objective,observations),false))
        if(chosen.provider == "Local") {
            if(vm.loadedModel == null) { vm.notice("Carga un GGUF o configura un proveedor en APIs."); return }
            awaiting = true
            vm.workFeature("Proponiendo un paso local, sin herramientas…") {
                try { accept(id,vm.generateAgentLocal(system,history)) }
                finally { if(id == revision) awaiting = false }
            }
        } else {
            try {
                val payload = TextProtocol.payload(chosen,history,system)
                val ready = vm.features.prepare("PROPONER UN PASO EN NUBE\n${chosen.provider} · ${chosen.model} · esfuerzo ${chosen.effort}\nEndpoint: ${chosen.url()}\n\nComparte el objetivo y fragmentos de hasta dos resultados anteriores, que pueden contener código privado. NO recuerdos, chat ni pantalla. Revisa TODO el JSON abajo antes de confirmar. Puede tener coste; máximo ${ThinkingPolicy.cloudTokens(chosen)} tokens (incluye pensamiento en Gemini) no es un tope monetario. Una petición, sin reintento ni continuación automática.\nSolicitud completa sin clave:\n${payload.toString(2)}") {
                    check(id == revision && active && chosen == vm.skills.config) { "La sesión/configuración cambió." }
                    text.reset()
                    val answer = withContext(Dispatchers.IO) {
                        check(vm.internetAllowed)
                        text.generate(chosen,store.read(chosen.key).orEmpty(),history,system)
                    }
                    check(vm.internetAllowed && id == revision) { "Respuesta descartada por cancelación." }
                    accept(id,answer)
                    "Propuesta recibida; ninguna herramienta ejecutada. Revisa en Agente."
                }
                if(ready) {
                    awaiting = true
                    vm.features.observePending("agent") { result ->
                        if(id == revision) { awaiting = false; if(result.isFailure) events = events + "Petición de modelo cancelada o fallida. Sin reintento." }
                    }
                }
            } catch(e: Exception) { vm.notice(e.message ?: "Configuración inválida.") }
        }
    }
    private fun accept(id: Int, raw: String) {
        if(id != revision || !active) return
        steps++
        try {
            when(val parsed = ToolCallParser.parse(raw)) {
                is ParsedAction.Final -> { final = parsed.text; active = false; proposal = ""; events = events + "Respuesta final del modelo; no prueba ejecución de tareas." }
                is ParsedAction.Call -> { proposal = raw; events = events + "Propuesta $steps/${AgentTools.MAX_STEPS}: ${parsed.name}. NO ejecutada." }
            }
        } catch(e: Exception) {
            proposal = ""; events = events + "Propuesta $steps rechazada: ${e.message ?: "JSON inválido"}. No se ejecutó nada."
        }
        if(steps >= AgentTools.MAX_STEPS && proposal.isEmpty()) active = false
    }
    fun skip() {
        if(vm.busy || awaiting || proposal.isEmpty()) return
        observations = observations + "Propuesta rechazada por el usuario; no se ejecutó."
        proposal = ""; if(steps >= AgentTools.MAX_STEPS) active = false
    }
    fun prepare() {
        if(vm.busy || awaiting || !active || proposal.isEmpty()) return
        try {
            val call = ToolCallParser.parse(proposal) as? ParsedAction.Call ?: error("No es una herramienta.")
            val id = revision
            fun arg(key: String) = call.args.getValue(key)
            // Remove unrelated previews so a failed validator cannot attach to an old action.
            vm.features.dismiss()
            when(call.name) {
                "web_search" -> vm.features.prepare("BUSCAR Brave API\nConsulta exacta: ${arg("query")}\nHasta 8 resultados. Comparte consulta con Brave, puede consumir cuota/dinero. Resultados NO confiables; no se abren ni se envían al modelo solos.") {
                    text.reset()
                    withContext(Dispatchers.IO) { check(vm.internetAllowed); text.search(store.read("brave_key").orEmpty(),arg("query")) }
                        .joinToString("\n") { "${it.title}\n${it.url}\n${it.description}" }.ifBlank { "Brave devolvió cero resultados." }
                }
                "fetch_text" -> vm.features.prepare("LEER WEB PÚBLICA\nURL exacta: ${arg("url")}\nUna petición, sin cookies, claves, redirecciones ni JavaScript. Límite 256 KiB/30 segundos; muestra hasta 12000 caracteres. Contenido NO confiable. No se envía al modelo automáticamente.") {
                    web.reset()
                    withContext(Dispatchers.IO) { check(vm.internetAllowed); web.fetchText(arg("url")) }
                }
                "github_read" -> vm.features.prepare("LEER ARCHIVO GitHub\n${arg("owner")}/${arg("repo")} @ ${arg("branch")}\nRuta: ${arg("path")}\nUsa token cifrado de GitHub si existe; solo lectura. El archivo puede ser privado. Se muestra localmente y no se envía al modelo sin revisar la siguiente petición.") {
                    github.reset()
                    val file = withContext(Dispatchers.IO) { check(vm.internetAllowed); github.read(arg("owner"),arg("repo"),arg("branch"),arg("path"),store.read("github_token").orEmpty()) }
                    "SHA ${file.sha}\n${file.path}\n${file.text}"
                }
                "github_tree" -> vm.features.prepare("LISTAR ARCHIVOS DE GitHub\n${arg("owner")}/${arg("repo")} @ ${arg("branch")}\nSolo lectura, hasta 300 blobs. No descarga contenido, no escribe y no abre apps. El listado puede revelar nombres privados; se muestra localmente.") {
                    github.reset()
                    withContext(Dispatchers.IO) { check(vm.internetAllowed); github.tree(arg("owner"),arg("repo"),arg("branch"),store.read("github_token").orEmpty()) }
                }
                "github_open" -> vm.features.prepare("ABRIR ARCHIVO EN EL PANEL DE GITHUB\n${arg("owner")}/${arg("repo")} @ ${arg("branch")}\nRuta: ${arg("path")}\nSolo lectura: GitHub devuelve el SHA actual y Sakura abre el editor local. Ningún commit se prepara ni se envía sin otra revisión.") {
                    github.reset()
                    val file = withContext(Dispatchers.IO) { check(vm.internetAllowed); github.read(arg("owner"),arg("repo"),arg("branch"),arg("path"),store.read("github_token").orEmpty()) }
                    vm.showRepoFile(file)
                    "Archivo ${file.path} abierto en GitHub (SHA ${file.sha}). Revisa el diff y confirma por separado si decides editarlo."
                }
                "github_issues" -> vm.features.prepare("LISTAR ISSUES ABIERTOS EN GITHUB\n${arg("owner")}/${arg("repo")}\nSolo lectura, primera página (30 issues). Muestra número, estado, título y URL; contenido NO confiable, no se envía al modelo automáticamente.") {
                    github.reset()
                    withContext(Dispatchers.IO) { check(vm.internetAllowed); github.list(arg("owner"),arg("repo"),"main","Issues",store.read("github_token").orEmpty()) }
                }
                "github_write" -> vm.features.prepare("REEMPLAZAR ARCHIVO COMPLETO EN GITHUB (todavía NO se escribe)\n${arg("owner")}/${arg("repo")} @ ${arg("branch")}\nRuta: ${arg("path")}\nCommit: ${arg("message")}\nContenido nuevo (primeros 2000 caracteres):\n${arg("content").take(2000)}\n\nSe leerá el SHA actual y se abrirá ANTES/DESPUÉS completo en el panel GitHub. Solo tú puedes confirmar el commit; usa una rama de trabajo.", true) {
                    github.reset()
                    val file = withContext(Dispatchers.IO) { check(vm.internetAllowed); github.read(arg("owner"),arg("repo"),arg("branch"),arg("path"),store.read("github_token").orEmpty()) }
                    check(vm.showRepoFull(file,arg("content"),arg("message"))) { "El contenido nuevo supera el límite o no cambia el archivo; no se preparó nada." }
                    "Archivo ${file.path} leído (SHA ${file.sha}). Reemplazo completo abierto para revisión; NO se ha escrito nada."
                }
                "github_create" -> vm.features.prepare("CREAR ARCHIVO NUEVO CON COMMIT REAL\n${arg("owner")}/${arg("repo")} @ ${arg("branch")}\nRuta: ${arg("path")}\nCommit: ${arg("message")}\nContenido UTF-8 (≤64 KiB):\n${arg("content").take(6000)}\n\nSi el archivo ya existe, GitHub rechazará la petición: nunca sobrescribe implícitamente. Revisa ruta y contenido antes de confirmar.", true) {
                    github.reset()
                    val sha = withContext(Dispatchers.IO) { check(vm.internetAllowed); github.createFile(arg("owner"),arg("repo"),arg("branch"),arg("path"),arg("content"),arg("message"),store.read("github_token").orEmpty()) }
                    "GitHub confirmó commit de creación $sha en ${arg("path")}. Revisa el resultado en la rama indicada."
                }
                "github_delete" -> vm.features.prepare("BORRAR ARCHIVO CON COMMIT REAL\n${arg("owner")}/${arg("repo")} @ ${arg("branch")}\nRuta: ${arg("path")}\nCommit: ${arg("message")}\n\nAl confirmar se leerá el SHA actual y se enviará un único commit de borrado. No se borran el repositorio ni su historial. Si quieres revisar el contenido antes, pide github_open.", true) {
                    github.reset()
                    val file = withContext(Dispatchers.IO) { check(vm.internetAllowed); github.read(arg("owner"),arg("repo"),arg("branch"),arg("path"),store.read("github_token").orEmpty()) }
                    val sha = withContext(Dispatchers.IO) { github.deleteFile(file,arg("message"),store.read("github_token").orEmpty()) }
                    "GitHub confirmó commit de borrado $sha para ${file.path} (SHA leído ${file.sha}). Historial no alterado."
                }
                "github_branch" -> vm.features.prepare("CREAR RAMA DE TRABAJO EN GITHUB\n${arg("owner")}/${arg("repo")}\nDesde: ${arg("from")}\nNueva: ${arg("branch")}\n\nSolo crea una referencia; no cambia archivos ni mezcla código. Requiere permiso Contents: write.", true) {
                    github.reset()
                    val sourceSha = withContext(Dispatchers.IO) { check(vm.internetAllowed); github.branchSha(arg("owner"),arg("repo"),arg("from"),store.read("github_token").orEmpty()) }
                    withContext(Dispatchers.IO) { github.createBranch(arg("owner"),arg("repo"),arg("branch"),sourceSha,store.read("github_token").orEmpty()) }
                }
                "github_pr" -> vm.features.prepare("CREAR PULL REQUEST EN GITHUB\n${arg("owner")}/${arg("repo")}\n${arg("head")} → ${arg("base")}\nTítulo: ${arg("title")}\nDescripción:\n${arg("body").take(4000)}\n\nNo mezcla ni aprueba automáticamente. Requiere permiso Pull requests: write.", true) {
                    github.reset()
                    withContext(Dispatchers.IO) { check(vm.internetAllowed); github.createPr(arg("owner"),arg("repo"),arg("head"),arg("base"),arg("title"),arg("body"),store.read("github_token").orEmpty()) }
                }
                "github_edit" -> vm.features.prepare("PREPARAR EDICIÓN DE GITHUB (todavía NO se escribe)\n${arg("owner")}/${arg("repo")} @ ${arg("branch")}\nRuta: ${arg("path")}\nFragmento exacto a sustituir:\n${arg("find")}\nReemplazo:\n${arg("replacement")}\nCommit propuesto: ${arg("message")}\n\nSe leerá el SHA actual y se abrirá ANTES/DESPUÉS en el panel. Solo si tú confirmas se enviará el commit real. Usa una rama de trabajo.",true) {
                    github.reset()
                    val file = withContext(Dispatchers.IO) { check(vm.internetAllowed); github.read(arg("owner"),arg("repo"),arg("branch"),arg("path"),store.read("github_token").orEmpty()) }
                    val ready = vm.showRepoEdit(file,arg("find"),arg("replacement"),arg("message"))
                    check(ready) { "El fragmento no aparece una sola vez en el archivo o el cambio no es válido; no se preparó ninguna edición." }
                    "Archivo ${file.path} leído (SHA ${file.sha}). Propuesta abierta en el panel GitHub; revisa ANTES/DESPUÉS. NO se ha escrito nada."
                }
                "model_list" -> vm.features.prepare("MOSTRAR CATÁLOGO LOCAL\nNo descarga ni carga modelos. Revisa licencias y límites.",false) {
                    ModelCatalog.entries.joinToString("\n") { "${it.file} · ${it.title} · ${it.bytes} bytes · ${it.license}" }
                }
                "download_model" -> { val model = ModelCatalog.entries.single { it.file == arg("name") }; vm.features.prepareDownload(model.url,"Modelo",model) }
                "download_url" -> vm.features.prepareDownload(arg("url"),arg("kind"))
                "download_gguf" -> vm.features.prepareDownload(arg("url"),"Modelo",expectedHash=arg("sha256"))
                "image_generate" -> vm.features.prepareCloud("Imagen",arg("prompt"))
                "video_generate" -> vm.features.prepareCloud("Video",arg("prompt"))
                "telegram" -> vm.features.prepareMessage("Telegram",arg("recipient"),arg("text"))
                "whatsapp" -> vm.features.prepareMessage("WhatsApp",arg("recipient"),arg("text"))
                "open_url" -> vm.skills.prepareExternal(arg("url"))
                "youtube_search" -> vm.skills.prepareExternal(Skills.searchUrl("YouTube",arg("query")),"com.google.android.youtube")
                "list_apps" -> vm.features.prepare("LISTAR APPS VISIBLES LOCALMENTE\nSolo actividades de inicio exportadas, máximo 150. Sin QUERY_ALL_PACKAGES. El inventario puede revelar información privada; no se envía al modelo automáticamente. Revisa la siguiente petición si usas nube.",false) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    context.packageManager.queryIntentActivities(intent,android.content.pm.PackageManager.ResolveInfoFlags.of(0))
                        .filter { it.activityInfo.exported }.distinctBy { it.activityInfo.packageName }.take(150)
                        .joinToString("\n") { "${it.loadLabel(context.packageManager)} · ${it.activityInfo.packageName}" }.ifBlank { "Android no muestra apps de inicio." }
                }
                "open_app" -> vm.features.prepare("ABRIR APP EXTERNA\nPaquete exacto: ${arg("package")}\nSolo actividad de inicio visible. Esa app usa sus propios permisos, conexiones y cuenta. No automatiza controles, compras ni mensajes; no comprueba su contenido.",false) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER).setPackage(arg("package"))
                    val entry = context.packageManager.queryIntentActivities(intent,android.content.pm.PackageManager.ResolveInfoFlags.of(0)).firstOrNull { it.activityInfo.exported } ?: error("App no visible o sin actividad de inicio.")
                    context.startActivity(intent.setComponent(android.content.ComponentName(entry.activityInfo.packageName,entry.activityInfo.name)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    "Android aceptó abrir ${arg("package")}; su contenido/resultado NO verificado."
                }
                "open_app_named" -> {
                    val name = arg("name").lowercase()
                    val pkg = ScreenPolicy.apps.getValue(name)
                    vm.features.prepare("ABRIR APP POR NOMBRE CONOCIDO\nNombre: $name\nPaquete resuelto: $pkg\nSolo actividad de inicio visible. Esa app usa sus propios permisos y cuenta; Sakura no automatiza controles, compras ni mensajes y no verifica su contenido.",false) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER).setPackage(pkg)
                        val entry = context.packageManager.queryIntentActivities(intent,android.content.pm.PackageManager.ResolveInfoFlags.of(0)).firstOrNull { it.activityInfo.exported } ?: error("App no visible o sin actividad de inicio.")
                        context.startActivity(intent.setComponent(android.content.ComponentName(entry.activityInfo.packageName,entry.activityInfo.name)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        "Android aceptó abrir $name ($pkg); su contenido/resultado NO verificado. Para buscar, usa video_search o browser_search con otra confirmación."
                    }
                }
                "screen_start" -> vm.features.prepare("INICIAR SESIÓN LIMITADA DE PANTALLA\nApp: ${arg("package")}\nConsulta exacta: ${arg("query")}\nHasta 3 minutos. Solo lee controles accesibles de esa app durante la sesión; sin capturas ni envío al modelo. Escribir/clicar necesita otra confirmación en Pantalla. Sin pagos/mensajes/contraseñas/instalaciones.",false) {
                    val service = SakuraAccessibilityService.connected ?: error("Activa Accesibilidad manualmente en Ajustes.")
                    service.startSession(arg("package"),arg("query")); vm.page = "Pantalla"
                    "Sesión iniciada; ve a Pantalla y revisa cada acción. Ninguna búsqueda/reproducción confirmada."
                }
                "google_search" -> vm.features.prepare("ABRIR BÚSQUEDA DE GOOGLE\nConsulta exacta: ${arg("query")}\nURL exacta: ${Skills.searchUrl("Google",arg("query"))}\nAbre navegador externo, sin extraer resultados ni enviarlos al modelo. La app externa usa su propia red y cuenta; Sakura no verifica los resultados.",false) {
                    val url = Skills.searchUrl("Google",arg("query"))
                    withContext(Dispatchers.Main) {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    "Android aceptó abrir la URL; resultados NO extraídos ni enviados al modelo."
                }
                "video_search" -> vm.skills.prepareExternal(Skills.searchUrl("YouTube",arg("query")),"com.google.android.youtube")
                "browser_search" -> vm.skills.prepareExternal(Skills.searchUrl("Web",arg("query")))
                "screen_read" -> vm.features.prepare("LEER TEXTO VISIBLE DE LA PANTALLA AUTORIZADA\nRequiere sesión activa y la app autorizada. Hasta 200 nodos, profundidad 12 y 4000 caracteres; excluye contraseñas, campos sensibles y etiquetas bloqueadas. NUNCA se envía al modelo ni a la red automáticamente. Se muestra en el panel y en Agente; revisa antes de compartir.",false) {
                    val service = SakuraAccessibilityService.connected ?: error("Activa el servicio y una sesión manualmente en Pantalla.")
                    val visible = service.readVisibleText()
                    if(visible.isBlank()) "No se encontró texto visible permitido en la app autorizada." else "Texto visible mostrado solo localmente:\n$visible"
                }
                "screen_type" -> vm.features.prepare("ESCRIBIR TEXTO EN LA PANTALLA AUTORIZADA\nTexto exacto a revisar:\n${arg("text")}\nRequiere sesión activa en la app autorizada. Al confirmar aquí solo se prepara el texto: el panel muestra los campos editables y exige pulsar «Confirmar escribir texto preparado». Sin Enter/IME automático; máximo 500 caracteres.",false) {
                    val service = SakuraAccessibilityService.connected ?: error("Activa el servicio y una sesión manualmente en Pantalla.")
                    service.prepareTypeText(arg("text"))
                }
                "file_list" -> vm.features.prepare("LISTAR ARCHIVOS DE LA CARPETA ELEGIDA (SAF)\nCarpeta: ${store.read(PHONE_FOLDER_KEY) ?: "sin elegir"}\nHasta 100 entradas (nombre y tamaño) de esa carpeta, sin permisos amplios de almacenamiento. No se abre ni se envía contenido a ningún servicio. Los resultados se muestran localmente y solo se reenvían si revisas la siguiente petición.",false) {
                    val raw = store.read(PHONE_FOLDER_KEY) ?: error("Elige una carpeta en la página Archivos antes de usar esta herramienta.")
                    withContext(Dispatchers.IO) { PhoneFiles.list(context,android.net.Uri.parse(raw)) }
                        .joinToString("\n") { "${it.name} · ${it.size} bytes" }
                        .ifBlank { "La carpeta elegida no muestra archivos." }
                }
                "file_read" -> {
                    val name = arg("name")
                    require(PhoneFiles.validName(name)) { "Nombre de archivo inválido." }
                    vm.features.prepare("LEER ARCHIVO DE LA CARPETA ELEGIDA (SAF)\nCarpeta: ${store.read(PHONE_FOLDER_KEY) ?: "sin elegir"}\nNombre exacto: $name\nHasta 64 KiB, UTF-8 estricto; se rechazan binarios y bytes nulos. Solo se muestra localmente; no se envía a la red ni al modelo sin que revises la siguiente petición.",false) {
                        val raw = store.read(PHONE_FOLDER_KEY) ?: error("Elige una carpeta en la página Archivos antes de usar esta herramienta.")
                        withContext(Dispatchers.IO) { PhoneFiles.readText(context,android.net.Uri.parse(raw),name) }
                    }
                }
                "apk_open_selected" -> {
                    val selected = vm.selectedApkUri ?: error("No hay APK seleccionada. Abre Herramientas y analiza una APK primero.")
                    vm.features.prepare("ABRIR APK SELECCIONADA\nURI exacta: $selected\nAndroid pedirá su propia confirmación para ver/instalar. Sakura NO verifica seguridad, firma ni compatibilidad; NO es instalación silenciosa.",false) {
                        withContext(Dispatchers.Main) {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW)
                                .setDataAndType(selected,"application/vnd.android.package-archive")
                                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        "Android aceptó abrir la APK; Sakura no ha instalado ni verificado nada."
                    }
                }
            }
            val attached = vm.features.observePending("agent") { result ->
                if(id == revision) {
                    val observation = result.getOrElse { "Resultado NO confirmado: ${it.message}. Revisa el servicio antes de repetir." }
                    val shared = if(call.name == "screen_read" && result.isSuccess)
                        "Texto visible revisado solo en pantalla; NO incluido en el contexto del modelo." else observation.take(1000)
                    observations = observations + "${call.name}: $shared"
                    events = events + "${call.name}: ${observation.take(12000)}"
                    proposal = ""; awaiting = false
                    if(steps >= AgentTools.MAX_STEPS) active = false
                    // No next() here: the user must explicitly request and review another step.
                }
            }
            awaiting = attached
        } catch(e: Exception) { vm.notice(e.message ?: "Propuesta inválida; nada ejecutado.") }
    }
    fun stop() {
        val wasAwaiting = awaiting
        revision++; active = false; awaiting = false; proposal = ""
        vm.features.dismissOwned("agent")
        if(wasAwaiting) { text.cancel(); web.cancel(); github.cancel(); vm.cancelGeneration(); vm.features.cancelDownload() }
        SakuraAccessibilityService.connected?.stopSession()
        events = events + "Sesión detenida. Una petición externa ya enviada puede haberse cobrado/aplicado; no se reintenta."
    }
    fun close() { revision++; text.cancel(); web.cancel(); github.cancel() }
}