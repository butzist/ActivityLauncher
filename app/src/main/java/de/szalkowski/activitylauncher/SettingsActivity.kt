package de.szalkowski.activitylauncher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.services.SettingsService
import de.szalkowski.activitylauncher.ui.SettingsFragment
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.activity_settings)

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, SettingsFragment()).commit()
    }
}