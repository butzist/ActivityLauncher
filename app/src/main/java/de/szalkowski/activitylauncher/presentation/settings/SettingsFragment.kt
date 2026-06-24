package de.szalkowski.activitylauncher.presentation.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences
    private var needsRestart: Boolean = false

    @Inject
    internal lateinit var settingsRepository: SettingsRepository

    @Inject
    internal lateinit var packageRepository: PackageRepository

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

        val hidePrivate: SwitchPreferenceCompat = findPreference("hide_private")!!
        val theme: ListPreference = findPreference("theme")!!
        val languages: ListPreference = findPreference("language")!!

        languages.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())
        populateLanguages(languages)
        languages.setOnPreferenceChangeListener { _, newValue ->
            onLanguageUpdated(
                newValue as String,
            )
        }

        hidePrivate.setOnPreferenceChangeListener { _, newValue ->
            onHidePrivateUpdated(
                newValue as Boolean,
            )
        }

        theme.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance())
        theme.setOnPreferenceChangeListener { _, newValue -> onThemeUpdated(newValue as String) }
    }

    private fun populateLanguages(languages: ListPreference) {
        val languageValues = resources.getStringArray(R.array.locales)
            .map { locale -> settingsRepository.getCountryName(locale) }
        languages.entries = languageValues.toTypedArray()
    }

    private fun onThemeUpdated(newValue: String): Boolean {
        prefs.edit().putString("theme", newValue).apply()
        settingsRepository.setTheme(newValue)
        return true
    }

    private fun onHidePrivateUpdated(newValue: Boolean): Boolean {
        prefs.edit().putBoolean("hide_hide_private", newValue).apply()
        packageRepository.invalidate()
        needsRestart = true
        return true
    }

    private fun onLanguageUpdated(newValue: String): Boolean {
        prefs.edit().putString("language", newValue).apply()

        settingsRepository.applyLocaleConfiguration(requireActivity().baseContext)
        packageRepository.invalidate()
        needsRestart = true
        return true
    }
}
