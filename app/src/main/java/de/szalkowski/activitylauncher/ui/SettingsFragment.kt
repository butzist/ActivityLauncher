package de.szalkowski.activitylauncher.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.MainActivity
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.RootDetectionService
import de.szalkowski.activitylauncher.services.SettingsService
import java.util.Objects
import javax.inject.Inject


@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences
    private var needsRestart: Boolean = false

    @Inject
    internal lateinit var rootDetectionService: RootDetectionService

    @Inject
    internal lateinit var settingsService: SettingsService

    override fun onDestroy() {
        super.onDestroy()
        if (!needsRestart) return

        // workaround for applying settings by restarting app - PRs welcome
        // FIXME reset the services state and reload affected activities
        restartApp()
    }

    private fun restartApp() {
        val intent = Intent(
            this.requireContext(),
            MainActivity::class.java,
        )

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        this.startActivity(intent)
        this.requireActivity().finishAffinity()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().baseContext)

        val hidePrivate: SwitchPreference = Objects.requireNonNull(findPreference("hide_private"))
        val allowRoot: SwitchPreference = Objects.requireNonNull(findPreference("allow_root"))
        val theme: ListPreference = Objects.requireNonNull(findPreference("theme"))
        val languages: ListPreference = Objects.requireNonNull(findPreference("language"))

        languages.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())
        populateLanguages(languages)
        languages.setOnPreferenceChangeListener { _, newValue ->
            onLanguageUpdated(
                newValue as String
            )
        }

        hidePrivate.setOnPreferenceChangeListener { _, newValue ->
            onHidePrivateUpdated(
                newValue as Boolean
            )
        }

        allowRoot.setOnPreferenceChangeListener { _, newValue ->
            onAllowRootUpdated(
                newValue as Boolean
            )
        }

        theme.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())
        theme.setOnPreferenceChangeListener { _, newValue -> onThemeUpdated(newValue as String) }
    }

    private fun populateLanguages(languages: ListPreference) {
        val languageValues = resources.getStringArray(R.array.locales)
            .map { locale -> settingsService.getCountryName(locale) }
        languages.entries = languageValues.toTypedArray()
    }

    private fun onAllowRootUpdated(newValue: Boolean): Boolean {
        val hasSU = rootDetectionService.detectSU()
        if (newValue && !hasSU) {
            Toast.makeText(activity, getText(R.string.warning_root_check), Toast.LENGTH_LONG).show()
        }
        prefs.edit().putBoolean("allow_root", newValue).apply()
        needsRestart = true
        return true
    }

    private fun onThemeUpdated(newValue: String): Boolean {
        prefs.edit().putString("theme", newValue).apply()
        settingsService.setTheme(newValue)
        return true
    }

    private fun onHidePrivateUpdated(newValue: Boolean): Boolean {
        prefs.edit().putBoolean("hide_hide_private", newValue).apply()
        needsRestart = true
        return true
    }

    private fun onLanguageUpdated(newValue: String): Boolean {
        prefs.edit().putString("language", newValue).apply()

        settingsService.applyLocaleConfiguration(requireActivity().baseContext)
        needsRestart = true
        return true
    }
}

