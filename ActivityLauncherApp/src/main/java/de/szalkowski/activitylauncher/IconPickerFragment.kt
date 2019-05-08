package de.szalkowski.activitylauncher

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.fragment.app.Fragment

class IconPickerFragment : Fragment(), AsyncProvider.Listener<IconListAdapter> {
    private lateinit var grid: GridView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.icon_picker, null)

        this.grid = view as GridView
        this.grid.onItemClickListener = OnItemClickListener { view, item, index, id -> Toast.makeText(activity, view.adapter.getItem(index).toString(), Toast.LENGTH_SHORT).show() }

        return view
    }

    override fun onAttach(activity: Context?) {
        super.onAttach(activity)

        val provider = IconListAsyncProvider(this.activity!!, this)
        provider.execute()
    }

    override fun onProviderFininshed(task: AsyncProvider<IconListAdapter>,
                                     value: IconListAdapter) {
        try {
            this.grid.adapter = value
        } catch (e: Exception) {
            Toast.makeText(this.activity, R.string.error_icons, Toast.LENGTH_SHORT).show()
        }

    }

}
