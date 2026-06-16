package de.szalkowski.activitylauncher.domain.settings

import android.content.Context
import android.content.res.Configuration

interface SettingsRepository {
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
