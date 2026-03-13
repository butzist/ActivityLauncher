package de.szalkowski.activitylauncher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.ui.SettingsFragment

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)
        this.setSupportActionBar(findViewById(R.id.toolbar))
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setTitle(R.string.activity_settings)

        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, SettingsFragment()).commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
