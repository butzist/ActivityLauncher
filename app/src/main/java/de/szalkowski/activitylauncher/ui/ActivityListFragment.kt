package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
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

        activityListAdapter.onItemClick = {
            val action = ActivityListFragmentDirections.actionSelectActivity(it.componentName)
            findNavController().navigate(action)
        }

        binding.rvActivities.adapter = activityListAdapter
        binding.rvActivities.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
