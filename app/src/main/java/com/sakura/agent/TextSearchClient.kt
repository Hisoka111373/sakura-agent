package com.sakura.agent

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.InetAddress
import java.net.Proxy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Public DNS pinned to the actual connection, no redirects/proxy/retries or response error-body leaks. */
class TextSearchClient {
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).proxy(Proxy.NO_PROXY).dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses=InetAddress.getAllByName(hostname).toList()
                require(addresses.isNotEmpty() && addresses.all { NetworkPolicy.publicAddress(it.address) }) { "Destino no público." }
                return addresses
            }
        }).connectTimeout(15,TimeUnit.SECONDS).readTimeout(90,TimeUnit.SECONDS).callTimeout(100,TimeUnit.SECONDS).build()
    @Volatile private var active: okhttp3.Call? = null
    fun cancel() { active?.cancel() }
    private fun request(url: String, headers: Map<String,String>, body: JSONObject? = null): JSONObject {
        NetworkPolicy.https(url)
        val builder=Request.Builder().url(url).header("Accept","application/json")
        headers.forEach { (k,v) -> builder.header(k,v) }
        if(body!=null) builder.post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        val call=http.newCall(builder.build()); active=call
        try {
            return call.execute().use { response ->
                require(response.code in 200..299) { "HTTP ${response.code}. Resultado no confirmado; sin reintento automático." }
                val source=response.body?.source() ?: error("Sin respuesta.")
                require(!source.request(1_048_577)) { "Respuesta supera 1 MiB." }
                JSONObject(source.readUtf8())
            }
        } catch(e:Exception) {
            // Never include server bodies, request URLs, headers or keys in error output.
            throw java.io.IOException(if(e is IllegalArgumentException) e.message?.take(160) else "Fallo de red/JSON o cancelación; resultado no confirmado. Revisa consumo antes de repetir.")
        } finally { active=null }
    }
    fun generate(config: TextConfig, key: String, history: List<ChatMessage>): String =
        generate(config, key, history, TextProtocol.system, 768)

    fun generate(config: TextConfig, key: String, history: List<ChatMessage>, system: String, maxTokens: Int): String {
        require(config.provider in TextProtocol.providers && config.provider != "Local") { "Proveedor de texto inválido." }
        require(key.isNotBlank() && NetworkPolicy.secret(key)) { "Configura la clave del proveedor en Privacidad." }
        val headers=when(config.provider) {
            "Gemini" -> mapOf("x-goog-api-key" to key)
            "Anthropic" -> mapOf("x-api-key" to key,"anthropic-version" to "2023-06-01")
            else -> mapOf("Authorization" to "Bearer $key")
        }
        return TextProtocol.answer(config.provider,request(config.url(),headers,
            TextProtocol.payload(config,history,system,maxTokens,maxHistory=21)))
    }
    fun search(key: String, query: String): List<SearchResult> {
        require(key.isNotBlank() && NetworkPolicy.secret(key)) { "Configura brave_key en Privacidad." }
        require(NetworkPolicy.text(query,500))
        return SearchProtocol.results(request("https://api.search.brave.com/res/v1/web/search?q=${URLEncoder.encode(query,"UTF-8")}&count=8&safesearch=moderate",
            mapOf("X-Subscription-Token" to key)))
    }
}