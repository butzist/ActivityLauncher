package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.databinding.FragmentActivityListBinding
import javax.inject.Inject

@AndroidEntryPoint
class ActivityListFragment : Fragment() {
    private val args: ActivityListFragmentArgs by navArgs()

    @Inject
    internal lateinit var activityListAdapterFactory: ActivityListAdapter.ActivityListAdapterFactory
    private lateinit var activityListAdapter: ActivityListAdapter

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
        activityListAdapter.filter = actionBar?.actionBarSearchText.orEmpty()
        actionBar?.onActionBarSearchListener = { search ->
            activityListAdapter.filter = search
        }

        activityListAdapter.onItemClick = {
            runCatching {
                val action = ActivityListFragmentDirections.actionSelectActivity(it.componentName)
                findNavController().navigate(action)
            }.onFailure { Log.e("Navigation", "Error while navigating from PackageListFragment") }
        }

        binding.rvActivities.adapter = activityListAdapter
        binding.rvActivities.isNestedScrollingEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
