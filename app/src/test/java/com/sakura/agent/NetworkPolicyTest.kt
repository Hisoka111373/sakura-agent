package com.sakura.agent

import org.junit.Assert.*
import org.junit.Test

class NetworkPolicyTest {
    private fun denied(block: () -> Unit) { try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) {} }
    @Test fun onlyHttps() { denied { NetworkPolicy.https("http://example.com/file.png") }; assertEquals("example.com",NetworkPolicy.https("https://example.com/file.png").host) }
    @Test fun noCredentialsOrPorts() { listOf("https://u:p@example.com/a","https://example.com:444/a","https://example.com/a#b").forEach { denied { NetworkPolicy.https(it) } } }
    @Test fun noControlCharacters() { denied { NetworkPolicy.https("https://example.com/\nfile") } }
    @Test fun deniesPrivateIpv4() { listOf(byteArrayOf(127,0,0,1),byteArrayOf(10,0,0,1),byteArrayOf(-84,16,0,1),byteArrayOf(-64,-88,1,1),byteArrayOf(100,64,0,1),byteArrayOf(-87,-2,1,1)).forEach { assertFalse(NetworkPolicy.publicAddress(it)) } }
    @Test fun allowsPublicIpv4() { assertTrue(NetworkPolicy.publicAddress(byteArrayOf(1,1,1,1))); assertTrue(NetworkPolicy.publicAddress(byteArrayOf(8,8,8,8))) }
    @Test fun restrictsIpv6() { assertFalse(NetworkPolicy.publicAddress(ByteArray(16))); assertFalse(NetworkPolicy.publicAddress(ByteArray(16).apply { this[0]=(-4).toByte() })); assertTrue(NetworkPolicy.publicAddress(ByteArray(16).apply { this[0]=0x26 })) }
    @Test fun safeBranches() { listOf("main","feature/sakura-v0.3","fix_abc").forEach { assertTrue(NetworkPolicy.branch(it)) }; listOf("../main","refs//a","/main","a.lock","a..b","a/./b","main ","a@{b").forEach { assertFalse(NetworkPolicy.branch(it)) } }
    @Test fun phonesAreInternationalDigits() { assertTrue(NetworkPolicy.phone("5215512345678")); assertFalse(NetworkPolicy.phone("+5215512345678")); assertFalse(NetworkPolicy.phone("123")); assertFalse(NetworkPolicy.phone("0".repeat(10))) }
    @Test fun telegramChats() { assertTrue(NetworkPolicy.chat("-100123456789")); assertTrue(NetworkPolicy.chat("@my_channel")); assertFalse(NetworkPolicy.chat("bob@example.com")); assertFalse(NetworkPolicy.chat("1/../../sendMessage")) }
    @Test fun textLimits() { assertTrue(NetworkPolicy.text("Hola\nmundo",4096)); assertFalse(NetworkPolicy.text(" ",4096)); assertFalse(NetworkPolicy.text("x".repeat(4097),4096)); assertFalse(NetworkPolicy.text("a\u0000b",4096)) }
    @Test fun secretsHaveNoHeaderInjection() { assertTrue(NetworkPolicy.secret("abc:key")); assertFalse(NetworkPolicy.secret("abc\r\nAuthorization: other")); assertFalse(NetworkPolicy.secret("a b")) }
    @Test fun mediaSignatureNotHtml() { assertNull(NetworkPolicy.mediaExtension("<html>test</html>".toByteArray(),"Imagen")); assertEquals("png",NetworkPolicy.mediaExtension(byteArrayOf(-119,80,78,71,13,10,26,10),"Imagen")); assertEquals("jpg",NetworkPolicy.mediaExtension(byteArrayOf(-1,-40,-1),"Imagen")) }
    @Test fun videoSignatures() { assertEquals("mp4",NetworkPolicy.mediaExtension(byteArrayOf(0,0,0,20,102,116,121,112,105,115,111,109),"Video")); assertNull(NetworkPolicy.mediaExtension(byteArrayOf(-119,80,78,71,13,10,26,10),"Video")) }
    @Test fun catalogFitsLimitsAndHasPins() { ModelCatalog.entries.forEach { m -> assertTrue(m.sha256.matches(Regex("[0-9a-f]{64}"))); assertTrue(m.revision.matches(Regex("[0-9a-f]{40}"))); assertTrue(m.bytes<=if(m.category=="Chat") AgentPolicy.MAX_MODEL_BYTES else NetworkPolicy.MAX_VISUAL_MODEL_BYTES) } }
}