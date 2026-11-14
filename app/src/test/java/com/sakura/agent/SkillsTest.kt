package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class SkillsTest {
    private fun denied(block: () -> Unit) { try { block(); fail("Expected rejection") } catch (_:IllegalArgumentException) {} }
    @Test fun registryHasRealPages() { assertTrue(Skills.entries.size>=20); assertEquals(Skills.entries.size,Skills.entries.map { it.name }.distinct().size) }
    @Test fun manaRoutesToYoutube() { assertEquals("YouTube",Skills.task("busca una canción de Maná").kind) }
    @Test fun gamesRouteToStore() { assertEquals("Juegos",Skills.task("búscame un juego de carreras").kind) }
    @Test fun ordinarySearchStaysWeb() { assertEquals("Web",Skills.task("buscar tutorial Kotlin").kind) }
    @Test fun searchUrlEscapesParameters() { val url=Skills.searchUrl("YouTube","Maná & test?#"); assertTrue(url.contains("%26")); assertFalse(url.contains("?#")) }
    @Test fun rejectsEmptyQuery() { denied { Skills.task(" ") }; denied { Skills.searchUrl("Other","test") } }
    @Test fun restrictedPackages() { assertFalse("com.whatsapp" in ScreenPolicy.packages.values); assertFalse("com.android.settings" in ScreenPolicy.packages.values) }
    @Test fun sensitiveControlsBlocked() { listOf("Enviar mensaje","Pay now","Instalar","Permitir","Contraseña","Confirmar","search comments","Search login").forEach { assertFalse(it,ScreenPolicy.safe(it,false)) }; assertFalse(ScreenPolicy.safe("Search",true)) }
    @Test fun onlySearchClicks() { assertTrue(ScreenPolicy.click("Buscar")); assertFalse(ScreenPolicy.click("Un video")); assertFalse(ScreenPolicy.click("Comprar juego")) }
    @Test fun searchFieldRecognized() { assertTrue(ScreenPolicy.searchField("com.google.android.youtube:id/search_edit_text")); assertFalse(ScreenPolicy.searchField("message_input")) }
    @Test fun compatibleEndpointRestricted() { listOf("http://example.com/v1/chat/completions","https://u:p@example.com/x","https://example.com/x?key=abc","https://127.0.0.1/x").forEach { denied { TextConfig("Compatible","model",it).url() } } }
    @Test fun endpointProtocolAndModelValidated() { denied { TextConfig("Other","a").url() }; denied { TextConfig("Gemini","a?key=abc").url() }; assertEquals("https://api.openai.com/v1/chat/completions",TextConfig("OpenAI","model","https://evil.example/x").url()) }
    @Test fun openAiPayloadBoundedAndNoTools() { val json=TextProtocol.payload(TextConfig("OpenAI","model"),listOf(ChatMessage("Hola",false))); assertEquals(768,json.getInt("max_completion_tokens")); assertFalse(json.has("tools")); assertEquals(2,json.getJSONArray("messages").length()) }
    @Test fun compatibleUsesLegacyLimit() { assertEquals(768,TextProtocol.payload(TextConfig("Compatible","m"),listOf(ChatMessage("x",false))).getInt("max_tokens")) }
    @Test fun geminiRolesAndSystem() { val json=TextProtocol.payload(TextConfig("Gemini","gemini-test"),listOf(ChatMessage("x",false),ChatMessage("y",true),ChatMessage("z",false))); assertTrue(json.has("systemInstruction")); assertEquals("model",json.getJSONArray("contents").getJSONObject(1).getString("role")) }
    @Test fun anthropicSystemOutsideMessages() { val json=TextProtocol.payload(TextConfig("Anthropic","m"),listOf(ChatMessage("x",false))); assertEquals(TextProtocol.system,json.getString("system")); assertEquals(1,json.getJSONArray("messages").length()) }
    @Test fun oversizedCloudHistoryRejected() { denied { TextProtocol.payload(TextConfig("OpenAI","m"),listOf(ChatMessage("x".repeat(24001),false))) } }
    @Test fun parsersTextOnly() { assertEquals("Hola",TextProtocol.answer("OpenAI",JSONObject("""{"choices":[{"message":{"content":"Hola"}}]}"""))); assertEquals("Hola",TextProtocol.answer("Anthropic",JSONObject("""{"content":[{"type":"tool_use","id":"x"},{"type":"text","text":"Hola"}]}"""))) }
    @Test fun geminiThoughtNotDisplayed() { assertEquals("Hola",TextProtocol.answer("Gemini",JSONObject("""{"candidates":[{"content":{"parts":[{"thought":true,"text":"private"},{"text":"Hola"}]}}]}"""))) }
    @Test fun searchFiltersUnsafeUrls() { val rows=SearchProtocol.results(JSONObject("""{"web":{"results":[{"title":"A","url":"https://example.com/x","description":"d"},{"title":"B","url":"javascript:alert(1)"},{"title":"C","url":"http://example.com/x"}]}}""")); assertEquals(1,rows.size) }
    @Test fun audioDoesNotAcceptHtml() { assertNull(NetworkPolicy.mediaExtension("<html>x</html>".toByteArray(),"Audio")); assertEquals("mp3",NetworkPolicy.mediaExtension("ID3test".toByteArray(),"Audio")); assertEquals("flac",NetworkPolicy.mediaExtension("fLaC123".toByteArray(),"Audio")) }
}