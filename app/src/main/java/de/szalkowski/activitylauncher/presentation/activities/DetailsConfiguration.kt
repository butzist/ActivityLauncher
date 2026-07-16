package de.szalkowski.activitylauncher.presentation.activities

import android.os.Parcel
import android.os.Parcelable

data class DetailsConfiguration(
    val showCreateShortcut: Boolean = true,
    val showLaunch: Boolean = true,
    val showShare: Boolean = true,
    val showFavorite: Boolean = true,
    val showSave: Boolean = true,
    val showLaunchPluginSelection: Boolean = false,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (showCreateShortcut) 1 else 0)
        parcel.writeByte(if (showLaunch) 1 else 0)
        parcel.writeByte(if (showShare) 1 else 0)
        parcel.writeByte(if (showFavorite) 1 else 0)
        parcel.writeByte(if (showSave) 1 else 0)
        parcel.writeByte(if (showLaunchPluginSelection) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DetailsConfiguration> {
        override fun createFromParcel(parcel: Parcel): DetailsConfiguration {
            return DetailsConfiguration(parcel)
        }

        override fun newArray(size: Int): Array<DetailsConfiguration?> {
            return arrayOfNulls(size)
        }

        val ALL = DetailsConfiguration(
            showCreateShortcut = true,
            showLaunch = true,
            showShare = true,
            showFavorite = true,
            showSave = false,
            showLaunchPluginSelection = false,
        )

        val FAVORITES = DetailsConfiguration(
            showCreateShortcut = true,
            showLaunch = false,
            showShare = true,
            showFavorite = true,
            showSave = true,
            showLaunchPluginSelection = true,
        )

        val RECENTS = DetailsConfiguration(
            showCreateShortcut = true,
            showLaunch = true,
            showShare = true,
            showFavorite = true,
            showSave = true,
            showLaunchPluginSelection = false,
        )

        val SHORTCUTS = DetailsConfiguration(
            showCreateShortcut = false,
            showLaunch = true,
            showShare = true,
            showFavorite = true,
            showSave = true,
            showLaunchPluginSelection = true,
        )
    }
}
