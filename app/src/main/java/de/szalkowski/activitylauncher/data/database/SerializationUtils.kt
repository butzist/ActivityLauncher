package de.szalkowski.activitylauncher.data.database

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object SerializationUtils {
    private const val TYPE_RESOURCE = 0
    private const val TYPE_BITMAP = 1
    private const val TYPE_LEGACY = 2

    private const val VAL_INT = 1
    private const val VAL_STRING = 2
    private const val VAL_BITMAP = 3

    fun iconToByteArray(icon: ActivityIcon): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        when (icon) {
            is ActivityIcon.Resource -> {
                dos.writeInt(TYPE_RESOURCE)
                dos.writeUTF(icon.packageName)
                dos.writeInt(icon.resId)
                dos.writeBoolean(icon.resourceName != null)
                icon.resourceName?.let { dos.writeUTF(it) }
            }
            is ActivityIcon.BitmapIcon -> {
                dos.writeInt(TYPE_BITMAP)
                val stream = ByteArrayOutputStream()
                icon.bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val bytes = stream.toByteArray()
                dos.writeInt(bytes.size)
                dos.write(bytes)
                dos.writeBoolean(icon.isAdaptive)
            }
            is ActivityIcon.Legacy -> {
                dos.writeInt(TYPE_LEGACY)
                writeBundle(dos, icon.bundle)
            }
        }

        dos.flush()
        return baos.toByteArray()
    }

    fun byteArrayToIcon(bytes: ByteArray): ActivityIcon? {
        val bais = ByteArrayInputStream(bytes)
        val dis = DataInputStream(bais)

        return try {
            when (dis.readInt()) {
                TYPE_RESOURCE -> {
                    val packageName = dis.readUTF()
                    val resId = dis.readInt()
                    val hasName = dis.readBoolean()
                    val resourceName = if (hasName) dis.readUTF() else null
                    ActivityIcon.Resource(packageName, resId, resourceName)
                }
                TYPE_BITMAP -> {
                    val bitmapSize = dis.readInt()
                    val bitmapBytes = ByteArray(bitmapSize)
                    dis.readFully(bitmapBytes)
                    val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapSize)
                    ActivityIcon.BitmapIcon(bitmap, dis.readBoolean())
                }
                TYPE_LEGACY -> ActivityIcon.Legacy(readBundle(dis))
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeBundle(dos: DataOutputStream, bundle: Bundle) {
        val keys = bundle.keySet()
        dos.writeInt(keys.size)
        for (key in keys) {
            dos.writeUTF(key)
            @Suppress("DEPRECATION")
            val value = bundle.get(key)
            when (value) {
                is Int -> {
                    dos.writeByte(VAL_INT)
                    dos.writeInt(value)
                }
                is String -> {
                    dos.writeByte(VAL_STRING)
                    dos.writeUTF(value)
                }
                is Bitmap -> {
                    dos.writeByte(VAL_BITMAP)
                    val stream = ByteArrayOutputStream()
                    value.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val bytes = stream.toByteArray()
                    dos.writeInt(bytes.size)
                    dos.write(bytes)
                }
                else -> dos.writeByte(0)
            }
        }
    }

    private fun readBundle(dis: DataInputStream): Bundle {
        val bundle = Bundle()
        val size = dis.readInt()
        repeat(size) {
            val key = dis.readUTF()
            when (dis.readByte().toInt()) {
                VAL_INT -> bundle.putInt(key, dis.readInt())
                VAL_STRING -> bundle.putString(key, dis.readUTF())
                VAL_BITMAP -> {
                    val bitmapSize = dis.readInt()
                    val bitmapBytes = ByteArray(bitmapSize)
                    dis.readFully(bitmapBytes)
                    val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapSize)
                    bundle.putParcelable(key, bitmap)
                }
            }
        }
        return bundle
    }

    fun intentToUri(intent: Intent): String {
        return intent.toUri(Intent.URI_INTENT_SCHEME)
    }

    fun uriToIntent(uri: String): Intent {
        return Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
    }
}
