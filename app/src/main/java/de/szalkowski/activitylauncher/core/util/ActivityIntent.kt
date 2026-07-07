package de.szalkowski.activitylauncher.core.util

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.core.os.bundleOf
import de.szalkowski.activitylauncher.domain.intent.ExtraDef
import de.szalkowski.activitylauncher.domain.intent.ExtraType
import de.szalkowski.activitylauncher.domain.intent.IntentDef

fun getActivityIntentFromIntentDef(activity: ComponentName?, intentDef: IntentDef?): Intent {
    val intent = Intent()
    intent.component = activity
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    if (intentDef != null) {
        if (!intentDef.action.isNullOrBlank()) {
            intent.action = intentDef.action
        }

        if (!intentDef.data.isNullOrBlank()) {
            intent.data = Uri.parse(intentDef.data)
        }

        if (!intentDef.mimeType.isNullOrBlank()) {
            intent.type = intentDef.mimeType
        }

        if (intentDef.data.isNullOrBlank() && intentDef.mimeType.isNullOrBlank()) {
            // clear data/type
        } else if (!intentDef.data.isNullOrBlank() && !intentDef.mimeType.isNullOrBlank()) {
            intent.data = Uri.parse(intentDef.data)
            intent.type = intentDef.mimeType
        }

        intentDef.categories
            .filter { it.isNotBlank() }
            .forEach { intent.addCategory(it) }

        val bundle = bundleOf()
        intentDef.extras.forEach { extra ->
            if (extra.key.isNotBlank()) {
                when (extra.type) {
                    ExtraType.STRING -> bundle.putString(extra.key, extra.value)
                    ExtraType.INT -> bundle.putInt(extra.key, extra.value.toIntOrNull() ?: 0)
                    ExtraType.LONG -> bundle.putLong(extra.key, extra.value.toLongOrNull() ?: 0L)
                    ExtraType.FLOAT -> bundle.putFloat(extra.key, extra.value.toFloatOrNull() ?: 0f)
                    ExtraType.DOUBLE -> bundle.putDouble(extra.key, extra.value.toDoubleOrNull() ?: 0.0)
                    ExtraType.BOOLEAN -> bundle.putBoolean(extra.key, extra.value.toBooleanStrictOrNull() ?: false)
                }
            }
        }
        if (!bundle.isEmpty) {
            intent.putExtras(bundle)
        }
    }

    return intent
}

fun getIntentDefFromActivityIntent(intent: Intent): IntentDef {
    val extras = mutableListOf<ExtraDef>()
    intent.extras?.let { bundle ->
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            val (stringValue, type) = when (value) {
                is String -> value to ExtraType.STRING
                is Int -> value.toString() to ExtraType.INT
                is Long -> value.toString() to ExtraType.LONG
                is Float -> value.toString() to ExtraType.FLOAT
                is Double -> value.toString() to ExtraType.DOUBLE
                is Boolean -> value.toString() to ExtraType.BOOLEAN
                else -> value?.toString().orEmpty() to ExtraType.STRING
            }
            extras.add(ExtraDef(key, stringValue, type))
        }
    }

    val categories = intent.categories?.toList() ?: emptyList()

    return IntentDef(
        action = intent.action,
        data = intent.dataString,
        mimeType = intent.type,
        categories = categories,
        extras = extras,
    )
}
