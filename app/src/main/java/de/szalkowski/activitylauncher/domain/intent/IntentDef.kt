package de.szalkowski.activitylauncher.domain.intent

import android.os.Parcel
import android.os.Parcelable

data class IntentDef(
    val action: String? = null,
    val data: String? = null,
    val mimeType: String? = null,
    val categories: List<String> = emptyList(),
    val extras: List<ExtraDef> = emptyList(),
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.createTypedArrayList(ExtraDef.CREATOR) ?: emptyList(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(action)
        parcel.writeString(data)
        parcel.writeString(mimeType)
        parcel.writeStringList(categories)
        parcel.writeTypedList(extras)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<IntentDef> {
        override fun createFromParcel(parcel: Parcel): IntentDef = IntentDef(parcel)
        override fun newArray(size: Int): Array<IntentDef?> = arrayOfNulls(size)
    }
}

data class ExtraDef(
    val key: String,
    val value: String,
    val type: ExtraType = ExtraType.STRING,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        ExtraType.valueOf(parcel.readString() ?: ExtraType.STRING.name),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
        parcel.writeString(value)
        parcel.writeString(type.name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ExtraDef> {
        override fun createFromParcel(parcel: Parcel): ExtraDef = ExtraDef(parcel)
        override fun newArray(size: Int): Array<ExtraDef?> = arrayOfNulls(size)
    }
}

enum class ExtraType {
    STRING,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    ;

    fun isValid(value: String): Boolean {
        if (value.isEmpty()) return true
        return when (this) {
            STRING -> true
            INT -> value.toIntOrNull() != null
            LONG -> value.toLongOrNull() != null
            FLOAT -> value.toFloatOrNull() != null
            DOUBLE -> value.toDoubleOrNull() != null
            BOOLEAN -> value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)
        }
    }
}
