package de.szalkowski.activitylauncher.presentation.common

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.external.AnalyticsLogger
import javax.inject.Inject

@AndroidEntryPoint
class PaidDialogFragment : DialogFragment() {
    @Inject
    internal lateinit var analyticsLogger: AnalyticsLogger

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pro_options, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_dialog_support)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel) { d, _ ->
                d.dismiss()
            }
            .create()

        dialogView.findViewById<Button>(R.id.btn_pro).setOnClickListener {
            analyticsLogger.logSupportOption("pro")
            openUrl(getString(R.string.url_pro))
            dialog.hide()
        }

        dialogView.findViewById<Button>(R.id.btn_github).setOnClickListener {
            analyticsLogger.logSupportOption("github")
            openUrl(getString(R.string.url_github_project))
            dialog.hide()
        }

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        analyticsLogger.logSupportOption("dismissed")
        super.onDismiss(dialog)
    }

    private fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            activity?.startActivity(intent)
        }
    }
}
