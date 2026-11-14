package com.sakura.agent

import java.net.InetAddress
import java.net.URI

/** Pure validation rules; credentials are never part of downloads or plans. */
object NetworkPolicy {
    const val MAX_MEDIA_BYTES = 512L * 1024 * 1024
    const val MAX_VISUAL_MODEL_BYTES = 5_000_000_000L
    val credentialKeys = setOf("telegram_token", "whatsapp_token", "whatsapp_phone_id", "fal_key",
        "openai_key", "openrouter_key", "groq_key", "deepseek_key", "mistral_key", "xai_key",
        "together_key", "fireworks_key", "compatible_key", "gemini_key", "anthropic_key", "brave_key")
    fun https(value: String): URI {
        require(value.length in 10..4096 && value.none { it.code < 32 }) { "URL inválida." }
        val uri = URI(value)
        require(uri.scheme == "https" && uri.userInfo == null && uri.fragment == null &&
            uri.port in listOf(-1, 443) && !uri.host.isNullOrBlank()) { "Usa una URL HTTPS sin usuario, fragmento ni puerto especial." }
        require(!uri.host.contains(':') && !uri.host.startsWith('[') && !uri.host.matches(Regex("[0-9.]+"))) { "Usa un dominio, no una dirección IP literal." }
        return uri
    }
    fun publicAddress(bytes: ByteArray): Boolean {
        val b = bytes.map { it.toInt() and 255 }
        if (b.size == 4) return b[0] !in listOf(0, 10, 127) && b[0] < 224 &&
            !(b[0] == 100 && b[1] in 64..127) && !(b[0] == 169 && b[1] == 254) &&
            !(b[0] == 172 && b[1] in 16..31) && !(b[0] == 192 && b[1] == 168) &&
            !(b[0] == 198 && b[1] in 18..19)
        // Only global-unicast IPv6; deny mapped IPv4, loopback, ULA, multicast and link-local.
        return b.size == 16 && b[0] in 0x20..0x3f
    }
    fun publicDownload(value: String): URI {
        val uri = https(value)
        require(uri.host.contains('.') && uri.host != "localhost" && !uri.host.endsWith(".local")) { "No se permiten destinos locales." }
        val addresses = InetAddress.getAllByName(uri.host)
        require(addresses.isNotEmpty() && addresses.all { publicAddress(it.address) }) { "El destino no tiene solo direcciones públicas." }
        return uri
    }
    private val youtubeHosts = listOf("googlevideo.com", "youtube.com", "youtu.be", "ytimg.com", "youtube-nocookie.com")
    /** Direct media only: YouTube/Piped stream URLs are for playback, never for extraction. */
    fun directDownload(value: String): URI {
        val uri = https(value)
        val host = uri.host.lowercase()
        require(!host.contains("piped") && youtubeHosts.none { host == it || host.endsWith(".$it") }) {
            "No se descargan streams de YouTube. Usa el enlace para reproducir en la app o el navegador."
        }
        return uri
    }
    fun branch(value: String): Boolean = value.length in 1..200 &&
        value.matches(Regex("[A-Za-z0-9_./-]+")) && !value.startsWith('/') && !value.endsWith('/') &&
        !value.endsWith('.') && !value.contains("..") && !value.contains("//") &&
        value.split('/').none { it.startsWith('.') || it.endsWith(".lock") }
    fun phone(value: String): Boolean = value.matches(Regex("[1-9][0-9]{6,14}"))
    fun chat(value: String): Boolean = value.matches(Regex("-?[0-9]{1,20}|@[A-Za-z][A-Za-z0-9_]{4,31}"))
    fun text(value: String, limit: Int): Boolean = value.isNotBlank() && value.length <= limit &&
        value.none { it.code < 32 && it != '\n' && it != '\t' }
    fun secret(value: String): Boolean = value.length <= 512 && value.none { it.code <= 32 }
    fun mediaExtension(header: ByteArray, kind: String): String? {
        if (kind == "Imagen") {
            if (header.size >= 8 && header.take(8).toByteArray().contentEquals(byteArrayOf(-119,80,78,71,13,10,26,10))) return "png"
            if (header.size >= 3 && header[0] == (-1).toByte() && header[1] == (-40).toByte() && header[2] == (-1).toByte()) return "jpg"
            if (header.size >= 6 && header.take(6).toByteArray().toString(Charsets.US_ASCII) in listOf("GIF87a", "GIF89a")) return "gif"
            if (header.size >= 12 && header.copyOfRange(0,4).toString(Charsets.US_ASCII) == "RIFF" && header.copyOfRange(8,12).toString(Charsets.US_ASCII) == "WEBP") return "webp"
        } else if (kind == "Audio") {
            if (header.size >= 3 && header.copyOfRange(0,3).toString(Charsets.US_ASCII) == "ID3") return "mp3"
            if (header.size >= 4 && header[0] == (-1).toByte() && (header[1].toInt() and 0xe6) == 0xe2) return "mp3"
            if (header.size >= 4 && header.copyOfRange(0,4).toString(Charsets.US_ASCII) == "fLaC") return "flac"
            if (header.size >= 4 && header.copyOfRange(0,4).toString(Charsets.US_ASCII) == "OggS") return "ogg"
            if (header.size >= 12 && header.copyOfRange(0,4).toString(Charsets.US_ASCII) == "RIFF" && header.copyOfRange(8,12).toString(Charsets.US_ASCII) == "WAVE") return "wav"
        } else if (kind == "Video") {
            if (header.size >= 12 && header.copyOfRange(4,8).toString(Charsets.US_ASCII) == "ftyp") return "mp4"
            if (header.size >= 4 && header.take(4).toByteArray().contentEquals(byteArrayOf(26,69,-33,-93))) return "webm"
        }
        return null
    }
}