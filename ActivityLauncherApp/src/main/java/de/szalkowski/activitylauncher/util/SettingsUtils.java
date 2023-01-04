package de.szalkowski.activitylauncher.util;

import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class SettingsUtils {
    public final static String THEME_DEFAULT = "0";
    public final static String THEME_LIGHT = "1";
    public final static String THEME_DARK = "2";

    public static Configuration createLocaleConfiguration(String language) {
        Configuration config = new Configuration();
        if (language.contains("_")) {
            String[] parts = language.split("_");
            Locale locale = new Locale(parts[0], parts[1]);
            Locale.setDefault(locale);
            config.locale = locale;
        }
        return config;
    }

    public static String getCountryName(String name) {
        for (Locale locale : Locale.getAvailableLocales()) {
            if (name.equals(locale.getLanguage() + '_' + locale.getCountry())) {
                String language = locale.getDisplayName(locale);
                return language.substring(0, 1).toUpperCase() + language.substring(1);
            }
        }
        return name;
    }

    public static void setTheme(String theme) {
        switch (theme) {
            default:
            case THEME_DEFAULT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }
}
