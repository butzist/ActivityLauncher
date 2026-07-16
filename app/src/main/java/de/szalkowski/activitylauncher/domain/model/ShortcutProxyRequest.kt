package de.szalkowski.activitylauncher.domain.model

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import de.szalkowski.activitylauncher.core.util.parseIntentUriOrNull
import de.szalkowski.activitylauncher.core.util.readParcelableCompat
import de.szalkowski.activitylauncher.domain.launcher.ShortcutCreator

/**
 * Encapsulates the minimal data required by a shortcut plugin or the system shortcut manager
 * to create a shortcut. The [intent] is already the final LAUNCH_SHORTCUT intent string.
 */
data class ShortcutProxyRequest(
    val name: String,
    val icon: ActivityIcon,
    val intent: Intent,
    val source: LaunchSource,
) : Parcelable {
    fun toShortcutRequest(): ShortcutRequest {
        val targetIntent = if (intent.action == ShortcutCreator.INTENT_LAUNCH_SHORTCUT) {
            val targetUri = intent.getStringExtra(ShortcutCreator.INTENT_EXTRA_INTENT)
            parseIntentUriOrNull(targetUri, Intent.URI_INTENT_SCHEME) ?: intent
        } else {
            intent
        }
        return ShortcutRequest(name, targetIntent, icon, source = source)
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readParcelableCompat(ActivityIcon::class.java)!!,
        parcel.readParcelableCompat(Intent::class.java)!!,
        LaunchSource.valueOf(parcel.readString() ?: LaunchSource.SAVED.name),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(icon, flags)
        parcel.writeParcelable(intent, flags)
        parcel.writeString(source.name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ShortcutProxyRequest> {
        override fun createFromParcel(parcel: Parcel): ShortcutProxyRequest = ShortcutProxyRequest(parcel)
        override fun newArray(size: Int): Array<ShortcutProxyRequest?> = arrayOfNulls(size)
    }
}
