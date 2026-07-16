package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import de.szalkowski.activitylauncher.core.util.readParcelableCompat

data class ShortcutRequest(
    val name: String,
    val intent: Intent,
    val icon: ActivityIcon,
    val launcherPlugin: ComponentName? = null,
    val source: LaunchSource,
) : Parcelable {
    fun toLaunchRequest(source: LaunchSource? = null): LaunchRequest {
        return LaunchRequest(
            intent = intent,
            name = name,
            icon = icon,
            launcherPlugin = launcherPlugin,
            source = source ?: this.source,
        )
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readParcelableCompat(Intent::class.java)!!,
        parcel.readParcelableCompat(ActivityIcon::class.java)!!,
        parcel.readParcelableCompat(ComponentName::class.java),
        LaunchSource.valueOf(parcel.readString() ?: LaunchSource.SAVED.name),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(intent, flags)
        parcel.writeParcelable(icon, flags)
        parcel.writeParcelable(launcherPlugin, flags)
        parcel.writeString(source.name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ShortcutRequest> {
        override fun createFromParcel(parcel: Parcel): ShortcutRequest = ShortcutRequest(parcel)
        override fun newArray(size: Int): Array<ShortcutRequest?> = arrayOfNulls(size)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ShortcutRequest) return false

        if (name != other.name) return false
        if (intent.toUri(0) != other.intent.toUri(0)) return false
        if (launcherPlugin != other.launcherPlugin) return false
        if (source != other.source) return false
        return icon == other.icon
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (intent.toUri(0).hashCode())
        result = 31 * result + (launcherPlugin?.hashCode() ?: 0)
        result = 31 * result + source.hashCode()
        result = 31 * result + icon.hashCode()
        return result
    }
}
