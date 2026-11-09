package com.sakura.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class LocalModel(
    val id: String,
    val name: String,
    val fileName: String,
    val downloadUrl: String?,
    val sizeDescription: String,
    val license: String,
    val bundledInApk: Boolean = false
)

class LocalModelManager(context: Context) {
    private val appContext = context.applicationContext
    private val modelsDirectory = File(appContext.filesDir, "models").apply {
        if (!exists()) mkdirs()
    }

    val catalog = listOf(
        LocalModel(
            id = "qwen-1.5b-q4km",
            name = "Qwen2.5 1.5B Instruct · Q4_K_M",
            fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            downloadUrl = null,
            sizeDescription = "Alrededor de 1 GB · modelo principal",
            license = "Apache-2.0",
            bundledInApk = true
        ),
        LocalModel(
            id = "qwen-0.5b-q4km",
            name = "Qwen2.5 0.5B Instruct · Q4_K_M",
            fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            sizeDescription = "Descarga opcional · modelo más ligero",
            license = "Apache-2.0"
        )
    )

    fun fileFor(model: LocalModel): File =
        File(modelsDirectory, model.fileName)

    fun isInstalled(model: LocalModel): Boolean {
        val file = fileFor(model)
        return file.isFile && file.length() > MINIMUM_MODEL_BYTES
    }

    fun installedModels(): List<LocalModel> =
        catalog.filter(::isInstalled)

    /**
     * Copia el modelo principal desde app/src/main/assets/sakura-default.gguf.
     * El APK de CI tendrá que incluir ese archivo GGUF en sus assets.
     */
    suspend fun installBundledModel(
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val model = catalog.first { it.bundledInApk }
        val destination = fileFor(model)

        if (isInstalled(model)) {
            onProgress(100)
            return@withContext destination
        }

        val temporaryFile = File(modelsDirectory, "${model.fileName}.partial")
        temporaryFile.delete()

        appContext.assets.open(BUNDLED_ASSET_NAME).use { input ->
            temporaryFile.outputStream().buffered().use { output ->
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                var copied = 0L
                val totalBytes = runCatching {
                    appContext.assets.openFd(BUNDLED_ASSET_NAME).use { it.length }
                }.getOrDefault(-1L)

                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break

                    output.write(buffer, 0, count)
                    copied += count

                    if (totalBytes > 0) {
                        onProgress(((copied * 100) / totalBytes).toInt().coerceIn(0, 100))
                    }
                }
            }
        }

        check(temporaryFile.length() > MINIMUM_MODEL_BYTES) {
            "El modelo incluido no existe o está incompleto."
        }

        destination.delete()
        check(temporaryFile.renameTo(destination)) {
            "No se pudo guardar el modelo incluido."
        }

        onProgress(100)
        destination
    }

    /**
     * Descarga un modelo del catálogo a almacenamiento privado de la app.
     */
    suspend fun download(
        model: LocalModel,
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        require(!model.bundledInApk) {
            "El modelo principal se copia desde la APK, no se descarga aquí."
        }
        val downloadUrl = requireNotNull(model.downloadUrl) {
            "Este modelo no tiene una dirección de descarga."
        }

        val destination = fileFor(model)
        val temporaryFile = File(modelsDirectory, "${model.fileName}.partial")
        temporaryFile.delete()

        var connection: HttpURLConnection? = null

        try {
            connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                requestMethod = "GET"
                connect()
            }

            check(connection.responseCode in 200..299) {
                "Falló la descarga (HTTP ${connection.responseCode})."
            }

            val totalBytes = connection.contentLengthLong

            connection.inputStream.use { input ->
                temporaryFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var receivedBytes = 0L

                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break

                        output.write(buffer, 0, count)
                        receivedBytes += count

                        if (totalBytes > 0) {
                            onProgress(
                                ((receivedBytes * 100) / totalBytes)
                                    .toInt()
                                    .coerceIn(0, 100)
                            )
                        }
                    }
                }
            }

            check(temporaryFile.length() > MINIMUM_MODEL_BYTES) {
                "La descarga parece incompleta; comprueba la conexión e inténtalo otra vez."
            }

            destination.delete()
            check(temporaryFile.renameTo(destination)) {
                "No se pudo guardar el modelo descargado."
            }

            onProgress(100)
            destination
        } catch (error: Exception) {
            temporaryFile.delete()
            throw error
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun deleteOptionalModel(model: LocalModel) = withContext(Dispatchers.IO) {
        require(!model.bundledInApk) {
            "El modelo incluido en la APK no se elimina desde esta opción."
        }
        fileFor(model).delete()
    }

    private companion object {
        const val BUNDLED_ASSET_NAME = "sakura-default.gguf"
        const val MINIMUM_MODEL_BYTES = 100_000_000L
        const val COPY_BUFFER_SIZE = 1024 * 1024
        const val DOWNLOAD_BUFFER_SIZE = 256 * 1024
        const val CONNECTION_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 60_000
    }
}
