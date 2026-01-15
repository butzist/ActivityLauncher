package de.szalkowski.activitylauncher.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import de.szalkowski.activitylauncher.QSTileNavHostActivity

@RequiresApi(Build.VERSION_CODES.N)
class QSTileService : TileService() {
    override fun onClick() {
        super.onClick()

        val intent = Intent(
            this,
            QSTileNavHostActivity::class.java,
        ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)

        @Suppress("StartActivityAndCollapseDeprecated", "DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return startActivityAndCollapse(intent)
        }

        return startActivityAndCollapse(
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }
}
