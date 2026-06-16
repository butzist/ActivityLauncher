package de.szalkowski.activitylauncher.domain.launcher

import android.content.Intent

interface IntentSigner {
    fun signIntent(intent: Intent): String
    fun validateIntentSignature(intent: Intent, signature: String): Boolean
}
