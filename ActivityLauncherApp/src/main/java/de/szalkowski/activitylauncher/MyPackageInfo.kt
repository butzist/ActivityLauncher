package de.szalkowski.activitylauncher

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

import java.util.Arrays

class MyPackageInfo internal constructor(info: PackageInfo, pm: PackageManager, cache: PackageManagerCache) : Comparable<MyPackageInfo> {
    var packageName: String
    var icon: Drawable
    protected var icon_resource: Int = 0
    var iconResourceName: String? = null
        protected set
    var name: String
    protected var activities: Array<MyActivityInfo>

    val activitiesCount: Int
        get() = activities.size

    init {
        this.packageName = info.packageName
        val app = info.applicationInfo

        if (app != null) {
            this.name = pm.getApplicationLabel(app).toString()
            try {
                this.icon = pm.getApplicationIcon(app)
            } catch (e: ClassCastException) {
                this.icon = pm.defaultActivityIcon
            }

            this.icon_resource = app.icon
        } else {
            this.name = info.packageName
            this.icon = pm.defaultActivityIcon
            this.icon_resource = 0
        }

        this.iconResourceName = null
        if (this.icon_resource != 0) {
            try {
                this.iconResourceName = pm.getResourcesForApplication(app).getResourceName(this.icon_resource)
            } catch (e: Exception) {
            }

        }

        if (info.activities == null) {
            this.activities = emptyArray()
        } else {
            this.activities = info.activities.mapNotNull { activity ->
                if (activity.isEnabled && activity.exported) {
                    assert(activity.packageName == info.packageName)
                    val acomp = ComponentName(activity.packageName, activity.name)
                    cache.getActivityInfo(acomp)
                } else {
                    null
                }
            }.toTypedArray()

            Arrays.sort(this.activities)
        }
    }

    fun getActivity(i: Int): MyActivityInfo {
        return activities[i]
    }

    override fun compareTo(other: MyPackageInfo): Int {
        val cmp_name = this.name.compareTo(other.name)
        return if (cmp_name != 0) cmp_name else packageName.compareTo(other.packageName)

    }

    override fun equals(other: Any?): Boolean {
        if (other!!.javaClass != MyPackageInfo::class.java) {
            return false
        }

        val other_info = other as MyPackageInfo?
        return this.packageName == other_info!!.packageName
    }
}
