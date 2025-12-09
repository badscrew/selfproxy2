package com.selfproxy.vpn

import android.util.Base64
import io.mockk.every
import io.mockk.mockkStatic

/**
 * Test utility for generating valid WireGuard keys and VLESS UUIDs.
 * 
 * Provides valid base64-encoded 32-byte keys for testing.
 */
object TestKeys {
    /**
     * Valid WireGuard private key (base64-encoded 32 bytes).
     */
    const val VALID_PRIVATE_KEY = "YAnz5TF+lXXJte14tji3Zlx3TGmHRcGgOoEdS4ss5uU="
    
    /**
     * Valid WireGuard public key (base64-encoded 32 bytes).
     */
    const val VALID_PUBLIC_KEY = "HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw="
    
    /**
     * Valid WireGuard preshared key (base64-encoded 32 bytes).
     */
    const val VALID_PRESHARED_KEY = "FpCyhws9cxwWoV4xELtfJvjJN+zQVRPISllRWgeopVE="
    
    /**
     * Another valid WireGuard public key for testing.
     */
    const val VALID_PUBLIC_KEY_2 = "xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg="
    
    /**
     * Valid VLESS UUID (RFC 4122 format).
     */
    const val VALID_UUID = "550e8400-e29b-41d4-a716-446655440000"
    
    /**
     * Another valid VLESS UUID for testing.
     */
    const val VALID_UUID_2 = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    
    /**
     * Mocks Android Base64 for unit tests.
     * Call this in @Before setup methods.
     */
    fun mockAndroidBase64() {
        mockkStatic(Base64::class)
        every { Base64.decode(any<String>(), any()) } answers {
            val input = firstArg<String>()
            java.util.Base64.getDecoder().decode(input)
        }
        every { Base64.encode(any<ByteArray>(), any()) } answers {
            val input = firstArg<ByteArray>()
            java.util.Base64.getEncoder().encode(input)
        }
        every { Base64.encodeToString(any<ByteArray>(), any()) } answers {
            val input = firstArg<ByteArray>()
            java.util.Base64.getEncoder().encodeToString(input)
        }
    }
}
