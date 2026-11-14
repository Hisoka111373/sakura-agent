package com.sakura.agent

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit

/** Minimal HTML-to-text conversion, testable and bounded. */
object WebText {
    fun plain(html: String): String {
        var text = html
            .replace(Regex("(?is)<script.*?</script>"), " ")
            .replace(Regex("(?is)<style.*?</style>"), " ")
            .replace(Regex("(?is)<!--.*?-->"), " ")
            .replace(Regex("(?is)<br\\s*/?>"), "\n")
            .replace(Regex("(?is)</(p|div|li|h[1-6]|tr|section|article)>"), "\n")
            .replace(Regex("(?s)<[^>]+>"), " ")
        text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
            .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        return text.lines().map { it.replace(Regex("[ \\t\\u000B\\f\\r]+"), " ").trim() }
            .filter { it.isNotEmpty() }.joinToString("\n").take(12_000)
    }
}

/** Reads public HTTPS text pages with pinned DNS, no redirects to private hosts and a 256 KiB cap. */
class WebClient {
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).proxy(Proxy.NO_PROXY).dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = InetAddress.getAllByName(hostname).toList()
                require(addresses.isNotEmpty() && addresses.all { NetworkPolicy.publicAddress(it.address) }) { "Destino no público." }
                return addresses
            }
        }).connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    @Volatile private var active: okhttp3.Call? = null
    fun cancel() { active?.cancel() }

    fun fetchText(url: String): String {
        var uri: URI = NetworkPolicy.publicDownload(url)
        var redirects = 0
        while (true) {
            val call = http.newCall(Request.Builder().url(uri.toString())
                .header("Accept", "text/html,text/plain,application/json,application/xml")
                .header("User-Agent", "Sakura-Agent-Android").header("Accept-Encoding", "identity").build())
            active = call
            val response = try { call.execute() } finally { active = null }
            try {
                if (response.code in listOf(301, 302, 303, 307, 308)) {
                    require(redirects++ < 3) { "Demasiadas redirecciones." }
                    val location = response.header("Location") ?: error("Redirección sin destino.")
                    uri = NetworkPolicy.publicDownload(uri.resolve(location).toString())
                    continue
                }
                require(response.code in 200..299) { "HTTP ${response.code}. No se pudo leer la página." }
                val type = response.header("Content-Type").orEmpty().lowercase()
                require(type.startsWith("text/") || "json" in type || "xml" in type) { "El destino no es texto legible." }
                val body = response.body ?: error("Respuesta sin contenido.")
                require(body.contentLength() <= 262_144) { "La página supera 256 KiB." }
                val bytes = body.byteStream().readNBytes(262_145)
                require(bytes.size <= 262_144) { "La página supera 256 KiB." }
                val text = WebText.plain(bytes.toString(Charsets.UTF_8))
                require(text.isNotBlank()) { "La página no contiene texto legible." }
                return text
            } finally { response.close() }
        }
    }
}
