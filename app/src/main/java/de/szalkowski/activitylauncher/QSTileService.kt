package de.szalkowski.activitylauncher

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.domain.external.AnalyticsLogger
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.N)
@AndroidEntryPoint
class QSTileService : TileService() {
    @Inject
    lateinit var analyticsLogger: AnalyticsLogger

    override fun onTileAdded() {
        super.onTileAdded()
        analyticsLogger.logQsTileAction("add")
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        analyticsLogger.logQsTileAction("remove")
    }

    override fun onClick() {
        super.onClick()
        analyticsLogger.logQsTileAction("clicked")

        val intent = Intent(
            this,
            QSTileNavHostActivity::class.java,
        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

        unlockAndRun {
            @Suppress("StartActivityAndCollapseDeprecated", "DEPRECATION")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return@unlockAndRun startActivityAndCollapse(intent)
            }

            return@unlockAndRun startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
    }
}
