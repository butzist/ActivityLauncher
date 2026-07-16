package de.szalkowski.activitylauncher.entrypoint

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import de.szalkowski.activitylauncher.R

class ShortcutUpdateActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlertDialog.Builder(this)
            .setTitle(R.string.title_dialog_update_shortcut)
            .setMessage(R.string.dialog_update_shortcut)
            .setPositiveButton(R.string.action_update) { _, _ ->
                ShortcutUpdateConfirmationImpl.onResult(true)
                finish()
            }
            .setNegativeButton(R.string.action_add_new) { _, _ ->
                ShortcutUpdateConfirmationImpl.onResult(false)
                finish()
            }
            .setOnCancelListener {
                ShortcutUpdateConfirmationImpl.onResult(false)
                finish()
            }
            .show()
    }
}
