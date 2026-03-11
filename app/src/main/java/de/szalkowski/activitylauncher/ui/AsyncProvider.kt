package de.szalkowski.activitylauncher.ui

import android.content.Context
import android.os.AsyncTask
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.ProgressDialogBinding
import java.text.NumberFormat
import java.util.Locale

abstract class AsyncProvider<ReturnType> internal constructor(
    context: Context,
    private val listener: Listener<ReturnType>?,
    showProgressDialog: Boolean,
) : AsyncTask<Void?, Int?, ReturnType>() {
    private val message = context.getText(R.string.dialog_progress_loading)
    private var dialog: AlertDialog? = null
    private var binding: ProgressDialogBinding? = null
    private val progressPercentFormat: NumberFormat = NumberFormat.getPercentInstance()
    private var max = 0

    init {
        if (showProgressDialog) {
            this.binding = ProgressDialogBinding.inflate(LayoutInflater.from(context))
            this.dialog = AlertDialog.Builder(context).setView(binding!!.getRoot()).create()
            progressPercentFormat.maximumFractionDigits = 0
        } else {
            this.binding = null
            this.dialog = null
        }
    }

    protected fun onProgressUpdate(vararg values: Int) {
        if (values.isNotEmpty()) {
            val value = values[0]

            if (value == 0) {
                binding?.progress?.isIndeterminate = false
                binding?.progress?.max = this.max
            }

            binding?.progress?.progress = value
            binding?.progressNumber?.text = String.format(
                Locale.getDefault(),
                "%1d/%2d",
                value,
                this.max,
            )
            val percent = value.toDouble() / max.toDouble()
            binding?.progressPercent?.text = progressPercentFormat.format(percent)
        }
    }

    override fun onPreExecute() {
        super.onPreExecute()

        dialog?.setCancelable(false)
        dialog?.setTitle(this.message)
        binding?.progress?.isIndeterminate = true
        dialog?.show()
    }

    override fun onPostExecute(result: ReturnType) {
        super.onPostExecute(result)
        listener?.onProviderFinished(this, result)

        runCatching {
            dialog?.dismiss()
        }
    }

    protected abstract fun run(updater: Updater?): ReturnType

    override fun doInBackground(vararg params: Void?): ReturnType {
        return run(Updater(this))
    }

    interface Listener<ReturnType> {
        fun onProviderFinished(task: AsyncProvider<ReturnType>?, value: ReturnType)
    }

    inner class Updater(private val provider: AsyncProvider<ReturnType>) {
        fun update(value: Int) {
            provider.publishProgress(value)
        }

        fun updateMax(value: Int) {
            provider.max = value
        }
    }
}
