package de.szalkowski.activitylauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

import org.thirdparty.LauncherIconCreator

class AllTasksListFragment : Fragment(), AsyncProvider.Listener<AllTasksListAdapter> {
    private lateinit var list: ExpandableListView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.frament_all_list, null)

        this.list = view.findViewById(R.id.expandableListView1)
        this.list.setOnChildClickListener { parent, v, groupPosition, childPosition, id ->
            val adapter = parent.expandableListAdapter
            val info = adapter.getChild(groupPosition, childPosition) as MyActivityInfo
            LauncherIconCreator.launchActivity(activity!!, info.componentName)
            false
        }

        val provider = AllTasksListAsyncProvider(this.activity!!, this)
        provider.execute()

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        this.registerForContextMenu(this.list)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View,
                                     menuInfo: ContextMenuInfo) {
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.context_action_shortcut)
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.context_action_launch)

        val info = menuInfo as ExpandableListContextMenuInfo
        val list = view!!.findViewById<ExpandableListView>(R.id.expandableListView1)

        when (ExpandableListView.getPackedPositionType(info.packedPosition)) {
            ExpandableListView.PACKED_POSITION_TYPE_CHILD -> {
                val activity = list.expandableListAdapter.getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition)) as MyActivityInfo
                menu.setHeaderIcon(activity.icon)
                menu.setHeaderTitle(activity.name)
                menu.add(Menu.NONE, 2, Menu.NONE, R.string.context_action_edit)
            }
            ExpandableListView.PACKED_POSITION_TYPE_GROUP -> {
                val pack = list.expandableListAdapter.getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition)) as MyPackageInfo
                menu.setHeaderIcon(pack.icon)
                menu.setHeaderTitle(pack.name)
            }
        }

        super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem?): Boolean {
        val info = item!!.menuInfo as ExpandableListContextMenuInfo
        val list = view!!.findViewById<ExpandableListView>(R.id.expandableListView1)

        when (ExpandableListView.getPackedPositionType(info.packedPosition)) {
            ExpandableListView.PACKED_POSITION_TYPE_CHILD -> {
                val a = list.expandableListAdapter.getChild(ExpandableListView.getPackedPositionGroup(info.packedPosition), ExpandableListView.getPackedPositionChild(info.packedPosition)) as MyActivityInfo
                when (item.itemId) {
                    0 -> LauncherIconCreator.createLauncherIcon(activity!!, a)
                    1 -> LauncherIconCreator.launchActivity(activity!!, a.componentName)
                    2 -> {
                        val dialog = ShortcutEditDialogFragment()
                        val args = Bundle()
                        args.putParcelable("activity", a.componentName)
                        dialog.arguments = args
                        dialog.show(this.fragmentManager!!, "ShortcutEditor")
                    }
                }
            }

            ExpandableListView.PACKED_POSITION_TYPE_GROUP -> {
                val pack = list.expandableListAdapter.getGroup(ExpandableListView.getPackedPositionGroup(info.packedPosition)) as MyPackageInfo
                when (item.itemId) {
                    0 -> LauncherIconCreator.createLauncherIcon(activity!!, pack)
                    1 -> {
                        val pm = activity!!.packageManager
                        val intent = pm.getLaunchIntentForPackage(pack.packageName)
                        Toast.makeText(activity, String.format(getText(R.string.starting_application).toString(), pack.name), Toast.LENGTH_LONG).show()
                        activity!!.startActivity(intent)
                    }
                }
            }
        }
        return super.onContextItemSelected(item)
    }

    override fun onProviderFininshed(task: AsyncProvider<AllTasksListAdapter>, value: AllTasksListAdapter) {
        try {
            this.list.setAdapter(value)
        } catch (e: Exception) {
            Toast.makeText(this.activity, R.string.error_tasks, Toast.LENGTH_SHORT).show()
        }

    }
}