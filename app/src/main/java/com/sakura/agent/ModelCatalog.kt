package com.sakura.agent

data class CatalogModel(val title: String, val category: String, val repo: String, val revision: String,
    val file: String, val bytes: Long, val sha256: String, val license: String, val warning: String) {
    val url get() = "https://huggingface.co/$repo/resolve/$revision/$file"
    val card get() = "https://huggingface.co/$repo"
}

object ModelCatalog {
    // Hugging Face repository metadata and LFS hashes checked on 2026-11-12.
    val entries = listOf(
        CatalogModel("Qwen2.5 3B Instruct · Q4_K_M", "Chat", "Qwen/Qwen2.5-3B-Instruct-GGUF",
            "7dabda4d13d513e3e842b20f0d435c732f172cbe", "qwen2.5-3b-instruct-q4_k_m.gguf", 2104932768,
            "626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d", "Qwen Research (revisar tarjeta)",
            "Candidato inicial para tu teléfono de 8 GB. Archivo ≈2.10 GB / 1.96 GiB; NO equivale a RAM necesaria ni garantiza rapidez."),
        CatalogModel("Qwen2.5 1.5B Instruct · Q4_K_M", "Chat", "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            "91cad51170dc346986eccefdc2dd33a9da36ead9", "qwen2.5-1.5b-instruct-q4_k_m.gguf", 1117320736,
            "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e", "Apache 2.0",
            "Alternativa menor si 3B usa demasiada memoria o tarda. No probado en este teléfono."),
        CatalogModel("Stable Diffusion 1.5 · checkpoint", "Imagen", "stable-diffusion-v1-5/stable-diffusion-v1-5",
            "451f4fe16113bff5a5d2269ed5ad43b0592e9a14", "v1-5-pruned-emaonly.safetensors", 4265146304,
            "6ce0161689b3853acaa03779ec93eafe75a02f4ced659bee03f50797806fa2fa", "CreativeML Open RAIL-M",
            "Solo descarga para evaluación externa. Sakura NO incluye motor local de difusión. No cargar con llama.cpp."),
        CatalogModel("LTX Video 2B 0.9.8 distilled · FP8", "Video", "Lightricks/LTX-Video",
            "8984fa25007f376c1a299016d0957a37a2f797bb", "ltxv-2b-0.9.8-distilled-fp8.safetensors", 4461695684,
            "d6d8fa8ed3a98346787c2503ac80fb5d7cebcf80e356b79a2ba361fbadf97e15", "Licencia específica LTX (revisar tarjeta)",
            "Solo checkpoint para evaluación externa: puede requerir encoder/VAE y más memoria. Sin generación local en Sakura; 8 GB no garantiza viabilidad.")
    )
}