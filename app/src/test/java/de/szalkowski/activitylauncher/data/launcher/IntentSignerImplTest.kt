package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
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
    fun testSignAndValidateRequest() = withMockedBase64 {
        val icon = ActivityIcon.Resource("pkg", 1)
        val intent = mock<Intent>()
        val shortcutRequest = ShortcutRequest("Test", intent, icon, source = LaunchSource.SHORTCUT)
        val launchRequest = LaunchRequest(intent, source = LaunchSource.SHORTCUT)

        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        val signature = signer.signRequest(shortcutRequest)

        assertNotNull(signature)
        assertTrue(signer.validateRequestSignature(launchRequest, signature))
    }

    @Test
    fun testSignatureWithPlugin() = withMockedBase64 {
        val icon = ActivityIcon.Resource("pkg", 1)
        val plugin = mock<ComponentName>()
        whenever(plugin.flattenToString()).thenReturn("com.example/.Plugin")
        val intent = mock<Intent>()
        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        val shortcutRequestWithPlugin = ShortcutRequest("Test", intent, icon, launcherPlugin = plugin, source = LaunchSource.SHORTCUT)
        val shortcutRequestWithoutPlugin = ShortcutRequest("Test", intent, icon, source = LaunchSource.SHORTCUT)
        val launchRequestWithPlugin = LaunchRequest(intent, launcherPlugin = plugin, source = LaunchSource.SHORTCUT)
        val launchRequestWithoutPlugin = LaunchRequest(intent, source = LaunchSource.SHORTCUT)

        val signatureWithPlugin = signer.signRequest(shortcutRequestWithPlugin)
        val signatureWithoutPlugin = signer.signRequest(shortcutRequestWithoutPlugin)

        assertNotEquals(signatureWithPlugin, signatureWithoutPlugin)
        assertTrue(signer.validateRequestSignature(launchRequestWithPlugin, signatureWithPlugin))
        assertFalse(signer.validateRequestSignature(launchRequestWithPlugin, signatureWithoutPlugin))
        assertFalse(signer.validateRequestSignature(launchRequestWithoutPlugin, signatureWithPlugin))
    }

    @Test
    fun testKnownSignature() = withMockedBase64 {
        val icon = ActivityIcon.Resource("pkg", 1)
        val intent = mock<Intent>()
        val request = ShortcutRequest("Test", intent, icon, source = LaunchSource.SHORTCUT)

        whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

        val signature = signer.signRequest(request)
        // Verified known signature for "intent:#Intent;action=com.test.ACTION;end" with key "test_key"
        assertEquals("kPThuLUm6BnZkfNuIuRuVZfj8IXOinD+dURnRv1Ytd8=", signature)
    }
}
