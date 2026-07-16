package de.szalkowski.activitylauncher.core.util

import android.content.Intent
import android.os.Build
import android.os.Parcelable

fun <T : Parcelable> Intent.getParcelableExtraCompat(name: String, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name)
    }
}

fun <T : Parcelable> android.os.Parcel.readParcelableCompat(clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        readParcelable(clazz.classLoader, clazz)
    } else {
        @Suppress("DEPRECATION")
        readParcelable(clazz.classLoader)
    }
}

fun parseIntentUriOrNull(uri: String?, flags: Int): Intent? {
    if (uri == null) return null
    return try {
        Intent.parseUri(uri, flags)
    } catch (_: Exception) {
        null
    }
}
