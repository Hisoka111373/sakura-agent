package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class ThinkingPolicyTest {
    private fun gemini(effort: String) = TextProtocol.payload(TextConfig("Gemini","gemini-3.1-flash-lite",effort=effort),listOf(ChatMessage("Prueba",false)))
    @Test fun localOutputBounds() { assertEquals(192,ThinkingPolicy.localTokens("Bajo")); assertEquals(384,ThinkingPolicy.localTokens("Alto")) }
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
}