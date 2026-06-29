package de.szalkowski.activitylauncher.domain.launcher

import de.szalkowski.activitylauncher.domain.model.ShortcutRequest

interface IntentSigner {
    fun signRequest(request: ShortcutRequest): String
    fun validateRequestSignature(request: ShortcutRequest, signature: String): Boolean
}
