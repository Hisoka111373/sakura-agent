package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class Sakura010Test {
    private fun rejects(block: () -> Unit) { try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) {} }

    @Test fun expandedSkillLibraryKeepsUniqueNames() {
        assertTrue(Skills.entries.size >= 140)
        assertEquals(Skills.entries.size, Skills.entries.map { it.name }.toSet().size)
        assertTrue(Skills.entries.filter { it.page == "Chat" }.all { it.instruction.length <= 1000 })
    }

    @Test fun newSkillsAreDetectedLocally() {
        assertEquals("Modo local rápido", Skills.textSkill("activa el modo rapido")?.name)
        assertEquals("Mascota Sakura viva", Skills.textSkill("muestra la mascota")?.name)
        assertEquals("Abrir app con revisión", Skills.textSkill("abrir app")?.name)
    }

    @Test fun agentGainsReadOnlyGithubAndVideoShortcuts() {
        assertEquals(setOf("owner", "repo", "branch"), AgentTools.fields["github_tree"])
        assertEquals(setOf("owner", "repo", "branch", "path"), AgentTools.fields["github_open"])
        assertEquals(setOf("query"), AgentTools.fields["video_search"])
        assertEquals(setOf("query"), AgentTools.fields["browser_search"])
        assertEquals(setOf("owner", "repo", "branch", "path", "find", "replacement", "message"), AgentTools.fields["github_edit"])
        assertEquals(setOf("owner", "repo"), AgentTools.fields["github_issues"])
        assertEquals(setOf("name"), AgentTools.fields["open_app_named"])
        assertTrue(AgentTools.prompt.contains("github_tree"))
        assertTrue(AgentTools.prompt.contains("github_edit"))
        assertTrue(AgentTools.prompt.contains("open_app_named"))
        assertTrue(AgentTools.prompt.contains("NUNCA escriben commits"))
        assertTrue(AgentTools.prompt.length <= 2500)
    }

    @Test fun agentCanProposeReviewedEditAndNamedApps() {
        AgentTools.validate(ParsedAction.Call("github_edit", mapOf(
            "owner" to "Hisoka111373", "repo" to "sakura-agent", "branch" to "main",
            "path" to "README.md", "find" to "Sakura", "replacement" to "Sakura Agent", "message" to "docs: rename")))
        AgentTools.validate(ParsedAction.Call("github_issues", mapOf("owner" to "Hisoka111373", "repo" to "sakura-agent")))
        AgentTools.validate(ParsedAction.Call("open_app_named", mapOf("name" to "explorador")))
        rejects { AgentTools.validate(ParsedAction.Call("github_edit", mapOf(
            "owner" to "o", "repo" to "r", "branch" to "main", "path" to "README.md",
            "find" to "Sakura", "replacement" to "x", "message" to ""))) }
        rejects { AgentTools.validate(ParsedAction.Call("github_edit", mapOf(
            "owner" to "o", "repo" to "r", "branch" to "main", "path" to "../x",
            "find" to "Sakura", "replacement" to "x", "message" to "m"))) }
        rejects { AgentTools.validate(ParsedAction.Call("open_app_named", mapOf("name" to "whatsapp"))) }
        assertEquals("com.android.chrome", ScreenPolicy.apps.getValue("explorador"))
        assertEquals("com.google.android.youtube", ScreenPolicy.apps.getValue("youtube"))
    }

    @Test fun newAgentToolsValidateWithoutCredentials() {
        AgentTools.validate(ParsedAction.Call("github_tree", mapOf("owner" to "Hisoka111373", "repo" to "sakura-agent", "branch" to "main")))
        AgentTools.validate(ParsedAction.Call("github_open", mapOf("owner" to "Hisoka111373", "repo" to "sakura-agent", "branch" to "main", "path" to "README.md")))
        AgentTools.validate(ParsedAction.Call("video_search", mapOf("query" to "Maná")))
        AgentTools.validate(ParsedAction.Call("browser_search", mapOf("query" to "kotlin")))
        rejects { AgentTools.validate(ParsedAction.Call("github_tree", mapOf("owner" to "..", "repo" to "sakura-agent", "branch" to "main"))) }
        rejects { AgentTools.validate(ParsedAction.Call("github_open", mapOf("owner" to "o", "repo" to "r", "branch" to "main", "path" to "../secreto"))) }
        rejects { AgentTools.validate(ParsedAction.Call("video_search", mapOf("query" to ""))) }
        rejects { AgentTools.validate(ParsedAction.Call("browser_search", mapOf("query" to "a".repeat(501)))) }
        rejects { AgentTools.validate(ParsedAction.Call("github_open", mapOf("owner" to "o", "repo" to "r", "branch" to "main", "path" to "README.md", "token" to "x"))) }
    }

    @Test fun searchShortcutsUseSafeEncodedUrls() {
        assertTrue(Skills.searchUrl("YouTube", "Maná & test").contains("%26"))
        assertTrue(Skills.searchUrl("Web", "café + tilde").contains("%2B"))
        rejects { Skills.searchUrl("Web", " ") }
    }

    @Test fun mascotExpressionsAreBoundedAndDeterministic() {
        assertFalse(MascotExpressions.blink(0f))
        assertTrue(MascotExpressions.blink(0.78f))
        assertEquals("saludando", MascotExpressions.mood("Saludo"))
        assertEquals("preocupada", MascotExpressions.mood("Error"))
        assertEquals("contigo", MascotExpressions.mood("desconocido"))
        assertEquals(MascotExpressions.gazeX(0.25f), MascotExpressions.gazeX(0.25f))
        assertTrue(abs(MascotExpressions.gazeX(0.25f)) <= 0.30f)
        assertTrue(abs(MascotExpressions.gazeY(0.25f)) <= 0.10f)
        assertTrue(abs(MascotExpressions.handWave(0.25f, false)) <= 1f)
        assertTrue(abs(MascotExpressions.handWave(0.25f, true)) <= 0.35f)
        assertTrue(MascotExpressions.happy("Saltito"))
        assertFalse(MascotExpressions.happy("Error"))
    }

    @Test fun mouthReflectsSpeechWithoutClaimingLipSync() {
        val speaking = (0 until 20).map { MascotExpressions.mouthOpen(it / 20f, true) }
        assertTrue(speaking.all { it >= 0.45f && it <= 1f })
        assertTrue(speaking.maxOrNull()!! > speaking.minOrNull()!!)
        val idle = MascotExpressions.mouthOpen(0.4f, false)
        assertTrue(idle in 0.10f..0.18f)
    }

    @Test fun secondaryMascotMotionStaysBounded() {
        assertTrue(MascotExpressions.blinkWithDouble(0.21f))
        assertTrue(MascotExpressions.blinkWithDouble(0.78f))
        assertFalse(MascotExpressions.blinkWithDouble(0.5f))
        assertTrue(abs(MascotExpressions.hairSway(0.25f, false)) <= 0.07f)
        assertTrue(abs(MascotExpressions.hairSway(0.25f, true)) <= 0.035f)
        assertTrue(MascotExpressions.blush("Saltito", 0.25f) > 0f)
        assertEquals(0f, MascotExpressions.blush("Error", 0.25f), 0f)
        assertEquals(MascotExpressions.pollenAngle(1, 0.5f), MascotExpressions.pollenAngle(1, 0.5f))
    }
}
