package com.sakura.agent

fun interface NativeProgress { fun onProgress(tokens: Int, text: ByteArray) }

/** All load/generate/unload calls must use one worker. Cancellation is atomic in JNI. */
class NativeLlama {
    external fun nativeLoad(modelPath: String)
    private external fun nativeGenerate(system: ByteArray, roles: Array<String>, texts: Array<ByteArray>, maxTokens: Int, timeoutMs: Long, progress: NativeProgress?): ByteArray
    external fun nativeUnload()
    external fun nativeCancel()
    external fun nativeResetCancellation()

    fun generate(system: String, messages: List<ChatMessage>, maxTokens: Int = 384, timeoutMs: Long = 90_000L, progress: NativeProgress? = null): String = nativeGenerate(
        system.toByteArray(Charsets.UTF_8),
        messages.map { if (it.fromSakura) "assistant" else "user" }.toTypedArray(),
        messages.map { it.text.toByteArray(Charsets.UTF_8) }.toTypedArray(), maxTokens, timeoutMs, progress
    ).toString(Charsets.UTF_8)

    companion object {
        // Do not hide a missing JNI library behind a fake successful initialization.
        init { System.loadLibrary("sakura_llm") }
    }
}