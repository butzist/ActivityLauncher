package de.szalkowski.activitylauncher.entrypoint

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {
    @Inject
    lateinit var packageRepository: PackageRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                        packageRepository.loadDetails(packageName)
                    }
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                        if (!isReplacing) {
                            packageRepository.removePackage(packageName)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
