package de.szalkowski.activitylauncher

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class IconPickerDialogFragment : DialogFragment(), AsyncProvider.Listener<IconListAdapter> {
    private var grid: GridView? = null
    private var listener: IconPickerListener? = null

    override fun onAttach(activity: Context?) {
        super.onAttach(activity)

        val provider = IconListAsyncProvider(this.activity!!, this)
        provider.execute()
    }

    fun attachIconPickerListener(listener: IconPickerListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val inflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.icon_picker, null)

        this.grid = view as GridView
        this.grid!!.onItemClickListener = OnItemClickListener { view, item, index, id ->
            if (this@IconPickerDialogFragment.listener != null) {
                this@IconPickerDialogFragment.listener!!.iconPicked(view.adapter.getItem(index).toString())
                this@IconPickerDialogFragment.dialog.dismiss()
            }
        }

        builder.setTitle(R.string.title_dialog_icon_picker)
                .setView(view)
                .setNegativeButton(android.R.string.cancel) { dialog, which -> this@IconPickerDialogFragment.dialog.cancel() }

        return builder.create()
    }

    override fun onProviderFininshed(task: AsyncProvider<IconListAdapter>,
                                     value: IconListAdapter) {
        try {
            this.grid!!.adapter = value
        } catch (e: Exception) {
            Toast.makeText(this.activity, R.string.error_icons, Toast.LENGTH_SHORT).show()
        }

    }

    interface IconPickerListener {
        fun iconPicked(icon: String)
    }
}
