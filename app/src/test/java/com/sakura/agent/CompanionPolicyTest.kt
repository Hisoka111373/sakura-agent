package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class CompanionPolicyTest {
    @Test fun everyEventHasShortLocalPhrases() { CompanionPolicy.events.forEach { event -> assertTrue(CompanionPolicy.phrases(event).all { it.isNotBlank() && it.length < 160 }) } }
    @Test fun deterministicPhraseRotation() { val event="Saludo"; assertEquals(CompanionPolicy.phrase(event,0),CompanionPolicy.phrase(event,3)); assertNotEquals(CompanionPolicy.phrase(event,0),CompanionPolicy.phrase(event,1)) }
    @Test fun unknownEventSafeFallback() { assertTrue(CompanionPolicy.phrase("?",Long.MAX_VALUE).isNotBlank()) }
    @Test fun noContinuationWithoutAnswer() { assertFalse(CompanionPolicy.canContinue(emptyList(),"")); assertFalse(CompanionPolicy.canContinue(listOf(ChatMessage("Pregunta",false)),"")) }
    @Test fun noContinuationOverDraft() { assertFalse(CompanionPolicy.canContinue(listOf(ChatMessage("Respuesta",true)),"Mi borrador")) }
    @Test fun continuationRequiresUsefulAnswer() { assertTrue(CompanionPolicy.canContinue(listOf(ChatMessage("Respuesta",true)),"")); assertFalse(CompanionPolicy.canContinue(listOf(ChatMessage(" ",true)),"")) }
    @Test fun continuationCannotRouteToSearch() { assertFalse(Skills.isSearchRequest(CompanionPolicy.continuation)); assertTrue(CompanionPolicy.continuation.contains("No ejecutes acciones")) }
    @Test fun fastHistoryIsBoundedAndMarked() { val input=listOf(ChatMessage("x".repeat(9000),false),ChatMessage("y".repeat(9000),true)); val output=ThinkingPolicy.boundedHistory(input,"Bajo"); assertEquals(2,output.size); assertTrue(output.all { it.text.length < 1900 && it.text.contains("recortado") }); assertEquals(9000,input[0].text.length) }
    @Test fun shortHistoryIsUnchanged() { val input=listOf(ChatMessage("Pregunta",false),ChatMessage("Respuesta",true)); assertEquals(input,ThinkingPolicy.boundedHistory(input,"Bajo")) }
    @Test fun highHistoryRemainsBounded() { val input=(1..12).map { ChatMessage("z".repeat(9000),it%2==0) }; val output=ThinkingPolicy.boundedHistory(input,"Alto"); assertEquals(6,output.size); assertTrue(output.all { it.text.length<4200 }) }
}