package de.szalkowski.activitylauncher

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class DisclaimerDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.title_dialog_disclaimer)
                .setMessage(R.string.dialog_disclaimer)
                .setPositiveButton(android.R.string.yes) { dialog, which ->
                    val editor = activity!!.getPreferences(Context.MODE_PRIVATE).edit()
                    editor.putBoolean("disclaimer_accepted", true)
                    editor.apply()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, which ->
                    val editor = activity!!.getPreferences(Context.MODE_PRIVATE).edit()
                    editor.putBoolean("disclaimer_accepted", false)
                    editor.apply()
                    activity!!.finish()
                }

        return builder.create()
    }
}
