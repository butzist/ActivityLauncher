package de.szalkowski.activitylauncher.presentation.common

import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import de.szalkowski.activitylauncher.domain.model.ShortcutRequest

object ShortcutRequestDiffCallback : DiffUtil.ItemCallback<ShortcutRequest>() {
    override fun areItemsTheSame(oldItem: ShortcutRequest, newItem: ShortcutRequest): Boolean {
        return oldItem.intent.toUri(0) == newItem.intent.toUri(0)
    }

    override fun areContentsTheSame(oldItem: ShortcutRequest, newItem: ShortcutRequest): Boolean {
        return oldItem == newItem
    }
}

fun bindShortcutRequest(item: ShortcutRequest, tvName: TextView, tvClass: TextView, ivIcon: ImageView) {
    tvName.text = item.name
    tvClass.text = item.intent.component?.shortClassName ?: ""
    val context = ivIcon.context
    ivIcon.setImageDrawable(item.icon.loadInternalDrawable(context) ?: context.packageManager.defaultActivityIcon)
}
