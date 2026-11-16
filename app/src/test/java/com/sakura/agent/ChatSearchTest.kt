package com.sakura.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSearchTest {
    @Test fun detectsSpanishAndEnglishSearchRequests() {
        assertTrue(Skills.isSearchRequest("busca una canción de Maná"))
        assertTrue(Skills.isSearchRequest("Búscame recetas de ramen"))
        assertTrue(Skills.isSearchRequest("buscar noticias de anime"))
        assertTrue(Skills.isSearchRequest("encuentra la documentación de Compose"))
        assertTrue(Skills.isSearchRequest("search Kotlin coroutines"))
        assertTrue(Skills.isSearchRequest("investiga sobre llama.cpp"))
    }

    @Test fun doesNotTreatNormalChatAsSearch() {
        assertFalse(Skills.isSearchRequest("explícame Kotlin Flow"))
        assertFalse(Skills.isSearchRequest("resume este texto"))
        assertFalse(Skills.isSearchRequest("buscaremos una solución juntos"))
        assertFalse(Skills.isSearchRequest("a".repeat(501)))
    }

    @Test fun stripsSearchPrefixAndRoutesKinds() {
        val video = Skills.task("Busca una canción de Maná")
        assertEquals("YouTube", video.kind)
        assertEquals("una canción de Maná", video.query)

        val web = Skills.task("busca documentación de Kotlin")
        assertEquals("Web", web.kind)
        assertEquals("documentación de Kotlin", web.query)
    }

    @Test fun formatsSearchResultsWithoutInventingLinks() {
        val empty = Skills.searchSummary("nada", emptyList())
        assertTrue(empty.contains("no devolvió resultados"))

        val summary = Skills.searchSummary("kotlin", listOf(
            SearchResult("Kotlin Docs", "https://kotlinlang.org/docs/", "Documentación oficial"),
            SearchResult("Compose", "https://developer.android.com/jetpack/compose", "")
        ))
        assertTrue(summary.contains("1. Kotlin Docs"))
        assertTrue(summary.contains("https://kotlinlang.org/docs/"))
        assertTrue(summary.contains("2. Compose"))
        assertTrue(summary.contains("No abrí ni descargué"))
    }

    @Test fun starterModelIsPinnedAndRecognized() {
        assertEquals(1_117_320_736L, StarterModel.entry.bytes)
        assertEquals("6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e", StarterModel.entry.sha256)
        assertTrue(StarterModel.matches("uuid_qwen2.5-1.5b-instruct-q4_k_m.gguf"))
        assertFalse(StarterModel.matches("qwen2.5-3b-instruct-q4_k_m.gguf"))
    }

    @Test fun providersHaveEditableModelSuggestions() {
        TextProtocol.providers.filter { it != "Local" }.forEach { provider ->
            assertTrue("Falta sugerencia para $provider", TextProtocol.suggestions[provider].orEmpty().isNotEmpty())
            assertTrue(TextProtocol.firstModel(provider).isNotBlank())
        }
        assertEquals("openai_key", TextConfig("OpenAI", "gpt-4o-mini").key)
        assertTrue(NetworkPolicy.credentialKeys.contains("brave_key"))
    }
}
