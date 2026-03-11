package de.szalkowski.activitylauncher.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.FragmentFavoritesBinding
import de.szalkowski.activitylauncher.services.ActivityLauncherService
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.FavoritesService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var favoritesService: FavoritesService

    @Inject
    internal lateinit var activityListService: ActivityListService

    @Inject
    internal lateinit var activityLauncherService: ActivityLauncherService

    private lateinit var adapter: FavoritesListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.getItem(position)
                favoritesService.removeFavorite(item.componentName)
                adapter.removeItem(position)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.rvFavorites)
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }

    private fun updateList() {
        val favoriteActivities = favoritesService.getFavorites()
        val activityInfos = favoriteActivities.mapNotNull { componentName ->
            try {
                activityListService.getActivity(componentName)
            } catch (e: Exception) {
                null
            }
        }.toMutableList()

        adapter = FavoritesListAdapter(activityInfos)
        adapter.onItemClick = { info ->
            activityLauncherService.launchActivity(info.componentName, asRoot = false, showToast = true)
        }
        adapter.onItemLongClick = { info ->
            runCatching {
                val action = FavoritesFragmentDirections.actionSelectActivity(info.componentName)
                findNavController().navigate(action)
            }.onFailure { Log.e("Navigation", "Error while navigating from FavoritesFragment") }
        }
        binding.rvFavorites.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class FavoritesListAdapter(private val activities: MutableList<MyActivityInfo>) :
        RecyclerView.Adapter<FavoritesListAdapter.ViewHolder>() {

        var onItemClick: ((MyActivityInfo) -> Unit)? = null
        var onItemLongClick: ((MyActivityInfo) -> Unit)? = null

        fun getItem(position: Int): MyActivityInfo = activities[position]

        fun removeItem(position: Int) {
            activities.removeAt(position)
            notifyItemRemoved(position)
        }

        inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
            val tvName: TextView = viewItem.findViewById(R.id.tvName)
            val tvClass: TextView = viewItem.findViewById(R.id.tvClass)
            val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)

            init {
                itemView.setOnClickListener {
                    onItemClick?.invoke(activities[bindingAdapterPosition])
                }
                itemView.setOnLongClickListener {
                    onItemLongClick?.invoke(activities[bindingAdapterPosition])
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.list_item_activity_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = activities[position]
            holder.tvName.text = item.name
            holder.tvClass.text = item.componentName.shortClassName
            holder.ivIcon.setImageDrawable(item.icon)
        }

        override fun getItemCount(): Int = activities.size
    }
}
