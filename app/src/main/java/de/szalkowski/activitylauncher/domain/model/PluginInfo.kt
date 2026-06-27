package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.graphics.drawable.IconCompat

data class PluginInfo(
    val name: String,
    val componentName: ComponentName,
    val icon: IconCompat? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readParcelable(ComponentName::class.java.classLoader)!!,
        parcel.readBundle(Bundle::class.java.classLoader)?.let { IconCompat.createFromBundle(it) },
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeParcelable(componentName, flags)
        parcel.writeBundle(icon?.toBundle())
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
