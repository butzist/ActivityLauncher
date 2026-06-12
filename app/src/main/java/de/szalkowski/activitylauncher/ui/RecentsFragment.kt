package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentRecentsBinding

@AndroidEntryPoint
class RecentsFragment : BaseActivityListFragment() {
    override val viewModel: RecentsViewModel by viewModels()
    override val recyclerViewId: Int = R.id.rvRecents
    override val logTag: String = "RecentsFragment"
    override fun navigateToDetailsAction(componentName: android.content.ComponentName): NavDirections =
        RecentsFragmentDirections.actionSelectActivity(componentName)

    private var _binding: FragmentRecentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
