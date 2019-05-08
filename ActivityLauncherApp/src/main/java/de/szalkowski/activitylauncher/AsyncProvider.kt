package de.szalkowski.activitylauncher

import android.app.ProgressDialog
import android.content.Context
import android.os.AsyncTask

abstract class AsyncProvider<ReturnType> internal constructor(context: Context, private val listener: Listener<ReturnType>?, showProgressDialog: Boolean) : AsyncTask<Void, Int, ReturnType>() {
    private val message: CharSequence
    private var max: Int = 0
    private var progress: ProgressDialog? = null

    init {
        this.message = context.getText(R.string.dialog_progress_loading)

        if (showProgressDialog) {
            this.progress = ProgressDialog(context)
        } else {
            progress = null
        }
    }

    protected fun onProgressUpdate(vararg values: Int) {
        if (this.progress != null && values.size > 0) {
            val value = values[0]

            if (value == 0) {
                this.progress!!.isIndeterminate = false
                this.progress!!.max = this.max
            }

            this.progress!!.progress = value
        }
    }

    override fun onPreExecute() {
        super.onPreExecute()

        if (this.progress != null) {
            this.progress!!.setCancelable(false)
            this.progress!!.setMessage(this.message)
            this.progress!!.isIndeterminate = true
            this.progress!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            this.progress!!.show()
        }
    }

    override fun onPostExecute(result: ReturnType) {
        super.onPostExecute(result)
        if (this.listener != null) {
            this.listener.onProviderFininshed(this, result)
        }

        if (this.progress != null) {
            this.progress!!.dismiss()
        }
    }

    protected abstract fun run(updater: Updater): ReturnType

    override fun doInBackground(vararg params: Void): ReturnType {
        return run(Updater(this))
    }

    interface Listener<ReturnType> {
        fun onProviderFininshed(task: AsyncProvider<ReturnType>, value: ReturnType)
    }

    inner class Updater(private val provider: AsyncProvider<ReturnType>) {

        fun update(value: Int) {
            this.provider.publishProgress(value)
        }

        fun updateMax(value: Int) {
            this.provider.max = value
        }
    }
}
