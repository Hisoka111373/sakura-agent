package com.sakura.agent

import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** One public HTTPS request: DNS bound to connection, no cookies/auth/redirects/retry. */
class WebClient {
    private val cancelled = AtomicBoolean(false)
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).proxy(Proxy.NO_PROXY).dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = InetAddress.getAllByName(hostname).toList()
                require(addresses.isNotEmpty() && addresses.all { NetworkPolicy.publicAddress(it.address) }) { "Destino no público." }
                return addresses
            }
        }).connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).build()
    @Volatile private var active: okhttp3.Call? = null
    fun reset() { cancelled.set(false) }
    fun cancel() { cancelled.set(true); active?.cancel() }
    fun fetchText(url: String): String {
        NetworkPolicy.https(url)
        check(!cancelled.get()) { "Lectura cancelada." }
        val call = http.newCall(Request.Builder().url(url).header("Accept","text/html,text/plain,application/json,application/xml")
            .header("Accept-Encoding","identity").header("User-Agent","Sakura-Agent-Android").build())
        active = call
        if(cancelled.get()) call.cancel()
        try {
            return call.execute().use { response ->
                require(response.code in 200..299) { "HTTP ${response.code}; no se siguen redirecciones. Prepara la URL final manualmente." }
                val type = response.header("Content-Type").orEmpty().lowercase()
                require(type.startsWith("text/") || "json" in type || "xml" in type) { "El destino no es texto." }
                val body = response.body ?: error("Sin contenido.")
                require(body.contentLength() <= 262144) { "Página supera 256 KiB." }
                val source = body.source()
                require(!source.request(262145)) { "Página supera 256 KiB." }
                check(!cancelled.get()) { "Lectura cancelada." }
                WebText.plain(source.readUtf8()).also { require(it.isNotBlank()) { "Sin texto legible." } }
            }
        } catch(e: java.io.IOException) { error("Lectura fallida o cancelada. No se reintentó automáticamente.") }
        finally { active = null }
    }
}