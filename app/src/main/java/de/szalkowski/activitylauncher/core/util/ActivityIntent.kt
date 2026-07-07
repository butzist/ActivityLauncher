package de.szalkowski.activitylauncher.core.util

import android.content.ComponentName
import android.content.Intent
import androidx.core.net.toUri
import de.szalkowski.activitylauncher.domain.intent.ExtraDef
import de.szalkowski.activitylauncher.domain.intent.ExtraType
import de.szalkowski.activitylauncher.domain.intent.IntentDef

fun getActivityIntentFromIntentDef(activity: ComponentName?, intentDef: IntentDef?): Intent {
    val intent = Intent()
    intent.component = activity
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

    if (intentDef != null) {
        intent.action = intentDef.action?.ifBlank { null }
        intent.setDataAndType(
            intentDef.data?.ifBlank { null }?.toUri(),
            intentDef.mimeType?.ifBlank { null },
        )
        intentDef.categories.forEach {
            if (it.isNotBlank()) {
                intent.addCategory(it)
            }
        }
        intentDef.extras.forEach { extra ->
            if (extra.key.isNotBlank()) {
                when (extra.type) {
                    ExtraType.STRING -> intent.putExtra(extra.key, extra.value)
                    ExtraType.INT -> intent.putExtra(extra.key, extra.value.toIntOrNull() ?: 0)
                    ExtraType.LONG -> intent.putExtra(extra.key, extra.value.toLongOrNull() ?: 0L)
                    ExtraType.FLOAT -> intent.putExtra(extra.key, extra.value.toFloatOrNull() ?: 0f)
                    ExtraType.DOUBLE -> intent.putExtra(extra.key, extra.value.toDoubleOrNull() ?: 0.0)
                    ExtraType.BOOLEAN -> intent.putExtra(extra.key, extra.value.toBoolean())
                }
            }
        }
    }

    return intent
}

fun getIntentDefFromActivityIntent(intent: Intent): IntentDef {
    val extras = mutableListOf<ExtraDef>()
    intent.extras?.let { bundle ->
        for (key in bundle.keySet()) {
            @Suppress("DEPRECATION")
            val value = bundle.get(key)
            val type = when (value) {
                is Int -> ExtraType.INT
                is Long -> ExtraType.LONG
                is Float -> ExtraType.FLOAT
                is Double -> ExtraType.DOUBLE
                is Boolean -> ExtraType.BOOLEAN
                else -> ExtraType.STRING
            }
            extras.add(ExtraDef(key, value?.toString() ?: "", type))
        }
    }

    return IntentDef(
        action = intent.action,
        data = intent.dataString,
        mimeType = intent.type,
        categories = intent.categories?.toList() ?: emptyList(),
        extras = extras,
    )
}
