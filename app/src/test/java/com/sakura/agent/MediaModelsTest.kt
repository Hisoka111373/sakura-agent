package com.sakura.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaModelsTest {
    @Test fun onlineModelsNeedAKey() {
        assertEquals(3, MediaModels.online().size)
        assertTrue(MediaModels.online().all { it.credential in NetworkPolicy.credentialKeys })
    }

    @Test fun localSectionIsHonest() {
        assertEquals(1, MediaModels.localText().size)
        assertTrue(MediaModels.entries.none { it.kind in listOf("Imagen local", "Vídeo local") })
    }
}
