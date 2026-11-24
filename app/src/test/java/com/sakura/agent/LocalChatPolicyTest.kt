package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class LocalChatPolicyTest {
    @Test fun holaUsesTinyPrompt() { val text=LocalChatPolicy.system("hola",null,List(100) { Memory("$it","Proyecto","x".repeat(500)) },false); assertTrue(text.length<240); assertFalse(text.contains("Dato")); assertTrue(text.contains("Saluda")) }
    @Test fun greetingHistoryDoesNotDragContext() { val h=LocalChatPolicy.history(List(12) { ChatMessage("x".repeat(5000),true) },"hola",false); assertEquals(listOf(ChatMessage("hola",false)),h) }
    @Test fun greetingDetectionIsExact() { assertTrue(LocalChatPolicy.greeting("¡hola".removePrefix("¡"))); assertTrue(LocalChatPolicy.greeting("Buenos días!")); assertFalse(LocalChatPolicy.greeting("hola revisa mi código")) }
    @Test fun normalFastOutputIsBounded() { assertEquals(32,LocalChatPolicy.outputTokens("hola",false)); assertEquals(96,LocalChatPolicy.outputTokens("explica variables",false)); assertEquals(256,LocalChatPolicy.outputTokens("explica variables",true)) }
    @Test fun selectedSkillActuallyEntersPrompt() { val skill=Skills.entries.first { it.name=="Traducción" }; val p=LocalChatPolicy.system("traduce esto",skill,emptyList(),false); assertTrue(p.contains(skill.name)); assertTrue(p.contains(skill.instruction.take(180))); assertTrue(p.contains("no ejecuta herramientas")) }
    @Test fun fastPromptHasAtMostOneShortNote() { val p=LocalChatPolicy.system("proyecto",null,listOf(Memory("1","Proyecto","primera"),Memory("2","Proyecto","ultima".repeat(100))),false); assertFalse(p.contains("primera")); assertTrue(p.length<400) }
    @Test fun hugeSkillInstructionIsBounded() { val p=LocalChatPolicy.system("codigo",SakuraSkill("Prueba","Chat","x".repeat(10000)),emptyList(),false); assertTrue(p.length<450) }
    @Test fun preservesNewUserMessage() { val text="x".repeat(5000); val h=LocalChatPolicy.history(emptyList(),text,false); assertEquals(text,h.last().text) }
    @Test fun boundsOldHistoryWithoutChangingChat() { val original=List(10) { ChatMessage("z".repeat(9000),true) }; val h=LocalChatPolicy.history(original,"continua",false); assertEquals(3,h.size); assertTrue(h.dropLast(1).all { it.text.length<650 }); assertEquals(9000,original.first().text.length) }
    @Test fun tinyModelsPinnedAndVisualsNotChat() { assertTrue(ModelCatalog.entries.count { it.category=="Chat" && it.bytes<500_000_000 } >= 2); assertTrue(ModelCatalog.entries.filter { it.category!="Chat" }.all { it.warning.contains("Descarga externa") }) }
}