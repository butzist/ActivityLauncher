package de.szalkowski.activitylauncher;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class MainActivity extends FragmentActivity {

    private final String LOG = "de.szalkowski.activitylauncher.MainActivity";
    private Filterable filterTarget = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPreferences(Context.MODE_PRIVATE).getBoolean("disclaimer_accepted", false)) {
            DialogFragment dialog = new DisclaimerDialogFragment();
            dialog.show(getSupportFragmentManager(), "DisclaimerDialogFragment");
        }

        AllTasksListFragment fragment = new AllTasksListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment).commit();
        filterTarget = fragment;
    }

    /**
     * Backward-compatible version of {@link ActionBar#getThemedContext()} that
     * simply returns the {@link android.app.Activity} if
     * <code>getThemedContext</code> is unavailable.
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private Context getActionBarThemedContextCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return getActionBar().getThemedContext();
        } else {
            return this;
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
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
        switch (item.getItemId()) {
            case R.id.action_view_source:
                Intent i2 = new Intent(Intent.ACTION_VIEW);
                i2.setData(Uri.parse(this.getString(R.string.url_source)));
                this.startActivity(i2);
                return true;

            case R.id.action_view_translation:
                Intent i3 = new Intent(Intent.ACTION_VIEW);
                i3.setData(Uri.parse(this.getString(R.string.url_translation)));
                this.startActivity(i3);
                return true;

            case R.id.action_view_bugs:
                Intent i4 = new Intent(Intent.ACTION_VIEW);
                i4.setData(Uri.parse(this.getString(R.string.url_bugs)));
                this.startActivity(i4);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Serialize the current dropdown position.
        super.onSaveInstanceState(outState);
    }
}
