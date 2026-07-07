package de.szalkowski.activitylauncher.presentation.common

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
import de.szalkowski.activitylauncher.domain.model.LaunchRequest
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest
import de.szalkowski.activitylauncher.domain.usecase.launcher.LaunchActivityUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseActivityListFragment : Fragment() {
    @Inject
    internal lateinit var launchActivityUseCase: LaunchActivityUseCase

    protected abstract val viewModel: BaseActivityListViewModel
    protected abstract val recyclerViewId: Int
    protected abstract val logTag: String
    protected abstract fun navigateToDetailsAction(request: ShortcutRequest): NavDirections

    private lateinit var adapter: ActivityInfoAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ActivityInfoAdapter()
        adapter.onItemClick = { request ->
            launchActivityUseCase(
                LaunchRequest(
                    intent = request.intent,
                    name = request.name,
                    icon = request.icon,
                    launcherPlugin = request.launcherPlugin,
                ),
            )
        }
        adapter.onItemLongClick = { request ->
            runCatching {
                val action = navigateToDetailsAction(request)
                findNavController().navigate(action)
            }.onFailure { Log.e("Navigation", "Error while navigating from $logTag") }
        }

        val recyclerView = requireView().findViewById<RecyclerView>(recyclerViewId)
        recyclerView.adapter = adapter

        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = adapter.currentList[position]
                    viewModel.removeItem(item)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collect { items ->
                    adapter.submitList(items)
                }
            }
        }
    }
}
