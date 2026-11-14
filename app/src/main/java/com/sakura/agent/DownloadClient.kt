package com.sakura.agent

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.Proxy
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request

data class DownloadedFile(val id: String, val name: String, val bytes: Long)
data class DownloadResult(val file: File, val sha256: String)

/** Streams to private storage, verifies limits/hash, then atomically publishes. No cookies/tokens. */
class DownloadClient {
    private val cancelled = AtomicBoolean(false)
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .proxy(Proxy.NO_PROXY).dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val addresses = InetAddress.getAllByName(hostname).toList()
                require(addresses.isNotEmpty() && addresses.all { NetworkPolicy.publicAddress(it.address) }) { "El destino no tiene solo direcciones públicas." }
                return addresses // Exact validated IPs, not a second DNS resolution.
            }
        }).connectTimeout(15,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS)
        .callTimeout(45,TimeUnit.MINUTES).build()
    @Volatile private var active: Call? = null
    fun cancel() { cancelled.set(true); active?.cancel() }
    fun reset() { cancelled.set(false) }
    fun dir(context: Context) = File(context.noBackupFilesDir, "downloads").apply { mkdirs() }
    fun files(context: Context) = dir(context).listFiles().orEmpty().filter { it.isFile && it.extension != "part" }
        .map { DownloadedFile(it.name, it.name.substringAfter('_'), it.length()) }.sortedBy { it.name }

    fun download(context: Context, url: String, kind: String, model: CatalogModel? = null,
        progress: (Long, Long) -> Unit): DownloadResult {
        require(kind in listOf("Imagen", "Video", "Modelo"))
        require(kind != "Modelo" || model != null && url == model.url) { "Los modelos se descargan desde el catálogo verificado." }
        val targetDir = if (model?.category == "Chat") LocalFiles.modelDir(context) else dir(context)
        val reserve = 256L * 1024 * 1024
        val cap = if (kind == "Modelo") {
            if (model?.category == "Chat") AgentPolicy.MAX_MODEL_BYTES else NetworkPolicy.MAX_VISUAL_MODEL_BYTES
        } else NetworkPolicy.MAX_MEDIA_BYTES
        val limit = minOf(cap, targetDir.usableSpace - reserve)
        require(limit > 0 && (model == null || model.bytes <= limit)) { "Espacio insuficiente o archivo por encima del límite." }
        val prefix = UUID.randomUUID().toString() + "_"
        val temporary = File(targetDir, prefix + "download.part")
        var uri = NetworkPolicy.https(url)
        val started = System.nanoTime()
        try {
            var redirects = 0
            while (true) {
                check(!cancelled.get()) { "Descarga cancelada." }
                val call = client.newCall(Request.Builder().url(uri.toString())
                    .header("User-Agent","Sakura-Agent-Android").header("Accept-Encoding","identity").build())
                active = call
                if (cancelled.get()) call.cancel()
                call.execute().use { connection ->
                    val code = connection.code
                    if (code in listOf(301,302,303,307,308)) {
                        require(redirects++ < 5) { "Demasiadas redirecciones." }
                        val location = connection.header("Location") ?: error("Redirección sin destino.")
                        uri = NetworkPolicy.https(uri.resolve(location).toString())
                        return@use
                    }
                    require(code == 200) { "Descarga HTTP $code. No se ha guardado el archivo." }
                    val body = connection.body ?: error("Respuesta sin archivo.")
                    val expected = body.contentLength()
                    require(expected <= limit) { "Archivo demasiado grande o espacio insuficiente." }
                    if (model != null && expected >= 0) require(expected == model.bytes) { "El tamaño no coincide con el catálogo." }
                    val hash = MessageDigest.getInstance("SHA-256")
                    var total = 0L; var last = -1L
                    body.byteStream().use { input ->
                        temporary.outputStream().use { output ->
                            val buffer = ByteArray(65536)
                            while (true) {
                                check(!cancelled.get()) { "Descarga cancelada." }
                                check(System.nanoTime() - started < 45L * 60 * 1_000_000_000) { "Descarga supera 45 minutos. Archivo parcial descartado; sin reintento automático." }
                                val count = input.read(buffer); if (count < 0) break
                                total += count
                                require(total <= limit) { "Descarga supera el límite de tamaño." }
                                hash.update(buffer, 0, count); output.write(buffer, 0, count)
                                if (total / (1024 * 1024) != last) { last = total / (1024 * 1024); progress(total, expected) }
                            }
                        }
                    }
                    require(total > 0 && (expected < 0 || total == expected)) { "Descarga incompleta." }
                    check(!cancelled.get()) { "Descarga cancelada." }
                    val digest = hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                    val extension: String
                    val name: String
                    if (model != null) {
                        require(total == model.bytes && digest == model.sha256) { "SHA-256 o tamaño del modelo incorrecto; archivo descartado." }
                        if (model.category == "Chat") require(AgentPolicy.validGguf(temporary.inputStream().use { it.readNBytes(8) })) { "Cabecera GGUF inválida." }
                        name = model.file; extension = model.file.substringAfterLast('.')
                    } else {
                        extension = NetworkPolicy.mediaExtension(temporary.inputStream().use { it.readNBytes(16) }, kind)
                            ?: error("No es un formato de $kind admitido. No se descargan páginas HTML ni listas de reproducción.")
                        name = "${kind.lowercase()}.$extension"
                    }
                    val target = File(targetDir, prefix + name)
                    check(target.extension == extension && temporary.renameTo(target)) { "No se pudo finalizar la descarga." }
                    return DownloadResult(target, digest)
                }
                active = null
            }
        } catch (_: IOException) {
            error(if (cancelled.get()) "Descarga cancelada." else "Fallo de red o almacenamiento; descarga descartada. No se reintentará automáticamente.")
        } finally { active = null; temporary.delete() }
    }
}