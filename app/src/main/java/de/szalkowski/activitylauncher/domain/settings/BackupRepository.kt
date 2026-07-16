package de.szalkowski.activitylauncher.domain.settings

import android.net.Uri

interface BackupRepository {
    suspend fun exportBackup(uri: Uri): Result<Unit>
    suspend fun importBackup(uri: Uri): Result<Unit>
}
