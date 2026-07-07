package de.szalkowski.activitylauncher.presentation.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.databinding.FragmentActivityListBinding
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.presentation.common.ActionBarSearch
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityListFragment : Fragment() {
    private val args: ActivityListFragmentArgs by navArgs()

    @Inject
    internal lateinit var activityListAdapterFactory: ActivityListAdapter.ActivityListAdapterFactory

    @Inject
    internal lateinit var getActivityIconUseCase: GetActivityIconUseCase

    private val activityListAdapter: ActivityListAdapter by lazy {
        activityListAdapterFactory.create(args.packageName)
    }

    private val viewModel: ActivityListViewModel by viewModels()

    private var _binding: FragmentActivityListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
                val icon = getActivityIconUseCase(it.iconResourceName, it.componentName)
                val intent = Intent().setComponent(it.componentName)
                val request = ShortcutRequest(it.name, intent, icon)
                val action = ActivityListFragmentDirections.actionSelectActivity(shortcutRequest = request)
                findNavController().navigate(action)
            }.onFailure { e -> Log.e("Navigation", "Error while navigating from ActivityListFragment", e) }
        }

        binding.rvActivities.adapter = activityListAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
