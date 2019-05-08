package de.szalkowski.activitylauncher

import android.content.ComponentName
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable

class MyActivityInfo internal constructor(activity: ComponentName, pm: PackageManager) : Comparable<MyActivityInfo> {
    var icon: Drawable
    var name: String
    var componentName: ComponentName
        internal set
    internal var icon_resource: Int = 0
    var iconResouceName: String? = null
        internal set

    init {
        this.componentName = activity

        val act: ActivityInfo
        try {
            act = pm.getActivityInfo(activity, 0)
            this.name = act.loadLabel(pm).toString()
            try {
                this.icon = act.loadIcon(pm)
            } catch (e: ClassCastException) {
                this.icon = pm.defaultActivityIcon
            }

            this.icon_resource = act.iconResource
        } catch (e: NameNotFoundException) {
            this.name = activity.shortClassName
            this.icon = pm.defaultActivityIcon
            this.icon_resource = 0
        }

        this.iconResouceName = null
        if (this.icon_resource != 0) {
            try {
                this.iconResouceName = pm.getResourcesForActivity(activity).getResourceName(this.icon_resource)
            } catch (ignored: Exception) {
            }

        }
    }

    override fun compareTo(another: MyActivityInfo): Int {
        val cmp_name = this.name.compareTo(another.name)
        return if (cmp_name != 0) cmp_name else this.componentName.compareTo(another.componentName)

    }

    override fun equals(other: Any?): Boolean {
        if (other!!.javaClass != MyPackageInfo::class.java) {
            return false
        }

        val other_info = other as MyActivityInfo?
        return this.componentName == other_info!!.componentName
    }
}