package com.sakura.agent

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AgentV05Test {
    private fun rejected(raw: String) { try { ToolCallParser.parse(raw, AgentTools.names); fail("Expected rejection") } catch (_: Exception) {} }

    @Test fun parsesToolAndFinal() {
        val call = ToolCallParser.parse("""{"tool":"web_search","args":{"query":"anime 2026"}}""", AgentTools.names) as ParsedAction.Call
        assertEquals("web_search", call.name); assertEquals("anime 2026", call.args["query"])
        val final = ToolCallParser.parse("""{"tool":"finish","args":{"text":"Listo"}}""", AgentTools.names) as ParsedAction.Final
        assertEquals("Listo", final.text)
    }

    @Test fun acceptsOpenAiShapeAndFence() {
        val raw = "```json\n{\"name\":\"youtube_search\",\"arguments\":\"{\\\"query\\\":\\\"mana\\\"}\"}\n```"
        val call = ToolCallParser.parse(raw, AgentTools.names) as ParsedAction.Call
        assertEquals("youtube_search", call.name); assertEquals("mana", call.args["query"])
    }

    @Test fun plainTextIsFinalNotAction() {
        val final = ToolCallParser.parse("No necesito herramientas.", AgentTools.names) as ParsedAction.Final
        assertEquals("No necesito herramientas.", final.text)
    }

    @Test fun rejectsUnsafeOrMalformed() {
        rejected("""{"tool":"shell","args":{"cmd":"id"}}""")
        rejected("""{"tool":"web_search","args":{"query":"x"},"execute":true}""")
        rejected("""{"tool":"web_search","args":{"query":"x","api_key":"secret"}}""")
        rejected("""{"tool":"web_search","args":{"query":false}}""")
        rejected(JSONObject().put("tool", "web_search").put("args", JSONObject().put("query", "x".repeat(5000))).toString())
        rejected("{")
    }

    @Test fun catalogIsBounded() {
        assertTrue(AgentTools.names.contains("finish"))
        assertTrue(AgentTools.specs.all { it.args.size <= 4 && it.description.length <= 120 })
        assertTrue(AgentTools.specs.count { it.confirmed } >= 5)
    }

    @Test fun mediaIds() {
        assertEquals("dQw4w9WgXcQ", MediaProtocol.videoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1"))
        assertEquals("abcDEF12345", MediaProtocol.videoId("/watch?v=abcDEF12345"))
        assertNull(MediaProtocol.videoId("https://example.com/video"))
        assertNull(MediaProtocol.videoId("/watch?v=bad id"))
    }

    @Test fun parsesPipedItems() {
        val json = JSONObject("""{"items":[{"url":"/watch?v=abcDEF12345","title":"Cancion","uploaderName":"Canal","duration":210,"type":"stream"},{"url":"/channel/x","title":"Canal"}]}""")
        val items = MediaProtocol.items(json)
        assertEquals(1, items.size); assertEquals("abcDEF12345", items[0].videoId); assertEquals(210, items[0].duration)
    }

    @Test fun parsesStreamsAndMarksCombined() {
        val json = JSONObject("""{
            "audioStreams":[{"url":"https://audio.example/a.m4a?x=1","bitrate":128000,"mimeType":"audio/mp4"}],
            "videoStreams":[
                {"url":"https://video.example/v.mp4?x=1","quality":"720p","mimeType":"video/mp4","videoOnly":false},
                {"url":"https://video.example/v2.mp4?x=2","quality":"1080p","mimeType":"video/mp4","videoOnly":true}
            ]}""")
        val options = MediaProtocol.streams(json)
        assertEquals(3, options.size)
        assertTrue(options.any { it.kind == "Audio" })
        assertEquals(true, options.first { it.quality == "720p" }.combined)
        assertEquals(false, options.first { it.quality == "1080p" }.combined)
    }

    @Test fun stripsHtml() {
        val text = WebText.plain("<html><style>x{}</style><script>alert(1)</script><body><h1>Hola</h1><p>A &amp; B</p></body></html>")
        assertTrue(text.contains("Hola")); assertTrue(text.contains("A & B")); assertFalse(text.contains("alert")); assertFalse(text.contains("<h1>"))
    }

    @Test fun providersUseExpectedHostsAndKeys() {
        assertEquals("https://api.groq.com/openai/v1/chat/completions", TextConfig("Groq", "llama-3.1-8b-instant").url())
        assertEquals("https://openrouter.ai/api/v1/chat/completions", TextConfig("OpenRouter", "openai/gpt-4o-mini").url())
        assertEquals("https://api.deepseek.com/v1/chat/completions", TextConfig("DeepSeek", "deepseek-chat").url())
        assertEquals("openrouter_key", TextConfig("OpenRouter", "x").key)
        assertEquals("xai_key", TextConfig("xAI", "grok-2").key)
        assertFalse(TextProtocol.isOpenAi("Gemini"))
    }

    @Test fun payloadsRespectProtocol() {
        val history = listOf(ChatMessage("hola", false))
        val openAi = TextProtocol.payload(TextConfig("DeepSeek", "deepseek-chat"), history, "sys", 512)
        assertEquals("deepseek-chat", openAi.getString("model"))
        assertEquals(512, openAi.getInt("max_tokens"))
        val gemini = TextProtocol.payload(TextConfig("Gemini", "gemini-2.0-flash"), history, "sys", 512)
        assertEquals("user", gemini.getJSONArray("contents").getJSONObject(0).getString("role"))
        val anthropic = TextProtocol.payload(TextConfig("Anthropic", "claude-3-5-haiku-latest"), history, "sys", 512)
        assertEquals(512, anthropic.getInt("max_tokens"))
    }

    @Test fun blocksYoutubeStreamDownloads() {
        listOf("https://rr3---sn-x.googlevideo.com/videoplayback?id=1",
            "https://www.youtube.com/watch?v=x", "https://youtu.be/x", "https://i.ytimg.com/x.jpg",
            "https://pipedproxy.kavin.rocks/stream/audio")
            .forEach { url -> try { NetworkPolicy.directDownload(url); fail("Expected rejection: $url") } catch (_: Exception) {} }
        assertEquals("https://example.com/a.mp4", NetworkPolicy.directDownload("https://example.com/a.mp4").toString())
    }
}
