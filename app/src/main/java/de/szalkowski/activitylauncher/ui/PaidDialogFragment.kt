package de.szalkowski.activitylauncher.ui

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.AnalyticsService
import de.szalkowski.activitylauncher.services.SettingsService
import javax.inject.Inject

@AndroidEntryPoint
class PaidDialogFragment : DialogFragment() {
    @Inject
    internal lateinit var settingsService: SettingsService

    @Inject
    internal lateinit var analyticsService: AnalyticsService

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pro_options, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_dialog_support)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel) { d, _ ->
                d.dismiss()
            }
            .create()

        dialogView.findViewById<Button>(R.id.btn_pro).setOnClickListener {
            analyticsService.logSupportOption("pro")
            openUrl(getString(R.string.url_pro))
            dialog.hide()
        }

        dialogView.findViewById<Button>(R.id.btn_github).setOnClickListener {
            analyticsService.logSupportOption("github")
            openUrl(getString(R.string.url_github_project))
            dialog.hide()
        }

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        analyticsService.logSupportOption("dismissed")
        super.onDismiss(dialog)
    }

    private fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            activity?.startActivity(intent)
        }
    }
}
