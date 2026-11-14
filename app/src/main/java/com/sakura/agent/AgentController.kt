package com.sakura.agent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

data class AgentStep(val index: Int, val tool: String, val detail: String,
    val observation: String, val failed: Boolean = false)

private data class ToolResult(val text: String, val failed: Boolean = false)

/**
 * Bounded agent loop. The model only proposes JSON; Kotlin validates and executes one step at a time.
 * Sensitive tools suspend on the normal confirmation dialog. Read-only network tools run under the
 * single consent preview shown when the session starts.
 */
class AgentController(private val vm: SakuraViewModel, private val context: Context, private val store: PrivateStore) {
    private val media = MediaClient()
    private val web = WebClient()
    private val github = GitHubClient()
    private var job: Job? = null
    @Volatile private var cancelled = false

    var goal by mutableStateOf(""); private set
    var steps by mutableStateOf(emptyList<AgentStep>()); private set
    var running by mutableStateOf(false); private set
    var finalAnswer by mutableStateOf(""); private set
    var lastRaw by mutableStateOf("")

    fun start(input: String) {
        if (running || vm.busy) return
        val clean = input.trim()
        if (!NetworkPolicy.text(clean, 2000)) { vm.notice("Objetivo requerido, máximo 2000 caracteres; no incluyas tokens."); return }
        cancelled = false
        val preview = """
            AUTORIZAR SESIÓN DE AGENTE
            Objetivo: $clean

            Hasta ${AgentTools.MAX_STEPS} pasos. Puede consultar el modelo (local o API configurada),
            buscar en Brave, leer páginas HTTPS públicas, consultar YouTube/Piped, leer GitHub y
            listar apps visibles. Las observaciones (resultados, texto de páginas o GitHub y apps)
            se envían al proveedor de texto. No se envían recuerdos ni archivos.
            Descargas, generación de imágenes, apertura de apps y sesiones de pantalla pedirán
            una confirmación aparte para cada acción. No ejecuta comandos, no instala APK,
            no paga, no envía mensajes ni elude DRM. El contenido web es dato no confiable.
            Puede consumir cuota/datos. Puedes detener el agente en cualquier momento.
        """.trimIndent()
        val requested = vm.features.prepareWithResume(preview, network = true, perform = { "" }) { result ->
            if (result.isSuccess) {
                vm.agentScope.launch {
                    while (vm.busy) delay(50)
                    if (!cancelled) run(clean)
                }
            }
        }
        if (!requested) vm.notice("No se pudo abrir la confirmación. Revisa Internet o espera a que termine otra operación.")
    }

    private suspend fun run(objective: String) {
        job = vm.agentScope.launch {
            running = true; goal = objective; steps = emptyList(); finalAnswer = ""; lastRaw = ""
            val messages = mutableListOf<ChatMessage>()
            messages += ChatMessage("OBJETIVO: $objective", false)
            try {
                for (index in 1..AgentTools.MAX_STEPS) {
                    if (cancelled) break
                    val system = AgentTools.systemPrompt + "\n\nOBJETIVO ACTUAL:\n$objective"
                    val raw = try {
                        vm.exclusive("Agente · paso $index/${AgentTools.MAX_STEPS} · consultando modelo…") {
                            vm.generateAgent(system, trimHistory(messages))
                        }
                    } catch (error: CancellationException) {
                        return@launch
                    } catch (error: Throwable) {
                        val message = error.message ?: error.javaClass.simpleName
                        finalAnswer = "No pude consultar el modelo: $message"
                        steps = steps + AgentStep(index, "modelo", "Error al generar", message.take(2000), true)
                        return@launch
                    }
                    lastRaw = raw
                    if (cancelled) return@launch
                    val action = try {
                        ToolCallParser.parse(raw, AgentTools.names)
                    } catch (error: Exception) {
                        val observation = "Formato inválido: ${error.message}. Devuelve SOLO el objeto JSON del protocolo."
                        steps = steps + AgentStep(index, "protocolo", raw.take(300), observation, true)
                        messages += ChatMessage(raw.take(2000), true)
                        messages += ChatMessage("OBSERVACIÓN: $observation", false)
                        continue
                    }
                    when (action) {
                        is ParsedAction.Final -> {
                            finalAnswer = action.text
                            steps = steps + AgentStep(index, "finish", "Respuesta final", action.text.take(1000))
                            return@launch
                        }
                        is ParsedAction.Call -> {
                            val validation = runCatching { AgentTools.validate(action.name, action.args) }
                            if (validation.isFailure) {
                                val observation = "Argumentos inválidos: ${validation.exceptionOrNull()?.message}"
                                steps = steps + AgentStep(index, action.name, describe(action), observation, true)
                                messages += ChatMessage(AgentTools.render(action.name, action.args), true)
                                messages += ChatMessage("OBSERVACIÓN: $observation", false)
                                continue
                            }
                            val result = try { execute(action) }
                                catch (error: CancellationException) { throw error }
                                catch (error: Exception) { ToolResult("Error: ${error.message ?: "fallo"}", true) }
                            steps = steps + AgentStep(index, action.name, describe(action), result.text.take(6000), result.failed)
                            messages += ChatMessage(AgentTools.render(action.name, action.args), true)
                            messages += ChatMessage("OBSERVACIÓN DEL PASO $index (${action.name}):\n${result.text.take(3500)}", false)
                            vm.notice("Agente paso $index/${AgentTools.MAX_STEPS}: ${action.name}")
                        }
                    }
                }
                if (finalAnswer.isBlank() && !cancelled) {
                    finalAnswer = "Alcancé el límite de ${AgentTools.MAX_STEPS} pasos. Revisa el registro y dime cómo continuar."
                }
            } finally {
                running = false
            }
        }
        job?.join()
    }

