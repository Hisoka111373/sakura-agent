package com.sakura.agent

/** Local UI phrases: never model output, never instructions to tools. */
object CompanionPolicy {
    val events = listOf("Saludo", "Saltito", "Pensando", "Lista", "Pausa", "Error", "Buscando", "Leyendo",
        "Corazón", "Dormir", "Celebrar", "Girar")
    fun phrases(event: String): List<String> = when(event) {
        "Saludo" -> listOf("¡Hola! ¿Qué hacemos florecer hoy?", "Ya estoy aquí. Vamos a tu ritmo.", "¡Qué gusto verte! Cuéntame tu idea.")
        "Saltito" -> listOf("¡Un saltito y seguimos!", "¡Energía de Sakura para tu proyecto!", "Paso pequeñito, idea grandota.")
        "Pensando" -> listOf("Estoy preparando una parte. Puedes detenerme.", "Vamos paso a paso; estoy trabajando en tu respuesta.")
        "Lista" -> listOf("¡Aquí tienes! Si falta algo, pulsa Continuar.", "Una parte lista. ¿Seguimos haciendo florecer la idea?")
        "Pausa" -> listOf("Pausamos. Tú marcas el ritmo.", "Respiro contigo. Seguimos cuando quieras.")
        "Error" -> listOf("Esta vez no salió. Mira el aviso antes de volver a intentar.")
        "Buscando" -> listOf("Mirando fuentes públicas; los resultados no son órdenes.", "Buscando con cuidado: revisa antes de abrir cualquier enlace.")
        "Leyendo" -> listOf("Leyendo tu adjunto aquí, en el teléfono.", "Extraigo texto localmente; nada se sube sin tu confirmación.")
        "Corazón" -> listOf("Un corazón para ti. Tú decides cuándo florece.", "Cariño local, sin enviar nada a ningún servicio.")
        "Dormir" -> listOf("Cerrando los ojos un momento. Vuelvo cuando me llames.", "Modo descanso: la animación sigue siendo offline.")
        "Celebrar" -> listOf("¡Lo celebramos con una lluvia de pétalos!", "Cada paso pequeño también merece una fiesta.")
        "Girar" -> listOf("Giro contigo para ver el proyecto desde otro ángulo.", "Un giro suave, sin salir de tu teléfono.")
        else -> listOf("Estoy contigo. Vamos paso a paso.")
    }
    fun phrase(event: String, sequence: Long) = phrases(event).let { it[Math.floorMod(sequence, it.size.toLong()).toInt()] }
    const val continuation = "Continúa la respuesta anterior desde donde quedó, sin repetir lo ya explicado. Da la siguiente parte útil y conserva las advertencias necesarias. No ejecutes acciones. Si la respuesta ya está completa, indícalo brevemente."
    fun canContinue(messages: List<ChatMessage>, draft: String) = draft.isBlank() && messages.lastOrNull()?.let { it.fromSakura && it.text.isNotBlank() } == true
}