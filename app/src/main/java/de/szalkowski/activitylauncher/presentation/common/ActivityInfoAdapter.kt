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

class ActivityInfoAdapter<T : Any>(
    diffCallback: DiffUtil.ItemCallback<T>,
    private val binder: (T, TextView, TextView, ImageView) -> Unit,
) : ListAdapter<T, ViewHolder<T>>(diffCallback) {
    var onItemClick: ((T) -> Unit)? = null
    var onItemLongClick: ((T) -> Unit)? = null
    var onItemSwiped: ((T) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<T> {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item_activity_list, parent, false)
        return ViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) {
        val item = getItem(position)
        holder.bind(item, binder)
    }
}

class ViewHolder<T : Any>(
    viewItem: View,
    private val onItemClick: ((T) -> Unit)?,
    private val onItemLongClick: ((T) -> Unit)?,
) : RecyclerView.ViewHolder(viewItem) {
    val tvName: TextView = viewItem.findViewById(R.id.tvName)
    val tvPackage: TextView = viewItem.findViewById(R.id.tvClass)
    val ivIcon: ImageView = viewItem.findViewById(R.id.ivIcon)
    private lateinit var item: T

    init {
        itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
        itemView.setOnLongClickListener {
            onItemLongClick?.invoke(item)
            true
        }
    }

    fun bind(item: T, binder: (T, TextView, TextView, ImageView) -> Unit) {
        this.item = item
        binder(item, tvName, tvPackage, ivIcon)
    }
}
