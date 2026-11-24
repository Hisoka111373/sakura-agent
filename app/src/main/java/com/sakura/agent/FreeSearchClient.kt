package com.sakura.agent

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.InetAddress
import java.net.Proxy
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class FreeResult(val title: String, val url: String, val snippet: String)

/** Key-free web search used from the chat: Wikipedia first, DuckDuckGo Lite as fallback. One request each. */
class FreeSearchClient {
    private val cancelled = AtomicBoolean(false)
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).proxy(Proxy.NO_PROXY).dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = InetAddress.getAllByName(hostname).toList()
                require(addresses.isNotEmpty() && addresses.all { NetworkPolicy.publicAddress(it.address) }) { "Destino no público." }
                return addresses
            }
        }).connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build()
    @Volatile private var active: okhttp3.Call? = null

    fun reset() { cancelled.set(false) }
    fun cancel() { cancelled.set(true); active?.cancel() }

    fun search(query: String): List<FreeResult> {
        require(NetworkPolicy.text(query, 300))
        val wikipedia = try { wikipedia(query) } catch (_: Exception) { emptyList() }
        if (wikipedia.isNotEmpty()) return wikipedia
        return duckDuckGo(query)
    }

    private fun get(url: String): String {
        NetworkPolicy.https(url)
        check(!cancelled.get()) { "Búsqueda cancelada." }
        val call = http.newCall(Request.Builder().url(url).header("Accept", "application/json,text/html")
            .header("Accept-Encoding", "identity").header("User-Agent", "Sakura-Agent-Android").build())
        active = call
        if (cancelled.get()) call.cancel()
        try {
            return call.execute().use { response ->
                require(response.code in 200..299) { "HTTP ${response.code}" }
                val body = response.body ?: error("Sin respuesta.")
                val source = body.source()
                require(!source.request(262145)) { "Respuesta demasiado grande." }
                check(!cancelled.get()) { "Búsqueda cancelada." }
                source.readUtf8()
            }
        } finally { active = null }
    }

    private fun wikipedia(query: String): List<FreeResult> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val raw = get("https://es.wikipedia.org/w/api.php?action=opensearch&search=$q&limit=6&namespace=0&format=json")
        val array = JSONArray(raw)
        if (array.length() < 4) return emptyList()
        val titles = array.getJSONArray(1); val snippets = array.getJSONArray(2); val urls = array.getJSONArray(3)
        return (0 until minOf(titles.length(), urls.length())).map { i ->
            FreeResult(titles.optString(i), urls.optString(i), snippets.optString(i).ifBlank { "Resultado de Wikipedia en español." })
        }.filter { NetworkPolicy.https(it.url).host.endsWith("wikipedia.org") }
    }

    private fun duckDuckGo(query: String): List<FreeResult> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val html = get("https://lite.duckduckgo.com/lite/?q=$q")
        val links = Regex("""<a[^>]+class="result-link"[^>]+href="([^"]+)"[^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
        val snippets = Regex("""<td[^>]+class="result-snippet"[^>]*>(.*?)</td>""", RegexOption.IGNORE_CASE)
        val snippetList = snippets.findAll(html).map { plain(it.groupValues[1]) }.toList()
        return links.findAll(html).mapIndexed { index, match ->
            val target = decodeTarget(match.groupValues[1])
            FreeResult(plain(match.groupValues[2]), target, snippetList.getOrNull(index).orEmpty().take(400))
        }.filter { runCatching { NetworkPolicy.https(it.url) }.isSuccess }.take(6).toList()
    }

    private fun decodeTarget(raw: String): String {
        val value = raw.replace("&amp;", "&")
        val encoded = Regex("[?&]uddg=([^&]+)").find(value)?.groupValues?.get(1) ?: return value
        return runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(value)
    }

    private fun plain(value: String) = WebText.plain(value).replace("\n", " ").take(240)
}
