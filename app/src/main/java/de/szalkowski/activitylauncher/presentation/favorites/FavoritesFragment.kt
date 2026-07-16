package de.szalkowski.activitylauncher.presentation.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentFavoritesBinding
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import de.szalkowski.activitylauncher.presentation.activities.DetailsConfiguration
import de.szalkowski.activitylauncher.presentation.common.ActivityInfoAdapter
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListFragment
import de.szalkowski.activitylauncher.presentation.common.ShortcutRequestDiffCallback
import de.szalkowski.activitylauncher.presentation.common.bindShortcutRequest
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : BaseActivityListFragment<ShortcutRequest>() {
    @Inject
    internal lateinit var launchActivityUseCase: LaunchActivityUseCase

    val viewModel: FavoritesViewModel by viewModels()
    override val items get() = viewModel.items
    override val recyclerViewId: Int = R.id.rvFavorites
    override val logTag: String = "FavoritesFragment"
    override val detailsConfiguration: DetailsConfiguration = DetailsConfiguration.FAVORITES

    override val adapter: ActivityInfoAdapter<ShortcutRequest> by lazy {
        ActivityInfoAdapter(ShortcutRequestDiffCallback, ::bindShortcutRequest).also {
            it.onItemClick = { request ->
                launchActivityUseCase(
                    LaunchRequest(
                        intent = request.intent,
                        name = request.name,
                        icon = request.icon,
                        launcherPlugin = request.launcherPlugin,
                    ),
                )
            }
            it.onItemSwiped = { request -> viewModel.removeItem(request) }
        }
    }

    override fun navigateToDetailsAction(item: ShortcutRequest, configuration: DetailsConfiguration): NavDirections =
        FavoritesFragmentDirections.actionSelectActivity(
            shortcutRequest = item,
            configuration = configuration,
        )

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
