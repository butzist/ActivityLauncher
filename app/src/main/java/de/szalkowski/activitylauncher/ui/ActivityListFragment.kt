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
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentActivityListBinding
import de.szalkowski.activitylauncher.services.ViewIntentParserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ActivityListFragment : Fragment() {
    private val args: ActivityListFragmentArgs by navArgs()

    @Inject
    internal lateinit var activityListAdapterFactory: ActivityListAdapter.ActivityListAdapterFactory
    private lateinit var activityListAdapter: ActivityListAdapter
    private var filterJob: Job? = null

    @Inject
    internal lateinit var viewIntentParserService: ViewIntentParserService

    private var _binding: FragmentActivityListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityListAdapter = activityListAdapterFactory.create(args.packageName)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actionBar = activity as? ActionBarSearch
        updateFilter(actionBar?.actionBarSearchText.orEmpty(), false)
        actionBar?.onActionBarSearchListener = { search ->
            updateFilter(search, true)
        }

        activityListAdapter.onItemClick = {
            runCatching {
                val action = ActivityListFragmentDirections.actionSelectActivity(it.componentName)
                findNavController().navigate(action)
            }.onFailure { Log.e("Navigation", "Error while navigating from PackageListFragment") }
        }

        binding.rvActivities.adapter = activityListAdapter

        runCatching {
            val intent = activity?.intent ?: return
            // clear view intent
            activity?.intent = null

            val componentName =
                viewIntentParserService.componentNameFromIntent(intent) ?: return
            val action = ActivityListFragmentDirections.actionSelectActivity(componentName)
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
                activityListAdapter.performFilter(query)
            }
            activityListAdapter.submitList(filtered)
            actionBar?.isSearching = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
