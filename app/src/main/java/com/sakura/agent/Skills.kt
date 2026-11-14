package com.sakura.agent

import java.net.URLEncoder
import java.text.Normalizer

data class SakuraSkill(val name: String, val page: String, val instruction: String = "")
data class SearchTask(val kind: String, val query: String)

/** Curated capabilities inside the Android app; not executable model tools. */
object Skills {
    val entries = listOf(
        SakuraSkill("Programar", "Chat", "Ayúdame a programar; pide lenguaje y requisitos si faltan."),
        SakuraSkill("Depurar", "Chat", "Revisa este código, explica errores y propón un parche sin ejecutarlo."),
        SakuraSkill("Revisar seguridad", "Chat", "Revisa riesgos del fragmento y distingue hallazgos de hipótesis."),
        SakuraSkill("Resumir", "Chat", "Resume el texto siguiente sin inventar datos."),
        SakuraSkill("Traducir", "Chat", "Traduce este texto; pregunta el idioma de destino si falta."),
        SakuraSkill("Aprender", "Chat", "Explícame este tema paso a paso con ejemplos y un ejercicio."),
        SakuraSkill("Redactar", "Chat", "Ayúdame a redactar este texto; no envíes nada."),
        SakuraSkill("Comparar", "Chat", "Compara las opciones con criterios explícitos y señala datos faltantes."),
        SakuraSkill("Agente autónomo con herramientas", "Agente"),
        SakuraSkill("Buscar, leer y resumir webs", "Agente"),
        SakuraSkill("YouTube: buscar audio/video", "Agente"),
        SakuraSkill("Generar imagen (Pollinations)", "Agente"),
        SakuraSkill("Planificar", "Plan"), SakuraSkill("Buscar web", "Buscar"),
        SakuraSkill("Buscar música / YouTube", "Buscar"), SakuraSkill("Buscar juegos oficiales", "Buscar"),
        SakuraSkill("Descargar archivo autorizado", "Descargas"), SakuraSkill("Leer código / APK", "Herramientas"),
        SakuraSkill("Abrir e interactuar con apps", "Pantalla"), SakuraSkill("GitHub con revisión", "GitHub"),
        SakuraSkill("Memoria privada", "Memoria"), SakuraSkill("Modelos locales", "Modelos"),
        SakuraSkill("Imagen / video en nube", "Crear"), SakuraSkill("Mensajes revisados", "Mensajes"),
        SakuraSkill("Proveedores de texto", "APIs")
    )
    fun normalized(value: String) = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
    fun task(input: String): SearchTask {
        require(NetworkPolicy.text(input,500)) { "Consulta requerida, máximo 500 caracteres." }
        val plain = normalized(input)
        val kind = when {
            listOf("youtube", "cancion", "musica", "video", "mana").any { it in plain } -> "YouTube"
            listOf("juego", "play store").any { it in plain } -> "Juegos"
            else -> "Web"
        }
        val query = input.trim().replace(Regex("^(busca(me)?|búsca(me)?|buscar)\\s+",RegexOption.IGNORE_CASE),"")
        return SearchTask(kind,query)
    }
    fun searchUrl(kind: String, query: String): String {
        require(NetworkPolicy.text(query,500))
        val q = URLEncoder.encode(query,"UTF-8")
        return when(kind) {
            "YouTube" -> "https://www.youtube.com/results?search_query=$q"
            "Juegos" -> "https://play.google.com/store/search?q=$q&c=apps"
            "Web" -> "https://search.brave.com/search?q=$q"
            else -> throw IllegalArgumentException("Destino desconocido.")
        }
    }
}

/** Restrictive allowlist: app automation is not a general arbitrary-package capability. */
object ScreenPolicy {
    val packages = linkedMapOf("YouTube" to "com.google.android.youtube", "Chrome" to "com.android.chrome",
        "Firefox" to "org.mozilla.firefox", "Play Store" to "com.android.vending")
    private val blocked = Regex("send|enviar|pagar|pay|comprar|buy|purchase|delete|borrar|eliminar|install|instalar|permitir|allow|password|contrasena|sign.?in|iniciar sesion|subscribe|suscrib|aceptar|accept|confirm|confirmar|checkout|login|log.?in|cerrar sesion|logout|transfer|autoriz|guardar|save|publicar|publish|post|coment|comment|reply|responder")
    fun safe(label: String, password: Boolean): Boolean = !password && label.isNotBlank() &&
        label.length <= 300 && !blocked.containsMatchIn(Skills.normalized(label))
    fun searchField(label: String): Boolean = safe(label,false) &&
        Regex("search|buscar|busqueda|query").containsMatchIn(Skills.normalized(label))
    fun click(label: String): Boolean = safe(label,false) &&
        Regex("^(search|buscar|busqueda|search youtube|buscar en youtube|search in|buscar en|back|atras|close|cerrar)( .*)?$")
            .matches(Skills.normalized(label.trim()))
    private val sensitive = setOf("com.android.settings", "com.google.android.packageinstaller",
        "com.android.packageinstaller", "com.android.permissioncontroller",
        "com.google.android.permissioncontroller", "com.google.android.gms")
    fun generalPackage(value: String): Boolean = value.length in 3..200 &&
        value.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")) &&
        value != "com.sakura.agent" && value !in sensitive
}