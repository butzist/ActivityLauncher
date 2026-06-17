package de.szalkowski.activitylauncher.presentation.packages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.model.MyPackageInfo
import de.szalkowski.activitylauncher.domain.packages.PackageRepository
import de.szalkowski.activitylauncher.domain.usecase.packages.GetPackageIconUseCase
import kotlinx.coroutines.yield
import javax.inject.Inject

class PackageListAdapter @Inject constructor(
    private val packageRepository: PackageRepository,
    private val getPackageIconUseCase: GetPackageIconUseCase,
) : ListAdapter<MyPackageInfo, PackageListAdapter.ViewHolder>(PackageDiffCallback) {

    private var allPackages: List<MyPackageInfo> = emptyList()

    var onItemClick: ((MyPackageInfo) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    fun updatePackages() {
        allPackages = packageRepository.packages
        submitList(allPackages)
    }

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        val tvName: TextView = viewItem.findViewById(R.id.tvName)
        val tvPackage: TextView = viewItem.findViewById(R.id.tvClass)
        val tvVersion: TextView = viewItem.findViewById(R.id.tvVersion)
        val tvActivities: TextView = viewItem.findViewById(R.id.tvActivities)
        val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)
        val pbLoading: View = viewItem.findViewById(R.id.pbLoading)
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
                    defaultActivityName = defaultActivity,
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.item = item

        val activityCount = item.activityNames.size + (item.defaultActivityName?.let { 1 } ?: 0)
        holder.tvName.text = item.name
        holder.tvVersion.text = item.version
        holder.tvPackage.text = item.packageName
        holder.tvActivities.text = "($activityCount)"

        val icon = getPackageIconUseCase(item.iconResourceName, item.packageName)
        val context = holder.itemView.context
        val drawable = icon.loadDrawable(context) ?: context.packageManager.defaultActivityIcon
        holder.ivIcon.setImageDrawable(drawable)

        if (item.isFullyLoaded) {
            holder.itemView.alpha = 1.0f
            holder.pbLoading.visibility = View.GONE
            holder.tvActivities.visibility = View.VISIBLE
            holder.itemView.isEnabled = true
        } else {
            holder.itemView.alpha = 0.5f
            holder.pbLoading.visibility = View.VISIBLE
            holder.tvActivities.visibility = View.GONE
            holder.itemView.isEnabled = false
            // Prioritize loading from the start of the list up to the current position
            // to ensure stability of the top of the list
            for (i in 0..position) {
                val p = getItem(i)
                if (!p.isFullyLoaded) {
                    packageRepository.loadDetails(p.packageName)
                }
            }
        }
    }

    @Suppress("DiffUtilEquals")
    private object PackageDiffCallback : DiffUtil.ItemCallback<MyPackageInfo>() {
        override fun areItemsTheSame(oldItem: MyPackageInfo, newItem: MyPackageInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MyPackageInfo, newItem: MyPackageInfo): Boolean {
            return oldItem.packageName == newItem.packageName &&
                oldItem.name == newItem.name &&
                oldItem.version == newItem.version &&
                oldItem.activityNames == newItem.activityNames &&
                oldItem.defaultActivityName == newItem.defaultActivityName &&
                oldItem.isFullyLoaded == newItem.isFullyLoaded
        }
    }
}
