package de.szalkowski.activitylauncher.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.services.SettingsService
import javax.inject.Inject

@AndroidEntryPoint
class DisclaimerDialogFragment : DialogFragment() {
    @Inject
    internal lateinit var settingsService: SettingsService

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.title_dialog_disclaimer).setMessage(R.string.dialog_disclaimer)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                settingsService.disclaimerAccepted = true
            }.setNegativeButton(android.R.string.cancel) { _, _ ->
                settingsService.disclaimerAccepted = false
                requireActivity().finish()
            }
        return builder.create()
    }
}
