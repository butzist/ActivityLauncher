package de.szalkowski.activitylauncher;

import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

public class Utils extends AppCompatActivity {
    public final static String THEME_DEFAULT = "0";
    public final static String THEME_LIGHT = "1";
    public final static String THEME_DARK = "2";

    public static Configuration setLocale(String language) {
        String[] parts = language.split("_");
        Locale locale = new Locale(parts[0],parts[1]);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        return config;
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
