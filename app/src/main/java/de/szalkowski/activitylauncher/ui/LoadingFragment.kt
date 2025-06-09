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
import de.szalkowski.activitylauncher.services.PackageListService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class LoadingFragment : Fragment() {
    @Inject
    internal lateinit var packageListService: Provider<PackageListService>

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                // preload package list
                packageListService.get()
            }

            withResumed {
                // navigate to next screen
                val action = LoadingFragmentDirections.actionLoadingFinished()
                findNavController().navigate(action)
            }
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }
}