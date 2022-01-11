package de.szalkowski.activitylauncher;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;


public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        var prefs = getActivity().getPreferences(Context.MODE_PRIVATE);

        SwitchPreference privateActivities = findPreference("private_activities");
        SwitchPreference rootmode = findPreference("root_mode");

        ListPreference theme = findPreference("theme");
        ListPreference languages = findPreference("language");

        theme.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        languages.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());

        String[] locales = getResources().getStringArray(R.array.languages);
        ArrayList<String> language = new ArrayList<>();
        for(String locale : locales){
            language.add(toDisplayName(locale));
        }
        String[] languageValue = language.toArray(new String[0]);
        languages.setEntries(languageValue);
        languages.setEntryValues(locales);

        privateActivities.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                prefs.edit().putBoolean("private_activity",(Boolean) newValue).apply();
                return true;
            }
        });
        rootmode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                boolean isChecked = (Boolean) newValue;
                if(isChecked){
                    var hasSU = RootDetection.detectSU();
                    if(hasSU){
                        prefs.edit().putBoolean("allow_root", hasSU).apply();
                    }else{
                        Toast.makeText(getActivity(), getText(R.string.root_check), Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
                return true;
            }
        });
        theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                Utils.setTheme(newValue.toString());
                return true;
            }
        });
        languages.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                prefs.edit().putString("locale", (String)newValue).apply();
                Configuration config = Utils.setLocale(newValue.toString());
                getActivity().getBaseContext().getResources().updateConfiguration(config,
                        getActivity().getBaseContext().getResources().getDisplayMetrics());
                getActivity().recreate();
                return true;
            }
        });
    }

    public String toDisplayName(String name) {
        for (Locale locale : Locale.getAvailableLocales()) {
            if (name.equals(locale.getLanguage() + '_' + locale.getCountry())){
                return locale.getDisplayName();
            }
        }
        return name;
    }
}