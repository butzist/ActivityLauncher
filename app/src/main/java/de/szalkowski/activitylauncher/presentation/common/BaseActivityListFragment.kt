package de.szalkowski.activitylauncher.presentation.common

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.usecase.launcher.GetActivityIconUseCase
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import de.szalkowski.activitylauncher.presentation.activities.ActivityInfoAdapter
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseActivityListFragment : Fragment() {
    @Inject
    internal lateinit var launchActivityUseCase: LaunchActivityUseCase

    @Inject
    internal lateinit var packageRepository: PackageRepository

    @Inject
    internal lateinit var iconLoader: IconLoader

    @Inject
    internal lateinit var getActivityIconUseCase: GetActivityIconUseCase

    protected abstract val viewModel: BaseActivityListViewModel
    protected abstract val recyclerViewId: Int
    protected abstract val logTag: String
    protected abstract fun navigateToDetailsAction(componentName: ComponentName): NavDirections

    private lateinit var adapter: ActivityInfoAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ActivityInfoAdapter { info ->
            val icon = getActivityIconUseCase(info.iconResourceName, info.componentName)
            icon.loadDrawable(requireContext()) ?: requireContext().packageManager.defaultActivityIcon
        }
        adapter.onItemClick = { info ->
            launchActivityUseCase(LaunchRequest(info.componentName))
        }
        adapter.onItemLongClick = { info ->
            runCatching {
                val action = navigateToDetailsAction(info.componentName)
                findNavController().navigate(action)
            }.onFailure { Log.e("Navigation", "Error while navigating from $logTag") }
        }

        val recyclerView = requireView().findViewById<RecyclerView>(recyclerViewId)
        recyclerView.adapter = adapter

        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = adapter.getItem(position)
                    viewModel.removeItem(item.componentName)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activities.collect { activities ->
                    adapter.submitList(activities)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }
}
