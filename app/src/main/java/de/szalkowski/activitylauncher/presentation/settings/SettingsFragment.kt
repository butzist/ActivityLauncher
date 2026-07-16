package de.szalkowski.activitylauncher.presentation.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.settings.BackupRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.entrypoint.MainActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences
    private var needsRestart: Boolean = false

    @Inject
    internal lateinit var settingsRepository: SettingsRepository

    @Inject
    internal lateinit var packageRepository: PackageRepository

    @Inject
    internal lateinit var backupRepository: BackupRepository

    @SuppressLint("NewApi")
    private val exportBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { performExport(it) }
        }

    @SuppressLint("NewApi")
    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { performImport(it) }
        }

    private fun performExport(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = backupRepository.exportBackup(uri)
            if (result.isSuccess) {
                Toast.makeText(requireContext(), R.string.backup_export_success, Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.backup_export_error, result.exceptionOrNull()?.message),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun performImport(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = backupRepository.importBackup(uri)
            if (result.isSuccess) {
                Toast.makeText(requireContext(), R.string.backup_import_success, Toast.LENGTH_SHORT)
                    .show()
                needsRestart = true
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.backup_import_error, result.exceptionOrNull()?.message),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

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

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            findPreference<androidx.preference.PreferenceCategory>("category_data_management")?.isVisible =
                false
        } else {
            val backupExport: Preference = findPreference("backup_export")!!
            val backupImport: Preference = findPreference("backup_import")!!

            backupExport.setOnPreferenceClickListener {
                exportBackupLauncher.launch("activity_launcher_backup.json")
                true
            }

            backupImport.setOnPreferenceClickListener {
                importBackupLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                true
            }
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
