package com.sakura.agent

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

object PhoneText {
    fun validName(name: String): Boolean = name.isNotBlank() && name.length <= 200 &&
        !name.contains('/') && !name.contains('\\') && !name.contains("..") &&
        name.none { it.code < 32 }

    fun decodeUtf8(bytes: ByteArray): String {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try { decoder.decode(ByteBuffer.wrap(bytes)).toString() }
        catch (e: CharacterCodingException) { throw IllegalArgumentException("El archivo no es UTF-8 válido.", e) }
    }
}
