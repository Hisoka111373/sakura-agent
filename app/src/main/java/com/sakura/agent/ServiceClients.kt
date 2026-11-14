package com.sakura.agent

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/** Fixed hosts, no redirects, bounded responses, sanitized failures (Telegram token is in URL). */
object ServiceHttp {
    fun json(method: String, url: String, host: String, authorization: String = "", body: JSONObject? = null): JSONObject {
        val uri = NetworkPolicy.https(url)
        require(uri.host == host && host in setOf("api.telegram.org", "graph.facebook.com", "queue.fal.run"))
        require(authorization.none { it.code < 32 })
        val connection = uri.toURL().openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = method
            connection.connectTimeout = 15000; connection.readTimeout = 30000
            connection.setRequestProperty("Accept", "application/json")
            if (authorization.isNotEmpty()) connection.setRequestProperty("Authorization", authorization)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            require(code in 200..299) { "Servicio HTTP $code. Comprueba permisos, cuota y configuración; no se reintenta automáticamente." }
            val bytes = connection.inputStream.use { it.readNBytes(1024 * 1024 + 1) }
            require(bytes.size <= 1024 * 1024) { "Respuesta del servicio demasiado grande." }
            return if (bytes.isEmpty()) JSONObject() else JSONObject(bytes.toString(Charsets.UTF_8))
        } catch (_: IOException) {
            error("Fallo de conexión. El servicio puede haber recibido la petición: revisa su historial antes de repetir.")
        } finally { connection.disconnect() }
    }
}

object MessageClient {
    fun telegram(token: String, chat: String, text: String): String {
        require(token.matches(Regex("[0-9]+:[A-Za-z0-9_-]+"))) { "Configura el token del bot de Telegram." }
        require(NetworkPolicy.chat(chat) && NetworkPolicy.text(text,4096)) { "Chat ID o mensaje inválido." }
        val r = ServiceHttp.json("POST", "https://api.telegram.org/bot$token/sendMessage", "api.telegram.org",
            body = JSONObject().put("chat_id",chat).put("text",text))
        require(r.optBoolean("ok")) { "Telegram rechazó el mensaje." }
        val result = r.getJSONObject("result")
        return "Telegram confirmó message_id=${result.getLong("message_id")} en chat ${result.getJSONObject("chat").getLong("id")}. No prueba lectura por el destinatario."
    }
    fun whatsapp(token: String, phoneId: String, recipient: String, text: String): String {
        require(token.isNotBlank() && NetworkPolicy.secret(token) && phoneId.matches(Regex("[0-9]{1,30}"))) { "Configura token Meta y Phone Number ID (no el teléfono)." }
        require(NetworkPolicy.phone(recipient) && NetworkPolicy.text(text,4096)) { "Número internacional sin + o mensaje inválido." }
        val body = JSONObject().put("messaging_product","whatsapp").put("to",recipient).put("type","text")
            .put("text",JSONObject().put("preview_url",false).put("body",text))
        val r = ServiceHttp.json("POST", "https://graph.facebook.com/v23.0/$phoneId/messages", "graph.facebook.com", "Bearer $token", body)
        return "Meta aceptó el mensaje: ${r.getJSONArray("messages").getJSONObject(0).getString("id")}. Entrega/lectura NO verificadas; requieren webhooks."
    }
}

data class CloudJob(val id: String, val kind: String, val statusUrl: String, val resultUrl: String, val cancelUrl: String)
data class CloudResult(val status: String, val urls: List<String>)

object CloudClient {
    val endpoints = mapOf("Imagen" to "fal-ai/flux/schnell", "Video" to "fal-ai/ltx-video")
    private fun auth(key: String): String {
        require(key.isNotBlank() && NetworkPolicy.secret(key)) { "Configura tu clave de fal.ai en Privacidad." }
        return "Key $key"
    }
    private fun queueUrl(url: String): String {
        val uri = NetworkPolicy.https(url)
        require(uri.host == "queue.fal.run" && uri.path.startsWith("/fal-ai/") && uri.query == null) { "URL de trabajo no válida." }
        return uri.toString()
    }
    fun submit(key: String, kind: String, prompt: String): CloudJob {
        val model = endpoints[kind] ?: error("Tipo de generación inválido.")
        require(NetworkPolicy.text(prompt,2000)) { "Prompt requerido, hasta 2000 caracteres." }
        val input = JSONObject().put("prompt",prompt).put("enable_safety_checker",true)
        if (kind == "Imagen") input.put("num_images",1).put("image_size","square").put("num_inference_steps",4)
        val r = ServiceHttp.json("POST", "https://queue.fal.run/$model", "queue.fal.run", auth(key), input)
        return CloudJob(r.getString("request_id"),kind,queueUrl(r.getString("status_url")),
            queueUrl(r.getString("response_url")),queueUrl(r.getString("cancel_url")))
    }
    fun restore(kind: String, id: String): CloudJob {
        require(kind in endpoints && id.matches(Regex("[A-Za-z0-9-]{1,100}"))) { "ID o tipo inválido." }
        val base = endpoints.getValue(kind).split('/').take(2).joinToString("/")
        val url = "https://queue.fal.run/$base/requests/$id"
        return CloudJob(id,kind,"$url/status",url,"$url/cancel")
    }
    fun check(key: String, job: CloudJob): CloudResult {
        val authorization = auth(key)
        val status = ServiceHttp.json("GET",queueUrl(job.statusUrl),"queue.fal.run",authorization).getString("status")
        if (status != "COMPLETED") return CloudResult(status,emptyList())
        val r = ServiceHttp.json("GET",queueUrl(job.resultUrl),"queue.fal.run",authorization)
        val urls = if (job.kind == "Imagen") {
            val array = r.getJSONArray("images")
            (0 until minOf(array.length(),4)).map { array.getJSONObject(it).getString("url") }
        } else listOf(r.getJSONObject("video").getString("url"))
        urls.forEach { NetworkPolicy.https(it) }
        return CloudResult("COMPLETED (resultado recibido)",urls)
    }
    fun cancel(key: String, job: CloudJob): String = ServiceHttp.json("PUT",queueUrl(job.cancelUrl),"queue.fal.run",auth(key)).optString("status","Respuesta recibida")
}