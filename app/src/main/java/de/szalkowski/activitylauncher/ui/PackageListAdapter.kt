package de.szalkowski.activitylauncher.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.MyPackageInfo
import de.szalkowski.activitylauncher.services.PackageListService
import kotlinx.coroutines.yield
import javax.inject.Inject

class PackageListAdapter @Inject constructor(private val packageListService: PackageListService) :
    ListAdapter<MyPackageInfo, PackageListAdapter.ViewHolder>(PackageDiffCallback) {

    private var allPackages: List<MyPackageInfo> = emptyList()

    var onItemClick: ((MyPackageInfo) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    fun updatePackages() {
        allPackages = packageListService.packages
        submitList(allPackages)
    }

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        val tvName: TextView = viewItem.findViewById(R.id.tvName)
        val tvPackage: TextView = viewItem.findViewById(R.id.tvClass)
        val tvVersion: TextView = viewItem.findViewById(R.id.tvVersion)
        val tvActivities: TextView = viewItem.findViewById(R.id.tvActivities)
        val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)
        lateinit var item: MyPackageInfo

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    suspend fun performFilter(query: String): List<MyPackageInfo> {
        if (query.isEmpty()) return allPackages

        return allPackages.mapNotNull { p ->
            yield() // Be cooperative with cancellation
            val packageMatches = p.name.contains(query, ignoreCase = true) || p.packageName.contains(query, ignoreCase = true)
            val filteredActivities = p.activityNames.filter { it.name.contains(query, ignoreCase = true) || it.shortCls.contains(query, ignoreCase = true) }
            val defaultActivity = p.defaultActivityName?.takeIf { packageMatches || it.name.contains(query, ignoreCase = true) || it.shortCls.contains(query, ignoreCase = true) }

            if (filteredActivities.isNotEmpty() || defaultActivity != null) {
                p.copy(
                    activityNames = filteredActivities,
                    defaultActivityName = defaultActivity
                )
            } else {
                null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item_package_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.item = item
        
        val activityCount = item.activityNames.size + (item.defaultActivityName?.let { 1 } ?: 0)
        holder.tvName.text = item.name
        holder.tvVersion.text = item.version
        holder.tvPackage.text = item.packageName
        holder.tvActivities.text = "(${activityCount})"
        holder.ivIcon.setImageDrawable(item.icon)
    }

    private object PackageDiffCallback : DiffUtil.ItemCallback<MyPackageInfo>() {
        override fun areItemsTheSame(oldItem: MyPackageInfo, newItem: MyPackageInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MyPackageInfo, newItem: MyPackageInfo): Boolean {
            return oldItem.packageName == newItem.packageName &&
                    oldItem.name == newItem.name &&
                    oldItem.version == newItem.version &&
                    oldItem.activityNames == newItem.activityNames &&
                    oldItem.defaultActivityName == newItem.defaultActivityName
        }
    }
}
