package com.sakura.agent

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class FeatureConfirmation(val preview: String, val network: Boolean, val perform: suspend () -> String,
    val planIndex: Int? = null, val resume: ((Result<String>) -> Unit)? = null)

/** Shares Sakura's serialized UI work gate. A model proposal has no executable callback. */
class FeatureController(private val vm: SakuraViewModel, private val context: Context, private val store: PrivateStore) {
    private val downloads = DownloadClient()
    private val github = GitHubClient()
    var pending by mutableStateOf<FeatureConfirmation?>(null); private set
    var report by mutableStateOf(""); private set
    var downloading by mutableStateOf(false); private set
    var transfer by mutableStateOf(""); private set
    var files by mutableStateOf(emptyList<DownloadedFile>()); private set
    var configured by mutableStateOf(emptySet<String>()); private set
    var job by mutableStateOf<CloudJob?>(null); private set
    var mediaUrls by mutableStateOf(emptyList<String>()); private set
    var planJson by mutableStateOf("")
    var plan by mutableStateOf(emptyList<PlanStep>()); private set
    var evidence by mutableStateOf(emptyMap<Int,String>()); private set
    private var validatedPlan = ""

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            listOf(downloads.dir(context),LocalFiles.modelDir(context)).forEach { directory ->
                directory.listFiles().orEmpty().filter { it.isFile && it.extension == "part" }.forEach { it.delete() }
            }
            files = downloads.files(context)
            configured = NetworkPolicy.credentialKeys.filter { !store.read(it).isNullOrBlank() }.toSet()
            store.read("fal_job")?.let {
                try { val saved = JSONObject(it); job = CloudClient.restore(saved.getString("kind"),saved.getString("id")) }
                catch (_: Exception) { report = "Trabajo nube guardado inválido; usa recuperación manual por ID." }
            }
        }
    }
    fun revokeNetwork() { cancelPending("Red revocada antes de confirmar.") }
    fun close() { downloads.cancel(); cancelPending("Sakura se cerró antes de confirmar.") }
    fun dismiss() { cancelPending("Acción cancelada por la persona.") }
    private fun cancelPending(reason: String) {
        val old = pending; pending = null
        old?.resume?.let { callback -> runCatching { callback(Result.failure(IllegalStateException(reason))) } }
    }
    private fun replacePending(next: FeatureConfirmation) {
        cancelPending("Sustituida por otra acción.")
        pending = next
    }
    internal fun prepare(preview: String, network: Boolean = true, perform: suspend () -> String): Boolean {
        if (vm.busy) return false
        if (network && !vm.internetAllowed) { vm.notice("Autoriza Internet en Privacidad antes de preparar esta acción."); return false }
        replacePending(FeatureConfirmation(preview,network,perform))
        return true
    }
    internal fun prepareWithResume(preview: String, network: Boolean = true,
        perform: suspend () -> String, resume: (Result<String>) -> Unit): Boolean {
        if (vm.busy) return false
        if (network && !vm.internetAllowed) { vm.notice("Autoriza Internet en Privacidad antes de preparar esta acción."); return false }
        replacePending(FeatureConfirmation(preview,network,perform,resume=resume))
        return true
    }
    private fun validate(block: () -> Unit) {
        try { block() } catch (e: Exception) { vm.notice(e.message ?: "Datos inválidos.") }
    }
    fun confirm() {
        val action = pending ?: return
        if (vm.busy) return
        pending = null // single use, including on errors/timeouts
        vm.workFeature("Ejecutando únicamente la acción confirmada…") {
            check(!action.network || vm.internetAllowed) { "Red desautorizada." }
            report = ""
            try {
                report = action.perform()
                action.planIndex?.let { evidence = evidence + (it to report) }
                vm.notice(report.take(220))
                action.resume?.let { callback -> runCatching { callback(Result.success(report)) } }
            } catch (e: Exception) {
                report = "Resultado NO confirmado. ${e.message ?: "Fallo de operación."} Revisa el servicio antes de repetir una escritura."
                action.resume?.let { callback -> runCatching { callback(Result.failure(e)) } }
                throw e
            }
        }
    }

    fun saveCredential(key: String, input: String) = vm.workFeature("Guardando configuración cifrada…") {
        require(key in NetworkPolicy.credentialKeys)
        val value = input.trim()
        require(NetworkPolicy.secret(value)) { "Valor inválido." }
        withContext(Dispatchers.IO) { store.write(key,value.ifBlank { null }) }
        configured = if (value.isEmpty()) configured - key else configured + key
        vm.notice("Configuración guardada localmente. No se ha probado ni conectado el servicio.")
    }

    fun prepareDownload(url: String, kind: String, model: CatalogModel? = null) = validate {
        NetworkPolicy.directDownload(url)
        require(kind in listOf("Imagen","Video","Audio","Modelo") && (kind != "Modelo" || model == null || model in ModelCatalog.entries))
        if (kind == "Modelo" && model == null) {
            require(NetworkPolicy.https(url).path.orEmpty().endsWith(".gguf", true)) { "La URL personalizada debe terminar en .gguf." }
        }
        val details = when {
            model != null -> "${model.title}\n${model.bytes} bytes\nLicencia: ${model.license}\n${model.warning}\nSHA-256 esperado: ${model.sha256}"
            kind == "Modelo" -> "GGUF por URL personalizada · límite 2.5 GB.\nSin SHA-256 fijado: se comprueba cabecera GGUF, tamaño y espacio. Origen, licencia y calidad NO verificados. Usa solo pesos que tengas derecho a descargar."
            else -> "$kind · límite 512 MiB. URL directa, no página web, YouTube, HLS, DRM ni sesión privada."
        }
        prepare("DESCARGAR por Internet:\n$url\n$details\nSe permiten hasta 5 redirecciones a HTTPS público sin cookies ni tokens. Guardado privado, no ejecutado. Puede consumir datos y varios GB de espacio. Confirma que tienes derecho a descargarlo.") {
            performDownload(url,kind,model)
        }
    }

    internal suspend fun performDownload(url: String, kind: String, model: CatalogModel? = null): String {
        downloads.reset(); downloading = true; transfer = "Iniciando descarga…"
        try {
            val result = withContext(Dispatchers.IO) {
                downloads.download(context,url,kind,model) { done,total ->
                    transfer = "${done / (1024*1024)} MiB / ${if (total >= 0) "${total / (1024*1024)} MiB" else "tamaño desconocido"}"
                }
            }
            withContext(Dispatchers.IO) { files = downloads.files(context) }
            vm.refreshModels()
            val chatModel = kind == "Modelo" && (model == null || model.category == "Chat")
            val destination = when {
                chatModel -> "Importado en Modelos; cargar requiere otra confirmación."
                kind == "Modelo" -> "Checkpoint visual guardado en Descargas; no se ha ejecutado."
                else -> "Guardado en Descargas. No se ha ejecutado ni validado su seguridad."
            }
            return "Archivo guardado: ${result.file.name.substringAfter('_')}\n${result.file.length()} bytes\nSHA-256: ${result.sha256}\n$destination"
        } finally { downloading = false }
    }
    fun cancelDownload() { downloads.cancel(); vm.notice("Cancelación de descarga solicitada.") }
    fun prepareDelete(file: DownloadedFile) = prepare("Borrar ${file.name} del almacenamiento privado de Sakura. No se puede deshacer.",false) {
        withContext(Dispatchers.IO) {
            require(files.any { it.id == file.id })
            check(File(downloads.dir(context),file.id).delete()) { "No se pudo borrar." }
            files = downloads.files(context)
        }
        "Archivo privado borrado."
    }
    fun export(file: DownloadedFile, uri: android.net.Uri) = vm.workFeature("Exportando archivo seleccionado…") {
        withContext(Dispatchers.IO) {
            require(files.any { it.id == file.id })
            val source = File(downloads.dir(context),file.id)
            context.contentResolver.openOutputStream(uri,"wt")?.use { output -> source.inputStream().use { it.copyTo(output,65536) } }
                ?: error("Android no permitió exportar.")
        }
        vm.notice("Copia exportada al destino que elegiste. El archivo privado se conserva.")
    }

    fun prepareMessage(service: String, recipient: String, text: String) = validate {
        require(NetworkPolicy.text(text,4096)) { "Mensaje requerido, máximo 4096 caracteres." }
        require(if (service == "Telegram") NetworkPolicy.chat(recipient) else service == "WhatsApp" && NetworkPolicy.phone(recipient)) { "Destinatario inválido." }
        val warning = if (service == "Telegram") "Se envía como BOT, no desde tu cuenta personal. El destinatario debe haber iniciado el bot o permitirlo en su grupo." else "Se envía desde WhatsApp Business configurado. Solo texto en ventana de servicio de 24 h; fuera de ella se necesitan plantillas (no implementadas). Revisa consentimiento y posibles costes de Meta."
        prepare("ENVIAR MENSAJE REAL por $service\nDestinatario: $recipient\nTexto completo:\n$text\n\n$warning\nNo hay reintentos automáticos. Esto comparte el texto con el proveedor.") {
            withContext(Dispatchers.IO) {
                if (service == "Telegram") MessageClient.telegram(store.read("telegram_token") ?: "",recipient,text)
                else MessageClient.whatsapp(store.read("whatsapp_token") ?: "",store.read("whatsapp_phone_id") ?: "",recipient,text)
            }
        }
    }
    fun prepareCloud(kind: String, prompt: String) = validate {
        require(kind in CloudClient.endpoints && NetworkPolicy.text(prompt,2000)) { "Tipo o prompt inválido." }
        prepare("GENERAR $kind EN NUBE · fal.ai\nModelo: ${CloudClient.endpoints[kind]}\nPrompt completo:\n$prompt\n\nUna petición, facturada según la tarifa vigente del proveedor; Sakura NO impone un tope monetario. Revisa tarifa/saldo en fal.ai antes de confirmar. La generación puede consumir créditos aunque canceles. No se envían recuerdos ni chat. Las URLs del resultado pueden ser públicas. LTX 0.9.1: revisa su licencia de investigación. Sin garantía de rapidez.") {
            val created = withContext(Dispatchers.IO) { CloudClient.submit(store.read("fal_key") ?: "",kind,prompt) }
            job = created; mediaUrls = emptyList()
            var warning = ""
            try { withContext(Dispatchers.IO) { store.write("fal_job",JSONObject().put("id",created.id).put("kind",created.kind).toString()) } }
            catch (_: Exception) { warning = "\nNo se pudo persistir el ID; cópialo para recuperar el trabajo." }
            "fal.ai aceptó request_id=${created.id}. NO significa que ya generó el archivo. Pulsa Consultar para verificar sin reenviar.$warning"
        }
    }
    fun recoverCloud(kind: String, id: String) = validate {
        if (!vm.busy) { job = CloudClient.restore(kind,id.trim()); mediaUrls = emptyList(); vm.notice("ID preparado para consulta; no se ha enviado una generación.") }
    }
    fun checkCloud() = vm.workFeature("Consultando trabajo existente, sin generar otro…") {
        require(vm.internetAllowed)
        val existing = job ?: error("No hay trabajo para consultar.")
        val result = withContext(Dispatchers.IO) { CloudClient.check(store.read("fal_key") ?: "",existing) }
        mediaUrls = result.urls
        report = "Trabajo ${existing.id}: ${result.status}\n"+result.urls.joinToString("\n")
        vm.notice(result.status)
    }
    fun prepareCancelCloud() {
        val existing = job ?: return
        prepare("Solicitar cancelación en fal.ai del trabajo ${existing.id}. Si ya corre, puede completarse y cobrarse igualmente.") {
            val response = withContext(Dispatchers.IO) { CloudClient.cancel(store.read("fal_key") ?: "",existing) }
            "Cancelación: $response. No prueba reembolso ni interrupción si el trabajo ya estaba en marcha."
        }
    }

    fun listGithub(owner: String, repo: String, branch: String, kind: String) = vm.workFeature("Consultando $kind en GitHub…") {
        require(vm.internetAllowed)
        report = withContext(Dispatchers.IO) { github.list(owner.trim(),repo.trim(),branch.trim(),kind,store.read("github_token") ?: "") }
        vm.notice("Consulta recibida. Ningún cambio escrito.")
    }
    fun prepareBranch(owner: String, repo: String, source: String, newBranch: String) = vm.workFeature("Leyendo SHA de la rama base para revisar…") {
        require(vm.internetAllowed && NetworkPolicy.branch(newBranch))
        val sha = withContext(Dispatchers.IO) { github.branchSha(owner,repo,source,store.read("github_token") ?: "") }
        // Work gate is still busy here; set this read-only preview explicitly.
        pending = FeatureConfirmation("CREAR RAMA en $owner/$repo\nNueva: $newBranch\nBase: $source\nSHA exacto: $sha\nNo cambia ni borra la rama base.",true, perform = {
            withContext(Dispatchers.IO) { github.createBranch(owner,repo,newBranch,sha,store.read("github_token") ?: "") }
        })
        vm.notice("SHA leído; revisa y confirma la creación de rama.")
    }
    fun prepareCreateFile(owner: String, repo: String, branch: String, path: String, text: String, message: String) = validate {
        require(AgentPolicy.validRepo(owner,repo) && NetworkPolicy.branch(branch) && AgentPolicy.validPath(path) &&
            text.toByteArray(Charsets.UTF_8).size <= AgentPolicy.MAX_SOURCE_BYTES && NetworkPolicy.text(message,200)) { "Datos inválidos." }
        prepare("CREAR ARCHIVO y commit REAL\n$owner/$repo @ $branch\nRuta: $path\nCommit: $message\nContenido completo:\n$text\n\nSi el archivo existe, la API rechazará la operación. No sobrescribe implícitamente.") {
            val sha = withContext(Dispatchers.IO) { github.createFile(owner,repo,branch,path,text,message,store.read("github_token") ?: "") }
            "GitHub confirmó commit de creación: $sha"
        }
    }
    fun prepareDeleteFile(source: RepoFile, message: String) = validate {
        require(NetworkPolicy.text(message,200))
        prepare("BORRAR ARCHIVO con commit REAL\n${source.owner}/${source.repo} @ ${source.branch}\n${source.path}\nSHA leído: ${source.sha}\nCommit: $message\nContenido que desaparecerá:\n${source.text}\nNo se borran el repositorio ni su historial. Usa una rama de trabajo.") {
            try {
                val sha = withContext(Dispatchers.IO) { github.deleteFile(source,message,store.read("github_token") ?: "") }
                "GitHub confirmó commit de borrado: $sha"
            } finally { vm.invalidateRepo() }
        }
    }
    fun preparePr(owner: String, repo: String, head: String, base: String, title: String, body: String) = validate {
        require(AgentPolicy.validRepo(owner,repo) && NetworkPolicy.branch(head) && NetworkPolicy.branch(base) && head != base &&
            NetworkPolicy.text(title,200) && body.length <= 10000)
        prepare("CREAR PULL REQUEST\n$owner/$repo\n$head → $base\nTítulo: $title\nDescripción completa:\n$body\nNo mezcla código ni omite protecciones. Permiso Pull requests: write requerido.") {
            withContext(Dispatchers.IO) { github.createPr(owner,repo,head,base,title,body,store.read("github_token") ?: "") }
        }
    }

    fun receivePlan(text: String) {
        planJson = text
        try { plan = AgentPlanner.parse(text); validatedPlan = text; evidence = emptyMap(); pending = null; vm.notice("Plan propuesto, NO ejecutado. Revisa cada paso.") }
        catch (_: Exception) { plan = emptyList(); validatedPlan = ""; pending = null; vm.notice("Plan JSON inválido o herramienta no permitida. Edítalo o pide uno más corto; no se ejecutó nada.") }
    }
    fun prepareStep(index: Int) = validate {
        if (vm.busy) return@validate
        require(planJson == validatedPlan) { "El JSON cambió. Valida los cambios antes de preparar un paso." }
        val step = plan[index]
        fun arg(key: String) = step.args[key] ?: error("Falta $key; completa el dato en el plan y vuelve a validarlo.")
        when (step.action) {
            "DOWNLOAD" -> prepareDownload(arg("url"),arg("kind"))
            "TELEGRAM" -> prepareMessage("Telegram",arg("recipient"),arg("text"))
            "WHATSAPP" -> prepareMessage("WhatsApp",arg("recipient"),arg("text"))
            "CLOUD" -> prepareCloud(arg("kind"),arg("prompt"))
            "GITHUB_READ" -> prepare("LEER GitHub: ${arg("owner")}/${arg("repo")} @ ${arg("branch")}\nArchivo: ${arg("path")}\nNo se escribirá ni enviará el contenido al modelo.") {
                val file = withContext(Dispatchers.IO) { github.read(arg("owner"),arg("repo"),arg("branch"),arg("path"),store.read("github_token") ?: "") }
                "GitHub devolvió SHA ${file.sha}\n${file.path}\n${file.text}"
            }
            else -> { vm.draft = step.detail; vm.page = "Chat"; vm.notice("Paso de revisión llevado al borrador. No se ha enviado al modelo.") }
        }
        pending = pending?.copy(planIndex = index)
    }
}