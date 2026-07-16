package de.szalkowski.activitylauncher.entrypoint

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
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getParcelableExtraCompat
import de.szalkowski.activitylauncher.databinding.ActivityMainBinding
import de.szalkowski.activitylauncher.domain.external.AdManager
import de.szalkowski.activitylauncher.domain.external.AnalyticsLogger
import de.szalkowski.activitylauncher.domain.favorites.FavoritesRepository
import de.szalkowski.activitylauncher.domain.launcher.ViewIntentParser
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.LaunchSource
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.recents.RecentsRepository
import de.szalkowski.activitylauncher.domain.settings.SettingsRepository
import de.szalkowski.activitylauncher.domain.usecase.external.CalculateSupportReminderUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.presentation.common.ActionBarSearch
import de.szalkowski.activitylauncher.presentation.common.DisclaimerDialogFragment
import de.szalkowski.activitylauncher.presentation.common.PaidDialogFragment
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ActionBarSearch {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    @Inject
    internal lateinit var settingsRepository: SettingsRepository

    @Inject
    internal lateinit var favoritesRepository: FavoritesRepository

    @Inject
    internal lateinit var recentsRepository: RecentsRepository

    @Inject
    internal lateinit var viewIntentParser: ViewIntentParser

    @Inject
    internal lateinit var packageRepository: PackageRepository

    @Inject
    internal lateinit var adManager: AdManager

    @Inject
    internal lateinit var analyticsLogger: AnalyticsLogger

    @Inject
    internal lateinit var getActivityIconUseCase: GetActivityIconUseCase

    @Inject
    internal lateinit var calculateSupportReminderUseCase: CalculateSupportReminderUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        settingsRepository.applyLocaleConfiguration(baseContext)
        if (!settingsRepository.disclaimerAccepted) {
            DisclaimerDialogFragment().show(supportFragmentManager, "DisclaimerDialogFragment")
        } else if (calculateSupportReminderUseCase()) {
            PaidDialogFragment().show(supportFragmentManager, "PaidDialogFragment")
        }

        adManager.loadBanner(this, binding.adContainer.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        analyticsLogger.logDestination(navController.currentDestination)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            analyticsLogger.logDestination(destination)
        }

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
            if ((item.itemId == R.id.PackageListFragment) && !packageRepository.isLoaded) {
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
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.LoadingFragment,
                R.id.PackageListFragment,
                R.id.FavoritesFragment,
                R.id.RecentsFragment,
                R.id.ShortcutsFragment,
            ),
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

                R.id.FavoritesFragment, R.id.RecentsFragment, R.id.ShortcutsFragment -> {
                    appBarLayout.visibility = View.VISIBLE
                    searchContainer?.visibility = View.GONE
                    params.scrollFlags =
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
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
                    params.scrollFlags =
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                    toolbar.layoutParams = params
                }
            }
        }

        // Handle initial navigation if not already deep-linked
        if (savedInstanceState == null) {
            handleIntent(intent, navController)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        handleIntent(intent, navController)
    }

    private fun handleIntent(intent: Intent?, navController: NavController) {
        if (intent == null) {
            navigateDefault(navController)
            return
        }

        val launchRedirect = intent.getParcelableExtraCompat(EXTRA_SHORTCUT_LAUNCH_REDIRECT, LaunchRequest::class.java)
        val launchRequestFromIntent = launchRedirect ?: viewIntentParser.parseLaunchRequest(intent)

        val shortcutRequest = launchRequestFromIntent?.toShortcutRequest(packageRepository, getActivityIconUseCase)
        val componentName = shortcutRequest?.intent?.component

        if (shortcutRequest != null && componentName != null) {
            // Add deep links and redirects to recents
            // Only update metadata if it was a primary launch request from intent
            recentsRepository.addActivity(
                shortcutRequest,
                updateMetadata = launchRequestFromIntent.source == LaunchSource.PRIMARY,
            )

            // Establish the home destination (Favorites or Recents) as the root
            navigateDefault(navController)

            // Ensure we start from PackageListFragment to build the correct backstack
            if (navController.currentDestination?.id != R.id.PackageListFragment) {
                navController.navigate(R.id.PackageListFragment)
            }

            val packageBundle = Bundle().apply {
                putString("packageName", componentName.packageName)
            }
            navController.navigate(R.id.ActivityListFragment, packageBundle)

            val bundle = Bundle().apply {
                putParcelable("shortcutRequest", shortcutRequest)
            }
            navController.navigate(R.id.ActivityDetailsFragment, bundle)
            return
        }

        navigateDefault(navController)
    }

    private fun navigateDefault(navController: NavController) {
        if (favoritesRepository.getFavorites().isNotEmpty()) {
            navController.navigate(R.id.FavoritesFragment)
        } else if (recentsRepository.getRecentActivities().isNotEmpty()) {
            navController.navigate(R.id.RecentsFragment)
        } else {
            navigateToAll(navController)
        }
    }

    override fun onDestroy() {
        adManager.removeBanner(binding.adContainer.root)
        super.onDestroy()
    }

    private fun navigateToAll(navController: NavController) {
        if (packageRepository.isLoaded) {
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
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object {
        const val EXTRA_SHORTCUT_LAUNCH_REDIRECT = "shortcutLaunchRedirect"
    }
}
