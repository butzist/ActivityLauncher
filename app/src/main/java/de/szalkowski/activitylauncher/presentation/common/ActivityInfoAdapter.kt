package de.szalkowski.activitylauncher.presentation.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest

class ActivityInfoAdapter : ListAdapter<ShortcutRequest, ActivityInfoAdapter.ViewHolder>(ShortcutDiffCallback) {
    var onItemClick: ((ShortcutRequest) -> Unit)? = null
    var onItemLongClick: ((ShortcutRequest) -> Unit)? = null

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        val tvName: TextView = viewItem.findViewById(R.id.tvName)
        val tvPackage: TextView = viewItem.findViewById(R.id.tvClass)
        val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)
        lateinit var item: ShortcutRequest

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
        holder.tvPackage.text = item.intent.component?.shortClassName ?: ""

        val context = holder.itemView.context
        val drawable = item.icon.loadDrawable(context) ?: context.packageManager.defaultActivityIcon
        holder.ivIcon.setImageDrawable(drawable)
    }

    object ShortcutDiffCallback : DiffUtil.ItemCallback<ShortcutRequest>() {
        override fun areItemsTheSame(oldItem: ShortcutRequest, newItem: ShortcutRequest): Boolean {
            return oldItem.intent.toUri(0) == newItem.intent.toUri(0)
        }

        override fun areContentsTheSame(oldItem: ShortcutRequest, newItem: ShortcutRequest): Boolean {
            return oldItem == newItem
        }
    }
}