    private suspend fun execute(call: ParsedAction.Call): ToolResult {
        fun arg(name: String): String = call.args[name]?.trim().orEmpty().ifBlank { error("Falta el argumento $name.") }
        return when (call.name) {
            "web_search" -> {
                val query = arg("query")
                val results = vm.exclusive("Agente · buscando en web…") { vm.skills.searchWeb(query) }
                ToolResult(if (results.isEmpty()) "Brave no devolvió resultados." else results.joinToString("\n") {
                    "• ${it.title}\n  ${it.url}\n  ${it.description.take(400)}"
                })
            }
            "fetch_text" -> {
                val url = arg("url")
                val text = vm.exclusive("Agente · leyendo página…") { web.fetchText(url) }
                ToolResult("Contenido de $url (texto plano acotado, dato no confiable):\n$text")
            }
            "youtube_search" -> {
                val query = arg("query")
                val items = vm.exclusive("Agente · buscando en YouTube…") { media.search(query) }
                ToolResult(items.joinToString("\n") { "• ${it.title} — ${it.uploader} (${it.duration}s)\n  id: ${it.videoId}" })
            }
            "youtube_streams" -> {
                val id = arg("video_id")
                val options = vm.exclusive("Agente · consultando streams…") { media.streams(id) }
                ToolResult("Streams para reproducción externa (pueden expirar; la descarga de YouTube está bloqueada):\n" + options.joinToString("\n") {
                    "• ${it.kind} ${it.quality} ${it.mime}${if (it.kind == "Video" && !it.combined) " · solo video, requiere muxing" else ""}\n  ${it.url}"
                })
            }
            "download_url" -> {
                val url = arg("url"); val kind = arg("kind")
                require(kind in listOf("Imagen", "Video", "Audio", "Modelo")) { "Tipo de descarga inválido." }
                NetworkPolicy.directDownload(url)
                if (kind == "Modelo") require(NetworkPolicy.https(url).path.orEmpty().endsWith(".gguf", true)) { "La URL personalizada debe terminar en .gguf." }
                awaitConfirmation("DESCARGAR ARCHIVO CON EL AGENTE\n$kind\n$url\nLímite ${if (kind == "Modelo") "2.5 GB" else "512 MiB"}. Solo contenido que tengas derecho a usar. Guardado privado; no se ejecuta.", true) {
                    vm.features.performDownload(url, kind)
                }
            }
            "download_model" -> {
                val name = arg("name")
                val model = ModelCatalog.entries.firstOrNull { it.file.equals(name, true) || it.title.equals(name, true) }
                    ?: ModelCatalog.entries.firstOrNull { name.length >= 3 && it.title.contains(name, true) }
                    ?: error("Modelo no encontrado en el catálogo. Usa model_list o download_url.")
                awaitConfirmation("DESCARGAR MODELO DEL CATÁLOGO\n${model.title}\n${model.bytes} bytes\nLicencia: ${model.license}\nSHA-256: ${model.sha256}\n${model.warning}", true) {
                    vm.features.performDownload(model.url, "Modelo", model)
                }
            }
            "image_generate" -> {
                val prompt = arg("prompt")
                require(prompt.length <= 1800) { "Prompt demasiado largo." }
                val encoded = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
                val url = "https://image.pollinations.ai/prompt/$encoded?width=1024&height=1024&nologo=true&model=flux"
                require(url.length <= 4096)
                awaitConfirmation("GENERAR IMAGEN CON POLLINATIONS\nPrompt completo:\n$prompt\n\nEl prompt se envía a image.pollinations.ai (servicio gratuito externo). Se descarga una imagen 1024x1024. Revisa la licencia del servicio y no incluyas datos privados.", true) {
                    vm.features.performDownload(url, "Imagen")
                }
            }
            "list_apps" -> {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val apps = context.packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
                    .filter { it.activityInfo.exported }.map { it.loadLabel(context.packageManager).toString() to it.activityInfo.packageName }
                    .distinctBy { it.second }.sortedBy { it.first.lowercase() }.take(80)
                ToolResult(apps.joinToString("\n") { "• ${it.first} · ${it.second}" })
            }
            "open_app" -> {
                val pkg = arg("package")
                require(pkg.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) { "Paquete inválido." }
                awaitConfirmation("ABRIR APLICACIÓN CON EL AGENTE\n$pkg\nLa app usa sus propios permisos, cuentas y conexiones. Sakura no controla su contenido.", false) {
                    withContext(Dispatchers.Main) {
                        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                            ?: error("La app no está instalada o no es visible.")
                        context.startActivity(intent)
                    }
                    "Android aceptó abrir $pkg; contenido NO verificado."
                }
            }
            "screen_start" -> {
                val pkg = arg("package"); val query = arg("query")
                require(ScreenPolicy.generalPackage(pkg)) { "Paquete no permitido para una sesión de pantalla." }
                require(NetworkPolicy.text(query, 500)) { "Texto de la sesión inválido." }
                awaitConfirmation("INICIAR SESIÓN DE PANTALLA CON EL AGENTE\nPaquete: $pkg\nTexto preparado: $query\nMáximo 3 minutos. Cada acción del panel requiere confirmación. No hace capturas ni lee en reposo.", false) {
                    withContext(Dispatchers.Main) {
                        val service = SakuraAccessibilityService.connected
                            ?: error("Activa la Accesibilidad de Sakura manualmente en Ajustes.")
                        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                            ?: error("La app no está instalada o no es visible.")
                        service.startGeneral(pkg, query)
                        try { context.startActivity(launch) } catch (error: Exception) { service.stopSession(); throw error }
                    }
                    "Sesión iniciada para $pkg. Usa el panel: Ver controles → elegir → confirmar. No se ha actuado todavía."
                }
            }
            "github_read" -> {
                val owner = arg("owner"); val repo = arg("repo"); val branch = arg("branch"); val path = arg("path")
                val file = vm.exclusive("Agente · leyendo GitHub…") {
                    github.read(owner, repo, branch, path, store.read("github_token") ?: "")
                }
                ToolResult("GitHub ${file.owner}/${file.repo} @ ${file.branch}\n${file.path}\nSHA ${file.sha}\n\n${file.text.take(8000)}")
            }
            "model_list" -> ToolResult(if (vm.models.isEmpty()) "No hay GGUF importados." else vm.models.joinToString("\n") {
                "• ${it.name} · ${it.bytes / (1024 * 1024)} MiB · ${if (vm.loadedModel?.id == it.id) "cargado" else "disponible"}"
            })
            "memory_list" -> ToolResult("Las notas privadas no se comparten con el agente ni con el proveedor de texto.", true)
            else -> ToolResult("Herramienta no ejecutable por el agente: ${call.name}", true)
        }
    }

