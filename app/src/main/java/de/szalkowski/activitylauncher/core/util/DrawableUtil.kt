package de.szalkowski.activitylauncher.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.IconCompat

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
