package de.szalkowski.activitylauncher.domain.launcher

import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest

interface IntentSigner {
    fun signRequest(request: ShortcutRequest): String
    fun signRequest(request: LaunchRequest): String
    fun validateRequestSignature(request: LaunchRequest, signature: String): Boolean
}
