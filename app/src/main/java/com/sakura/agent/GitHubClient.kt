package com.sakura.agent

import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

data class RepoFile(val owner: String, val repo: String, val branch: String,
    val path: String, val sha: String, val text: String)
data class PendingEdit(val source: RepoFile, val replacement: String, val message: String)

/** HTTPS API only; no remote code execution. A read never calls PUT. */
class GitHubClient {
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    private fun base(owner: String, repo: String): String {
        require(AgentPolicy.validRepo(owner, repo)) { "Propietario o repositorio inválido." }
        return "/repos/${encode(owner)}/${encode(repo)}"
    }
    private fun ref(branch: String): String {
        require(branch.isNotBlank() && branch.length <= 200 && branch.none { it.code < 32 }) { "Rama inválida." }
        return encode(branch)
    }

    fun tree(owner: String, repo: String, branch: String, token: String): String {
        val result = request("GET", base(owner, repo) + "/git/trees/${ref(branch)}?recursive=1", token)
        val array = result.getJSONArray("tree")
        val lines = (0 until array.length()).asSequence().map { array.getJSONObject(it) }
            .filter { it.optString("type") == "blob" }.take(300).map { it.getString("path") }.toList()
        return "Archivos de $owner/$repo @ $branch (máximo 300):\n" + lines.joinToString("\n") +
            if (result.optBoolean("truncated")) "\nGitHub truncó el árbol; no es un listado completo." else ""
    }

    fun read(owner: String, repo: String, branch: String, path: String, token: String): RepoFile {
        require(AgentPolicy.validPath(path)) { "Ruta inválida." }
        val url = base(owner, repo) + "/contents/" + path.split('/').joinToString("/") { encode(it) } + "?ref=${ref(branch)}"
        val result = request("GET", url, token)
        require(result.optString("type") == "file" && result.optString("encoding") == "base64") { "Selecciona un archivo de texto, no una carpeta." }
        require(result.getLong("size") <= AgentPolicy.MAX_SOURCE_BYTES) { "El archivo supera 64 KiB." }
        val bytes = Base64.decode(result.getString("content"), Base64.DEFAULT)
        require(bytes.size <= AgentPolicy.MAX_SOURCE_BYTES && !bytes.contains(0.toByte())) { "Archivo binario o demasiado grande." }
        val decoded = bytes.toString(Charsets.UTF_8)
        require(decoded.toByteArray(Charsets.UTF_8).contentEquals(bytes)) { "El archivo no es texto UTF-8 válido. No se editará para evitar dañarlo." }
        return RepoFile(owner, repo, branch, path, result.getString("sha"), decoded)
    }

    /** Called exclusively after the user confirms the exact PendingEdit in the UI. */
    fun writeConfirmed(edit: PendingEdit, token: String): String {
        require(token.isNotBlank()) { "Se necesita un token de GitHub con permiso Contents: write." }
        require(edit.message.isNotBlank() && edit.message.length <= 200) { "Mensaje de commit inválido." }
        require(edit.replacement.toByteArray(Charsets.UTF_8).size <= AgentPolicy.MAX_SOURCE_BYTES) { "El cambio supera 64 KiB." }
        require(edit.replacement != edit.source.text) { "No hay cambios." }
        val source = edit.source
        require(AgentPolicy.validPath(source.path)) { "Ruta inválida." }
        ref(source.branch)
        val url = base(source.owner, source.repo) + "/contents/" + source.path.split('/').joinToString("/") { encode(it) }
        val body = JSONObject().put("message", edit.message).put("branch", source.branch)
            .put("sha", source.sha).put("content", Base64.encodeToString(edit.replacement.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
        val response = request("PUT", url, token, body.toString())
        return response.getJSONObject("commit").getString("sha")
    }

    private fun request(method: String, path: String, token: String, body: String? = null): JSONObject {
        require(token.none { it.code < 32 }) { "Token inválido." }
        val connection = URI("https://api.github.com$path").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false // never forward credentials to a redirect host
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "Sakura-Agent-Android")
            if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            require(code in 200..299) {
                when (code) {
                    401 -> "GitHub: token inválido o expirado (401)."
                    403 -> "GitHub: permisos insuficientes o límite de consultas (403)."
                    404 -> "GitHub: archivo, rama o repositorio no encontrado (404)."
                    409, 422 -> "GitHub: conflicto o cambio rechazado. Vuelve a leer el archivo antes de editar ($code)."
                    in 300..399 -> "GitHub redirigió la consulta. Actualiza el nombre del repositorio."
                    else -> "GitHub devolvió HTTP $code."
                }
            }
            val result = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    require(output.size() + size <= 2 * 1024 * 1024) { "Respuesta de GitHub demasiado grande." }
                    output.write(buffer, 0, size)
                }
                output.toString("UTF-8")
            }
            return JSONObject(result)
        } finally { connection.disconnect() }
    }
}
