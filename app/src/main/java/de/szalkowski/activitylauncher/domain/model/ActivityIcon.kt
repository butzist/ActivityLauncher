package de.szalkowski.activitylauncher.domain.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toDrawable
import de.szalkowski.activitylauncher.core.util.ensureSoftware
import de.szalkowski.activitylauncher.core.util.getLauncherLargeIconSize
import de.szalkowski.activitylauncher.core.util.resize
import de.szalkowski.activitylauncher.core.util.toBitmap

/**
 * A wrapper for icons that provides safe ways to render them in-app
 * and safe ways to send them to external processes (like the Launcher).
 */
sealed class ActivityIcon : Parcelable {

    /**
     * Returns an [IconCompat] optimized for internal logic.
     */
    abstract fun toIconCompat(context: Context): IconCompat

    /**
     * Returns an [IconCompat] safe for external processes like Launcher shortcuts.
     * Guaranteed to be software-backed and consistent in appearance across devices.
     */
    abstract fun toShortcutIcon(context: Context): IconCompat

    /**
     * Returns a [Drawable] for standard UI rendering.
     * Use this for in-app display to ensure consistency with other icons.
     */
    abstract fun loadInternalDrawable(context: Context, size: Int? = null, masked: Boolean = false): Drawable?

    /**
     * Returns the raw [Drawable] if available.
     */
    abstract fun loadDrawable(context: Context): Drawable?

    companion object {
        fun from(icon: IconCompat): ActivityIcon {
            val bundle = icon.toBundle()
            val type = bundle.getInt("type")
            return when (type) {
                IconCompat.TYPE_RESOURCE -> {
                    val packageName = bundle.getString("obj") ?: ""
                    val resId = bundle.getInt("int1")
                    Resource(packageName, resId)
                }
                IconCompat.TYPE_BITMAP, IconCompat.TYPE_ADAPTIVE_BITMAP -> {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle.getParcelable("obj", Bitmap::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        bundle.getParcelable("obj")
                    } ?: throw IllegalArgumentException("Bitmap icon missing bitmap object")
                    BitmapIcon(bitmap, type == IconCompat.TYPE_ADAPTIVE_BITMAP)
                }
                else -> {
                    Legacy(bundle)
                }
            }
        }

        fun from(drawable: Drawable, maxSize: Int? = null): ActivityIcon {
            return from(bakeIcon(drawable, maxSize))
        }

        fun fromResource(packageName: String, resId: Int, resourceName: String? = null): ActivityIcon {
            return Resource(packageName, resId, resourceName)
        }

        fun fromBitmap(bitmap: Bitmap, isAdaptive: Boolean = false): ActivityIcon {
            return BitmapIcon(bitmap, isAdaptive)
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<ActivityIcon> {
            override fun createFromParcel(parcel: Parcel): ActivityIcon {
                return when (parcel.readInt()) {
                    0 -> Resource(parcel.readString() ?: "", parcel.readInt(), parcel.readString())
                    1 -> BitmapIcon(parcel.readParcelable(Bitmap::class.java.classLoader)!!, parcel.readInt() == 1)
                    2 -> Legacy(parcel.readBundle(Bundle::class.java.classLoader)!!)
                    else -> throw IllegalArgumentException("Unknown ActivityIcon type")
                }
            }

            override fun newArray(size: Int): Array<ActivityIcon?> = arrayOfNulls(size)
        }
    }

    data class Resource(val packageName: String, val resId: Int, val resourceName: String? = null) : ActivityIcon() {
        override fun toIconCompat(context: Context): IconCompat {
            return if (packageName == context.packageName) {
                IconCompat.createWithResource(context, resId)
            } else {
                val packageContext = context.createPackageContext(packageName, 0)
                IconCompat.createWithResource(packageContext, resId)
            }
        }

        override fun toShortcutIcon(context: Context): IconCompat {
            return if (packageName == context.packageName) {
                toIconCompat(context)
            } else {
                val drawable = runCatching { loadDrawable(context) }.getOrNull()
                    ?: ContextCompat.getDrawable(context, android.R.drawable.sym_def_app_icon)!!
                bakeIcon(drawable, 256)
            }
        }

        override fun loadInternalDrawable(context: Context, size: Int?, masked: Boolean): Drawable? {
            val drawable = loadDrawable(context) ?: return null
            val finalSize = size ?: context.getLauncherLargeIconSize().coerceAtLeast(1)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                val bitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                val totalSize = (finalSize * 108f / 72f).toInt()
                val offset = (finalSize - totalSize) / 2
                drawable.background?.let {
                    it.setBounds(offset, offset, offset + totalSize, offset + totalSize)
                    it.draw(canvas)
                }
                drawable.foreground?.let {
                    it.setBounds(offset, offset, offset + totalSize, offset + totalSize)
                    it.draw(canvas)
                }

                if (masked) {
                    applyMask(bitmap, finalSize).toDrawable(context.resources)
                } else {
                    bitmap.toDrawable(context.resources)
                }
            } else {
                if (masked) {
                    val bitmap = drawable.toBitmap(finalSize, finalSize)
                    applyMask(bitmap, finalSize).toDrawable(context.resources)
                } else {
                    drawable
                }
            }
        }

        override fun loadDrawable(context: Context): Drawable? {
            return if (packageName == context.packageName) {
                ContextCompat.getDrawable(context, resId)
            } else {
                runCatching {
                    val res = context.packageManager.getResourcesForApplication(packageName)
                    val id = if (resId != 0) resId else resourceName?.let { res.getIdentifier(it, null, null) } ?: 0
                    if (id != 0) {
                        ResourcesCompat.getDrawable(res, id, null)
                    } else {
                        null
                    }
                }.getOrNull() ?: toIconCompat(context).loadDrawable(context)
            }
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(0) // Type
            parcel.writeString(packageName)
            parcel.writeInt(resId)
            parcel.writeString(resourceName)
        }

        override fun describeContents(): Int = 0
    }

