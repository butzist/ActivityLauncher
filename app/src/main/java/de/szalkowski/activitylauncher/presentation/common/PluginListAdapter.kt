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
import de.szalkowski.activitylauncher.domain.model.PluginInfo

class PluginListAdapter(
    private val onPluginSelected: (PluginInfo?) -> Unit,
) : ListAdapter<PluginInfo, PluginListAdapter.ViewHolder>(PluginDiffCallback) {

    private var selectedPlugin: PluginInfo? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_plugin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plugin = getItem(position)
        holder.bind(plugin, plugin == selectedPlugin)
    }

    fun setSelectedPlugin(plugin: PluginInfo?) {
        val oldPlugin = selectedPlugin
        selectedPlugin = plugin
        val oldIndex = currentList.indexOf(oldPlugin)
        val newIndex = currentList.indexOf(plugin)
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        private val tvName: TextView = view.findViewById(R.id.tvName)

        fun bind(plugin: PluginInfo, isSelected: Boolean) {
            ivIcon.setImageDrawable(plugin.icon?.loadDrawable(itemView.context))
            tvName.text = plugin.name
            if (isSelected) {
                itemView.setBackgroundResource(R.color.color_selection)
            } else {
                itemView.setBackgroundResource(android.R.color.transparent)
            }
            itemView.setOnClickListener {
                if (selectedPlugin == plugin) {
                    setSelectedPlugin(null)
                    onPluginSelected(null)
                } else {
                    setSelectedPlugin(plugin)
                    onPluginSelected(plugin)
                }
            }
        }
    }

    object PluginDiffCallback : DiffUtil.ItemCallback<PluginInfo>() {
        override fun areItemsTheSame(oldItem: PluginInfo, newItem: PluginInfo): Boolean {
            return oldItem.componentName == newItem.componentName
        }

        override fun areContentsTheSame(oldItem: PluginInfo, newItem: PluginInfo): Boolean {
            return oldItem == newItem
        }
    }
}
