package de.szalkowski.activitylauncher;

import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;

import java.util.HashMap;
import java.util.Map;

class PackageManagerCache {
    private static PackageManagerCache instance = null;
    private final Map<String, MyPackageInfo> packageInfos;
    private final Map<ComponentName, MyActivityInfo> activityInfos;
    private final PackageManager pm;

    private PackageManagerCache(PackageManager pm) {
        this.pm = pm;
        this.packageInfos = new HashMap<>();
        this.activityInfos = new HashMap<>();
    }

    static PackageManagerCache getPackageManagerCache(PackageManager pm) {
        if (instance == null) {
            instance = new PackageManagerCache(pm);
        }
        return instance;
    }

    static void resetPackageManagerCache() {
        instance = null;
    }

    MyPackageInfo getPackageInfo(String packageName, Configuration config) throws NameNotFoundException {
        MyPackageInfo myInfo;

        synchronized (packageInfos) {
            if (packageInfos.containsKey(packageName)) {
                return packageInfos.get(packageName);
            }

            PackageInfo info;
            info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);

            myInfo = MyPackageInfo.fromPackageInfo(this, info, config);
            packageInfos.put(packageName, myInfo);
        }

        return myInfo;
    }

    MyActivityInfo getActivityInfo(ComponentName activityName, Configuration config) {
        MyActivityInfo myInfo;

        synchronized (activityInfos) {
            if (activityInfos.containsKey(activityName)) {
                return activityInfos.get(activityName);
            }

            myInfo = MyActivityInfo.fromComponentName(pm, activityName, config);
            activityInfos.put(activityName, myInfo);
        }

        return myInfo;
    }

    PackageManager getPackageManager() {
        return this.pm;
    }
}
