package de.szalkowski.activitylauncher.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.IconLoaderService
import javax.inject.Inject

@AndroidEntryPoint
class IconPickerDialogFragment : DialogFragment(), AsyncProvider.Listener<IconListAdapter> {
    private lateinit var grid: GridView
    private var listener: IconPickerListener? = null

    @Inject
    internal lateinit var iconListAsyncProviderFactory: IconListAsyncProvider.IconListAsyncProviderFactory

    override fun onAttach(activity: Context) {
        super.onAttach(activity)

        val provider = iconListAsyncProviderFactory.create(this)
        provider.execute()
    }

    fun attachIconPickerListener(listener: IconPickerListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val view = layoutInflater.inflate(R.layout.icon_picker, null)

        this.grid = view as GridView
        grid.onItemClickListener =
            OnItemClickListener { adapterView: AdapterView<*>, _: View?, index: Int, _: Long ->
                listener?.iconPicked(
                    // FIXME ugly and unsafe
                    (adapterView.adapter.getItem(index) as IconLoaderService.IconInfo).iconResourceName
                )
                requireDialog().dismiss()
            }

        builder.setTitle(R.string.title_dialog_icon_picker).setView(view).setNegativeButton(
            android.R.string.cancel
        ) { dialog: DialogInterface?, _: Int ->
            dialog?.cancel()
        }

        return builder.create()
    }

    override fun onProviderFinished(task: AsyncProvider<IconListAdapter>?, value: IconListAdapter) {
        try {
            grid.adapter = value
        } catch (ignored: Exception) {
            Toast.makeText(this.activity, R.string.error_icons, Toast.LENGTH_SHORT).show()
        }
    }

    fun interface IconPickerListener {
        fun iconPicked(icon: String)
    }
}
