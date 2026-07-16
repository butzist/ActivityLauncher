package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.os.Parcel
import android.os.Parcelable
import de.szalkowski.activitylauncher.core.util.readParcelableCompat

data class PluginInfo(
    val name: String,
    val componentName: ComponentName,
    val icon: ActivityIcon? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readParcelableCompat(ComponentName::class.java)!!,
        parcel.readParcelableCompat(ActivityIcon::class.java),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(componentName, flags)
        parcel.writeParcelable(icon, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PluginInfo> {
        override fun createFromParcel(parcel: Parcel): PluginInfo {
            return PluginInfo(parcel)
        }

        override fun newArray(size: Int): Array<PluginInfo?> {
            return arrayOfNulls(size)
        }
    }
}
