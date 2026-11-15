package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class AgentIntegrationTest {
    private fun rejects(raw: String) {
        try { ToolCallParser.parse(raw); fail("Should reject: $raw") } catch(_: IllegalArgumentException) { } catch(_: IllegalStateException) { }
    }
    @Test fun validOneTool() { assertEquals(ParsedAction.Call("web_search",mapOf("query" to "Kotlin")),ToolCallParser.parse("""{"tool":"web_search","args":{"query":"Kotlin"}}""")) }
    @Test fun emptyArgumentsOnlyForModelList() { assertTrue(ToolCallParser.parse("""{"tool":"model_list","args":{}}""") is ParsedAction.Call); rejects("""{"tool":"web_search","args":{}}""") }
    @Test fun finalAndPlainTextHaveNoTool() { assertTrue(ToolCallParser.parse("""{"final":"Falta el destinatario."}""") is ParsedAction.Final); assertTrue(ToolCallParser.parse("Necesito datos.") is ParsedAction.Final) }
    @Test fun duplicateRootRejected() { rejects("""{"tool":"model_list","tool":"web_search","args":{}}""") }
    @Test fun duplicateArgRejected() { rejects("""{"tool":"web_search","args":{"query":"x","query":"y"}}""") }
    @Test fun noLooseJson() { rejects("{tool:'model_list',args:{}}"); rejects("""{"tool":"model_list","args":{},}""") }
    @Test fun noMixedOrMultipleJson() { rejects("Haz esto: {\"tool\":\"model_list\",\"args\":{}}"); rejects("""{"tool":"model_list","args":{}} {"final":"x"}"""); rejects("```json\n{\"tool\":\"model_list\",\"args\":{}}\n```") }
    @Test fun noCoercionOrNestedArgs() { rejects("""{"tool":"web_search","args":{"query":123}}"""); rejects("""{"tool":"web_search","args":{"query":{"x":"y"}}}"""); rejects("""{"tool":"web_search","args":{"query":null}}""") }
    @Test fun noExtraFieldsOrSecrets() { rejects("""{"tool":"web_search","args":{"query":"x","api_key":"secret"}}"""); rejects("""{"tool":"model_list","args":{},"confirm":"yes"}""") }
    @Test fun noShellInstallOrUnknown() { listOf("shell","install_apk","merge_pr","youtube_streams","unknown").forEach { rejects("""{"tool":"$it","args":{}}""") }; assertTrue(ToolCallParser.parse("""{"tool":"list_apps","args":{}}""") is ParsedAction.Call) }
    @Test fun noSensitiveScreenPackages() { rejects("""{"tool":"screen_start","args":{"package":"com.whatsapp","query":"hola"}}""") }
    @Test fun exactCatalogModelRequired() { rejects("""{"tool":"download_model","args":{"name":"Qwen"}}"""); val file = ModelCatalog.entries.first().file; assertTrue(ToolCallParser.parse("""{"tool":"download_model","args":{"name":"$file"}}""") is ParsedAction.Call) }
    @Test fun noDownloadStreamsIncludingDotAndCase() { listOf("youtube.com","www.youtube.com","youtu.be","r1.googlevideo.com","pipedapi.example.com","WWW.YOUTUBE.COM.").forEach { host -> rejects("""{"tool":"download_url","args":{"url":"https://$host/media.mp4","kind":"Video"}}""") } }
    @Test fun customGgufNeedsPublishedHash() { assertFalse(NetworkPolicy.ggufHash("abc")); assertFalse(NetworkPolicy.ggufHash("A".repeat(64))); assertTrue(NetworkPolicy.ggufHash("a".repeat(64))); rejects("""{"tool":"download_gguf","args":{"url":"https://example.com/model.gguf","sha256":"inventado"}}""") }
    @Test fun noControlOrLongArguments() { rejects("""{"tool":"web_search","args":{"query":"\u0000"}}"""); rejects("""{"tool":"web_search","args":{"query":"${"a".repeat(501)}"}}""") }
    @Test fun escapeStrings() { assertEquals("una \"frase\"\nlínea",(ToolCallParser.parse("""{"final":"una \"frase\"\nlínea"}""") as ParsedAction.Final).text) }
    @Test fun boundedContextNoImplicitMemory() { val result=AgentTools.context("Mi objetivo",List(20) { "obs$it:"+"x".repeat(3000) }); assertTrue(result.length < 2200); assertFalse(result.contains("obs0:")); assertTrue(result.contains("obs19:")); assertTrue(AgentTools.prompt.length <= 2500); assertEquals(10,AgentTools.MAX_STEPS) }
    @Test fun cloudPreviewUsesSameProtocol() { val body=TextProtocol.payload(TextConfig("Groq","test-model"),listOf(ChatMessage(AgentTools.context("objetivo",listOf("fragmento privado")),false)),AgentTools.prompt); assertTrue(body.toString().contains("fragmento privado")); assertFalse(body.toString().contains("Authorization")); assertEquals(768,body.getInt("max_tokens")) }
    @Test fun encryptedProviderKeysAndFixedEndpoints() { val hosts=mapOf("Groq" to "api.groq.com","OpenRouter" to "openrouter.ai","DeepSeek" to "api.deepseek.com","Mistral" to "api.mistral.ai","xAI" to "api.x.ai","Together" to "api.together.ai","Fireworks" to "api.fireworks.ai"); hosts.forEach { (provider,host) -> val config=TextConfig(provider,"test/model"); assertEquals(host,NetworkPolicy.https(config.url()).host); assertTrue(config.key in NetworkPolicy.credentialKeys) } }
    @Test fun webTextStripsActiveContentAndBoundsOutput() { assertEquals("Hola & adiós",WebText.plain("<script>fetch('secret')</script><style>x</style><!--comment--><p>Hola &amp; adiós</p>")); assertEquals(12000,WebText.plain("x".repeat(20000)).length) }
    @Test fun parserOversizeRejected() { rejects("a".repeat(25000)); rejects("""{"final":""}""") }
    @Test fun downloadStillAllowsAuthorizedPublicFile() { assertEquals("example.com",NetworkPolicy.directDownload("https://example.com/authorized.mp4").host) }
}