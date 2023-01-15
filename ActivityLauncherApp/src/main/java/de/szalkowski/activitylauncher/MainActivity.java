package de.szalkowski.activitylauncher;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int SEARCH_QUERY_THRESHOLD = 2;

    private SharedPreferences prefs;
    private String localeString;
    private SearchView searchView;
    private Filterable filterTarget = null;
    private String filter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        this.localeString = prefs.getString("language", "System Default");
        Configuration config = SettingsUtils.createLocaleConfiguration(this.localeString);
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

        if (!this.prefs.getBoolean("disclaimer_accepted", false)) {
            DialogFragment dialog = new DisclaimerDialogFragment();
            dialog.show(getSupportFragmentManager(), "DisclaimerDialogFragment");
        }

        AllTasksListFragment fragment = new AllTasksListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment).commit();
        this.filterTarget = fragment;
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

        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setQueryHint(this.getText(R.string.filter_hint));
        searchView.setSuggestionsAdapter(new SimpleCursorAdapter(
                MainActivity.this, android.R.layout.simple_list_item_1, null,
                new String[] { SearchManager.SUGGEST_COLUMN_TEXT_1 },
                new int[] { android.R.id.text1 }));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() >= SEARCH_QUERY_THRESHOLD) {
                    new AddSearchSuggestionTask().execute(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (query.length() >= SEARCH_QUERY_THRESHOLD) {
                    new FetchSearchSuggestionsTask().execute(query);
                } else {
                    searchView.getSuggestionsAdapter().changeCursor(null);
                }

                MainActivity.this.filter = query;
                MainActivity.this.updateFilter(query);
                return true;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

            @Override
            public boolean onSuggestionSelect(int position) {
                Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
                CharSequence value = cursor.getString(cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1));
                searchView.setQuery(value, false);

                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                return onSuggestionSelect(position);
            }
        });

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!this.localeString.equals(this.prefs.getString("language", "System Default"))) {
            recreateFragments();
        }
        updateFilter(this.filter);
    }

    private void recreateFragments() {
        recreate();
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    private void updateFilter(String query) {
        Filter filter = this.filterTarget.getFilter();
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

    // Добавить новую строку в историю
    private class AddSearchSuggestionTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            Db.insertItem(MainActivity.this, params[0]);
            return null;
        }
    }

    // Достать историю поиска по введенной строке
    private class FetchSearchSuggestionsTask extends AsyncTask<String, Void, Cursor> {

        private final String[] sAutocompleteColNames = new String[] {
                BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1
        };

        @Override
        protected Cursor doInBackground(String... params) {
            MatrixCursor cursor = new MatrixCursor(sAutocompleteColNames);

            List<String> queries = Db.getItems(MainActivity.this, params[0]);
            for (int index = 0; index < queries.size(); index++) {
                Object[] row = new Object[] { index, queries.get(index) };
                cursor.addRow(row);
            }

            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            searchView.getSuggestionsAdapter().changeCursor(result);
        }
    }
}
