package de.szalkowski.activitylauncher.services.internal

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager

fun ActivityInfo.isPrivate(packageManager: PackageManager): Boolean {
    if (!this.exported) return true

    val enabledState = packageManager.getComponentEnabledSetting(this.componentName)
    return when (enabledState) {
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> false
        else -> !this.isEnabled
    }
}

val ActivityInfo.componentName
    get() = ComponentName(this.packageName, this.name)
