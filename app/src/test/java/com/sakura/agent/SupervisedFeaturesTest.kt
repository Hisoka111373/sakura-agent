package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class SupervisedFeaturesTest {
    private fun rejects(block: () -> Unit) { try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) {} }

    @Test fun googleUrlUsesExactEncodedQuery() {
        assertEquals("https://www.google.com/search?q=kotlin",Skills.searchUrl("Google","kotlin"))
        assertEquals("https://www.google.com/search?q=Man%C3%A1+%26+test%3F",Skills.searchUrl("Google","Maná & test?"))
        assertTrue(Skills.searchUrl("Google","café + tilde").contains("%2B"))
        rejects { Skills.searchUrl("Google"," ") }
        rejects { Skills.searchUrl("Google","a".repeat(501)) }
    }

    @Test fun newToolsHaveExactFieldsAndAreDescribed() {
        assertEquals(setOf("query"),AgentTools.fields["google_search"])
        assertEquals(emptySet<String>(),AgentTools.fields["screen_read"])
        assertEquals(setOf("text"),AgentTools.fields["screen_type"])
        assertEquals(emptySet<String>(),AgentTools.fields["file_list"])
        assertEquals(setOf("name"),AgentTools.fields["file_read"])
        assertEquals(emptySet<String>(),AgentTools.fields["apk_open_selected"])
        assertTrue(AgentTools.prompt.contains("google_search"))
        assertTrue(AgentTools.prompt.contains("screen_read"))
        assertTrue(AgentTools.prompt.contains("apk_open_selected"))
        assertTrue(AgentTools.prompt.length <= 2500)
    }

    @Test fun newToolsValidateArguments() {
        AgentTools.validate(ParsedAction.Call("google_search",mapOf("query" to "Maná")))
        AgentTools.validate(ParsedAction.Call("screen_read",emptyMap()))
        AgentTools.validate(ParsedAction.Call("screen_type",mapOf("text" to "hola")))
        AgentTools.validate(ParsedAction.Call("file_list",emptyMap()))
        AgentTools.validate(ParsedAction.Call("file_read",mapOf("name" to "notas.txt")))
        AgentTools.validate(ParsedAction.Call("apk_open_selected",emptyMap()))
        rejects { AgentTools.validate(ParsedAction.Call("google_search",mapOf("query" to ""))) }
        rejects { AgentTools.validate(ParsedAction.Call("screen_read",mapOf("extra" to "x"))) }
        rejects { AgentTools.validate(ParsedAction.Call("screen_type",mapOf("text" to "a".repeat(501)))) }
        rejects { AgentTools.validate(ParsedAction.Call("file_read",mapOf("name" to "../secreto.txt"))) }
        rejects { AgentTools.validate(ParsedAction.Call("apk_open_selected",mapOf("uri" to "x"))) }
    }

    @Test fun phoneFileNamesRejectTraversalAndPaths() {
        assertTrue(PhoneText.validName("notas.txt"))
        assertTrue(PhoneText.validName("mi archivo (1).md"))
        listOf("","..","../clave.txt","a/b.txt","a\\b.txt","..oculto","a".repeat(201),"li\nnea.txt").forEach {
            assertFalse(it,PhoneText.validName(it))
        }
    }

    @Test fun fileTextIsStrictUtf8() {
        assertEquals("hola mínimo",PhoneText.decodeUtf8("hola mínimo".toByteArray(Charsets.UTF_8)))
        rejects { PhoneText.decodeUtf8(byteArrayOf(0xFF.toByte(),0xFE.toByte())) }
        rejects { PhoneText.decodeUtf8(byteArrayOf(0xC3.toByte())) }
    }

    @Test fun screenTextExcludesSensitiveAndBlockedContent() {
        assertEquals("Maná & otros",ScreenPolicy.visibleText("Resultados","Maná & otros",false,false))
        assertEquals("Versión 1.2",ScreenPolicy.visibleText("Etiqueta","  Versión 1.2  ",false,false))
        assertNull(ScreenPolicy.visibleText("Campo","secreto",true,false))
        assertNull(ScreenPolicy.visibleText("Campo","secreto",false,true))
        assertNull(ScreenPolicy.visibleText("Confirmar","aceptar",false,false))
        assertNull(ScreenPolicy.visibleText("Etiqueta","Pagar ahora",false,false))
        assertEquals(2000,ScreenPolicy.visibleText("Etiqueta","a".repeat(2500),false,false)!!.length)
    }

    @Test fun screenTextSummaryDeduplicatesAndBoundsOutput() {
        val parts=listOf("uno","uno","dos"," ".repeat(10),"tres")
        assertEquals("uno\ndos\ntres",ScreenPolicy.visibleTextSummary(parts))
        assertEquals(4000,ScreenPolicy.visibleTextSummary(listOf("a".repeat(3000),"b".repeat(3000))).length)
    }
}
