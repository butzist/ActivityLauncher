package de.szalkowski.activitylauncher

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.databinding.ActivityMainBinding
import de.szalkowski.activitylauncher.services.FavoritesService
import de.szalkowski.activitylauncher.services.PackageListService
import de.szalkowski.activitylauncher.services.RecentActivitiesService
import de.szalkowski.activitylauncher.services.SettingsService
import de.szalkowski.activitylauncher.services.ViewIntentParserService
import de.szalkowski.activitylauncher.ui.ActionBarSearch
import de.szalkowski.activitylauncher.ui.DisclaimerDialogFragment
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActionBarSearch {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @Inject
    internal lateinit var settingsService: SettingsService

    @Inject
    internal lateinit var favoritesService: FavoritesService

    @Inject
    internal lateinit var recentActivitiesService: RecentActivitiesService

    @Inject
    internal lateinit var viewIntentParserService: ViewIntentParserService

    @Inject
    internal lateinit var packageListService: PackageListService

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

        val tilSearch = findViewById<TextInputLayout>(R.id.tilSearch)
        tilSearch.setEndIconOnClickListener {
            actionBarSearchText = ""
        }

        progressBar = findViewById(R.id.pbSearch)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.PackageListFragment && !packageListService.isLoaded) {
                navController.navigate(R.id.LoadingFragment)
                true
            } else {
                // Default behavior for other items or if already loaded
                if (item.itemId != navController.currentDestination?.id) {
                    navController.navigate(item.itemId)
                }
                true
            }
        }

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

        val searchContainer = findViewById<View>(R.id.searchContainer)
        val appBarLayout = findViewById<AppBarLayout>(R.id.appBar)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Update bottom navigation selection to reflect current destination
            binding.bottomNavigation.menu.findItem(destination.id)?.isChecked = true

            // Bottom Navigation visibility
            binding.bottomNavigation.visibility = View.VISIBLE

            // AppBar and Search Bar visibility
            val params = toolbar.layoutParams as AppBarLayout.LayoutParams
            when (destination.id) {
                R.id.LoadingFragment -> {
                    appBarLayout.visibility = View.VISIBLE
                    searchContainer?.visibility = View.GONE
                }
                R.id.FavoritesFragment, R.id.RecentsFragment -> {
                    appBarLayout.visibility = View.VISIBLE
                    searchContainer?.visibility = View.GONE
                    params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                    toolbar.layoutParams = params
                }
                R.id.ActivityDetailsFragment -> {
                    appBarLayout.visibility = View.VISIBLE
                    searchContainer?.visibility = View.GONE
                    params.scrollFlags = 0 // Fixed toolbar for details
                    toolbar.layoutParams = params
                    appBarLayout.setExpanded(true, true)
                }
                else -> {
                    appBarLayout.visibility = View.VISIBLE
                    searchContainer?.visibility = View.VISIBLE
                    params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                            AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                    toolbar.layoutParams = params
                }
            }
        }

        // Handle initial navigation if not already deep-linked
        if (savedInstanceState == null) {
            val intent = intent
            val hasIntent = intent != null && viewIntentParserService.packageFromIntent(intent) != null
            
            if (hasIntent) {
                navigateToAll(navController)
            } else if (favoritesService.getFavorites().isNotEmpty()) {
                navController.navigate(R.id.FavoritesFragment)
            } else if (recentActivitiesService.getRecentActivities().isNotEmpty()) {
                navController.navigate(R.id.RecentsFragment)
            } else {
                navigateToAll(navController)
            }
        }
    }

    private fun navigateToAll(navController: NavController) {
        if (packageListService.isLoaded) {
            navController.navigate(R.id.PackageListFragment)
        } else {
            navController.navigate(R.id.LoadingFragment)
        }
    }

    override var onActionBarSearchListener: ((String) -> Unit)? = null
    private var actionBarSearchView: TextInputEditText? = null
    private var progressBar: CircularProgressIndicator? = null

    override var actionBarSearchText: String
        get() = actionBarSearchView?.text.toString()
        set(value) {
            actionBarSearchView?.setText(value)
        }

    override var isSearching: Boolean
        get() = progressBar?.visibility == View.VISIBLE
        set(value) {
            progressBar?.visibility = if (value) View.VISIBLE else View.GONE
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
