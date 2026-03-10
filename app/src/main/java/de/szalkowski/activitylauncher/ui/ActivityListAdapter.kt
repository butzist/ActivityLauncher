package de.szalkowski.activitylauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import kotlinx.coroutines.yield
import javax.inject.Inject

class ActivityListAdapter @AssistedInject constructor(
    activityListService: ActivityListService, @Assisted private val packageName: String
) : ListAdapter<MyActivityInfo, ActivityListAdapter.ViewHolder>(ActivityDiffCallback) {
    @AssistedFactory
    interface ActivityListAdapterFactory {
        fun create(packageName: String): ActivityListAdapter
    }

    private val allActivities = activityListService.getActivities(packageName)
    private val combinedActivities = listOfNotNull(allActivities.defaultActivity) + allActivities.activities
    var onItemClick: ((MyActivityInfo) -> Unit)? = null

    init {
        submitList(combinedActivities)
    }

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        val tvName: TextView = viewItem.findViewById(R.id.tvName)
        val tvPackage: TextView = viewItem.findViewById(R.id.tvClass)
        val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)
        lateinit var item: MyActivityInfo

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    suspend fun performFilter(query: String): List<MyActivityInfo> {
        if (query.isEmpty()) return combinedActivities

        val result = mutableListOf<MyActivityInfo>()
        
        // Check default activity with special rules (matches package/app name too)
        allActivities.defaultActivity?.let { a ->
            yield()
            if (allActivities.packageName.contains(query, ignoreCase = true) ||
                allActivities.name.contains(query, ignoreCase = true) ||
                a.name.contains(query, ignoreCase = true) ||
                a.componentName.className.contains(query, ignoreCase = true)
            ) {
                result.add(a)
            }
        }

        // Check regular activities
        for (a in allActivities.activities) {
            yield()
            if (a.name.contains(query, ignoreCase = true) ||
                a.componentName.shortClassName.contains(query, ignoreCase = true)
            ) {
                if (result.indexOf(a) == -1) {
                    result.add(a)
                }
            }
        }
        
        return result
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item_activity_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.item = item
        holder.tvName.text = if (item.isPrivate) "(${item.name})" else item.name
        holder.tvPackage.text = item.componentName.shortClassName
        holder.ivIcon.setImageDrawable(item.icon)
    }

    object ActivityDiffCallback : DiffUtil.ItemCallback<MyActivityInfo>() {
        override fun areItemsTheSame(oldItem: MyActivityInfo, newItem: MyActivityInfo): Boolean {
            return oldItem.componentName == newItem.componentName
        }

        override fun areContentsTheSame(oldItem: MyActivityInfo, newItem: MyActivityInfo): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.isPrivate == newItem.isPrivate &&
                    oldItem.iconResourceName == newItem.iconResourceName
        }
    }
}
