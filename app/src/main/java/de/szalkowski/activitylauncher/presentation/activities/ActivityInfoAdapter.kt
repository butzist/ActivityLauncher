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
import de.szalkowski.activitylauncher.domain.model.SystemActivity

class ActivityInfoAdapter(
    private val iconProvider: (SystemActivity) -> android.graphics.drawable.Drawable,
) : ListAdapter<SystemActivity, ActivityInfoAdapter.ViewHolder>(ActivityDiffCallback) {

    var onItemClick: ((SystemActivity) -> Unit)? = null
    var onItemLongClick: ((SystemActivity) -> Unit)? = null

    public override fun getItem(position: Int): SystemActivity = super.getItem(position)

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        val tvName: TextView = viewItem.findViewById(R.id.tvName)
        val tvPackage: TextView = viewItem.findViewById(R.id.tvClass)
        val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)
        lateinit var item: SystemActivity

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

    private object ActivityDiffCallback : DiffUtil.ItemCallback<SystemActivity>() {
        override fun areItemsTheSame(oldItem: SystemActivity, newItem: SystemActivity): Boolean {
            return oldItem.componentName == newItem.componentName
        }

        override fun areContentsTheSame(oldItem: SystemActivity, newItem: SystemActivity): Boolean {
            return oldItem == newItem
        }
    }
}
