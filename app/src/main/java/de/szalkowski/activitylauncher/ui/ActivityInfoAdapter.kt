package de.szalkowski.activitylauncher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.MyActivityInfo

class ActivityInfoAdapter : ListAdapter<MyActivityInfo, ActivityInfoAdapter.ViewHolder>(ActivityDiffCallback) {

    var onItemClick: ((MyActivityInfo) -> Unit)? = null
    var onItemLongClick: ((MyActivityInfo) -> Unit)? = null

    public override fun getItem(position: Int): MyActivityInfo = super.getItem(position)

    inner class ViewHolder(viewItem: View) : RecyclerView.ViewHolder(viewItem) {
        val tvName: TextView = viewItem.findViewById(R.id.tvName)
        val tvClass: TextView = viewItem.findViewById(R.id.tvClass)
        val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick?.invoke(getItem(position))
                }
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
        holder.tvName.text = item.name
        holder.tvClass.text = item.componentName.shortClassName
        holder.ivIcon.setImageDrawable(item.icon)
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
