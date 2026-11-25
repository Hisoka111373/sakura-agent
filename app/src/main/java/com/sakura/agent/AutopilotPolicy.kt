package com.sakura.agent

/**
 * Risk gate for the optional supervised autopilot.
 *
 * Autopilot never grants blanket consent: only read-only/local actions may be
 * auto-approved, and only while the user has explicitly enabled the mode.
 * Writes, network calls with cost, installs, messages and GitHub changes always
 * require the normal confirmation panel.
 *
 * Pure Kotlin (no Android imports) so the rules are testable on the JVM.
 */
object AutopilotPolicy {
    enum class Risk { LOW, MEDIUM, HIGH }

    private val low = setOf(
        "model_list", "list_apps", "file_list", "file_read", "github_tree",
        "github_read", "github_open", "github_issues", "free_search", "search"
    )
    private val medium = setOf(
        "web_search", "fetch_text", "browser_search", "video_search", "youtube_search",
        "image_understand", "pdf_create", "open_url", "open_app", "open_app_named",
        "download_model", "download_url", "download_gguf"
    )
    private val high = setOf(
        "github_write", "github_create", "github_edit", "github_delete", "github_branch",
        "github_pr", "telegram", "whatsapp", "image_generate", "video_generate",
        "install_apk", "screen_tap", "screen_type", "memory_save", "memory_delete"
    )

    fun risk(action: String): Risk = when (action) {
        in low -> Risk.LOW
        in medium -> Risk.MEDIUM
        in high -> Risk.HIGH
        else -> Risk.HIGH // unknown actions are never auto-approved
    }

    /**
     * True only for explicitly enabled autopilot + known read-only/local action.
     * MEDIUM/HIGH always return false, so the existing confirmation UI remains.
     */
    fun autoApprove(enabled: Boolean, action: String): Boolean =
        enabled && risk(action) == Risk.LOW

    fun description(action: String): String = when (risk(action)) {
        Risk.LOW -> "Lectura/local: se puede automaticizar si activas el piloto automático."
        Risk.MEDIUM -> "Requiere revisión: usa red, descarga, abre apps o crea archivos."
        Risk.HIGH -> "Requiere confirmación explícita: escribe, envía, instala o puede costar dinero."
    }
}
