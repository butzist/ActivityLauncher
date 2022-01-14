package de.szalkowski.activitylauncher;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {

    private Filterable filterTarget = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        var prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (!prefs.getBoolean("disclaimer_accepted", false)) {
            DialogFragment dialog = new DisclaimerDialogFragment();
            dialog.show(getSupportFragmentManager(), "DisclaimerDialogFragment");
        }
        Configuration config = Utils.createLocaleConfiguration(prefs.getString("locale","en_US"));
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

        AllTasksListFragment fragment = new AllTasksListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment).commit();
        filterTarget = fragment;
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setQueryHint(this.getText(R.string.filter_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                onFilter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onFilter(newText);
                return true;
            }
        });

        return true;
    }

    private void onFilter(String query) {
        Filter filter = filterTarget.getFilter();
        if (filter != null) {
            filter.filter(query);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Serialize the current dropdown position.
        super.onSaveInstanceState(outState);
    }

    public boolean isRootAllowed() {
        return PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("allow_root", false);
    }
}
