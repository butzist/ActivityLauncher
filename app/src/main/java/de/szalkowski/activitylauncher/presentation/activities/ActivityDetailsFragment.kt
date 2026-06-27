package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentActivityDetailsBinding
import de.szalkowski.activitylauncher.domain.external.ReviewRequester
import de.szalkowski.activitylauncher.presentation.common.IconPickerDialogFragment
import de.szalkowski.activitylauncher.presentation.common.PluginChooserDialogFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityDetailsFragment : Fragment() {
    private val viewModel: ActivityDetailsViewModel by viewModels()

    @Inject
    internal lateinit var reviewRequester: ReviewRequester

    private var _binding: FragmentActivityDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(PluginChooserDialogFragment.REQUEST_KEY) { _, bundle ->
            val launchPlugin = bundle.getParcelable<ComponentName>(PluginChooserDialogFragment.RESULT_LAUNCH_PLUGIN)
            val shortcutPlugin = bundle.getParcelable<ComponentName>(PluginChooserDialogFragment.RESULT_SHORTCUT_PLUGIN)
            viewModel.selectLaunchPlugin(launchPlugin)
            viewModel.selectShortcutPlugin(shortcutPlugin)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentActivityDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_activity_details, menu)
                }

                override fun onPrepareMenu(menu: Menu) {
                    val favoriteItem = menu.findItem(R.id.action_favorite)
                    if (viewModel.isFavorite.value) {
                        favoriteItem.setIcon(R.drawable.ic_favorite)
                    } else {
                        favoriteItem.setIcon(R.drawable.ic_favorite_border)
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_favorite -> {
                            viewModel.toggleFavorite()
                            true
                        }
                        R.id.action_share -> {
                            viewModel.shareActivity()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFavorite.collect { isFavorite ->
                        updateFavoriteUI(isFavorite)
                        activity?.invalidateOptionsMenu()
                    }
                }
                launch {
                    viewModel.activityInfo.collect { info ->
                        if (info != null) {
                            binding.tiName.setText(viewModel.editedName.value)
                            binding.tiPackage.setText(viewModel.editedPackage.value)
                            binding.tiClass.setText(viewModel.editedClass.value)
                            binding.tiIcon.setText(viewModel.editedIconResourceName.value)
                        }
                    }
                }
                launch {
                    viewModel.editedIcon.collect { icon ->
                        val drawable = icon?.loadDrawable(requireContext()) ?: requireContext().packageManager.defaultActivityIcon
                        binding.ibIconPicker.setImageDrawable(drawable)
                    }
                }
                launch {
                    viewModel.showLaunchChooser.collect { isVisible ->
                        binding.btLaunchChooser.isVisible = isVisible
                    }
                }
                launch {
                    viewModel.showShortcutChooser.collect { isVisible ->
                        binding.btCreateShortcutChooser.isVisible = isVisible
                    }
                }
                launch {
                    viewModel.errorMessage.collect { resId ->
                        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.tiName.doAfterTextChanged { viewModel.updateName(it.toString()) }
        binding.tiPackage.doAfterTextChanged { viewModel.updatePackage(it.toString()) }
        binding.tiClass.doAfterTextChanged { viewModel.updateClass(it.toString()) }
        binding.tiIcon.doAfterTextChanged { viewModel.updateIconResourceName(it.toString()) }

        binding.ibIconPicker.setOnClickListener {
            val dialog = IconPickerDialogFragment()
            dialog.attachIconPickerListener { icon ->
                binding.tiIcon.setText(icon)
            }
            dialog.show(childFragmentManager, "icon picker")
        }

        binding.btCreateShortcut.setOnClickListener {
            viewModel.createShortcut()
        }

        binding.btCreateShortcutChooser.setOnClickListener {
            val dialog = PluginChooserDialogFragment.newInstance(viewModel.launchPlugins.value, viewModel.shortcutPlugins.value)
            dialog.show(childFragmentManager, "plugin chooser")
        }

        binding.btLaunch.setOnClickListener {
            viewModel.launchActivity()
        }

        binding.btLaunchChooser.setOnClickListener {
            val dialog = PluginChooserDialogFragment.newInstance(viewModel.launchPlugins.value, viewModel.shortcutPlugins.value)
            dialog.show(childFragmentManager, "plugin chooser")
        }

        binding.btShareShortcut.setOnClickListener {
            viewModel.shareActivity()
        }

        activity?.let { reviewRequester.showInAppReview(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateFavoriteUI(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btFavorite.setText(R.string.context_action_favorite_remove)
            binding.btFavorite.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite, 0, 0, 0)
        } else {
            binding.btFavorite.setText(R.string.context_action_favorite_add)
            binding.btFavorite.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorite_border, 0, 0, 0)
        }
    }
}
