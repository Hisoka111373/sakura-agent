package com.sakura.agent

/** Deterministic chat routing: turns natural language into a supervised, confirmable tool proposal. */
sealed class ChatAction {
    data class Search(val query: String, val video: Boolean = false) : ChatAction()
    data class GithubRead(val owner: String, val repo: String, val path: String, val branch: String = "main") : ChatAction()
    data class GithubEdit(val owner: String, val repo: String, val path: String, val find: String,
                          val replacement: String, val message: String, val branch: String = "main") : ChatAction()
    data class DownloadModel(val file: String) : ChatAction()
    data class OpenApp(val alias: String) : ChatAction()
    data class MemoryNote(val text: String) : ChatAction()
}

object ChatRouter {
    // Detection uses accent-free lowercase text; extracted values keep the user's original case
    // because GitHub paths, commit messages and search queries are case-sensitive.
    private val searchCue = Regex("""(?:^|\b)(busca|buscar|buscame|busqueda|investiga|noticias de|que es|como se hace|busca en youtube|video de)\b""", RegexOption.IGNORE_CASE)
    private val searchPrefix = Regex("""^(?:buscame|buscar|busca|b[uú]squeda(?:\s+de)?|investiga|noticias de|que es|qu[eé] es|c[oó]mo se hace|video de)\s+""", RegexOption.IGNORE_CASE)
    private val repoPath = Regex("""(?<path>[\w./-]+\.[A-Za-z0-9]{1,6})\s+(?:de|en|del repo(?:sitorio)?)\s+(?<owner>[A-Za-z0-9_.-]+)/(?<repo>[A-Za-z0-9_.-]+)(?:\s+(?:@|en rama)\s+(?<branch>[\w./-]+))?""", RegexOption.IGNORE_CASE)
    private val editCue = Regex("""(?:cambia|reemplaza|modifica|sustituye)\s+(?:el texto\s+)?[«"']?(?<find>[^"']{2,300})[»"']?\s+(?:por|a|con)\s+[«"']?(?<replacement>[^"']{1,300})[»"']?\s+en\s+(?<rest>.+)""", RegexOption.IGNORE_CASE)
    private val downloadCue = Regex("""(?:descarga|baja|instala modelo)\s+(?:el modelo\s+)?(?<name>[\w.-]+(?:\s+[\w.-]+){0,3})""", RegexOption.IGNORE_CASE)
    private val readCue = Regex("""(?:lee|muestra|ver|abre|muéstrame|muestrame)\s+(?:el\s+)?(?:archivo\s+|código\s+)?""", RegexOption.IGNORE_CASE)
    private val noteCue = Regex("""(?:^|\b)(recuerda|guarda nota|anota)\s+(.+)""", RegexOption.IGNORE_CASE)
    private val notePrefix = Regex("""(?:^|\b)(recuerda|guarda nota|anota)\s+""", RegexOption.IGNORE_CASE)

    fun detect(input: String): ChatAction? {
        val original = input.trim()
        val text = Skills.normalized(original)
        if (text.isBlank() || original.length > 900) return null

        editCue.find(original)?.let { match ->
            val rest = match.groups["rest"]?.value.orEmpty()
            repoPath.find(rest)?.let { target ->
                val find = match.groups["find"]!!.value.trim()
                val replacement = match.groups["replacement"]!!.value.trim()
                return ChatAction.GithubEdit(
                    target.groups["owner"]!!.value, target.groups["repo"]!!.value, target.groups["path"]!!.value,
                    find, replacement,
                    "Sakura: ${find.take(40)} → ${replacement.take(40)}",
                    target.groups["branch"]?.value ?: "main")
            }
        }

        if (readIntent(text)) {
            repoPath.find(original)?.let { target ->
                return ChatAction.GithubRead(
                    target.groups["owner"]!!.value, target.groups["repo"]!!.value, target.groups["path"]!!.value,
                    target.groups["branch"]?.value ?: "main")
            }
        }

        downloadCue.find(text)?.let { match ->
            val wanted = match.groups["name"]!!.value.trim().lowercase().split(Regex("""\s+"""))
                .filter { it.length > 1 }.take(4)
            if (wanted.isNotEmpty()) {
                ModelCatalog.entries.firstOrNull { model ->
                    val haystack = "${model.title} ${model.file} ${model.category} ${model.repo}".lowercase()
                    wanted.all { it in haystack }
                }?.let { return ChatAction.DownloadModel(it.file) }
            }
        }

        val app = ScreenPolicy.apps.entries.firstOrNull { (alias, _) ->
            Regex("""(?:^|\b)(abre|abrir|inicia|lanza)\s+${Regex.escape(alias)}(?:\b|$)""", RegexOption.IGNORE_CASE).containsMatchIn(original)
        }
        if (app != null) return ChatAction.OpenApp(app.key)

        if (noteCue.containsMatchIn(text)) {
            val note = notePrefix.replace(original, "").trim()
            if (note.isNotBlank()) return ChatAction.MemoryNote(note.take(500))
        }

        if (searchCue.containsMatchIn(text)) {
            val query = searchPrefix.replace(original, "").trim().take(300)
            if (query.isNotBlank()) return ChatAction.Search(query, Regex("youtube|video", RegexOption.IGNORE_CASE).containsMatchIn(text))
        }
        return null
    }

    private fun readIntent(text: String): Boolean =
        readCue.containsMatchIn(text) || Regex("""\bREADME\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)

    fun summary(action: ChatAction): String = when (action) {
        is ChatAction.Search -> "buscar «${action.query.take(120)}»"
        is ChatAction.GithubRead -> "leer ${action.owner}/${action.repo}:${action.path}"
        is ChatAction.GithubEdit -> "editar ${action.owner}/${action.repo}:${action.path}"
        is ChatAction.DownloadModel -> "descargar ${action.file}"
        is ChatAction.OpenApp -> "abrir ${action.alias}"
        is ChatAction.MemoryNote -> "guardar nota"
    }
}
