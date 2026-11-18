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
    const val localContextTokens = 2048
    const val localPromptTrimmed = "\n[Contexto recortado para caber en 2048 tokens]"
    fun localPromptChars(level: String) = if(level == "Alto") 3000 else 3400
    fun localMemories(notes: List<Memory>, level: String) = notes.takeLast(memories(level)).map { it.copy(text = it.text.take(120)) }
    fun fitLocalPrompt(level: String, system: String, history: List<ChatMessage>): Pair<String, List<ChatMessage>> {
        val budget = localPromptChars(level)
        val messages = history.takeLast(chatHistory(level))
        if (messages.isEmpty()) return (if (system.length <= budget) system else system.take(budget - localPromptTrimmed.length) + localPromptTrimmed) to messages
        val last = messages.last()
        val newest = last
        val systemRoom = (budget - newest.text.length - 200).coerceAtLeast(600)
        val fittedSystem = if (system.length <= systemRoom) system else system.take(systemRoom - localPromptTrimmed.length) + localPromptTrimmed
        var room = budget - fittedSystem.length - newest.text.length - localPromptTrimmed.length
        val older = mutableListOf<ChatMessage>()
        for (message in messages.dropLast(1).asReversed()) {
            if (room <= 0) break
            val text = if (message.text.length <= room) message.text else "[Fragmento final; contexto anterior recortado]\n" + message.text.takeLast(room - 48)
            older.add(0, message.copy(text = text))
            room -= text.length + 32
        }
        return fittedSystem to (older + newest)
    }
    fun cloudTokens(config: TextConfig) = if(config.provider == "Gemini") {
        if(config.effort == "Alto") 2048 else 512
    } else if(config.effort == "Alto") 1024 else 768
    fun geminiThinking(model: String, effort: String): String? = if(model == "gemini-3.1-flash-lite") {
        if(effort == "Alto") "HIGH" else "MINIMAL"
    } else null // Unknown models keep provider defaults; no guessed unsupported thinking controls.
}