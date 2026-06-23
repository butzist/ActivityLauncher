package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.*
import java.util.Base64

class IntentSignerImplTest {
    private val context: Context = mock()
    private val sharedPreferences: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    private lateinit var signer: IntentSignerImpl
    private val testKey = "test_key"

    @Before
    fun setup() {
        whenever(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        whenever(sharedPreferences.contains(anyString())).thenReturn(true)
        whenever(sharedPreferences.getString(eq("key"), anyString())).thenReturn(testKey)
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putString(anyString(), anyString())).thenReturn(editor)

        signer = IntentSignerImpl(context)
    }

    private fun withMockedBase64(block: () -> Unit) {
        mockStatic(android.util.Base64::class.java).use { mockedBase64 ->
            mockedBase64.`when`<String> {
                android.util.Base64.encodeToString(any<ByteArray>(), anyInt())
            }.thenAnswer { invocation ->
                val bytes = invocation.arguments[0] as ByteArray
                Base64.getEncoder().encodeToString(bytes)
            }
            block()
        }
    }

    @Test
    fun testSignAndValidateIntent() = withMockedBase64 {
        val intent = mock<Intent>()
        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        val signature = signer.signIntent(intent)

        assertNotNull(signature)
        assertTrue(signer.validateIntentSignature(intent, signature))
    }

    @Test
    fun testBackwardCompatibilityNoPlugin() = withMockedBase64 {
        val intent = mock<Intent>()
        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        // When launchPlugin is null, it should be signed exactly as before
        val signature1 = signer.signIntent(intent)
        val signature2 = signer.signIntent(intent, null)

        assertEquals(signature1, signature2)
        assertTrue(signer.validateIntentSignature(intent, signature1))
        assertTrue(signer.validateIntentSignature(intent, signature1, null))
    }

    @Test
    fun testSignatureWithPlugin() = withMockedBase64 {
        val intent = mock<Intent>()
        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        val plugin = "com.example/.Plugin"
        val signatureWithPlugin = signer.signIntent(intent, plugin)
        val signatureWithoutPlugin = signer.signIntent(intent)

        assertNotEquals(signatureWithPlugin, signatureWithoutPlugin)
        assertTrue(signer.validateIntentSignature(intent, signatureWithPlugin, plugin))
        assertFalse(signer.validateIntentSignature(intent, signatureWithPlugin, null))
        assertFalse(signer.validateIntentSignature(intent, signatureWithoutPlugin, plugin))
    }

    @Test
    fun testSignatureChangesWithPlugin() = withMockedBase64 {
        val intent = mock<Intent>()
        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        val plugin1 = "com.example/.Plugin1"
        val plugin2 = "com.example/.Plugin2"

        val sig1 = signer.signIntent(intent, plugin1)
        val sig2 = signer.signIntent(intent, plugin2)

        assertNotEquals(sig1, sig2)
    }

    @Test
    fun testKnownSignature() = withMockedBase64 {
        val intent = mock<Intent>()
        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        val signature = signer.signIntent(intent)
        // Verified known signature for "intent:#Intent;action=com.test.ACTION;end" with key "test_key"
        assertEquals("kPThuLUm6BnZkfNuIuRuVZfj8IXOinD+dURnRv1Ytd8=", signature)
    }

    @Test
    fun testFailForTamperedIntent() = withMockedBase64 {
        val intent = mock<Intent>()
        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        val tamperedIntent = mock<Intent>()
        whenever(tamperedIntent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.TAMPERED;end")

        val signature = signer.signIntent(intent)
        assertFalse(signer.validateIntentSignature(tamperedIntent, signature))
    }
}
