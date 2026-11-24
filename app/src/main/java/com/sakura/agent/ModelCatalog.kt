package com.sakura.agent

data class CatalogModel(val title: String, val category: String, val repo: String, val revision: String,
    val file: String, val bytes: Long, val sha256: String, val license: String, val warning: String) {
    val url get() = "https://huggingface.co/$repo/resolve/$revision/$file"
    val card get() = "https://huggingface.co/$repo"
}

object ModelCatalog {
    // Hugging Face repository metadata and LFS hashes checked on 2026-11-18.
    val entries = listOf(
        CatalogModel("Qwen2.5 0.5B Instruct · Q4_K_M · ligero", "Chat", "Qwen/Qwen2.5-0.5B-Instruct-GGUF",
            "9217f5db79a29953eb74d5343926648285ec7e67", "qwen2.5-0.5b-instruct-q4_k_m.gguf", 491400032,
            "74a4da8c9fdbcd15bd1f6d01d621410d31c6fc00986f5eb687824e7b93d7a9db", "Apache 2.0",
            "Prueba este modelo si el contexto local agota el tiempo. Menor carga de CPU/memoria; respuestas menos capaces. Archivo ≈491 MB; No probado en este teléfono; velocidad no medida."),
        CatalogModel("Qwen2.5 Coder 0.5B Instruct · Q4_K_M", "Chat", "QuantFactory/Qwen2.5-Coder-0.5B-Instruct-GGUF",
            "e91d128ba1c331fdec779e0ff21efa7568274e08", "Qwen2.5-Coder-0.5B-Instruct.Q4_K_M.gguf", 491400512,
            "5d933e04310b9184f9bc97e511820e71b5dc7704eda6f7c937126a241828e93e", "Apache 2.0",
            "Modelo pequeño especializado en código, cuantización de terceros. Puede equivocarse; no ejecuta código. Archivo ≈491 MB; rendimiento físico pendiente."),
        CatalogModel("Qwen2.5 3B Instruct · Q4_K_M", "Chat", "Qwen/Qwen2.5-3B-Instruct-GGUF",
            "7dabda4d13d513e3e842b20f0d435c732f172cbe", "qwen2.5-3b-instruct-q4_k_m.gguf", 2104932768,
            "626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d", "Qwen Research (revisar tarjeta)",
            "Más pesado: empieza por 0.5B si el contexto tarda. Archivo ≈2.10 GB / 1.96 GiB; NO equivale a RAM necesaria ni garantiza rapidez."),
        CatalogModel("Qwen2.5 1.5B Instruct · Q4_K_M", "Chat", "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            "91cad51170dc346986eccefdc2dd33a9da36ead9", "qwen2.5-1.5b-instruct-q4_k_m.gguf", 1117320736,
            "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e", "Apache 2.0",
            "Alternativa menor si 3B usa demasiada memoria o tarda. No probado en este teléfono."),
        CatalogModel("Qwen2.5 1.5B Instruct · Q3_K_M", "Chat", "Qwen/Qwen2.5-1.5B-Instruct-GGUF",
            "91cad51170dc346986eccefdc2dd33a9da36ead9", "qwen2.5-1.5b-instruct-q3_k_m.gguf", 924455968,
            "58cb5c05ecef48e82961f1a2be6544145ea26136f69dddda4bbbd092f0e4b993", "Apache 2.0",
            "GGUF reducido (~0.92 GB) frente a Q4_K_M (1.1 GB). Misma familia; calidad distinta. No probado en este teléfono."),
        CatalogModel("Stable Diffusion 1.5 · checkpoint", "Imagen · descarga externa", "stable-diffusion-v1-5/stable-diffusion-v1-5",
            "451f4fe16113bff5a5d2269ed5ad43b0592e9a14", "v1-5-pruned-emaonly.safetensors", 4265146304,
            "6ce0161689b3853acaa03779ec93eafe75a02f4ced659bee03f50797806fa2fa", "CreativeML Open RAIL-M",
            "Descarga externa para evaluación en otro motor. Sakura NO incluye motor local de difusión ni genera imágenes con este archivo; no cargar con llama.cpp."),
        CatalogModel("LTX Video 2B 0.9.8 distilled · FP8", "Video · descarga externa", "Lightricks/LTX-Video",
            "8984fa25007f376c1a299016d0957a37a2f797bb", "ltxv-2b-0.9.8-distilled-fp8.safetensors", 4461695684,
            "d6d8fa8ed3a98346787c2503ac80fb5d7cebcf80e356b79a2ba361fbadf97e15", "Licencia específica LTX (revisar tarjeta)",
            "Descarga externa: checkpoint sin motor local en Sakura; puede requerir encoder/VAE y otro runtime. 8 GB no garantiza viabilidad."),
        // 0.12.0: catálogo ampliado con archivos públicos verificados por la API de Hugging Face.
        CatalogModel("Openjourney 1.5 · checkpoint ligero", "Imagen · descarga externa", "prompthero/openjourney",
            "f4572661b028c732b2b97c8fbdc32fa5db3afe03", "model.safetensors", 492265879,
            "40a255878ab9382df07730db8d1719512fe60a61c931288d152511ab6443b4af", "CreativeML Open RAIL-M",
            "Descarga externa: checkpoint SD 1.5 de ~492 MB. Necesita un motor local de difusión distinto; Sakura solo descarga y verifica."),
        CatalogModel("Segmind SSD-1B · checkpoint", "Imagen · descarga externa", "segmind/SSD-1B",
            "60987f37e94cd59c36b1cba832b9f97b57395a10", "SSD-1B.safetensors", 4465671506,
            "0bf1ce6b065a6b969ab02dc8e8fa21eb20ee189b10935c49ce68c77a7e432c1c", "Apache 2.0 (revisar tarjeta)",
            "Descarga externa: modelo destilado (~4.47 GB) para un motor local distinto; no se ejecuta dentro de Sakura."),
        CatalogModel("SD Turbo · checkpoint", "Imagen · descarga externa", "stabilityai/sd-turbo",
            "b261bac6fd2cf515557d5d0707481eafa0485ec2", "sd_turbo.safetensors", 5214561328,
            "3f067a1b943cf162f2b8f8588f6cf5824bd5b4c7d1d88d87164b9ca123616549", "Licencia Stability (revisar tarjeta)",
            "Descarga externa: ~5.21 GB. Antes no se podía bajar porque el límite era de 5 GB; ahora es de 16 GB. Necesitas espacio libre y un motor local distinto."),
        CatalogModel("LTX 2B 0.9.6 distilled · checkpoint", "Video · descarga externa", "Lightricks/LTX-Video",
            "8984fa25007f376c1a299016d0957a37a2f797bb", "ltxv-2b-0.9.6-distilled-04-25.safetensors", 6340744028,
            "94891bd4bd08de30d484befbfc54fdcffe6d1596a131baad700b9baa5e1de86b", "Licencia específica LTX (revisar tarjeta)",
            "Descarga externa: checkpoint de vídeo ~6.34 GB. Descarga verificada; la generación requiere un motor local distinto y más memoria."),
        CatalogModel("LTX spatial upscaler 0.9.8", "Video · descarga externa", "Lightricks/LTX-Video",
            "8984fa25007f376c1a299016d0957a37a2f797bb", "ltxv-spatial-upscaler-0.9.8.safetensors", 505024432,
            "5b076031c6f860db9037a54f3bb819f10bfb5532ea26a6d30062292428a0c208", "Licencia específica LTX (revisar tarjeta)",
            "Descarga externa: módulo de mejora espacial para vídeo (~505 MB) de otro motor local; no es texto a vídeo independiente."),
        CatalogModel("LTX temporal upscaler 0.9.8", "Video · descarga externa", "Lightricks/LTX-Video",
            "8984fa25007f376c1a299016d0957a37a2f797bb", "ltxv-temporal-upscaler-0.9.8.safetensors", 523894768,
            "92ee7c34a06007da23e83c9c2db1ea74344d95a3b8baa6a3a1d18a0aa20a39b8", "Licencia específica LTX (revisar tarjeta)",
            "Descarga externa: módulo de mejora temporal para vídeo (~524 MB) de otro motor local; no es texto a vídeo independiente."),
        CatalogModel("Whisper Tiny · audio local (ggml)", "Audio · descarga externa", "ggerganov/whisper.cpp",
            "5359861c739e955e79d9a303bcbc70fb988958b1", "ggml-tiny.bin", 77691713,
            "be07e048e1e599ad46341c8d2a135645097a538221678b7acdd1b1919c6e1b21", "MIT",
            "Descarga externa: transcripción de voz de ~78 MB para whisper.cpp u otro motor local compatible. Sakura no incluye motor Whisper; solo descarga y verifica."),
        CatalogModel("Whisper Tiny English Q5 · audio local", "Audio · descarga externa", "ggerganov/whisper.cpp",
            "5359861c739e955e79d9a303bcbc70fb988958b1", "ggml-tiny.en-q5_1.bin", 32166155,
            "c77c5766f1cef09b6b7d47f21b546cbddd4157886b3b5d6d4f709e91e66c7c2b", "MIT",
            "Descarga externa: solo inglés, ~32 MB. Descarga verificada para un motor local compatible; no se ejecuta en Sakura."),
        CatalogModel("Whisper Base · audio local (ggml)", "Audio · descarga externa", "ggerganov/whisper.cpp",
            "5359861c739e955e79d9a303bcbc70fb988958b1", "ggml-base.bin", 147951465,
            "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe", "MIT",
            "Descarga externa: modelo multilingüe de ~148 MB para whisper.cpp. Requiere otro motor local y su propia licencia/uso.")
    )
    fun visual(): List<CatalogModel> = entries.filter { it.category.startsWith("Imagen") || it.category.startsWith("Video") }
    fun audio(): List<CatalogModel> = entries.filter { it.category.startsWith("Audio") }
    fun chat(): List<CatalogModel> = entries.filter { it.category == "Chat" }
}

/** First-run model: downloaded once by the app, verified, then kept private. Never bundled in the APK. */
object StarterModel {
    const val AUTO_KEY = "starter_auto_download"
    const val ATTEMPT_KEY = "starter_attempted"
    const val DONE_KEY = "starter_completed"
    const val AUTO_LOAD_KEY = "starter_auto_load"
    const val FAST_KEY = "starter_fast_download"
    val entry: CatalogModel = ModelCatalog.entries.first { it.file == "qwen2.5-1.5b-instruct-q4_k_m.gguf" }
    /** Small verified GGUF kept locally so the fast mode works offline after the first download. */
    val fast: CatalogModel = ModelCatalog.entries.first { it.file == "qwen2.5-0.5b-instruct-q4_k_m.gguf" }
    fun matches(name: String): Boolean = name.contains("qwen2.5-1.5b-instruct-q4_k_m", ignoreCase = true)
    fun matchesFast(name: String): Boolean = name.contains("qwen2.5-0.5b-instruct-q4_k_m", ignoreCase = true)
}