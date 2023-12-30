package de.szalkowski.activitylauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.szalkowski.activitylauncher.R
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
            filteredPackages = allPackages.filter { p ->
                listOf(p.name, p.packageName).any {
                    it.contains(
                        field, ignoreCase = true
                    )
                }
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvPackage = view.findViewById<TextView>(R.id.tvPackage)
        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)

        val item = filteredPackages[position]
        holder.item = item
        tvName.text = item.name
        tvPackage.text = item.packageName
        ivIcon.setImageDrawable(item.icon)
    }
}


