package de.szalkowski.activitylauncher.ui

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.IconPickerBinding
import de.szalkowski.activitylauncher.services.IconLoaderService
import javax.inject.Inject

@AndroidEntryPoint
class IconPickerDialogFragment : DialogFragment(), AsyncProvider.Listener<IconListAdapter> {
    @Inject
    internal lateinit var iconListAsyncProviderFactory: IconListAsyncProvider.IconListAsyncProviderFactory

    private var listener: IconPickerListener? = null
    private var _binding: IconPickerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
        _binding = IconPickerBinding.inflate(layoutInflater, null, false)
        val view = _binding!!.root

        binding.gvIcons.onItemClickListener =
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
            binding.gvIcons.adapter = value
            binding.progressCircular.visibility = View.GONE
            binding.gvIcons.visibility = View.VISIBLE
        } catch (ignored: Exception) {
            Toast.makeText(this.activity, R.string.error_icons, Toast.LENGTH_SHORT).show()
        }
    }

    fun interface IconPickerListener {
        fun iconPicked(icon: String)
    }
}
