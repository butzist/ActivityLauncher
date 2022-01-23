package de.szalkowski.activitylauncher;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Objects;


public class SettingsFragment extends PreferenceFragmentCompat {
    private SharedPreferences prefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().getBaseContext());;

        SwitchPreference hidePrivate = Objects.requireNonNull(findPreference("hide_private"));
        SwitchPreference allowRoot = Objects.requireNonNull(findPreference("allow_root"));
        ListPreference theme = Objects.requireNonNull(findPreference("theme"));
        ListPreference languages = Objects.requireNonNull(findPreference("language"));

        theme.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        languages.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

        populateLanguages(languages);

        hidePrivate.setOnPreferenceChangeListener((preference, newValue) -> onHidePrivateUpdated((Boolean) newValue));
        allowRoot.setOnPreferenceChangeListener((preference, newValue) -> onAllowRootUpdated((Boolean) newValue));
        theme.setOnPreferenceChangeListener((preference, newValue) -> onThemeUpdated((String) newValue));
        languages.setOnPreferenceChangeListener((preference, newValue) -> onLanguageUpdated((String) newValue));
    }

    private void populateLanguages(ListPreference languages) {
        String[] locales = getResources().getStringArray(R.array.locales);
        ArrayList<String> language = new ArrayList<>();
        for (String locale : locales) {
            language.add(SettingsUtils.getCountryName(locale));
        }
        String[] languageValue = language.toArray(new String[0]);
        languages.setEntries(languageValue);
    }

    private boolean onAllowRootUpdated(boolean newValue) {
        var hasSU = RootDetection.detectSU();

        if (newValue && !hasSU) {
            Toast.makeText(getActivity(), getText(R.string.warning_root_check), Toast.LENGTH_LONG).show();
        }

        prefs.edit().putBoolean("allow_root", newValue).apply();
        return true;
    }

    private boolean onThemeUpdated(String newValue) {
        this.prefs.edit().putString("theme", newValue).apply();
        SettingsUtils.setTheme(newValue);
        return true;
    }

    private boolean onHidePrivateUpdated(boolean newValue) {
        this.prefs.edit().putBoolean("hide_hide_private", newValue).apply();
        return true;
    }

    private boolean onLanguageUpdated(String newValue) {
        this.prefs.edit().putString("language", newValue).apply();
        Configuration config;
        if (newValue.equals("System Default")) {
            config = SettingsUtils.createLocaleConfiguration(Resources.getSystem().getConfiguration().locale.toString());
        } else {
            config = SettingsUtils.createLocaleConfiguration(newValue);
        }

        var resources = requireActivity().getBaseContext().getResources();
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        PackageManagerCache.resetPackageManagerCache();
        requireActivity().recreate();
        return true;
    }
}
