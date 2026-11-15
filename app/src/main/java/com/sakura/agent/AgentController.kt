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
        val history = listOf(ChatMessage(AgentTools.context(objective,observations),false))
        if(chosen.provider == "Local") {
            if(vm.loadedModel == null) { vm.notice("Carga un GGUF o configura un proveedor en APIs."); return }
            awaiting = true
            vm.workFeature("Proponiendo un paso local, sin herramientas…") {
                try { accept(id,vm.generateAgentLocal(AgentTools.prompt,history)) }
                finally { if(id == revision) awaiting = false }
            }
        } else {
            try {
                val payload = TextProtocol.payload(chosen,history,AgentTools.prompt)
                val ready = vm.features.prepare("PROPONER UN PASO EN NUBE\n${chosen.provider} · ${chosen.model}\nEndpoint: ${chosen.url()}\n\nComparte el objetivo y fragmentos de hasta dos resultados anteriores, que pueden contener código privado. NO recuerdos, chat ni pantalla. Revisa TODO el JSON abajo antes de confirmar. Puede tener coste; máximo 768 tokens no es un tope monetario. Una petición, sin reintento ni continuación automática.\nSolicitud completa sin clave:\n${payload.toString(2)}") {
                    check(id == revision && active && chosen == vm.skills.config) { "La sesión/configuración cambió." }
                    text.reset()
                    val answer = withContext(Dispatchers.IO) {
                        check(vm.internetAllowed)
                        text.generate(chosen,store.read(chosen.key).orEmpty(),history,AgentTools.prompt)
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
                "screen_start" -> vm.features.prepare("INICIAR SESIÓN LIMITADA DE PANTALLA\nApp: ${arg("package")}\nConsulta exacta: ${arg("query")}\nHasta 3 minutos. Solo lee controles accesibles de esa app durante la sesión; sin capturas ni envío al modelo. Escribir/clicar necesita otra confirmación en Pantalla. Sin pagos/mensajes/contraseñas/instalaciones.",false) {
                    val service = SakuraAccessibilityService.connected ?: error("Activa Accesibilidad manualmente en Ajustes.")
                    service.startSession(arg("package"),arg("query")); vm.page = "Pantalla"
                    "Sesión iniciada; ve a Pantalla y revisa cada acción. Ninguna búsqueda/reproducción confirmada."
                }
            }
            val attached = vm.features.observePending("agent") { result ->
                if(id == revision) {
                    val observation = result.getOrElse { "Resultado NO confirmado: ${it.message}. Revisa el servicio antes de repetir." }
                    observations = observations + "${call.name}: ${observation.take(1000)}"
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