package com.sakura.agent

data class CatalogModel(val title: String, val category: String, val repo: String, val revision: String,
    val file: String, val bytes: Long, val sha256: String, val license: String, val warning: String) {
    val url get() = "https://huggingface.co/$repo/resolve/$revision/$file"
    val card get() = "https://huggingface.co/$repo"
}

object ModelCatalog {
    // Hugging Face repository metadata and LFS hashes checked on 2026-11-18.
    val entries = listOf(
        CatalogModel("Qwen2.5 3B Instruct · Q4_K_M", "Chat", "Qwen/Qwen2.5-3B-Instruct-GGUF",
            "7dabda4d13d513e3e842b20f0d435c732f172cbe", "qwen2.5-3b-instruct-q4_k_m.gguf", 2104932768,
            "626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d", "Qwen Research (revisar tarjeta)",
            "Candidato inicial para tu teléfono de 8 GB. Archivo ≈2.10 GB / 1.96 GiB; NO equivale a RAM necesaria ni garantiza rapidez."),
        CatalogModel("Qwen2.5 1.5B Instruct · Q4_K_M", "Chat", "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            "91cad51170dc346986eccefdc2dd33a9da36ead9", "qwen2.5-1.5b-instruct-q4_k_m.gguf", 1117320736,
            "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e", "Apache 2.0",
            "Alternativa menor si 3B usa demasiada memoria o tarda. No probado en este teléfono."),
        CatalogModel("Qwen2.5 1.5B Instruct · Q3_K_M", "Chat", "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            "91cad51170dc346986eccefdc2dd33a9da36ead9", "qwen2.5-1.5b-instruct-q3_k_m.gguf", 924455968,
            "58cb5c05ecef48e82961f1a2be6544145ea26136f69dddda4bbbd092f0e4b993", "Apache 2.0",
            "GGUF reducido (~0.92 GB) frente a Q4_K_M (1.1 GB). Misma familia; calidad distinta. No probado en este teléfono."),
        CatalogModel("Qwen2.5 0.5B Instruct · Q4_K_M", "Chat", "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            "9217f5db79a29953eb74d5343926648285ec7e67", "qwen2.5-0.5b-instruct-q4_k_m.gguf", 491400032,
            "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db", "Apache 2.0",
            "GGUF pequeño (~0.49 GB) para probar inferencia local en CPU. Respuestas más limitadas; no probado en este teléfono."),
        CatalogModel("Stable Diffusion 1.5 · checkpoint", "Imagen · descarga externa", "stable-diffusion-v1-5/stable-diffusion-v1-5",
            "451f4fe16113bff5a5d2269ed5ad43b0592e9a14", "v1-5-pruned-emaonly.safetensors", 4265146304,
            "6ce0161689b3853acaa03779ec93eafe75a02f4ced659bee03f50797806fa2fa", "CreativeML Open RAIL-M",
            "Descarga externa para evaluación en otro motor. Sakura NO incluye motor local de difusión ni genera imágenes con este archivo; no cargar con llama.cpp."),
        CatalogModel("LTX Video 2B 0.9.8 distilled · FP8", "Video · descarga externa", "Lightricks/LTX-Video",
            "8984fa25007f376c1a299016d0957a37a2f797bb", "ltxv-2b-0.9.8-distilled-fp8.safetensors", 4461695684,
            "d6d8fa8ed3a98346787c2503ac80fb5d7cebcf80e356b79a2ba361fbadf97e15", "Licencia específica LTX (revisar tarjeta)",
            "Descarga externa: checkpoint sin motor local en Sakura; puede requerir encoder/VAE y otro runtime. 8 GB no garantiza viabilidad.")
    )
}

/** First-run model: downloaded once by the app, verified, then kept private. Never bundled in the APK. */
object StarterModel {
    const val AUTO_KEY = "starter_auto_download"
    const val ATTEMPT_KEY = "starter_attempted"
    const val DONE_KEY = "starter_completed"
    const val AUTO_LOAD_KEY = "starter_auto_load"
    val entry: CatalogModel = ModelCatalog.entries.first { it.file == "qwen2.5-1.5b-instruct-q4_k_m.gguf" }
    fun matches(name: String): Boolean = name.contains("qwen2.5-1.5b-instruct-q4_k_m", ignoreCase = true)
}