package de.szalkowski.activitylauncher

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.services.ActivityLauncherService
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutActivity : AppCompatActivity() {
    @Inject
    internal lateinit var activityLauncherService: ActivityLauncherService

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val launchIntent = Intent.parseUri(intent.getStringExtra("extra_intent"), 0)
            activityLauncherService.launchActivity(launchIntent.component!!,
                asRoot = false,
                showToast = false
            );
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            finish()
        }
    }
}

