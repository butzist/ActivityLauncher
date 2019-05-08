package de.szalkowski.activitylauncher

import android.content.ComponentName
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException

import java.util.HashMap

internal class PackageManagerCache private constructor(private val pm: PackageManager) {
    private val packageInfos: MutableMap<String, MyPackageInfo>
    private val activityInfos: MutableMap<ComponentName, MyActivityInfo>

    val allPackageInfo: Array<MyPackageInfo>?
        get() = null

    init {
        this.packageInfos = HashMap()
        this.activityInfos = HashMap()
    }

    @Throws(NameNotFoundException::class)
    fun getPackageInfo(packageName: String): MyPackageInfo? {
        var myInfo: MyPackageInfo

        synchronized(packageInfos) {
            if (packageInfos.containsKey(packageName)) {
                return packageInfos[packageName]
            }

            val info: PackageInfo
            info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)

            myInfo = MyPackageInfo(info, pm, this)
            packageInfos.put(packageName, myInfo)
        }

        return myInfo
    }

    fun getActivityInfo(activityName: ComponentName): MyActivityInfo? {
        var myInfo: MyActivityInfo

        synchronized(activityInfos) {
            if (activityInfos.containsKey(activityName)) {
                return activityInfos[activityName]
            }

            myInfo = MyActivityInfo(activityName, pm)
            activityInfos.put(activityName, myInfo)
        }

        return myInfo
    }

    companion object {
        private var instance: PackageManagerCache? = null

        fun getPackageManagerCache(pm: PackageManager): PackageManagerCache {
            if (instance == null) {
                instance = PackageManagerCache(pm)
            }
            return instance!!
        }
    }
}
