package de.szalkowski.activitylauncher;

import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Objects;


public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        var prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().getBaseContext());

        SwitchPreference privateActivities = Objects.requireNonNull(findPreference("private_activities"));
        SwitchPreference rootmode = Objects.requireNonNull(findPreference("root_mode"));
        ListPreference theme = Objects.requireNonNull(findPreference("theme"));
        ListPreference languages = Objects.requireNonNull(findPreference("language"));

        theme.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        languages.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

        String[] locales = getResources().getStringArray(R.array.locales);
        ArrayList<String> language = new ArrayList<>();
        for (String locale : locales) {
            language.add(SettingsUtils.getCountryName(locale));
        }

        String[] languageValue = language.toArray(new String[0]);
        languages.setEntries(languageValue);
        languages.setEntryValues(locales);

        privateActivities.setOnPreferenceChangeListener((preference, newValue) -> {
            prefs.edit().putBoolean("hide_private_activities", (Boolean) newValue).apply();
            return true;
        });

        rootmode.setOnPreferenceChangeListener((preference, newValue) -> {
            var hasSU = RootDetection.detectSU();
            boolean newValueBool = (Boolean) newValue;

            if (newValueBool && !hasSU) {
                Toast.makeText(getActivity(), getText(R.string.warning_root_check), Toast.LENGTH_LONG).show();
            }

            prefs.edit().putBoolean("allow_root", newValueBool).apply();
            return true;
        });

        theme.setOnPreferenceChangeListener((preference, newValue) -> {
            prefs.edit().putString("theme", newValue.toString()).apply();
            SettingsUtils.setTheme(newValue.toString());
            return true;
        });

        languages.setOnPreferenceChangeListener((preference, newValue) -> {
            prefs.edit().putString("locale", (String) newValue).apply();
            Configuration config = SettingsUtils.createLocaleConfiguration(newValue.toString());
            var resources = requireActivity().getBaseContext().getResources();
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            requireActivity().recreate();
            return true;
        });
    }


}
