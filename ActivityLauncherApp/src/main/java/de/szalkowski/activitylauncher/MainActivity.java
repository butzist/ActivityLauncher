package de.szalkowski.activitylauncher;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {

    protected final String LOG = "de.szalkowski.activitylauncher.MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPreferences(Context.MODE_PRIVATE).getBoolean("disclaimer_accepted", false)) {
            DialogFragment dialog = new DisclaimerDialogFragment();
            dialog.show(getSupportFragmentManager(), "DisclaimerDialogFragment");
        }

        Fragment fragment = new AllTasksListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment).commit();
    }

    /**
     * Backward-compatible version of {@link ActionBar#getThemedContext()} that
     * simply returns the {@link Activity} if
     * {@code getThemedContext} is unavailable.
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
        return true;
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
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        super.onSaveInstanceState(outState);
    }
}
