package de.szalkowski.activitylauncher.services

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

interface SettingsService {
    fun init()
    fun getLocaleConfiguration(): Configuration
    fun applyLocaleConfiguration(context: Context)
    fun getCountryName(name: String): String
    fun setTheme(theme: String?)

    var disclaimerAccepted: Boolean
    val hidePrivate: Boolean
    val language: String
    val allowRoot: Boolean
    val theme: String
}

class SettingsServiceImpl @Inject constructor(@ApplicationContext val context: Context) :
    SettingsService {
    companion object {
        const val THEME_DEFAULT = "0"
        const val THEME_LIGHT = "1"
        const val THEME_DARK = "2"

        const val LANGUAGE_DEFAULT = "System Default"

        const val PREF_ALLOW_ROOT = "allow_root"
        const val PREF_THEME = "theme"
        const val PREF_HIDE_HIDE_PRIVATE = "hide_hide_private"
        const val PREF_LANGUAGE = "language"
        const val PREF_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    @Inject
    internal lateinit var rootDetectionService: RootDetectionService

    override fun init() {
        setTheme(theme)

        if (!prefs.contains(PREF_ALLOW_ROOT)) {
            val hasSU = rootDetectionService.detectSU()
            prefs.edit { putBoolean(PREF_ALLOW_ROOT, hasSU) }
        }

        if (!prefs.contains(PREF_HIDE_HIDE_PRIVATE)) {
            prefs.edit { putBoolean(PREF_HIDE_HIDE_PRIVATE, false) }
        }

        if (!prefs.contains(PREF_LANGUAGE)) {
            prefs.edit { putString(PREF_LANGUAGE, LANGUAGE_DEFAULT) }
        }
    }

    override var disclaimerAccepted: Boolean
        get() = prefs.getBoolean(PREF_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(PREF_DISCLAIMER_ACCEPTED, value) }

    override val theme: String
        get() = prefs.getString(PREF_THEME, THEME_DEFAULT)!!

    override val allowRoot: Boolean
        get() = prefs.getBoolean(PREF_ALLOW_ROOT, false)

    override val language: String
        get() = prefs.getString(PREF_LANGUAGE, LANGUAGE_DEFAULT)!!

    override val hidePrivate: Boolean
        get() = prefs.getBoolean(PREF_HIDE_HIDE_PRIVATE, false)

    override fun applyLocaleConfiguration(context: Context) {
        val languageTag = if (language == LANGUAGE_DEFAULT) {
            ""
        } else {
            language.replace("_", "-")
        }
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    override fun getLocaleConfiguration(): Configuration {
        val language = if (language == LANGUAGE_DEFAULT) {
            val systemLocale = ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)
                ?: Locale.getDefault()
            systemLocale.toString()
        } else {
            language
        }

        val config = Configuration()
        if (language.contains("_")) {
            val parts = language.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                Locale.forLanguageTag(language.replace("_", "-"))
            } else {
                // Replaced with Locale.forLanguageTag when minSdk >= 21
                @Suppress("DEPRECATION")
                Locale(parts[0], parts[1])
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLocale(locale)
            } else {
                // Replaced with Configuration.setLocale when minSdk >= 17
                @Suppress("DEPRECATION")
                config.locale = locale
            }
        }

        return config
    }

    override fun getCountryName(name: String): String {
        for (locale in Locale.getAvailableLocales()) {
            if (name == locale.language + '_' + locale.country) {
                val language = locale.getDisplayName(locale)
                return language.substring(0, 1).uppercase(Locale.getDefault()) + language.substring(
                    1,
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
