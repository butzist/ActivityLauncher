package de.szalkowski.activitylauncher

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.databinding.ActivityMainBinding
import de.szalkowski.activitylauncher.services.SettingsService
import de.szalkowski.activitylauncher.ui.ActionBarSearch
import de.szalkowski.activitylauncher.ui.DisclaimerDialogFragment
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActionBarSearch {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @Inject
    internal lateinit var settingsService: SettingsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        settingsService.applyLocaleConfiguration(baseContext)
        if (!settingsService.disclaimerAccepted) {
            DisclaimerDialogFragment().show(supportFragmentManager, "DisclaimerDialogFragment")
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        actionBarSearchView = findViewById(R.id.tiSearch)
        actionBarSearchView?.addTextChangedListener {
            val query = it.toString()
            onActionBarSearchListener?.invoke(query)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        // define top level destinations (no back button)
        appBarConfiguration =
            AppBarConfiguration(
                setOf(
                    R.id.LoadingFragment,
                    R.id.PackageListFragment,
                    R.id.FavoritesFragment,
                    R.id.RecentsFragment
                )
            )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override var onActionBarSearchListener: ((String) -> Unit)? = null
    private var actionBarSearchView: TextInputEditText? = null
    override var actionBarSearchText: String
        get() = actionBarSearchView?.text.toString()
        set(value) {
            actionBarSearchView?.setText(value)
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
