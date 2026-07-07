package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import androidx.core.graphics.drawable.IconCompat

data class ShortcutRequest(
    val name: String,
    val intent: Intent,
    val icon: IconCompat,
    val launcherPlugin: ComponentName? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(Intent::class.java.classLoader, Intent::class.java)!!
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable<Intent>(Intent::class.java.classLoader)!!
        },
        IconCompat.createFromBundle(parcel.readBundle(IconCompat::class.java.classLoader)!!)!!,
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            parcel.readParcelable(ComponentName::class.java.classLoader, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            parcel.readParcelable<ComponentName>(ComponentName::class.java.classLoader)
        },
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(intent, flags)
        parcel.writeBundle(icon.toBundle())
        parcel.writeParcelable(launcherPlugin, flags)
    }

    override fun describeContents(): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShortcutRequest) return false

        if (name != other.name) return false
        if (intent.toUri(0) != other.intent.toUri(0)) return false
        return launcherPlugin == other.launcherPlugin
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (intent.toUri(0).hashCode())
        result = 31 * result + (launcherPlugin?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<ShortcutRequest> {
        override fun createFromParcel(parcel: Parcel): ShortcutRequest = ShortcutRequest(parcel)
        override fun newArray(size: Int): Array<ShortcutRequest?> = arrayOfNulls(size)
    }
}
