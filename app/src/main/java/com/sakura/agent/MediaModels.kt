package com.sakura.agent

/**
 * Honest catalog of media capabilities.
 *
 * Local: only text GGUF models are supported by the current llama.cpp JNI bridge; no
 * local diffusion/video engine is bundled, so no fake hashes are published here.
 * Online: image/video generation uses the user's fal.ai key; image understanding uses
 * the user's Gemini key. Nothing is sent until the normal confirmation panel is shown.
 */
data class MediaModel(val name: String, val kind: String, val provider: String,
    val model: String, val credential: String, val note: String)

object MediaModels {
    val entries = listOf(
        MediaModel("FLUX schnell", "Imagen", "fal.ai", "fal-ai/flux/schnell", "fal_key",
            "Generación de imágenes en la nube; coste según tarifa de fal.ai. Revisa licencia y uso comercial."),
        MediaModel("LTX Video 0.9.1", "Vídeo", "fal.ai", "fal-ai/ltx-video", "fal_key",
            "Generación de vídeo en la nube; licencia de investigación, revisar antes de uso comercial."),
        MediaModel("Gemini 2.0 Flash", "Visión", "Google", "gemini-2.0-flash", "gemini_key",
            "Describe imágenes adjuntas. Envía una imagen por petición tras confirmación; puede consumir cuota."),
        MediaModel("Qwen2.5 0.5B Q4_K_M", "Texto local", "Hugging Face", "qwen2.5-0.5b-instruct-q4_k_m.gguf", "",
            "GGUF verificado por SHA-256 en ModelCatalog; opción rápida sin red cuando el modelo principal es lento.")
    )
    fun online(): List<MediaModel> = entries.filter { it.credential.isNotBlank() }
    fun localText(): List<MediaModel> = entries.filter { it.kind == "Texto local" }
}
