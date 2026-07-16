package de.szalkowski.activitylauncher.domain.model

import android.content.ComponentName
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import de.szalkowski.activitylauncher.core.util.readParcelableCompat
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase

data class LaunchRequest(
    val intent: Intent,
    val name: String? = null,
    val icon: ActivityIcon? = null,
    val launcherPlugin: ComponentName? = null,
    val source: LaunchSource,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelableCompat(Intent::class.java)!!,
        parcel.readString(),
        parcel.readParcelableCompat(ActivityIcon::class.java),
        parcel.readParcelableCompat(ComponentName::class.java),
        LaunchSource.valueOf(parcel.readString() ?: LaunchSource.SAVED.name),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(intent, flags)
        parcel.writeString(name)
        parcel.writeParcelable(icon, flags)
        parcel.writeParcelable(launcherPlugin, flags)
        parcel.writeString(source.name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<LaunchRequest> {
        override fun createFromParcel(parcel: Parcel): LaunchRequest = LaunchRequest(parcel)
        override fun newArray(size: Int): Array<LaunchRequest?> = arrayOfNulls(size)
    }

    fun toShortcutRequest(
        packageRepository: PackageRepository,
        getActivityIconUseCase: GetActivityIconUseCase,
    ): ShortcutRequest {
        val component = intent.component
            ?: throw IllegalArgumentException("Intent must have a component")

        val activityInfo = runCatching { packageRepository.getActivity(component) }.getOrNull()
        val resolvedName = name ?: activityInfo?.name ?: component.className.substringAfterLast('.')
        val resolvedIcon = icon ?: getActivityIconUseCase(activityInfo?.iconResourceName, component)

        return ShortcutRequest(
            name = resolvedName,
            intent = intent,
            icon = resolvedIcon,
            launcherPlugin = launcherPlugin,
            source = source,
        )
    }
}
