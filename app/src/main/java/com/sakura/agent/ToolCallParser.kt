package com.sakura.agent

import org.json.JSONObject

/** One model-proposed tool call or a final answer. Data only: the model never gets a callback. */
sealed class ParsedAction {
    data class Call(val name: String, val args: Map<String, String>) : ParsedAction()
    data class Final(val text: String) : ParsedAction()
}

/**
 * Strict parser for the small JSON protocol used by the agent loop.
 * Accepts {"tool":"x","args":{...}} or {"name":"x","arguments":{...}}.
 * Text without a tool object is treated as a final answer, never as an action.
 */
object ToolCallParser {
    const val MAX_BYTES = 24 * 1024
    const val MAX_ARGS = 10
    const val MAX_ARG_LENGTH = 4096
    const val MAX_TOTAL_ARGS = 16 * 1024
    private val secretNames = listOf("token", "key", "secret", "password", "credential", "cookie")

    fun parse(raw: String, allowed: Set<String>): ParsedAction {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Propuesta demasiado grande." }
        val text = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        require(text.isNotEmpty()) { "Propuesta vacía." }
        val json = extractObject(text)
        if (json == null) {
            require(!(text.startsWith("{") || text.contains("\"tool\"") || text.contains("\"name\"") || text.contains("\"final\""))) {
                "JSON de herramienta incompleto."
            }
            return ParsedAction.Final(text)
        }

        val root = try { JSONObject(json) } catch (_: Exception) { error("JSON de herramienta inválido.") }
        val keys = root.keys().asSequence().toSet()
        if (keys == setOf("final")) {
            return ParsedAction.Final(root.getString("final").also { requireFinal(it) })
        }
        val name: String
        val argsJson: JSONObject
        when (keys) {
            setOf("tool", "args") -> { name = root.getString("tool"); argsJson = root.getJSONObject("args") }
            setOf("name", "arguments") -> {
                name = root.getString("name")
                argsJson = when (val value = root.get("arguments")) {
                    is JSONObject -> value
                    is String -> try { JSONObject(value) } catch (_: Exception) { error("Argumentos JSON inválidos.") }
                    else -> error("Argumentos inválidos.")
                }
            }
            setOf("tool") -> { name = root.getString("tool"); argsJson = JSONObject() }
            setOf("name") -> { name = root.getString("name"); argsJson = JSONObject() }
            else -> error("Esquema de herramienta inválido: no se permiten campos extra.")
        }
        if (name == "finish") {
            val answer = argsJson.optString("text", "")
            return ParsedAction.Final(answer.also { requireFinal(it) })
        }
        require(name in allowed) { "Herramienta no permitida: $name" }
        return ParsedAction.Call(name, arguments(argsJson))
    }

    fun arguments(args: JSONObject): Map<String, String> {
        val names = args.keys().asSequence().toList()
        require(names.size <= MAX_ARGS) { "Demasiados argumentos." }
        var total = 0
        return names.associateWith { key ->
            require(key.matches(Regex("[a-z][a-z0-9_]{0,39}"))) { "Nombre de argumento inválido." }
            require(secretNames.none { it in key }) { "No se aceptan credenciales como argumentos." }
            val value = args.get(key)
            require(value is String) { "Los argumentos deben ser texto." }
            require(value.length <= MAX_ARG_LENGTH) { "Argumento demasiado largo." }
            require(value.none { it.code < 32 && it != '\n' && it != '\t' }) { "Argumento con caracteres de control." }
            total += value.length
            require(total <= MAX_TOTAL_ARGS) { "Argumentos demasiado grandes." }
            value
        }
    }

    private fun requireFinal(text: String) {
        require(text.isNotBlank() && text.length <= 100_000) { "Respuesta final vacía o demasiado larga." }
    }

    /** Returns the first JSON object only when it looks like a tool protocol, not prose with braces. */
    private fun extractObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        val looksLikeTool = text.contains("\"tool\"") || text.contains("\"name\"") ||
            text.contains("\"final\"") || text.startsWith("{")
        if (!looksLikeTool) return null
        val end = text.lastIndexOf('}')
        return if (end > start) text.substring(start, end + 1) else null
    }
}
