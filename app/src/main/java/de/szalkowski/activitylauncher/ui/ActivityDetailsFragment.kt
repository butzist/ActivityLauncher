package de.szalkowski.activitylauncher.ui

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentActivityDetailsBinding
import de.szalkowski.activitylauncher.services.ActivityLauncherService
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.FavoritesService
import de.szalkowski.activitylauncher.services.IconCreatorService
import de.szalkowski.activitylauncher.services.IconLoaderService
import de.szalkowski.activitylauncher.services.InAppReviewService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import de.szalkowski.activitylauncher.services.RecentActivitiesService
import de.szalkowski.activitylauncher.services.SettingsService
import de.szalkowski.activitylauncher.services.ShareActivityService
import javax.inject.Inject

@AndroidEntryPoint
class ActivityDetailsFragment : Fragment() {
    private val args: ActivityDetailsFragmentArgs by navArgs()

    @Inject
    internal lateinit var activityListService: ActivityListService
    private lateinit var activityInfo: MyActivityInfo

    @Inject
    internal lateinit var activityLauncherService: ActivityLauncherService

    @Inject
    internal lateinit var iconCreatorService: IconCreatorService

    @Inject
    internal lateinit var shareActivityService: ShareActivityService

    @Inject
    internal lateinit var iconLoaderService: IconLoaderService

    @Inject
    internal lateinit var inAppReviewService: InAppReviewService

    @Inject
    internal lateinit var settingsService: SettingsService

    @Inject
    internal lateinit var recentActivitiesService: RecentActivitiesService

    @Inject
    internal lateinit var favoritesService: FavoritesService

    private var _binding: FragmentActivityDetailsBinding? = null
    private val binding get() = _binding!!

    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        activityInfo = activityListService.getActivity(args.activityComponentName)
        isFavorite = favoritesService.isFavorite(activityInfo.componentName)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the search bar
        activity?.findViewById<TextInputLayout>(R.id.tilSearch)?.visibility = View.GONE

        binding.tiName.setText(activityInfo.name)
        binding.tiPackage.setText(activityInfo.componentName.packageName)
        binding.tiClass.setText(activityInfo.componentName.className)
        binding.tiIcon.setText(activityInfo.iconResourceName ?: "")
        binding.ibIconPicker.setImageDrawable(activityInfo.icon)

        binding.ibIconPicker.setOnClickListener {
            val dialog = IconPickerDialogFragment()
            dialog.attachIconPickerListener { icon ->
                binding.tiIcon.setText(icon)
            }
            dialog.show(childFragmentManager, "icon picker")
        }

        binding.tiIcon.doAfterTextChanged { text ->
            val icon = text.toString()
            val drawable = iconLoaderService.getIcon(icon)
            binding.ibIconPicker.setImageDrawable(drawable)
        }

        binding.btCreateShortcut.setOnClickListener {
            iconCreatorService.createLauncherIcon(editedActivityInfo)
            recentActivitiesService.addActivity(editedActivityInfo.componentName, false)
        }

        binding.btCreateShortcutAsRoot.setOnClickListener {
            iconCreatorService.createRootLauncherIcon(editedActivityInfo)
            recentActivitiesService.addActivity(editedActivityInfo.componentName, true)
        }

        binding.btLaunch.setOnClickListener {
            activityLauncherService.launchActivity(
                editedActivityInfo.componentName, asRoot = false, showToast = true
            )
            recentActivitiesService.addActivity(editedActivityInfo.componentName, false)
        }

        binding.btLaunchAsRoot.setOnClickListener {
            activityLauncherService.launchActivity(
                editedActivityInfo.componentName, asRoot = true, showToast = true
            )
            recentActivitiesService.addActivity(editedActivityInfo.componentName, true)
        }

        binding.btShareShortcut.setOnClickListener {
            shareActivityService.shareActivity(editedActivityInfo.componentName)
        }

        if (!settingsService.allowRoot) {
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
        if (isFavorite) {
            favoriteItem.setIcon(R.drawable.ic_favorite)
        } else {
            favoriteItem.setIcon(R.drawable.ic_favorite_border)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorite -> {
                isFavorite = !isFavorite
                if (isFavorite) {
                    favoritesService.addFavorite(activityInfo.componentName)
                } else {
                    favoritesService.removeFavorite(activityInfo.componentName)
                }
                activity?.invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore search bar visibility
        activity?.findViewById<TextInputLayout>(R.id.tilSearch)?.visibility = View.VISIBLE
        _binding = null
    }

    private val editedActivityInfo: MyActivityInfo
        get() {
            val componentName =
                ComponentName(binding.tiPackage.text.toString(), binding.tiClass.text.toString())
            val iconResourceName = binding.tiIcon.text.toString()

            return MyActivityInfo(
                componentName,
                binding.tiName.text.toString(),
                binding.ibIconPicker.drawable,
                iconResourceName.ifBlank { null },
                false,
            )
        }
}
