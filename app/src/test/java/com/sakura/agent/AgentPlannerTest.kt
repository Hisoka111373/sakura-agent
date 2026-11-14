package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class AgentPlannerTest {
    private fun rejects(text: String) { var failed=false; try { AgentPlanner.parse(text) } catch (_: Exception) { failed=true }; assertTrue(failed) }
    @Test fun parsesOnlyProposalData() {
        val steps=AgentPlanner.parse("""{"steps":[{"action":"TELEGRAM","detail":"Revisar envío","args":{"recipient":"12345","text":"Hola"}}]}""")
        assertEquals(1,steps.size); assertEquals("Hola",steps[0].args["text"])
    }
    @Test fun rejectsUnknownTools() { rejects("""{"steps":[{"action":"SHELL","detail":"run command","args":{}}]}""") }
    @Test fun rejectsSecretArguments() { rejects("""{"steps":[{"action":"TELEGRAM","detail":"Hola","args":{"token":"secret"}}]}""") }
    @Test fun rejectsExtraExecutableFields() { rejects("""{"steps":[{"action":"REVIEW","detail":"Hola","args":{},"execute":true}]}""") }
    @Test fun rejectsEmptyOrLargePlans() { rejects("""{"steps":[]}"""); rejects("""{"steps":["""+List(7) { """{"action":"REVIEW","detail":"Hola","args":{}}""" }.joinToString(",")+"]}") }
    @Test fun roundTripPreservesReviewedText() { val original=listOf(PlanStep("DOWNLOAD","Revisar licencia",mapOf("url" to "https://example.com/image.png","kind" to "Imagen"))); assertEquals(original,AgentPlanner.parse(AgentPlanner.json(original))) }
    @Test fun rejectsMalformedAndNonStringArgs() { rejects("no es JSON"); rejects("""{"steps":[{"action":"REVIEW","detail":"Hola","args":{"text":false}}]}""") }
    @Test fun supportsMarkdownFenceWithoutRunningIt() { val json="""{"steps":[{"action":"REVIEW","detail":"Solicitar datos","args":{}}]}"""; assertEquals("REVIEW",AgentPlanner.parse("```json\n$json\n```")[0].action) }
}