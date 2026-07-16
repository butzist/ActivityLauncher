package de.szalkowski.activitylauncher.presentation.activities

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentActivityDetailsBinding
import de.szalkowski.activitylauncher.domain.external.ReviewRequester
import de.szalkowski.activitylauncher.domain.intent.IntentDef
import de.szalkowski.activitylauncher.presentation.common.CropIconDialogFragment
import de.szalkowski.activitylauncher.presentation.common.IconPickerDialogFragment
import de.szalkowski.activitylauncher.presentation.common.PluginChooserDialogFragment
import de.szalkowski.activitylauncher.presentation.intent.EditIntentDialogFragment
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityDetailsFragment : Fragment() {
    private val viewModel: ActivityDetailsViewModel by viewModels()

    @Inject
    internal lateinit var reviewRequester: ReviewRequester

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val dialog = CropIconDialogFragment.newInstance(it)
            dialog.setCropListener { icon ->
                viewModel.updateEditedIcon(icon)
            }
            dialog.show(childFragmentManager, "crop icon")
        }
    }

    private var _binding: FragmentActivityDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childFragmentManager.setFragmentResultListener(PluginChooserDialogFragment.REQUEST_KEY, this) { _, bundle ->
            val action = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                bundle.getSerializable(PluginChooserDialogFragment.RESULT_ACTION, PluginChooserDialogFragment.PluginAction::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getSerializable(PluginChooserDialogFragment.RESULT_ACTION) as? PluginChooserDialogFragment.PluginAction
            }
            val launchPlugin = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(PluginChooserDialogFragment.RESULT_LAUNCH_PLUGIN, ComponentName::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable<ComponentName>(PluginChooserDialogFragment.RESULT_LAUNCH_PLUGIN)
            }
            val shortcutPlugin = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(PluginChooserDialogFragment.RESULT_SHORTCUT_PLUGIN, ComponentName::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable<ComponentName>(PluginChooserDialogFragment.RESULT_SHORTCUT_PLUGIN)
            }

            viewModel.selectLaunchPlugin(launchPlugin)
            viewModel.selectShortcutPlugin(shortcutPlugin)

            if (action == PluginChooserDialogFragment.PluginAction.LAUNCH) {
                viewModel.launchActivity()
            } else if (action == PluginChooserDialogFragment.PluginAction.SHORTCUT) {
                viewModel.createShortcut()
            }
        }

        childFragmentManager.setFragmentResultListener(EditIntentDialogFragment.REQUEST_KEY, this) { _, bundle ->
            val intentDef = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(EditIntentDialogFragment.RESULT_INTENT_DEF, IntentDef::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable<IntentDef>(EditIntentDialogFragment.RESULT_INTENT_DEF)
            }
            intentDef?.let { viewModel.updateIntentDef(it) }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.invalidateOptionsMenu()
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
                    val advancedItem = menu.findItem(R.id.action_advanced)
                    advancedItem.isEnabled = viewModel.canLaunch.value

                    val favoriteItem = menu.findItem(R.id.action_favorite)
                    if (viewModel.isFavorite.value) {
                        favoriteItem.setIcon(R.drawable.ic_favorite)
                    } else {
                        favoriteItem.setIcon(R.drawable.ic_favorite_border)
                    }
                    favoriteItem.isEnabled = viewModel.canFavorite.value

                    val shareItem = menu.findItem(R.id.action_share)
                    shareItem.isEnabled = viewModel.canShare.value

                    val saveAsNewItem = menu.findItem(R.id.action_save_as_new)
                    saveAsNewItem.isVisible = viewModel.isEditMode.value
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_advanced -> {
                            val dialog = EditIntentDialogFragment.newInstance(viewModel.intentDef.value)
                            dialog.show(childFragmentManager, "edit intent")
                            true
                        }
                        R.id.action_favorite -> {
                            viewModel.toggleFavorite()
                            true
                        }
                        R.id.action_share -> {
                            viewModel.shareActivity()
                            true
                        }
                        R.id.action_save_as_new -> {
                            viewModel.saveAsNewShortcut()
                            findNavController().popBackStack()
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
                    viewModel.editedIcon.collect { icon ->
                        android.util.Log.d("ActivityDetails", "New icon emitted: $icon (type=${icon?.type})")
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
                    viewModel.showCreateShortcut.collect { isVisible ->
                        binding.llCreateShortcut.isVisible = isVisible
                    }
                }
                launch {
                    viewModel.showLaunch.collect { isVisible ->
                        binding.llLaunch.isVisible = isVisible
                    }
                }
                launch {
                    viewModel.showShare.collect { isVisible ->
                        binding.btShareShortcut.isVisible = isVisible
                    }
                }
                launch {
                    viewModel.showFavorite.collect { isVisible ->
                        binding.btFavorite.isVisible = isVisible
                    }
                }
                launch {
                    viewModel.showSave.collect { isVisible ->
                        binding.btSave.isVisible = isVisible
                    }
                }
                launch {
                    viewModel.showLaunchPluginSelection.collect { isVisible ->
                        binding.tilLaunchPlugin.isVisible = isVisible
                    }
                }
                launch {
                    viewModel.launchPlugins.collect { plugins ->
                        val adapter = PluginDropdownAdapter(requireContext(), plugins)
                        binding.atvLaunchPlugin.setAdapter(adapter)
                    }
                }
                launch {
                    viewModel.selectedLaunchPlugin.collect { plugin ->
                        if (!binding.atvLaunchPlugin.isFocused) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                binding.atvLaunchPlugin.setText(plugin?.name ?: "", false)
                            } else {
                                binding.atvLaunchPlugin.setText(plugin?.name ?: "")
                            }
                        }
                        binding.tilLaunchPlugin.startIconDrawable = plugin?.icon?.loadDrawable(requireContext())
                    }
                }
                launch {
                    viewModel.onSaveComplete.collect { request ->
                        setFragmentResult(RESULT_SAVED, bundleOf(EXTRA_SAVED_SHORTCUT to request))
                        findNavController().popBackStack()
                    }
                }
                launch {
                    combine(
                        viewModel.canLaunch,
                        viewModel.canFavorite,
                        viewModel.canShare,
                    ) { canLaunch, canFavorite, canShare ->
                        Triple(canLaunch, canFavorite, canShare)
                    }.collect { states ->
                        binding.btLaunch.isEnabled = states.first
                        binding.btLaunchChooser.isEnabled = states.first
                        binding.btFavorite.isEnabled = states.second
                        binding.btShareShortcut.isEnabled = states.third
                        activity?.invalidateOptionsMenu()
                    }
                }
                launch {
                    viewModel.canCreateShortcut.collect { isEnabled ->
                        binding.btSave.isEnabled = isEnabled
                        binding.btCreateShortcut.isEnabled = isEnabled
                        binding.btCreateShortcutChooser.isEnabled = isEnabled
                    }
                }
                launch {
                    viewModel.errorMessage.collect { resId ->
                        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Initialize fields with current values from ViewModel
        binding.tiName.setText(viewModel.editedName.value)
        binding.tiPackage.setText(viewModel.editedPackage.value)
        binding.tiClass.setText(viewModel.editedClass.value)
        updateFavoriteUI(viewModel.isFavorite.value)

        binding.btFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.tiName.doAfterTextChanged { viewModel.updateName(it.toString()) }
        binding.tiPackage.doAfterTextChanged { viewModel.updatePackage(it.toString()) }
        binding.tiClass.doAfterTextChanged { viewModel.updateClass(it.toString()) }

        binding.ibIconPicker.setOnClickListener {
            showIconPopupMenu(it)
        }

        binding.btSave.setOnClickListener {
            viewModel.saveShortcut()
        }

        binding.btCreateShortcut.setOnClickListener {
            viewModel.createShortcut()
        }

        binding.btCreateShortcutChooser.setOnClickListener {
            val dialog = PluginChooserDialogFragment.newInstance(
                PluginChooserDialogFragment.PluginAction.SHORTCUT,
                viewModel.launchPlugins.value,
                viewModel.shortcutPlugins.value,
            )
            dialog.show(childFragmentManager, "plugin chooser")
        }

        binding.atvLaunchPlugin.setOnItemClickListener { _, _, position, _ ->
            val plugin = viewModel.launchPlugins.value[position]
            viewModel.selectLaunchPlugin(plugin.componentName)
        }

        binding.btLaunch.setOnClickListener {
            viewModel.launchActivity()
        }

        binding.btLaunchChooser.setOnClickListener {
            val dialog = PluginChooserDialogFragment.newInstance(
                PluginChooserDialogFragment.PluginAction.LAUNCH,
                viewModel.launchPlugins.value,
                viewModel.shortcutPlugins.value,
            )
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
            binding.btFavorite.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite)
        } else {
            binding.btFavorite.setText(R.string.context_action_favorite_add)
            binding.btFavorite.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_favorite_border)
        }
    }

    private fun showIconPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add(Menu.NONE, 1, 1, R.string.action_pick_icon_library)
        popup.menu.add(Menu.NONE, 2, 2, R.string.action_pick_icon_file)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val dialog = IconPickerDialogFragment()
                    dialog.attachIconPickerListener { icon ->
                        viewModel.updateIconResourceName(icon)
                    }
                    dialog.show(childFragmentManager, "icon picker")
                    true
                }

                2 -> {
                    pickImageLauncher.launch("image/*")
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    companion object {
        const val RESULT_SAVED = "activity_details_saved"
        const val EXTRA_SAVED_SHORTCUT = "saved_shortcut"
    }
}
