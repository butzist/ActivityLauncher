package de.szalkowski.activitylauncher

import android.annotation.TargetApi
import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    protected val LOG = "de.szalkowski.activitylauncher.MainActivity"

    /**
     * Backward-compatible version of [ActionBar.getThemedContext] that
     * simply returns the [android.app.Activity] if
     * `getThemedContext` is unavailable.
     */
    private val actionBarThemedContextCompat: Context
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            actionBar!!.themedContext
        } else {
            this
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!getPreferences(Context.MODE_PRIVATE).getBoolean("disclaimer_accepted", false)) {
            val dialog = DisclaimerDialogFragment()
            dialog.show(supportFragmentManager, "DisclaimerDialogFragment")
        }

        val fragment = AllTasksListFragment()
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment).commit()
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        // Restore the previously serialized current dropdown position.
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_view_source -> {
                val i2 = Intent(Intent.ACTION_VIEW)
                i2.data = Uri.parse(this.getString(R.string.url_source))
                this.startActivity(i2)
                return true
            }

            R.id.action_view_translation -> {
                val i3 = Intent(Intent.ACTION_VIEW)
                i3.data = Uri.parse(this.getString(R.string.url_translation))
                this.startActivity(i3)
                return true
            }

            R.id.action_view_bugs -> {
                val i4 = Intent(Intent.ACTION_VIEW)
                i4.data = Uri.parse(this.getString(R.string.url_bugs))
                this.startActivity(i4)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }


    public override fun onSaveInstanceState(outState: Bundle) {
        // Serialize the current dropdown position.
        super.onSaveInstanceState(outState)
    }
}
