package de.szalkowski.activitylauncher.domain.launcher

import android.content.Intent

interface IntentSigner {
    fun signIntent(intent: Intent, launchPlugin: String? = null): String
    fun validateIntentSignature(intent: Intent, signature: String, launchPlugin: String? = null): Boolean
}
