package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntentSignerImplTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var signer: IntentSignerImpl

    @Before
    fun setup() {
        signer = IntentSignerImpl(context)
    }

    @Test
    fun testSignAndValidateIntent() {
        val intent = Intent("com.test.ACTION")
        val signature = signer.signIntent(intent)

        assertNotNull(signature)
        assertTrue(signer.validateIntentSignature(intent, signature))
    }

    @Test
    fun testFailForInvalidSignature() {
        val intent = Intent("com.test.ACTION")
        assertFalse(signer.validateIntentSignature(intent, "invalid_signature"))
    }

    @Test
    fun testFailForTamperedIntent() {
        val intent = Intent("com.test.ACTION")
        val signature = signer.signIntent(intent)

        val tamperedIntent = Intent("com.test.TAMPERED")
        assertFalse(signer.validateIntentSignature(tamperedIntent, signature))
    }
}
