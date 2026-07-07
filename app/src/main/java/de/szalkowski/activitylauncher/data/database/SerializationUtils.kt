package de.szalkowski.activitylauncher.data.database

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.core.graphics.drawable.IconCompat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object SerializationUtils {
    private const val VAL_INT = 1
    private const val VAL_STRING = 2
    private const val VAL_BITMAP = 3

    fun iconToByteArray(icon: IconCompat): ByteArray {
        val bundle = icon.toBundle()
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

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

                else -> {
                    dos.writeByte(0)
                }
            }
        }

        dos.flush()
        return baos.toByteArray()
    }

    fun byteArrayToIcon(bytes: ByteArray): IconCompat? {
        val bais = ByteArrayInputStream(bytes)
        val dis = DataInputStream(bais)

        return try {
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
            IconCompat.createFromBundle(bundle)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun intentToUri(intent: Intent): String {
        return intent.toUri(Intent.URI_INTENT_SCHEME)
    }

    fun uriToIntent(uri: String): Intent {
        return Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
    }
}
