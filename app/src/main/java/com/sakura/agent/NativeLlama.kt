package com.sakura.agent

/** JNI bridge to the native Sakura/llama.cpp engine. */
class NativeLlama {
    /** Returns an empty string on success, otherwise a user-readable error. */
    external fun nativeLoad(modelPath: String): String

    /** Generates a response from the loaded model. */
    external fun nativeGenerate(input: String): String

    /** Releases model and context resources. */
    external fun nativeUnload()

    companion object {
        init {
            System.loadLibrary("sakura_llm")
        }
    }
}
