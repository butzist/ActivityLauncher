package de.szalkowski.activitylauncher.ui

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.ProgressDialogBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

abstract class AsyncProvider<ReturnType> internal constructor(
    private val context: Context,
    private val listener: Listener<ReturnType>?,
    private val showProgressDialog: Boolean,
) {
    private val message = context.getText(R.string.dialog_progress_loading)
    private var dialog: AlertDialog? = null
    private var binding: ProgressDialogBinding? = null
    private val progressPercentFormat: NumberFormat = NumberFormat.getPercentInstance()
    private var max = 0

    init {
        progressPercentFormat.maximumFractionDigits = 0
    }

    fun execute(scope: CoroutineScope = MainScope()) {
        scope.launch {
            onPreExecute()
            val result = withContext(Dispatchers.Default) {
                run(Updater())
            }
            onPostExecute(result)
        }
    }

    private fun updateProgress(value: Int) {
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
        val percent = if (max > 0) value.toDouble() / max.toDouble() else 0.0
        binding?.progressPercent?.text = progressPercentFormat.format(percent)
    }

    private fun onPreExecute() {
        if (showProgressDialog) {
            this.binding = ProgressDialogBinding.inflate(LayoutInflater.from(context))
            this.dialog = AlertDialog.Builder(context).setView(binding!!.root).create()
            dialog?.setCancelable(false)
            dialog?.setTitle(this.message)
            binding?.progress?.isIndeterminate = true
            dialog?.show()
        }
    }

    private fun onPostExecute(result: ReturnType) {
        listener?.onProviderFinished(this, result)
        runCatching {
            dialog?.dismiss()
        }
    }

    protected abstract fun run(updater: Updater?): ReturnType

    interface Listener<ReturnType> {
        fun onProviderFinished(task: AsyncProvider<ReturnType>?, value: ReturnType)
    }

    inner class Updater {
        fun update(value: Int) {
            binding?.root?.post {
                updateProgress(value)
            }
        }

        fun updateMax(value: Int) {
            this@AsyncProvider.max = value
        }
    }
}
