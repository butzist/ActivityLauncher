package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.Intent
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

interface IntentSigningService {
    fun signIntent(intent: Intent): String
    fun validateIntentSignature(intent: Intent, signature: String): Boolean
}

class IntentSigningServiceImpl @Inject constructor(@ApplicationContext context: Context) :
    IntentSigningService {
    private val key: String

    init {
        val preferences = context.getSharedPreferences("signer", Context.MODE_PRIVATE)
        if (!preferences.contains("key")) {
            val random = SecureRandom()
            val bytes = ByteArray(256)
            random.nextBytes(bytes)
            key = Base64.encodeToString(bytes, Base64.NO_WRAP)
            preferences.edit().putString("key", key).apply()
        } else {
            key = preferences.getString("key", "")!!
        }
    }

    override fun signIntent(intent: Intent): String {
        val uri = intent.toUri(0)
        return hmac256(key, uri)
    }

    override fun validateIntentSignature(intent: Intent, signature: String): Boolean {
        val compSignature = signIntent(intent)
        return signature == compSignature
    }

    companion object {
        /**
         * Adapted from StackOverflow:
         * https://stackoverflow.com/questions/36004761/is-there-any-function-for-creating-hmac256-string-in-android
         */
        private fun hmac256(key: String?, message: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key!!.toByteArray(), "HmacSHA256"))
            val result = mac.doFinal(message.toByteArray())
            return Base64.encodeToString(result, Base64.NO_WRAP)
        }
    }
}
