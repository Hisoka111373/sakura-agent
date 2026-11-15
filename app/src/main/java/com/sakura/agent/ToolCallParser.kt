package com.sakura.agent

/** Strict JSON subset: objects and strings only, no duplicates/coercion/trailing prose. */
object ToolCallParser {
    fun parse(raw: String): ParsedAction {
        require(raw.toByteArray(Charsets.UTF_8).size <= 24*1024) { "Propuesta demasiado grande." }
        val text = raw.trim()
        require(text.isNotEmpty())
        if (!text.startsWith("{")) {
            require(!text.contains("\"tool\"") && !text.contains("\"args\"") && !text.contains("\"final\"") && !text.startsWith("```")) { "Se requiere un único JSON estricto, sin texto alrededor." }
            return ParsedAction.Final(text)
        }
        val reader = Reader(text)
        val root = reader.obj(0)
        reader.space(); require(reader.at == text.length) { "Contenido adicional tras el JSON." }
        if (root.keys == setOf("final")) {
            val final = root["final"] as? String ?: error("Respuesta final debe ser texto.")
            require(NetworkPolicy.text(final,24000))
            return ParsedAction.Final(final)
        }
        require(root.keys == setOf("tool","args")) { "Esquema incorrecto; solo tool y args." }
        val name = root["tool"] as? String ?: error("Herramienta debe ser texto.")
        val args = root["args"] as? Map<*,*> ?: error("Args debe ser un objeto.")
        require(args.size <= 10)
        val values = args.entries.associate { (key,value) ->
            require(key is String && key.matches(Regex("[a-z][a-z0-9_]{0,39}")))
            require(listOf("token","key","secret","password","credential","cookie").none { it in key }) { "No se aceptan credenciales." }
            require(value is String && value.length <= 4096 && value.none { it.code < 32 && it != '\n' && it != '\t' }) { "Argumentos deben ser texto acotado." }
            key to value
        }
        require(values.values.sumOf { it.length } <= 16384)
        return ParsedAction.Call(name,values).also(AgentTools::validate)
    }

    private class Reader(private val text: String) {
        var at = 0
        fun space() { while(at < text.length && text[at] in " \n\r\t") at++ }
        private fun take(c: Char) { space(); require(at < text.length && text[at++] == c) { "JSON inválido." } }
        fun obj(depth: Int): Map<String,Any> {
            require(depth <= 1) { "Objetos anidados no permitidos en argumentos." }
            take('{'); space()
            val result = linkedMapOf<String,Any>()
            if(at < text.length && text[at] == '}') { at++; return result }
            while(true) {
                val key = string(); require(key !in result) { "Clave JSON duplicada." }; take(':'); space()
                result[key] = if(at < text.length && text[at] == '{') obj(depth+1) else string()
                space(); require(at < text.length)
                when(text[at++]) { '}' -> return result; ',' -> Unit; else -> error("Separador JSON inválido.") }
            }
        }
        private fun string(): String {
            take('"'); val result = StringBuilder()
            while(at < text.length) {
                val c = text[at++]
                if(c == '"') return result.toString()
                require(c.code >= 32) { "Control JSON sin escapar." }
                if(c != '\\') { result.append(c); continue }
                require(at < text.length)
                when(val escape = text[at++]) {
                    '"','\\','/' -> result.append(escape)
                    'n' -> result.append('\n'); 't' -> result.append('\t'); 'r' -> result.append('\r')
                    'b' -> result.append('\b'); 'f' -> result.append('\u000C')
                    'u' -> {
                        require(at+4 <= text.length)
                        val hex = text.substring(at,at+4); require(hex.matches(Regex("[0-9a-fA-F]{4}")))
                        result.append(hex.toInt(16).toChar()); at += 4
                    }
                    else -> error("Escape JSON inválido.")
                }
            }
            error("Cadena JSON incompleta.")
        }
    }
}