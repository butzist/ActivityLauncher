package de.szalkowski.activitylauncher.services.internal

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle

fun getActivityIntent(activity: ComponentName?, extras: Bundle?): Intent {
    val intent = Intent()
    intent.setComponent(activity)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    if (extras != null) {
        intent.putExtras(extras)
    }
    return intent
}
