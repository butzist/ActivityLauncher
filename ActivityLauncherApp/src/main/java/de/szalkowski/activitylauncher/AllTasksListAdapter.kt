package de.szalkowski.activitylauncher

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import java.util.*

class AllTasksListAdapter internal constructor(context: Context) : BaseExpandableListAdapter() {
    private val pm: PackageManager
    private var packages: MutableList<MyPackageInfo>? = null
    private val inflater: LayoutInflater

    init {
        this.inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        this.pm = context.packageManager
    }

    internal fun resolve(updater: AsyncProvider<AllTasksListAdapter>.Updater) {
        val cache = PackageManagerCache.getPackageManagerCache(this.pm)
        val all_packages = this.pm.getInstalledPackages(0)
        updater.updateMax(all_packages.size)
        updater.update(0)

        this.packages = all_packages.mapIndexed { i, pack ->
            updater.update(i + 1)
            try {
                cache.getPackageInfo(pack.packageName)
            } catch (ignored: NameNotFoundException) {
                null
            }
        }.filterNotNull().filter {
            it.activitiesCount > 0
        }.toMutableList()

        for (i in all_packages.indices) {
            updater.update(i + 1)
            val pack = all_packages[i]
            val mypack: MyPackageInfo?
            try {
                mypack = cache.getPackageInfo(pack.packageName)
                if (mypack!!.activitiesCount > 0) {
                    this.packages!!.add(mypack)
                }
            } catch (ignored: NameNotFoundException) {
            }

        }

        Collections.sort(this.packages)
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return this.packages!![groupPosition].getActivity(childPosition)
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View, parent: ViewGroup): View {
        val activity = getChild(groupPosition, childPosition) as MyActivityInfo
        val view = this.inflater.inflate(R.layout.all_activities_child_item, null)

        val text1 = view.findViewById<TextView>(android.R.id.text1)
        text1.text = activity.name

        val text2 = view.findViewById<TextView>(android.R.id.text2)
        text2.text = activity.componentName.className

        val icon = view.findViewById<ImageView>(android.R.id.icon)
        icon.setImageDrawable(activity.icon)

        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return this.packages!![groupPosition].activitiesCount
    }

    override fun getGroup(groupPosition: Int): Any {
        return this.packages!![groupPosition]
    }

    override fun getGroupCount(): Int {
        return this.packages!!.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View, parent: ViewGroup): View {
        val pack = getGroup(groupPosition) as MyPackageInfo
        val view = this.inflater.inflate(R.layout.all_activities_group_item, null)

        val text = view.findViewById<TextView>(android.R.id.text1)
        text.text = pack.name

        val icon = view.findViewById<ImageView>(android.R.id.icon)
        icon.setImageDrawable(pack.icon)

        return view
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

}
