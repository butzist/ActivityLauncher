package de.szalkowski.activitylauncher.entrypoint

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.data.packages.PackageDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dataSource: PackageDataSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                scope.launch {
                    dataSource.loadDetails(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                scope.launch {
                    dataSource.removePackage(packageName)
                }
            }
        }
    }
}
