package app.ours.bridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OursNativeBridgeTest {

    @Test
    fun `allows official mnemonic domains and rejects untrusted public hosts on passive sync`() {
        assertTrue(OursNativeBridge.isTrustedServerUrl("https://ours.mnemonic.my.id"))
        assertTrue(OursNativeBridge.isTrustedServerUrl("https://stage.mnemonic.my.id"))
        assertFalse(OursNativeBridge.isTrustedServerUrl("https://evil.example", allowCustomHttps = false))
    }

    @Test
    fun `allows self-hosted https only when custom https permitted`() {
        assertTrue(
            OursNativeBridge.isTrustedServerUrl(
                "https://ours.example.org",
                allowCustomHttps = true,
            ),
        )
        assertFalse(OursNativeBridge.isTrustedServerUrl("https://169.254.169.254", allowCustomHttps = true))
        assertFalse(OursNativeBridge.isTrustedServerUrl("https://user:pass@ours.example.org", allowCustomHttps = true))
        assertFalse(OursNativeBridge.isTrustedServerUrl("https://foo.trycloudflare.com", allowCustomHttps = true))
    }

    @Test
    fun `enforces debug-only loopback http and blocks release cleartext localhost`() {
        assertTrue(
            OursNativeBridge.isTrustedServerUrl(
                "http://10.0.2.2:18787",
                isDebugBuild = true,
            ),
        )
        assertTrue(
            OursNativeBridge.isTrustedServerUrl(
                "http://127.0.0.1:18787",
                isDebugBuild = true,
            ),
        )
        assertFalse(
            OursNativeBridge.isTrustedServerUrl(
                "http://10.0.2.2:18787",
                isDebugBuild = false,
            ),
        )
        assertFalse(OursNativeBridge.isTrustedServerUrl("http://169.254.169.254/latest", isDebugBuild = true))
        assertFalse(OursNativeBridge.isTrustedServerUrl("javascript:alert(1)", isDebugBuild = true))
        assertFalse(OursNativeBridge.isTrustedServerUrl("file:///etc/passwd", isDebugBuild = true))
    }
}
