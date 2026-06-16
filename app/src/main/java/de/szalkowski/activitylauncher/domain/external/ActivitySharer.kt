package de.szalkowski.activitylauncher.domain.external

import android.content.ComponentName

interface ActivitySharer {
    fun shareActivity(activity: ComponentName)
}
