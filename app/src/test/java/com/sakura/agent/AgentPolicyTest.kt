package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class AgentPolicyTest {
    @Test fun acceptsExpectedRepository() {
        assertTrue(AgentPolicy.validRepo("Hisoka111373", "sakura-agent"))
        assertTrue(AgentPolicy.validRepo("org-name", "repo.kt"))
    }
    @Test fun rejectsRepositoryInjection() {
        listOf("../owner", "owner/name", "owner?x=1", "", "owner\n").forEach {
            assertFalse(AgentPolicy.validRepo(it, "repo"))
        }
        assertFalse(AgentPolicy.validRepo("owner", ".."))
    }
    @Test fun acceptsNestedSourcePaths() {
        assertTrue(AgentPolicy.validPath("app/src/main/java/MainActivity.kt"))
        assertTrue(AgentPolicy.validPath("carpeta/código ejemplo.py"))
    }
    @Test fun rejectsPathTraversal() {
        listOf("../secret", "/absolute", "a/../b", "a//b", "a/./b", "a\\b", "", "a\nb").forEach {
            assertFalse("path: $it", AgentPolicy.validPath(it))
        }
    }
    @Test fun validatesGgufMagicAndVersion() {
        assertTrue(AgentPolicy.validGguf(byteArrayOf(71, 71, 85, 70, 3, 0, 0, 0)))
        assertTrue(AgentPolicy.validGguf(byteArrayOf(71, 71, 85, 70, 2, 0, 0, 0)))
        assertFalse(AgentPolicy.validGguf(byteArrayOf(71, 71, 85, 70, 4, 0, 0, 0)))
        assertFalse(AgentPolicy.validGguf(byteArrayOf(71, 71, 85, 70, 3, 1, 0, 0)))
        assertFalse(AgentPolicy.validGguf("not-gguf".toByteArray()))
        assertFalse(AgentPolicy.validGguf(byteArrayOf()))
    }
    @Test fun defaultLevelAndNoAutomaticActions() {
        val prompt = AgentPolicy.systemPrompt("INVALID", false, emptyList())
        assertTrue(prompt.contains("Principiante"))
        assertTrue(prompt.contains("Evita bromas"))
        assertTrue(prompt.contains("No ejecutas código"))
        assertTrue(prompt.contains("Ninguna."))
    }
    @Test fun memoryContextIsLimitedAndExplicit() {
        val notes = (0..19).map { Memory("$it", "Proyecto", "nota-$it") }
        val prompt = AgentPolicy.systemPrompt("Avanzado", true, notes)
        assertTrue(prompt.contains("nota-19"))
        assertFalse(prompt.contains("nota-0\n"))
        assertFalse(prompt.contains("nota-7\n"))
        assertEquals(12, prompt.lineSequence().count { it.trim().startsWith("- Proyecto:") })
        assertTrue(prompt.contains("No inventes recuerdos"))
    }
    @Test fun truncatesLongContextNotes() {
        val prompt = AgentPolicy.systemPrompt("Intermedio", true, listOf(Memory("1", "Avance", "x".repeat(500))))
        assertTrue(prompt.contains("x".repeat(240)))
        assertFalse(prompt.contains("x".repeat(241)))
    }
}
