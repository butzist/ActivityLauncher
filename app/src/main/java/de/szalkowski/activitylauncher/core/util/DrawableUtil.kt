package de.szalkowski.activitylauncher.core.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.scale

fun Context.getLauncherLargeIconSize(): Int {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.launcherLargeIconSize
}

fun Bitmap.ensureSoftware(): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false)
    } else {
        this
    }
}

fun Bitmap.resize(maxSize: Int): Bitmap {
    val softwareBitmap = ensureSoftware()
    if ((softwareBitmap.width <= maxSize) && (softwareBitmap.height <= maxSize)) return softwareBitmap
    val aspectRatio = softwareBitmap.width.toFloat() / softwareBitmap.height.toFloat()
    val newWidth: Int
    val newHeight: Int
    if (aspectRatio > 1) {
        newWidth = maxSize
        newHeight = (maxSize / aspectRatio).toInt()
    } else {
        newHeight = maxSize
        newWidth = (maxSize * aspectRatio).toInt()
    }
    return softwareBitmap.scale(newWidth, newHeight, true)
}

fun Drawable.toBitmap(width: Int = intrinsicWidth, height: Int = intrinsicHeight): Bitmap {
    val finalWidth = if (width > 0) width else 1
    val finalHeight = if (height > 0) height else 1

    val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, finalWidth, finalHeight)
    this.draw(canvas)
    return bitmap
}
