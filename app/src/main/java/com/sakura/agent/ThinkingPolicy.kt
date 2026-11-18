package com.sakura.agent

/** Bounded answer effort, not exposure of hidden reasoning or a speed guarantee. */
object ThinkingPolicy {
    val levels = listOf("Bajo","Alto")
    fun localTokens(level: String) = if(level == "Alto") 384 else 160
    fun localTimeoutMs(level: String) = if(level == "Alto") 90_000L else 45_000L
    fun chatHistory(level: String) = if(level == "Alto") 6 else 2
    fun boundedHistory(messages: List<ChatMessage>, level: String): List<ChatMessage> {
        val limit = if(level == "Alto") 4096 else 1800
        return messages.takeLast(chatHistory(level)).map { message ->
            if(message.text.length <= limit) message else message.copy(text="[Fragmento final; contexto anterior recortado]\n" + message.text.takeLast(limit))
        }
    }
    fun memories(level: String) = if(level == "Alto") 12 else 4
    fun guidance(level: String) = if(level == "Alto")
        "\nEsfuerzo alto: verifica supuestos y presenta explicación útil, sin revelar razonamiento interno. No inventes acciones."
        else "\nEsfuerzo bajo: respuesta breve y directa en una parte útil. El usuario puede pedir otra parte con Continuar. Evita preámbulos y razonamiento largo; no omitas advertencias necesarias."
    const val kawaii = "\nTono kawaii suave de compañera adulta: cálido, animado y juguetón, con expresiones breves como '¡Vamos paso a paso!' cuando encaje. No infantilices, no coquetees ni sacrifiques precisión. No exageres muletillas."
    fun cloudTokens(config: TextConfig) = if(config.provider == "Gemini") {
        if(config.effort == "Alto") 2048 else 512
    } else if(config.effort == "Alto") 1024 else 768
    fun geminiThinking(model: String, effort: String): String? = if(model == "gemini-3.1-flash-lite") {
        if(effort == "Alto") "HIGH" else "MINIMAL"
    } else null // Unknown models keep provider defaults; no guessed unsupported thinking controls.
}