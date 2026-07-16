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
import de.szalkowski.activitylauncher.presentation.activities.DetailsConfiguration
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class BaseActivityListFragment<T : Any> : Fragment() {
    protected abstract val adapter: ActivityInfoAdapter<T>
    protected abstract val recyclerViewId: Int
    protected abstract val items: StateFlow<List<T>>
    protected abstract val logTag: String
    protected abstract fun navigateToDetailsAction(item: T, configuration: DetailsConfiguration): NavDirections

    protected open val detailsConfiguration: DetailsConfiguration = DetailsConfiguration.ALL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.onItemLongClick = { item ->
            runCatching {
                val action = navigateToDetailsAction(item, detailsConfiguration)
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
                    adapter.onItemSwiped?.invoke(item)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                items.collect { itemList ->
                    adapter.submitList(itemList)
                }
            }
        }
    }
}
