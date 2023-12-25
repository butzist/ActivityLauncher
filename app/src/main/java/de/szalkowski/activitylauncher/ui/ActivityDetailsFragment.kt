package de.szalkowski.activitylauncher.ui

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.databinding.FragmentActivityDetailsBinding
import de.szalkowski.activitylauncher.services.ActivityLauncherService
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.IconCreatorService
import de.szalkowski.activitylauncher.services.MyActivityInfo
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

        binding.etName.setText(activityInfo.name)
        binding.etPackage.setText(activityInfo.componentName.packageName)
        binding.etClass.setText(activityInfo.componentName.className)
        binding.etIcon.setText(activityInfo.iconResourceName ?: "")
        binding.ibIconPicker.setImageDrawable(activityInfo.icon)

        // TODO binding.ibIconPicker

        binding.btCreateShortcut.setOnClickListener {
            iconCreatorService.createLauncherIcon(editedActivityInfo)
        }

        binding.btLaunch.setOnClickListener {
            activityLauncherService.launchActivity(
                editedActivityInfo.componentName, asRoot = false, showToast = true
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val editedActivityInfo: MyActivityInfo
        get() {
            val componentName =
                ComponentName(binding.etPackage.text.toString(), binding.etClass.text.toString())
            val iconResourceName = binding.etIcon.text.toString()

            return MyActivityInfo(
                componentName,
                binding.etName.text.toString(),
                binding.ibIconPicker.drawable,
                iconResourceName.ifBlank { null },
                false,
            )
        }
}
