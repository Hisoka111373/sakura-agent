package com.sakura.agent

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

data class PhoneFileEntry(val name: String, val size: Long)

object PhoneFiles {
    private const val MAX_ENTRIES = 100
    private const val MAX_BYTES = 64 * 1024

    fun validName(name: String): Boolean = PhoneText.validName(name)

    fun decodeUtf8(bytes: ByteArray): String = PhoneText.decodeUtf8(bytes)

    private fun childrenUri(uri: Uri) = try {
        DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
    } catch (e: Exception) {
        throw IllegalArgumentException("Selecciona una carpeta con el gestor de documentos de Android.", e)
    }

    fun list(context: Context, uri: Uri): List<PhoneFileEntry> {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE)
        val found = mutableListOf<PhoneFileEntry>()
        val sort = "${DocumentsContract.Document.COLUMN_DISPLAY_NAME} ASC"
        val cursor = try { context.contentResolver.query(childrenUri(uri), projection, null, null, sort) }
        catch (e: Exception) { throw IllegalStateException("Android no dio acceso a la carpeta elegida; vuelve a seleccionarla.", e) }
            ?: throw IllegalStateException("Android no dio acceso a la carpeta elegida; vuelve a seleccionarla.")
        cursor.use {
            val nameAt = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeAt = it.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val mimeAt = it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (it.moveToNext() && found.size < MAX_ENTRIES) {
                if (nameAt < 0) continue
                val name = it.getString(nameAt) ?: continue
                if (!validName(name)) continue
                if (mimeAt >= 0 && it.getString(mimeAt) == DocumentsContract.Document.MIME_TYPE_DIR) continue
                val size = if (sizeAt >= 0 && !it.isNull(sizeAt)) it.getLong(sizeAt) else -1L
                found.add(PhoneFileEntry(name, size))
            }
        }
        return found.sortedBy { it.name.lowercase() }.take(MAX_ENTRIES)
    }

    fun readText(context: Context, uri: Uri, name: String): String {
        require(validName(name)) { "Nombre de archivo inválido." }
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val id = try {
            context.contentResolver.query(childrenUri(uri), projection,
                "${DocumentsContract.Document.COLUMN_DISPLAY_NAME}=?", arrayOf(name), null)?.use { cursor ->
                val idAt = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                if (idAt < 0 || !cursor.moveToFirst()) null else cursor.getString(idAt)
            }
        } catch (e: Exception) { throw IllegalStateException("Android no permitió buscar en la carpeta elegida.", e) }
        val documentId = id ?: error("Android no encontró «$name» en la carpeta elegida.")
        require(documentId.isNotBlank()) { "Android no encontró «$name» en la carpeta elegida." }
        val document = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
        val bytes = try { context.contentResolver.openInputStream(document)?.use { it.readNBytes(MAX_BYTES + 1) } }
        catch (e: Exception) { throw IllegalStateException("Android no permitió leer «$name».", e) }
            ?: throw IllegalStateException("Android no permitió leer «$name».")
        require(bytes.size <= MAX_BYTES) { "El archivo supera 64 KiB." }
        require(bytes.none { it == 0.toByte() }) { "El archivo parece binario; solo se lee texto." }
        return decodeUtf8(bytes)
    }
}
