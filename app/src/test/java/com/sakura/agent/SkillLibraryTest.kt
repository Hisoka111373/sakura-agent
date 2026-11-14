package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class SkillLibraryTest {
    @Test fun broadUniqueCatalog() { assertTrue(Skills.entries.size>=85); assertEquals(Skills.entries.size,Skills.entries.map { it.name }.toSet().size) }
    @Test fun categoriesPresent() { assertTrue(Skills.entries.map { it.category }.toSet().containsAll(listOf("Programación","Ingeniería","Aprendizaje","Productividad","Investigación","Datos","Creatividad","Vida diaria"))) }
    @Test fun allTextInstructionsBounded() { Skills.entries.filter { it.page=="Chat" }.forEach { assertTrue(it.instruction.isNotBlank()); assertTrue(it.instruction.length<=1000) } }
    @Test fun pythonSelected() { assertEquals("Python",Skills.textSkill("Haz una función en Python")?.name) }
    @Test fun kotlinSelected() { assertEquals("Kotlin / Android",Skills.textSkill("Ayúdame con Jetpack Compose Android")?.name) }
    @Test fun translationOverridesLanguageMention() { assertEquals("Traducción",Skills.textSkill("Traduce esta explicación de Python al inglés")?.name) }
    @Test fun debugIntentOverridesLanguage() { assertEquals("Depuración guiada",Skills.textSkill("Depura este código Kotlin")?.name) }
    @Test fun noPartialKeywordMatch() { assertNull(Skills.textSkill("Necesito apoyo para navegar tranquilamente")) }
    @Test fun emptyUsesGeneral() { assertNull(Skills.textSkill("")); assertNull(Skills.textSkill("Hola Sakura, buenas tardes")) }
    @Test fun manualSelectionOverridesDetection() { assertEquals("Rust",Skills.textSkill("Resume esto","Rust")?.name) }
    @Test fun toolNamesCannotBecomeTextCapability() { assertNull(Skills.textSkill("Busca en YouTube","Abrir e interactuar con apps")) }
    @Test fun oneGuidanceAndNoPermissions() { val guide=Skills.guidance(Skills.textSkill("Python")); assertTrue(guide.contains("Python")); assertTrue(guide.contains("no habilitan")); assertFalse(guide.contains("Kotlin / Android")); assertEquals("",Skills.guidance(null)) }
    @Test fun cloudPayloadIncludesReviewedSkillOnly() { val system=TextProtocol.system+Skills.guidance(Skills.textSkill("Python")); val payload=TextProtocol.payload(TextConfig("OpenAI","test"),listOf(ChatMessage("Python",false)),system); assertEquals(system,payload.getJSONArray("messages").getJSONObject(0).getString("content")); assertFalse(payload.has("tools")) }
}