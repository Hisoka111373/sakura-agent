package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

/** Pure JVM checks for the chat-first, multimedia and programmer features added in 0.12.0. */
class Sakura012Test {
    private fun rejects(block: () -> Unit) {
        try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) {}
    }

    @Test fun chatRouterProposesSupervisedActionsWithoutExecutingThem() {
        val search = ChatRouter.detect("busca documentación de Kotlin") as? ChatAction.Search
        assertNotNull(search)
        assertEquals("documentación de Kotlin", search!!.query)
        assertFalse(search.video)

        val read = ChatRouter.detect("lee README.md de Hisoka111373/sakura-agent") as? ChatAction.GithubRead
        assertNotNull(read)
        assertEquals("README.md", read!!.path)
        assertEquals("Hisoka111373", read.owner)

        val edit = ChatRouter.detect("cambia Sakura por Sakura Agent en README.md de Hisoka111373/sakura-agent") as? ChatAction.GithubEdit
        assertNotNull(edit)
        assertEquals("Sakura", edit!!.find)
        assertEquals("Sakura Agent", edit.replacement)

        val download = ChatRouter.detect("descarga whisper tiny") as? ChatAction.DownloadModel
        assertNotNull(download)
        assertEquals("ggml-tiny.bin", download!!.file)
        assertEquals("sd_turbo.safetensors", (ChatRouter.detect("descarga el modelo sd turbo") as ChatAction.DownloadModel).file)

        assertTrue(ChatRouter.detect("abre youtube") is ChatAction.OpenApp)
        assertTrue(ChatRouter.detect("recuerda probar la apk") is ChatAction.MemoryNote)
        assertNull(ChatRouter.detect("hola, ¿cómo estás?"))
    }

    @Test fun moreVerifiedModelsAreDownloadableFromTheCatalog() {
        assertTrue(ModelCatalog.visual().size >= 6)
        assertTrue(ModelCatalog.audio().size >= 3)
        assertTrue(ModelCatalog.audio().all { it.sha256.matches(Regex("[0-9a-f]{64}")) })
        assertTrue(ModelCatalog.visual().all { it.sha256.matches(Regex("[0-9a-f]{64}")) })
        assertTrue(ModelCatalog.entries.first { it.file == "sd_turbo.safetensors" }.bytes > 5_000_000_000L)
        assertEquals(16_000_000_000L, NetworkPolicy.MAX_VISUAL_MODEL_BYTES)
    }

    @Test fun programmerToolsValidateExactArguments() {
        assertEquals(setOf("owner","repo","branch","path","content","message"), AgentTools.fields["github_write"])
        assertEquals(setOf("owner","repo","branch","path","content","message"), AgentTools.fields["github_create"])
        assertEquals(setOf("owner","repo","branch","path","message"), AgentTools.fields["github_delete"])
        assertEquals(setOf("owner","repo","from","branch"), AgentTools.fields["github_branch"])
        assertEquals(setOf("owner","repo","head","base","title","body"), AgentTools.fields["github_pr"])

        AgentTools.validate(ParsedAction.Call("github_read", mapOf(
            "owner" to "Hisoka111373", "repo" to "sakura-agent", "branch" to "main", "path" to "README.md")))
        AgentTools.validate(ParsedAction.Call("github_write", mapOf(
            "owner" to "Hisoka111373", "repo" to "sakura-agent", "branch" to "feature/sakura",
            "path" to "src/Main.kt", "content" to "fun main() {}", "message" to "Sakura: archivo revisado")))
        AgentTools.validate(ParsedAction.Call("github_delete", mapOf(
            "owner" to "Hisoka111373", "repo" to "sakura-agent", "branch" to "feature/sakura",
            "path" to "tmp.txt", "message" to "Sakura: borrar temporal")))
        AgentTools.validate(ParsedAction.Call("github_branch", mapOf(
            "owner" to "Hisoka111373", "repo" to "sakura-agent", "from" to "main", "branch" to "feature/sakura")))
        AgentTools.validate(ParsedAction.Call("github_pr", mapOf(
            "owner" to "Hisoka111373", "repo" to "sakura-agent", "head" to "feature/sakura", "base" to "main",
            "title" to "Cambios revisados", "body" to "Generado con supervisión.")))
        rejects { AgentTools.validate(ParsedAction.Call("github_read", mapOf(
            "owner" to "o", "repo" to "r", "branch" to "main", "path" to "README.md", "message" to "x"))) }
        rejects { AgentTools.validate(ParsedAction.Call("github_branch", mapOf(
            "owner" to "o", "repo" to "r", "from" to "main", "branch" to "main"))) }
        rejects { AgentTools.validate(ParsedAction.Call("github_pr", mapOf(
            "owner" to "o", "repo" to "r", "head" to "main", "base" to "main", "title" to "x", "body" to ""))) }
    }

    @Test fun promptExplainsReadOnlyGithubAndStillFitsCloudGuard() {
        assertTrue(AgentTools.prompt.contains("NUNCA escriben commits"))
        assertTrue(AgentTools.prompt.contains("github_write"))
        assertTrue(AgentTools.prompt.contains("github_pr"))
        ThinkingPolicy.levels.forEach { assertTrue((AgentTools.prompt + ThinkingPolicy.guidance(it)).length <= 2500) }
    }

    @Test fun existingMascotGainsToolFocusWithoutANewCharacter() {
        assertTrue(CompanionPolicy.events.containsAll(listOf("Buscando", "Leyendo")))
        assertEquals("buscando", MascotExpressions.mood("Buscando"))
        assertEquals("leyendo", MascotExpressions.mood("Leyendo"))
        assertTrue(CompanionPolicy.phrases("Buscando").all { it.isNotBlank() && it.length < 160 })
        assertTrue(CompanionPolicy.phrases("Leyendo").all { it.isNotBlank() && it.length < 160 })
        assertFalse(MascotExpressions.happy("Buscando"))
    }
}