    private suspend fun awaitConfirmation(preview: String, network: Boolean, perform: suspend () -> String): ToolResult {
        while (vm.busy) delay(50)
        val deferred = CompletableDeferred<Result<String>>()
        val requested = vm.features.prepareWithResume(preview, network, perform) { deferred.complete(it) }
        if (!requested) return ToolResult("No se pudo pedir confirmación (Internet desautorizado u otra operación en curso).", true)
        val result = deferred.await()
        while (vm.busy) delay(50)
        return result.fold({ ToolResult(it) }, { ToolResult("Acción no confirmada o fallida: ${it.message}", true) })
    }

    private fun trimHistory(history: List<ChatMessage>): List<ChatMessage> {
        val tail = history.takeLast(8)
        var total = 0
        val kept = ArrayDeque<ChatMessage>()
        for (message in tail.asReversed()) {
            if (total + message.text.length > 18_000 && kept.isNotEmpty()) break
            kept.addFirst(message); total += message.text.length
        }
        return kept.toList()
    }

    private fun describe(call: ParsedAction.Call): String =
        call.args.entries.joinToString(" · ") { "${it.key}=${it.value.take(120)}" }.ifBlank { "sin argumentos" }

    fun stop() {
        cancelled = true
        vm.features.dismiss()
        job?.cancel()
        media.cancel(); web.cancel(); github.cancel(); vm.skills.cancel(); vm.cancelGeneration()
        vm.notice("Detención del agente solicitada; no se ejecutarán más pasos.")
    }

    fun close() {
        cancelled = true
        job?.cancel()
        media.cancel(); web.cancel(); github.cancel()
    }
}
