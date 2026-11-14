package com.sakura.agent

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.net.Proxy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class MediaItem(val videoId: String, val title: String, val uploader: String, val duration: Int, val url: String)
data class StreamOption(val kind: String, val quality: String, val mime: String, val url: String, val combined: Boolean)

/** Pure parsing of Piped responses, testable on the JVM. */
object MediaProtocol {
    fun videoId(value: String): String? {
        val raw = value.substringAfter("/watch?", value)
        if (!raw.contains("v=")) return null
        val id = raw.substringAfter("v=").substringBefore('&').substringBefore('#').substringBefore('?')
        return id.takeIf { it.matches(Regex("[A-Za-z0-9_-]{6,20}")) }
    }

    fun items(json: JSONObject): List<MediaItem> {
        val rows = json.optJSONArray("items") ?: return emptyList()
        return (0 until minOf(rows.length(), 12)).mapNotNull { index ->
            try {
                val row = rows.getJSONObject(index)
                val id = videoId(row.optString("url")) ?: return@mapNotNull null
                val title = row.optString("title").take(200)
                if (title.isBlank()) return@mapNotNull null
                MediaItem(id, title, row.optString("uploaderName").take(120),
                    row.optInt("duration", 0).coerceAtLeast(0), "https://www.youtube.com/watch?v=$id")
            } catch (_: Exception) { null }
        }.distinctBy { it.videoId }
    }

    fun streams(json: JSONObject): List<StreamOption> {
        val audio = options(json.optJSONArray("audioStreams"), "Audio")
            .sortedWith(compareByDescending<StreamOption> { it.mime.contains("mp4") }.thenByDescending { it.quality.toIntOrNull() ?: 0 })
            .take(3)
        val video = options(json.optJSONArray("videoStreams"), "Video")
            .sortedWith(compareByDescending<StreamOption> { it.combined }
                .thenByDescending { it.quality.substringBefore('p').toIntOrNull() ?: 0 })
            .take(4)
        return (audio + video).distinctBy { it.url }
    }

    private fun options(array: org.json.JSONArray?, kind: String): List<StreamOption> {
        if (array == null) return emptyList()
        return (0 until minOf(array.length(), 40)).mapNotNull { index ->
            try {
                val row = array.getJSONObject(index)
                val url = row.optString("url")
                if (url.isBlank()) return@mapNotNull null
                NetworkPolicy.https(url)
                val videoOnly = row.optBoolean("videoOnly", kind == "Video")
                val quality = when (kind) {
                    "Audio" -> (row.optInt("bitrate", 0) / 1000).toString()
                    else -> row.optString("quality").ifBlank { "desconocida" }
                }
                StreamOption(kind, quality, row.optString("mimeType").take(80), url,
                    combined = kind == "Video" && !videoOnly)
            } catch (_: Exception) { null }
        }
    }
}

/** Fixed Piped instances, pinned public DNS, no redirects, bounded responses. */
class MediaClient {
    private val instances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.private.coffee",
        "https://pipedapi.leptons.xyz"
    )
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).proxy(Proxy.NO_PROXY).dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = InetAddress.getAllByName(hostname).toList()
                require(addresses.isNotEmpty() && addresses.all { NetworkPolicy.publicAddress(it.address) }) { "Destino no público." }
                return addresses
            }
        }).connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS).build()
    @Volatile private var active: okhttp3.Call? = null
    private val stopped = java.util.concurrent.atomic.AtomicBoolean(false)
    fun cancel() { stopped.set(true); active?.cancel() }

    fun search(query: String): List<MediaItem> {
        require(NetworkPolicy.text(query, 500)) { "Consulta requerida." }
        val path = "/search?q=${URLEncoder.encode(query, "UTF-8")}&filter=videos"
        val json = get(path)
        return MediaProtocol.items(json).also { if (it.isEmpty()) error("Piped no devolvió videos para esa búsqueda.") }
    }

    fun streams(videoId: String): List<StreamOption> {
        require(videoId.matches(Regex("[A-Za-z0-9_-]{6,20}"))) { "ID de video inválido." }
        val json = get("/streams/$videoId")
        val options = MediaProtocol.streams(json)
        require(options.isNotEmpty()) { "Piped no devolvió streams directos; puede requerir PO token o estar limitado." }
        return options
    }

    private fun get(path: String): JSONObject {
        stopped.set(false)
        var last: Exception? = null
        for (base in instances) {
            if (stopped.get()) throw java.io.IOException("Consulta de Piped cancelada.")
            try {
                val url = base + path
                NetworkPolicy.https(url)
                require(NetworkPolicy.https(url).host in instances.map { NetworkPolicy.https(it).host }) { "Host no permitido." }
                val call = http.newCall(Request.Builder().url(url)
                    .header("Accept", "application/json").header("User-Agent", "Sakura-Agent-Android").build())
                active = call
                call.execute().use { response ->
                    require(response.code in 200..299) { "Piped HTTP ${response.code}." }
                    val source = response.body?.source() ?: error("Sin respuesta.")
                    require(!source.request(1_048_577)) { "Respuesta supera 1 MiB." }
                    return JSONObject(source.readUtf8())
                }
            } catch (e: Exception) {
                last = e
                if (stopped.get()) throw java.io.IOException("Consulta de Piped cancelada.")
            } finally { active = null }
        }
        throw java.io.IOException("Ninguna instancia de Piped respondió (${last?.javaClass?.simpleName ?: "sin detalle"}). No se reintenta automáticamente.")
    }
}
