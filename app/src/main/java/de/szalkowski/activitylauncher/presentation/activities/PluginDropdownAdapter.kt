package de.szalkowski.activitylauncher.presentation.activities

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.model.PluginInfo

class PluginDropdownAdapter(context: Context, plugins: List<PluginInfo>) :
    ArrayAdapter<PluginInfo>(context, 0, plugins) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_plugin_dropdown, parent, false)
        val plugin = getItem(position)

        val ivIcon = view.findViewById<ImageView>(R.id.ivPluginIcon)
        val tvName = view.findViewById<TextView>(R.id.tvPluginName)

        tvName.text = plugin?.name
        plugin?.icon?.let {
            ivIcon.setImageDrawable(it.loadDrawable(context))
        } ?: run {
            ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        return view
    }
}
