package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentFavoritesBinding

@AndroidEntryPoint
class FavoritesFragment : BaseActivityListFragment() {
    override val viewModel: FavoritesViewModel by viewModels()
    override val recyclerViewId: Int = R.id.rvFavorites
    override val logTag: String = "FavoritesFragment"
    override fun navigateToDetailsAction(componentName: android.content.ComponentName): NavDirections =
        FavoritesFragmentDirections.actionSelectActivity(componentName)

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
