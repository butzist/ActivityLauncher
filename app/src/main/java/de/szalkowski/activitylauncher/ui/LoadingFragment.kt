package de.szalkowski.activitylauncher.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.FavoritesService
import de.szalkowski.activitylauncher.services.PackageListService
import de.szalkowski.activitylauncher.services.RecentActivitiesService
import de.szalkowski.activitylauncher.services.ViewIntentParserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class LoadingFragment : Fragment() {
    @Inject
    internal lateinit var packageListService: Provider<PackageListService>

    @Inject
    internal lateinit var favoritesService: FavoritesService

    @Inject
    internal lateinit var recentActivitiesService: RecentActivitiesService

    @Inject
    internal lateinit var viewIntentParserService: ViewIntentParserService

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val intent = activity?.intent
        lifecycleScope.launch {
            var hasFavorites = false
            var hasRecents = false
            var hasIntent = false

            withContext(Dispatchers.IO) {
                // preload package list
                packageListService.get()
                
                // Check if there is a deep link intent that we should handle directly
                if (intent != null && viewIntentParserService.packageFromIntent(intent) != null) {
                    hasIntent = true
                } else {
                    hasFavorites = favoritesService.getFavorites().isNotEmpty()
                    hasRecents = recentActivitiesService.getRecentActivities().isNotEmpty()
                }
            }

            withResumed {
                // navigate to next screen
                if (hasIntent) {
                    val action = LoadingFragmentDirections.actionLoadingFinished()
                    findNavController().navigate(action)
                } else if (hasFavorites) {
                    val action = LoadingFragmentDirections.actionLoadingToFavorites()
                    findNavController().navigate(action)
                } else if (hasRecents) {
                    val action = LoadingFragmentDirections.actionLoadingToRecents()
                    findNavController().navigate(action)
                } else {
                    val action = LoadingFragmentDirections.actionLoadingFinished()
                    findNavController().navigate(action)
                }
            }
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }
}
