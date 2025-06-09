package de.szalkowski.activitylauncher.ui

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.databinding.FragmentActivityDetailsBinding
import de.szalkowski.activitylauncher.services.ActivityLauncherService
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.IconCreatorService
import de.szalkowski.activitylauncher.services.IconLoaderService
import de.szalkowski.activitylauncher.services.InAppReviewService
import de.szalkowski.activitylauncher.services.MyActivityInfo
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

    private var _binding: FragmentActivityDetailsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityInfo = activityListService.getActivity(args.activityComponentName)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actionBar = activity as? ActionBarSearch
        // FIXME just hide the search menu item, instead
        actionBar?.actionBarSearchText = ""

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
        }

        binding.btCreateShortcutAsRoot.setOnClickListener {
            iconCreatorService.createRootLauncherIcon(editedActivityInfo)
        }

        binding.btLaunch.setOnClickListener {
            activityLauncherService.launchActivity(
                editedActivityInfo.componentName, asRoot = false, showToast = true
            )
        }

        binding.btLaunchAsRoot.setOnClickListener {
            activityLauncherService.launchActivity(
                editedActivityInfo.componentName, asRoot = true, showToast = true
            )
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

    override fun onDestroyView() {
        super.onDestroyView()
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
