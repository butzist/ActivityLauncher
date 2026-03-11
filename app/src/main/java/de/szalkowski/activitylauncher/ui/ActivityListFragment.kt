package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentActivityListBinding
import de.szalkowski.activitylauncher.services.ViewIntentParserService
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityListFragment : Fragment() {
    private val args: ActivityListFragmentArgs by navArgs()
    
    @Inject
    internal lateinit var activityListAdapterFactory: ActivityListAdapter.ActivityListAdapterFactory
    private lateinit var activityListAdapter: ActivityListAdapter

    private val viewModel: ActivityListViewModel by viewModels()

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
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.activities.collect { activities ->
                        activityListAdapter.submitList(activities)
                    }
                }
                launch {
                    viewModel.isSearching.collect { isSearching ->
                        actionBar?.isSearching = isSearching
                    }
                }
            }
        }

        viewModel.filter(actionBar?.actionBarSearchText.orEmpty())
        actionBar?.onActionBarSearchListener = { search ->
            viewModel.filter(search)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
