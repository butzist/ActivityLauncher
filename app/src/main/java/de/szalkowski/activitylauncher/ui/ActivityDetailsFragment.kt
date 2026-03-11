package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentActivityDetailsBinding
import de.szalkowski.activitylauncher.services.InAppReviewService
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ActivityDetailsFragment : Fragment() {
    private val viewModel: ActivityDetailsViewModel by viewModels()

    @Inject
    internal lateinit var inAppReviewService: InAppReviewService

    private var _binding: FragmentActivityDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                            binding.ibIconPicker.setImageDrawable(viewModel.editedIconDrawable.value)
                        }
                    }
                }
                launch {
                    viewModel.editedIconDrawable.collect { drawable ->
                        binding.ibIconPicker.setImageDrawable(drawable)
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
            viewModel.createShortcut(asRoot = false)
        }

        binding.btCreateShortcutAsRoot.setOnClickListener {
            viewModel.createShortcut(asRoot = true)
        }

        binding.btLaunch.setOnClickListener {
            viewModel.launchActivity(asRoot = false)
        }

        binding.btLaunchAsRoot.setOnClickListener {
            viewModel.launchActivity(asRoot = true)
        }

        binding.btShareShortcut.setOnClickListener {
            viewModel.shareActivity()
        }

        if (!viewModel.settingsService.allowRoot) {
            binding.btCreateShortcutAsRoot.visibility = View.GONE
            binding.btLaunchAsRoot.visibility = View.GONE
        } else {
            binding.btCreateShortcutAsRoot.visibility = View.VISIBLE
            binding.btLaunchAsRoot.visibility = View.VISIBLE
        }

        activity?.let { inAppReviewService.showInAppReview(it) }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_activity_details, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val favoriteItem = menu.findItem(R.id.action_favorite)
        if (viewModel.isFavorite.value) {
            favoriteItem.setIcon(R.drawable.ic_favorite)
        } else {
            favoriteItem.setIcon(R.drawable.ic_favorite_border)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorite -> {
                viewModel.toggleFavorite()
                true
            }
            R.id.action_share -> {
                viewModel.shareActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
