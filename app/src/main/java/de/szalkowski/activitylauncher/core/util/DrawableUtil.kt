package de.szalkowski.activitylauncher.core.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.scale

fun Context.getLauncherLargeIconSize(): Int {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.launcherLargeIconSize
}

fun Bitmap.resize(maxSize: Int): Bitmap {
    if ((width <= maxSize) && (height <= maxSize)) return this
    val aspectRatio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int
    if (aspectRatio > 1) {
        newWidth = maxSize
        newHeight = (maxSize / aspectRatio).toInt()
    } else {
        newHeight = maxSize
        newWidth = (maxSize * aspectRatio).toInt()
    }
    return scale(newWidth, newHeight, true)
}

fun Bitmap.crop(rect: Rect): Bitmap {
    return Bitmap.createBitmap(
        this,
        rect.left.coerceIn(0, width),
        rect.top.coerceIn(0, height),
        rect.width().coerceAtMost(width - rect.left),
        rect.height().coerceAtMost(height - rect.top),
    )
}

fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable && this.bitmap != null) {
        return this.bitmap
    }

    return createBitmap(this) { canvas ->
        this.setBounds(0, 0, canvas.width, canvas.height)
        this.draw(canvas)
    }
}

fun Drawable.toIconCompat(): IconCompat {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable) {
        val bitmap = createBitmap(this) { canvas ->
            val width = canvas.width
            val height = canvas.height
            background?.let {
                it.setBounds(0, 0, width, height)
                it.draw(canvas)
            }
            foreground?.let {
                it.setBounds(0, 0, width, height)
                it.draw(canvas)
            }
        }
        IconCompat.createWithAdaptiveBitmap(bitmap)
    } else {
        IconCompat.createWithBitmap(this.toBitmap())
    }
}

private fun createBitmap(drawable: Drawable, drawBlock: (Canvas) -> Unit): Bitmap {
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawBlock(canvas)
    return bitmap
}
