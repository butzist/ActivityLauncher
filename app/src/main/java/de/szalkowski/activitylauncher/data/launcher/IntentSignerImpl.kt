package de.szalkowski.activitylauncher.data.launcher

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import de.szalkowski.activitylauncher.core.util.getActivityIntent
import de.szalkowski.activitylauncher.domain.launcher.IntentSigner
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class IntentSignerImpl @Inject constructor(@ApplicationContext context: Context) :
    IntentSigner {
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

    override fun signRequest(request: ShortcutRequest): String {
        val launchIntent = getActivityIntent(request.component, request.extras)
        val uri = launchIntent.toUri(0)
        val launcherPlugin = request.launcherPlugin?.flattenToString()
        val message = if (launcherPlugin == null) {
            uri
        } else {
            "$uri;$launcherPlugin"
        }
        return hmac256(key, message)
    }

    override fun validateRequestSignature(request: ShortcutRequest, signature: String): Boolean {
        val compSignature = signRequest(request)
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
