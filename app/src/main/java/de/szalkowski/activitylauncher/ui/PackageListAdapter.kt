package de.szalkowski.activitylauncher.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.ActivityName
import de.szalkowski.activitylauncher.services.MyPackageInfo
import de.szalkowski.activitylauncher.services.PackageListService
import javax.inject.Inject

class PackageListAdapter @Inject constructor(packageListService: PackageListService) :
    RecyclerView.Adapter<PackageListAdapter.ViewHolder>() {

    private val allPackages = packageListService.packages
    private var filteredPackages = allPackages

    var onItemClick: ((MyPackageInfo) -> Unit)? = null

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        lateinit var item: MyPackageInfo

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    var filter: String = ""
        set(value) {
            field = value
            filteredPackages = allPackages.map { p ->
                p.copy(
                    activityNames = p.activityNames.filter { it.matches(field) },
                    defaultActivityName = p.defaultActivityName?.takeIf { a ->
                        a.matches(field) || p.matches(field)
                    })
            }.filter { p ->
                p.activityNames.isNotEmpty() || p.defaultActivityName != null
            }

            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item_package_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return filteredPackages.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvPackage = view.findViewById<TextView>(R.id.tvClass)
        val tvVersion = view.findViewById<TextView>(R.id.tvVersion)
        val tvActivities = view.findViewById<TextView>(R.id.tvActivities)
        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)

        val item = filteredPackages[position]
        val activityCount = item.activityNames.size + (item.defaultActivityName?.let { 1 } ?: 0)
        holder.item = item
        tvName.text = item.name
        tvVersion.text = item.version
        tvPackage.text = item.packageName
        tvActivities.text = "(${activityCount})"

        ivIcon.setImageDrawable(item.icon)
    }
}


private fun ActivityName.matches(s: String): Boolean =
    listOf(this.name, this.shortCls).any {
        it.contains(
            s, ignoreCase = true
        )
    }

private fun MyPackageInfo.matches(s: String): Boolean =
    listOf(this.name, this.packageName).any {
        it.contains(
            s, ignoreCase = true
        )
    }
