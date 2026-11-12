package com.sakura.agent

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile

object LocalFiles {
    fun models(context: Context): List<ModelEntry> = modelDir(context).listFiles()
        .orEmpty().filter { it.isFile && it.extension == "gguf" }
        .sortedBy { it.name }.map { ModelEntry(it.name, it.name.substringAfter('_'), it.length()) }

    fun modelDir(context: Context): File = File(context.noBackupFilesDir, "models").apply { mkdirs() }

    fun importModel(context: Context, uri: Uri): ModelEntry {
        val name = displayName(context, uri).replace(Regex("[^A-Za-z0-9_.-]"), "_").take(100)
        require(name.endsWith(".gguf", true)) { "Selecciona un archivo .gguf." }
        val dir = modelDir(context)
        require(dir.usableSpace > 128L * 1024 * 1024) { "Espacio insuficiente." }
        val output = File(dir, UUID.randomUUID().toString() + "_" + name.removeSuffix(".gguf").removeSuffix(".GGUF") + ".gguf")
        val temp = File(dir, output.name + ".part")
        try {
            copySelected(context, uri, temp, minOf(AgentPolicy.MAX_MODEL_BYTES, dir.usableSpace - 128L * 1024 * 1024))
            val header = temp.inputStream().use { it.readNBytes(8) }
            require(AgentPolicy.validGguf(header)) { "El archivo no es GGUF v2/v3 válido." }
            require(temp.renameTo(output)) { "No se pudo finalizar la importación." }
            return ModelEntry(output.name, name, output.length())
        } finally { temp.delete() }
    }

    fun readSource(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readNBytes(AgentPolicy.MAX_SOURCE_BYTES + 1) }
            ?: error("No se pudo abrir el archivo.")
        require(bytes.size <= AgentPolicy.MAX_SOURCE_BYTES && !bytes.contains(0.toByte())) { "Selecciona código de texto de hasta 64 KiB." }
        val decoded = bytes.toString(Charsets.UTF_8)
        require(decoded.toByteArray(Charsets.UTF_8).contentEquals(bytes)) { "Selecciona código con codificación UTF-8." }
        return "Archivo: ${displayName(context, uri)}\n\n" + decoded
    }

    @Suppress("DEPRECATION")
    fun inspectApk(context: Context, uri: Uri): String {
        val temporary = File.createTempFile("apk_", ".apk", context.cacheDir)
        try {
            copySelected(context, uri, temporary, minOf(AgentPolicy.MAX_APK_BYTES, context.cacheDir.usableSpace / 2))
            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageArchiveInfo(temporary.path, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else pm.getPackageArchiveInfo(temporary.path, flags)
            requireNotNull(info) { "Android no pudo interpretar esta APK. Puede ser un archivo dañado o un paquete dividido." }
            val permissions = info.requestedPermissions.orEmpty().joinToString("\n") { "• $it" }
            val certs = info.signingInfo?.apkContentsSigners.orEmpty().joinToString("\n") { hexHash(it.toByteArray()) }
            val entries = ZipFile(temporary).use { zip ->
                zip.entries().asSequence().take(100_000).map { it.name }.toList()
            }
            val native = entries.filter { it.startsWith("lib/") }.take(30).joinToString("\n")
            val hash = temporary.inputStream().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(65536)
                while (true) { val size = input.read(buffer); if (size < 0) break; digest.update(buffer, 0, size) }
                hex(digest.digest())
            }
            return """
                Análisis estático local — ${displayName(context, uri)}
                Paquete: ${info.packageName}
                Versión: ${info.versionName ?: "sin dato"} (${info.longVersionCode})
                SDK mínimo: ${info.applicationInfo?.minSdkVersion}
                SDK objetivo: ${info.applicationInfo?.targetSdkVersion}
                SHA-256 del archivo: $hash
                Certificados SHA-256: ${certs.ifBlank { "No disponibles; firma NO verificada." }}

                Permisos declarados:
                ${permissions.ifBlank { "Ninguno identificado." }}

                Bibliotecas nativas (máximo 30):
                ${native.ifBlank { "Ninguna." }}

                No se instaló ni ejecutó la APK. Un certificado o la lista de permisos no
                prueban que sea segura. No es antivirus, descompilación ni verificación
                criptográfica completa de la firma. No se extraen archivos ZIP.
            """.trimIndent()
        } finally { temporary.delete() }
    }

    private fun displayName(context: Context, uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "archivo"

    private fun copySelected(context: Context, uri: Uri, target: File, limit: Long) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(65536)
                var total = 0L
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    total += size
                    require(total <= limit) { "Archivo demasiado grande o espacio insuficiente." }
                    output.write(buffer, 0, size)
                }
            }
        } ?: error("No se pudo leer el archivo seleccionado.")
    }

    private fun hexHash(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
