package de.szalkowski.activitylauncher.presentation.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.szalkowski.activitylauncher.databinding.DialogPluginChooserBinding
import de.szalkowski.activitylauncher.domain.model.PluginInfo
import kotlinx.coroutines.launch

class PluginChooserDialogFragment : BottomSheetDialogFragment() {
    private val viewModel: PluginChooserViewModel by viewModels()
    private var _binding: DialogPluginChooserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogPluginChooserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val launchPlugins = arguments?.getParcelableArrayList<PluginInfo>(ARG_LAUNCH_PLUGINS) ?: emptyList()
        val shortcutPlugins = arguments?.getParcelableArrayList<PluginInfo>(ARG_SHORTCUT_PLUGINS) ?: emptyList()
        viewModel.setPlugins(launchPlugins, shortcutPlugins)

        val launchAdapter = PluginListAdapter { viewModel.selectLaunchPlugin(it) }
        binding.rvLaunchPlugins.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvLaunchPlugins.adapter = launchAdapter

        val shortcutAdapter = PluginListAdapter { viewModel.selectShortcutPlugin(it) }
        binding.rvShortcutPlugins.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvShortcutPlugins.adapter = shortcutAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.launchPlugins.collect { plugins ->
                        launchAdapter.submitList(plugins)
                        binding.tvLaunchPluginsLabel.visibility = if (plugins.size > 1) View.VISIBLE else View.GONE
                        binding.rvLaunchPlugins.visibility = if (plugins.size > 1) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.shortcutPlugins.collect { plugins ->
                        shortcutAdapter.submitList(plugins)
                        binding.tvShortcutPluginsLabel.visibility = if (plugins.size > 1) View.VISIBLE else View.GONE
                        binding.rvShortcutPlugins.visibility = if (plugins.size > 1) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.selectedLaunchPlugin.collect { selected ->
                        launchAdapter.setSelectedPlugin(selected)
                    }
                }
                launch {
                    viewModel.selectedShortcutPlugin.collect { selected ->
                        shortcutAdapter.setSelectedPlugin(selected)
                    }
                }
                launch {
                    viewModel.isSelectionComplete.collect { complete ->
                        if (complete) {
                            submitResult()
                        }
                    }
                }
            }
        }

        binding.btCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun submitResult() {
        val result = viewModel.getResult()
        val bundle = Bundle().apply {
            putParcelable(RESULT_LAUNCH_PLUGIN, result.launchPlugin)
            putParcelable(RESULT_SHORTCUT_PLUGIN, result.shortcutPlugin)
        }
        setFragmentResult(REQUEST_KEY, bundle)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "plugin_chooser_request"
        const val ARG_LAUNCH_PLUGINS = "launch_plugins"
        const val ARG_SHORTCUT_PLUGINS = "shortcut_plugins"
        const val RESULT_LAUNCH_PLUGIN = "launch_plugin"
        const val RESULT_SHORTCUT_PLUGIN = "shortcut_plugin"

        fun newInstance(launchPlugins: List<PluginInfo>, shortcutPlugins: List<PluginInfo>): PluginChooserDialogFragment {
            return PluginChooserDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_LAUNCH_PLUGINS, ArrayList(launchPlugins))
                    putParcelableArrayList(ARG_SHORTCUT_PLUGINS, ArrayList(shortcutPlugins))
                }
            }
        }
    }
}
