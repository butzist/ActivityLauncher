package de.szalkowski.activitylauncher.presentation.shortcuts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.recyclerview.widget.DiffUtil
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentShortcutsBinding
import de.szalkowski.activitylauncher.domain.shortcuts.ShortcutsRepository
import de.szalkowski.activitylauncher.presentation.activities.DetailsConfiguration
import de.szalkowski.activitylauncher.presentation.common.ActivityInfoAdapter
import de.szalkowski.activitylauncher.presentation.common.BaseActivityListFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutsFragment : BaseActivityListFragment<ShortcutsRepository.ManagedShortcut>() {
    @Inject
    internal lateinit var createShortcutUseCase: de.szalkowski.activitylauncher.domain.usecase.launcher.CreateShortcutUseCase

    override val viewModel: ShortcutsViewModel by viewModels()
    override val items get() = viewModel.items
    override val recyclerViewId: Int = R.id.rvShortcuts
    override val logTag: String = "ShortcutsFragment"
    override val detailsConfiguration: DetailsConfiguration = DetailsConfiguration.SHORTCUTS

    override val adapter: ActivityInfoAdapter<ShortcutsRepository.ManagedShortcut> by lazy {
        ActivityInfoAdapter(ManagedShortcutDiffCallback, ::bindManagedShortcut).also {
            it.onItemClick = { managed ->
                viewLifecycleOwner.lifecycleScope.launch {
                    createShortcutUseCase(managed.request, shortcutId = managed.id)
                }
            }
            it.onItemSwiped = { managed -> viewModel.removeItem(managed) }
        }
    }

    override fun navigateToDetailsAction(
        item: ShortcutsRepository.ManagedShortcut,
        configuration: DetailsConfiguration,
    ): NavDirections =
        ShortcutsFragmentDirections.actionSelectShortcut(
            shortcutRequest = item.request,
            shortcutId = item.id,
            configuration = configuration,
        )

    private var _binding: FragmentShortcutsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentShortcutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val ManagedShortcutDiffCallback = object : DiffUtil.ItemCallback<ShortcutsRepository.ManagedShortcut>() {
            override fun areItemsTheSame(
                oldItem: ShortcutsRepository.ManagedShortcut,
                newItem: ShortcutsRepository.ManagedShortcut,
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ShortcutsRepository.ManagedShortcut,
                newItem: ShortcutsRepository.ManagedShortcut,
            ): Boolean = oldItem.request == newItem.request && oldItem.timestamp == newItem.timestamp
        }

        private fun bindManagedShortcut(
            item: ShortcutsRepository.ManagedShortcut,
            tvName: TextView,
            tvClass: TextView,
            ivIcon: ImageView,
        ) {
            val request = item.request
            tvName.text = request.name
            tvClass.text = request.intent.component?.flattenToShortString() ?: request.intent.toUri(0)
            val context = ivIcon.context
            ivIcon.setImageDrawable(request.icon.loadInternalDrawable(context) ?: context.packageManager.defaultActivityIcon)
        }
    }
}
