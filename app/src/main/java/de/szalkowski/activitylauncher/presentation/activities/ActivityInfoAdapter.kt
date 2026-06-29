package de.szalkowski.activitylauncher.presentation.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.model.MyActivityInfo

class ActivityInfoAdapter(
    private val iconProvider: (MyActivityInfo) -> android.graphics.drawable.Drawable,
) : ListAdapter<MyActivityInfo, ActivityInfoAdapter.ViewHolder>(ActivityDiffCallback) {

    var onItemClick: ((MyActivityInfo) -> Unit)? = null
    var onItemLongClick: ((MyActivityInfo) -> Unit)? = null

    public override fun getItem(position: Int): MyActivityInfo = super.getItem(position)

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        val tvName: TextView = viewItem.findViewById(R.id.tvName)
        val tvPackage: TextView = viewItem.findViewById(R.id.tvClass)
        val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)
        lateinit var item: MyActivityInfo

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(item)
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
        val item = getItem(position)
        holder.item = item
        holder.tvName.text = item.name
        holder.tvPackage.text = item.componentName.shortClassName

        holder.ivIcon.setImageDrawable(iconProvider(item))
    }

    private object ActivityDiffCallback : DiffUtil.ItemCallback<MyActivityInfo>() {
        override fun areItemsTheSame(oldItem: MyActivityInfo, newItem: MyActivityInfo): Boolean {
            return oldItem.componentName == newItem.componentName
        }

        override fun areContentsTheSame(oldItem: MyActivityInfo, newItem: MyActivityInfo): Boolean {
            return oldItem == newItem
        }
    }
}
