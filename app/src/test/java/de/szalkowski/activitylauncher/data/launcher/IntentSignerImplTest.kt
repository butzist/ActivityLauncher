package de.szalkowski.activitylauncher.data.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import de.szalkowski.activitylauncher.core.util.getActivityIntent
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
        val componentName = mock<ComponentName>()
        val icon = mock<androidx.core.graphics.drawable.IconCompat>()
        val request = ShortcutRequest("Test", componentName, icon)
        val intent = mock<Intent>()

        val intentUtilClass = Class.forName("de.szalkowski.activitylauncher.core.util.ActivityIntentKt")
        mockStatic(intentUtilClass).use { mockedIntentUtil ->
            mockedIntentUtil.`when`<Intent> {
                getActivityIntent(eq(componentName), anyOrNull())
            }.thenReturn(intent)
            whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

            val signature = signer.signRequest(request)

            assertNotNull(signature)
            assertTrue(signer.validateRequestSignature(request, signature))
        }
    }

    @Test
    fun testSignatureWithPlugin() = withMockedBase64 {
        val componentName = mock<ComponentName>()
        val icon = mock<androidx.core.graphics.drawable.IconCompat>()
        val plugin = mock<ComponentName>()
        whenever(plugin.flattenToString()).thenReturn("com.example/.Plugin")
        val requestWithPlugin = ShortcutRequest("Test", componentName, icon, launcherPlugin = plugin)
        val requestWithoutPlugin = ShortcutRequest("Test", componentName, icon)
        val intent = mock<Intent>()

        val intentUtilClass = Class.forName("de.szalkowski.activitylauncher.core.util.ActivityIntentKt")
        mockStatic(intentUtilClass).use { mockedIntentUtil ->
            mockedIntentUtil.`when`<Intent> {
                getActivityIntent(eq(componentName), anyOrNull())
            }.thenReturn(intent)
            whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

            val signatureWithPlugin = signer.signRequest(requestWithPlugin)
            val signatureWithoutPlugin = signer.signRequest(requestWithoutPlugin)

            assertNotEquals(signatureWithPlugin, signatureWithoutPlugin)
            assertTrue(signer.validateRequestSignature(requestWithPlugin, signatureWithPlugin))
            assertFalse(signer.validateRequestSignature(requestWithPlugin, signatureWithoutPlugin))
            assertFalse(signer.validateRequestSignature(requestWithoutPlugin, signatureWithPlugin))
        }
    }

    @Test
    fun testKnownSignature() = withMockedBase64 {
        val componentName = mock<ComponentName>()
        val icon = mock<androidx.core.graphics.drawable.IconCompat>()
        val request = ShortcutRequest("Test", componentName, icon)
        val intent = mock<Intent>()

        val intentUtilClass = Class.forName("de.szalkowski.activitylauncher.core.util.ActivityIntentKt")
        mockStatic(intentUtilClass).use { mockedIntentUtil ->
            mockedIntentUtil.`when`<Intent> {
                getActivityIntent(eq(componentName), anyOrNull())
            }.thenReturn(intent)
            whenever(intent.toUri(anyInt())).thenReturn("intent:#Intent;action=com.test.ACTION;end")

            val signature = signer.signRequest(request)
            // Verified known signature for "intent:#Intent;action=com.test.ACTION;end" with key "test_key"
            assertEquals("kPThuLUm6BnZkfNuIuRuVZfj8IXOinD+dURnRv1Ytd8=", signature)
        }
    }
}
