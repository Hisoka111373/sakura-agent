package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class ThinkingPolicyTest {
    private fun gemini(effort: String) = TextProtocol.payload(TextConfig("Gemini","gemini-3.1-flash-lite",effort=effort),listOf(ChatMessage("Prueba",false)))
    @Test fun localOutputBounds() { assertEquals(160,ThinkingPolicy.localTokens("Bajo")); assertEquals(384,ThinkingPolicy.localTokens("Alto")) }
    @Test fun cooperativeTimeLimits() { assertEquals(45000L,ThinkingPolicy.localTimeoutMs("Bajo")); assertEquals(90000L,ThinkingPolicy.localTimeoutMs("Alto")) }
    @Test fun lessHistoryInFastMode() { assertEquals(2,ThinkingPolicy.chatHistory("Bajo")); assertEquals(6,ThinkingPolicy.chatHistory("Alto")) }
    @Test fun fewerMemoryNotes() { assertEquals(4,ThinkingPolicy.memories("Bajo")); assertEquals(12,ThinkingPolicy.memories("Alto")) }
    @Test fun geminiMinimalUsesEnoughVisibleBudget() { val c=gemini("Bajo").getJSONObject("generationConfig"); assertEquals(512,c.getInt("maxOutputTokens")); assertEquals("MINIMAL",c.getJSONObject("thinkingConfig").getString("thinkingLevel")) }
    @Test fun geminiHighHasSeparateBound() { val c=gemini("Alto").getJSONObject("generationConfig"); assertEquals(2048,c.getInt("maxOutputTokens")); assertEquals("HIGH",c.getJSONObject("thinkingConfig").getString("thinkingLevel")) }
    @Test fun unknownModelsNotGivenGuessedThinking() { val c=TextProtocol.payload(TextConfig("Gemini","gemini-test",effort="Alto"),listOf(ChatMessage("x",false))).getJSONObject("generationConfig"); assertFalse(c.has("thinkingConfig")); assertNull(ThinkingPolicy.geminiThinking("gemini-3.1-flash-lite-image","Bajo")) }
    @Test fun cloudEffortAddsNoToolsOrKeys() { val body=gemini("Alto"); assertFalse(body.has("tools")); assertFalse(body.toString().contains("Authorization")); assertFalse(body.toString().contains("gemini_key")) }
    @Test fun compatibleHighUsesBoundedLegacyLimit() { assertEquals(1024,TextProtocol.payload(TextConfig("Compatible","model",effort="Alto"),listOf(ChatMessage("x",false))).getInt("max_tokens")) }
    @Test(expected=IllegalArgumentException::class) fun invalidEffortRejected() { TextProtocol.payload(TextConfig("Gemini","model",effort="Unlimited"),listOf(ChatMessage("x",false))) }
    @Test fun effortGuidanceDoesNotExposeThoughts() { assertTrue(ThinkingPolicy.guidance("Alto").contains("sin revelar razonamiento interno")); assertTrue(ThinkingPolicy.guidance("Bajo").contains("no omitas advertencias")) }
    @Test fun kawaiiKeepsSafetyAndAdultTone() { assertTrue(ThinkingPolicy.kawaii.contains("adulta")); assertTrue(ThinkingPolicy.kawaii.contains("No infantilices")); assertTrue(ThinkingPolicy.kawaii.contains("precisión")) }
    @Test fun oldCloudDefaultsCompatible() { assertEquals(768,ThinkingPolicy.cloudTokens(TextConfig("OpenAI","model"))); assertEquals("Bajo",TextConfig("Local","").effort) }
    @Test fun agentSystemStillFitsCloudGuard() { ThinkingPolicy.levels.forEach { assertTrue((AgentTools.prompt+ThinkingPolicy.guidance(it)).length<=2500) } }
    @Test fun localPromptKeepsGreetingAndMarksTrimmedSystem() {
        val system = "Eres Sakura. ".repeat(1000) + ThinkingPolicy.guidance("Bajo")
        val (fitted, history) = ThinkingPolicy.fitLocalPrompt("Bajo", system, listOf(ChatMessage("hola", false)))
        assertEquals("hola", history.single().text)
        assertTrue(fitted.startsWith("Eres Sakura."))
        assertTrue(fitted.contains(ThinkingPolicy.localPromptTrimmed))
        assertTrue(fitted.length <= ThinkingPolicy.localPromptChars("Bajo"))
    }
    @Test fun localPromptDropsOldestHistoryFirstDeterministically() {
        val history = (1..8).map { ChatMessage("m$it:" + "x".repeat(1500), it % 2 == 0) }
        val first = ThinkingPolicy.fitLocalPrompt("Bajo", "S", history)
        assertEquals(first, ThinkingPolicy.fitLocalPrompt("Bajo", "S", history))
        assertFalse(first.second.any { it.text.contains("m6:") })
        assertTrue(first.second.last().text.contains("m8:"))
        assertTrue(first.second.size <= ThinkingPolicy.chatHistory("Bajo") + 1)
    }
    @Test fun localPromptNeverExceedsEstimatedContext() {
        val notes = (1..20).map { Memory("$it", "Proyecto", "nota".repeat(100)) }
        val system = AgentPolicy.systemPrompt("Principiante", true, ThinkingPolicy.localMemories(notes, "Alto")) +
            ThinkingPolicy.guidance("Alto") + ThinkingPolicy.kawaii
        val history = (1..12).map { ChatMessage("y".repeat(4000), it % 2 == 0) } + ChatMessage("hola", false)
        val (fittedSystem, fittedHistory) = ThinkingPolicy.fitLocalPrompt("Alto", system, history)
        val totalChars = fittedSystem.length + fittedHistory.sumOf { it.text.length }
        assertTrue(totalChars <= ThinkingPolicy.localPromptChars("Alto"))
        assertTrue(totalChars / 3 + 64 + ThinkingPolicy.localTokens("Alto") < ThinkingPolicy.localContextTokens)
        assertEquals("hola", fittedHistory.last().text)
    }
    @Test fun localMemoriesAreCappedWithoutChangingTheirOrder() {
        val notes = (1..20).map { Memory("$it", "Proyecto", "n".repeat(500)) }
        val local = ThinkingPolicy.localMemories(notes, "Bajo")
        assertEquals(ThinkingPolicy.memories("Bajo"), local.size)
        assertTrue(local.all { it.text.length <= 120 })
        assertEquals(notes.last().id, local.last().id)
    }
    @Test fun continueNeedsAnAnswerAndAnEmptyDraft() {
        val answer = listOf(ChatMessage("Respuesta", true))
        assertTrue(CompanionPolicy.canContinue(answer, ""))
        assertFalse(CompanionPolicy.canContinue(answer, "borrador"))
        assertFalse(CompanionPolicy.canContinue(listOf(ChatMessage("Pregunta", false)), ""))
    }
}