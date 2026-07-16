package de.szalkowski.activitylauncher.presentation.activities

import android.os.Parcel
import android.os.Parcelable
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest

sealed class DetailsResult : Parcelable {
    data class Save(val request: ShortcutRequest, val id: String?) : DetailsResult() {
        constructor(parcel: Parcel) : this(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                parcel.readParcelable(ShortcutRequest::class.java.classLoader, ShortcutRequest::class.java)!!
            } else {
                @Suppress("DEPRECATION")
                parcel.readParcelable<ShortcutRequest>(ShortcutRequest::class.java.classLoader)!!
            },
            parcel.readString(),
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeParcelable(request, flags)
            parcel.writeString(id)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<Save> {
            override fun createFromParcel(parcel: Parcel): Save = Save(parcel)
            override fun newArray(size: Int): Array<Save?> = arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        when (this) {
            is Save -> {
                parcel.writeInt(1)
                writeToParcel(parcel, flags)
            }
        }
    }

    companion object CREATOR : Parcelable.Creator<DetailsResult> {
        override fun createFromParcel(parcel: Parcel): DetailsResult {
            return when (parcel.readInt()) {
                1 -> Save(parcel)
                else -> throw IllegalArgumentException("Unknown DetailsResult type")
            }
        }

        override fun newArray(size: Int): Array<DetailsResult?> = arrayOfNulls(size)
    }
}
