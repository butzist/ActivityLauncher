package de.szalkowski.activitylauncher.ui

import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.ImageView
import de.szalkowski.activitylauncher.services.IconLoaderService
import javax.inject.Inject

class IconListAdapter @Inject constructor(private val iconLoaderService: IconLoaderService) :
    BaseAdapter() {
    private lateinit var icons: List<IconLoaderService.IconInfo>

    fun resolve(updater: AsyncProvider<IconListAdapter>.Updater?) {
        this.icons = iconLoaderService.loadIcons(updater)
    }

    override fun getCount(): Int {
        return icons.size
    }

    override fun getItem(position: Int): Any {
        return icons[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = ImageView(parent.context)
        val layout = AbsListView.LayoutParams(50, 50)
        view.layoutParams = layout
        val icon = icons[position]
        view.setImageDrawable(icon.icon)
        return view
    }
}
