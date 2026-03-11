package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentPackageListBinding
import de.szalkowski.activitylauncher.services.ViewIntentParserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class PackageListFragment : Fragment() {
    @Inject
    internal lateinit var packageListAdapter: PackageListAdapter

    @Inject
    internal lateinit var viewIntentParserService: ViewIntentParserService

    private var _binding: FragmentPackageListBinding? = null
    private var filterJob: Job? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actionBar = activity as? ActionBarSearch
        
        // Ensure the adapter has the latest data from the service
        packageListAdapter.updatePackages()

        updateFilter(actionBar?.actionBarSearchText.orEmpty(), false)
        actionBar?.onActionBarSearchListener = { search ->
            updateFilter(search, true)
        }

        packageListAdapter.onItemClick = {
            runCatching {
                val action = PackageListFragmentDirections.actionSelectPackage(it.packageName)
                findNavController().navigate(action)
            }.onFailure { Log.e("Navigation", "Error while navigating from PackageListFragment") }

        }
        binding.rvPackages.adapter = packageListAdapter

        runCatching {
            val intent = activity?.intent ?: return
            val packageName = viewIntentParserService.packageFromIntent(intent) ?: return
            val action = PackageListFragmentDirections.actionSelectPackage(packageName)
            findNavController().navigate(action)
        }.onFailure {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_invalid_activity_link),
                Toast.LENGTH_LONG
            )
                .show()
        }
    }

    private fun updateFilter(query: String, debounce: Boolean) {
        val actionBar = activity as? ActionBarSearch
        filterJob?.cancel()
        filterJob = lifecycleScope.launch {
            if (debounce) {
                delay(300)
            }
            actionBar?.isSearching = true
            val filtered = withContext(Dispatchers.Default) {
                packageListAdapter.performFilter(query)
            }
            packageListAdapter.submitList(filtered)
            actionBar?.isSearching = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