    data class BitmapIcon(val bitmap: Bitmap, val isAdaptive: Boolean) : ActivityIcon() {
        override fun toIconCompat(context: Context): IconCompat = if (isAdaptive) {
            IconCompat.createWithAdaptiveBitmap(bitmap)
        } else {
            IconCompat.createWithBitmap(bitmap)
        }

        override fun toShortcutIcon(context: Context): IconCompat {
            val softwareBitmap = bitmap.ensureSoftware()
            val resized = softwareBitmap.resize(256)
            return if (isAdaptive) {
                IconCompat.createWithAdaptiveBitmap(resized)
            } else {
                IconCompat.createWithBitmap(resized)
            }
        }

        override fun loadInternalDrawable(context: Context, size: Int?, masked: Boolean): Drawable {
            val finalSize = size ?: context.getLauncherLargeIconSize().coerceAtLeast(1)
            val bitmap = if (isAdaptive) {
                val output = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val totalSize = (finalSize * 108f / 72f).toInt()
                val offset = (finalSize - totalSize) / 2
                val srcRect = Rect(0, 0, this.bitmap.width, this.bitmap.height)
                val destRect = Rect(offset, offset, offset + totalSize, offset + totalSize)
                canvas.drawBitmap(this.bitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                output
            } else {
                this.bitmap.resize(finalSize)
            }

            return if (masked) {
                applyMask(bitmap, finalSize).toDrawable(context.resources)
            } else {
                bitmap.toDrawable(context.resources)
            }
        }

        override fun loadDrawable(context: Context): Drawable = loadInternalDrawable(context)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(1) // Type
            val softwareBitmap = bitmap.resize(256)
            parcel.writeParcelable(softwareBitmap, flags)
            parcel.writeInt(if (isAdaptive) 1 else 0)
        }

        override fun describeContents(): Int = 0
    }

    data class Legacy(val bundle: Bundle) : ActivityIcon() {
        override fun toIconCompat(context: Context): IconCompat = IconCompat.createFromBundle(bundle)!!
        override fun toShortcutIcon(context: Context): IconCompat {
            val drawable = loadDrawable(context) ?: return toIconCompat(context)
            return bakeIcon(drawable, 256)
        }
        override fun loadInternalDrawable(context: Context, size: Int?, masked: Boolean): Drawable? {
            val drawable = loadDrawable(context) ?: return null
            val finalSize = size ?: context.getLauncherLargeIconSize().coerceAtLeast(1)
            return if (masked) {
                val bitmap = drawable.toBitmap(finalSize, finalSize)
                applyMask(bitmap, finalSize).toDrawable(context.resources)
            } else {
                drawable
            }
        }
        override fun loadDrawable(context: Context): Drawable? = toIconCompat(context).loadDrawable(context)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(2) // Type
            parcel.writeBundle(bundle)
        }

        override fun describeContents(): Int = 0
    }

    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: Parcel, flags: Int) {}
}

private fun applyMask(bitmap: Bitmap, size: Int): Bitmap {
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = Rect(0, 0, size, size)
    val rectF = RectF(rect)
    val roundPx = size * 0.25f

    canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)
    return output
}

private fun bakeIcon(drawable: Drawable, maxSize: Int? = null): IconCompat {
    var width = drawable.intrinsicWidth
    var height = drawable.intrinsicHeight

    if (maxSize != null && maxSize > 0) {
        if (width > maxSize || height > maxSize || width <= 0 || height <= 0) {
            val aspectRatio = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1f
            if (aspectRatio > 1) {
                width = maxSize
                height = (maxSize / aspectRatio).toInt()
            } else {
                height = maxSize
                width = (maxSize * aspectRatio).toInt()
            }
        }
    }

    val finalWidth = if (width > 0) width else 1
    val finalHeight = if (height > 0) height else 1

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
        val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.background?.let {
            it.setBounds(0, 0, finalWidth, finalHeight)
            it.draw(canvas)
        }
        drawable.foreground?.let {
            it.setBounds(0, 0, finalWidth, finalHeight)
            it.draw(canvas)
        }
        IconCompat.createWithAdaptiveBitmap(bitmap)
    } else {
        IconCompat.createWithBitmap(drawable.toBitmap(finalWidth, finalHeight))
    }
}
