package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class ModelCatalogExtrasTest {
    private val smallGguf = listOf("qwen2.5-0.5b-instruct-q4_k_m.gguf", "qwen2.5-1.5b-instruct-q3_k_m.gguf")

    @Test fun smallGgufFilesArePinnedAndDownloadableAsChat() {
        smallGguf.forEach { file ->
            val model = ModelCatalog.entries.single { it.file == file }
            assertEquals("Chat", model.category)
            assertTrue(model.sha256.matches(Regex("[0-9a-f]{64}")))
            assertTrue(model.revision.matches(Regex("[0-9a-f]{40}")))
            assertTrue(model.url.startsWith("https://huggingface.co/${model.repo}/resolve/${model.revision}/$file"))
            assertTrue(model.bytes in 1L..AgentPolicy.MAX_MODEL_BYTES)
            assertTrue(model.warning.contains("No probado") || model.warning.contains("no probado"))
        }
    }

    @Test fun smallerModelsAreActuallySmallerThanStarter() {
        val starter = StarterModel.entry.bytes
        smallGguf.forEach { file ->
            val bytes = ModelCatalog.entries.single { it.file == file }.bytes
            assertTrue(bytes < starter)
        }
        assertTrue(ModelCatalog.entries.single { it.file == smallGguf[0] }.bytes < starter / 2)
    }

    @Test fun imageAndVideoAreMarkedAsExternalDownloads() {
        val visual = ModelCatalog.entries.filter { it.category != "Chat" }
        assertTrue(visual.isNotEmpty())
        assertTrue(visual.all { it.category.contains("descarga externa") })
        assertTrue(visual.all { it.warning.contains("motor local") || it.warning.contains("Descarga externa") })
        assertTrue(visual.all { !it.warning.contains("Sakura genera") })
    }

    @Test fun catalogHasNoDuplicateFilesOrUrls() {
        assertEquals(ModelCatalog.entries.size, ModelCatalog.entries.map { it.file }.distinct().size)
        assertEquals(ModelCatalog.entries.size, ModelCatalog.entries.map { it.url }.distinct().size)
        assertTrue(ModelCatalog.entries.all { it.bytes > 0 && it.card.startsWith("https://huggingface.co/") })
    }
}
