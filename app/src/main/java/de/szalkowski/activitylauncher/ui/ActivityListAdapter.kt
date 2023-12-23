package de.szalkowski.activitylauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.ActivityListService
import de.szalkowski.activitylauncher.services.MyActivityInfo
import de.szalkowski.activitylauncher.services.MyPackageInfo
import de.szalkowski.activitylauncher.services.PackageListService
import javax.inject.Inject

class ActivityListAdapter @AssistedInject constructor(activityListService: ActivityListService, @Assisted private val packageName: String) :
    RecyclerView.Adapter<ActivityListAdapter.ViewHolder>() {
    @AssistedFactory
    interface ActivityListAdapterFactory {
        fun create(packageName: String): ActivityListAdapter
    }

    private val activities = activityListService.getActivities(packageName)
    var onItemClick: ((MyActivityInfo) -> Unit)? = null

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        lateinit var item: MyActivityInfo

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item_package_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return activities.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvPackage = view.findViewById<TextView>(R.id.tvPackage)
        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)

        val item = activities[position]
        holder.item = item
        tvName.text = if (item.isPrivate) { "(${item.name})" } else { item.name}
        tvPackage.text = item.componentName.shortClassName
        ivIcon.setImageDrawable(item.icon)
    }
}


