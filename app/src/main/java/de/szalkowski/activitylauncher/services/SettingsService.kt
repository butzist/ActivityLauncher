package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

const val THEME_DEFAULT = "0"
const val THEME_LIGHT = "1"
const val THEME_DARK = "2"

interface SettingsService {
    fun getLocaleConfiguration(): Configuration
    fun getCountryName(name: String): String
    fun setTheme(theme: String?)

    fun init()

    fun applyLocaleConfiguration(context: Context)
}

class SettingsServiceImpl @Inject constructor(@ApplicationContext val context: Context) : SettingsService {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context);

    @Inject
    internal lateinit var rootDetectionService: RootDetectionService

    override fun init() {
        setTheme(prefs.getString("theme", "0"))

        if (!prefs.contains("allow_root")) {
            val hasSU = rootDetectionService.detectSU()
            prefs.edit().putBoolean("allow_root", hasSU).apply()
        }

        if (!prefs.contains("hide_hide_private")) {
            prefs.edit().putBoolean("hide_hide_private", false).apply()
        }

        if (!prefs.contains("language")) {
            prefs.edit().putString("language", "System Default").apply()
        }
    }

    override fun applyLocaleConfiguration(context: Context) {
        val config = getLocaleConfiguration()
        Locale.setDefault(config.locale)
        context.resources.updateConfiguration(config,
            context.resources.displayMetrics);
    }

    override fun getLocaleConfiguration(): Configuration {
        val settingsLanguage = prefs.getString("language", "System Default")!!

        val language = if (settingsLanguage == "System Default") {
            Resources.getSystem().configuration.locale.toString()
        } else {
            settingsLanguage
        }

        val config = Configuration()
        if (language.contains("_")) {
            val parts = language.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val locale = Locale(parts[0], parts[1])
            config.locale = locale // FIXME
        }

        return config
    }

    override fun getCountryName(name: String): String {
        for (locale in Locale.getAvailableLocales()) {
            if (name == locale.language + '_' + locale.country) {
                val language = locale.getDisplayName(locale)
                return language.substring(0, 1).uppercase(Locale.getDefault()) + language.substring(
                    1
                )
            }
        }
        return name
    }

    override fun setTheme(theme: String?) {
        when (theme) {
            THEME_DEFAULT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

