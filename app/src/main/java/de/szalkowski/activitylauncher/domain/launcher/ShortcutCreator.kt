package de.szalkowski.activitylauncher.domain.launcher

import de.szalkowski.activitylauncher.domain.model.ShortcutRequest

interface ShortcutCreator {
    fun createLauncherIcon(request: ShortcutRequest)

    companion object {
        const val INTENT_LAUNCH_SHORTCUT = "de.szalkowski.activitylauncher.intent.action.LAUNCH_SHORTCUT"
        const val INTENT_EXTRA_NAME = "name"
        const val INTENT_EXTRA_INTENT = "intent"
        const val INTENT_EXTRA_ICON = "icon"
        const val INTENT_EXTRA_SIGNATURE = "signature"
        const val INTENT_EXTRA_LAUNCH_PLUGIN = "launch_plugin"
        const val INTENT_EXTRA_SHORTCUT_ACTIVITY = "shortcut_activity"
    }
}
