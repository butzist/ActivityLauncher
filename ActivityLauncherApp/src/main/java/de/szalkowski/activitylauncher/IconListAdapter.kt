package de.szalkowski.activitylauncher

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.ImageView
import java.util.TreeSet

class IconListAdapter internal constructor(private val context: Context) : BaseAdapter() {
    private var icons: Array<String>? = null
    private val pm: PackageManager

    init {
        this.pm = context.packageManager
    }

    internal fun resolve(updater: AsyncProvider<IconListAdapter>.Updater) {
        val all_packages = this.pm.getInstalledPackages(0)
        updater.updateMax(all_packages.size)
        updater.update(0)

        val cache = PackageManagerCache.getPackageManagerCache(this.pm)

        this.icons = all_packages.mapIndexed {
            i, pack ->

            updater.update(i + 1)
            try {
                val info = cache.getPackageInfo(pack.packageName)
                (0 until info!!.activitiesCount).mapNotNull { info.getActivity(it).iconResouceName }
            } catch (ignored: NameNotFoundException) {
                emptyList<String>()
            }
        }.flatten().toHashSet().toTypedArray()
    }

    override fun getCount(): Int {
        return icons!!.size
    }

    override fun getItem(position: Int): Any {
        return icons!![position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        val view = ImageView(this.context)
        val layout = AbsListView.LayoutParams(50, 50)
        view.layoutParams = layout
        val icon_resource_string = this.icons!![position]
        view.setImageDrawable(IconListAdapter.getIcon(icon_resource_string, this.pm))
        return view
    }

    companion object {

        internal fun getIcon(icon_resource_string: String, pm: PackageManager): Drawable {
            try {
                val pack = icon_resource_string.substring(0, icon_resource_string.indexOf(':'))
                val type = icon_resource_string.substring(icon_resource_string.indexOf(':') + 1, icon_resource_string.indexOf('/'))
                val name = icon_resource_string.substring(icon_resource_string.indexOf('/') + 1)
                val res = pm.getResourcesForApplication(pack)
                return res.getDrawable(res.getIdentifier(name, type, pack))
            } catch (e: Exception) {
                return pm.defaultActivityIcon
            }

        }
    }
}
