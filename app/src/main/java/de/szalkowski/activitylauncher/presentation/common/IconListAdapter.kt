package de.szalkowski.activitylauncher.presentation.common

import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.domain.launcher.IconLoader
import de.szalkowski.activitylauncher.domain.model.IconInfo
import java.util.Locale
import javax.inject.Inject

class IconListAdapter @Inject constructor(private val iconLoader: IconLoader) :
    RecyclerView.Adapter<IconListAdapter.ViewHolder>(), Filterable {
    private var allIcons: List<IconInfo> = emptyList()
    private var filteredIcons: List<IconInfo> = emptyList()
    var onFilterListener: OnFilterListener? = null
    var onItemClickListener: OnItemClickListener? = null

    val totalCount: Int get() = allIcons.size
    val filteredCount: Int get() = filteredIcons.size

    fun resolve(updater: AsyncProvider<IconListAdapter>.Updater?) {
        this.allIcons = iconLoader.loadIcons(updater)
        this.filteredIcons = allIcons
    }

    override fun getItemCount(): Int {
        return filteredIcons.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val view = ShapeableImageView(context).apply {
            val size = context.resources.getDimensionPixelSize(R.dimen.icon_size)
            layoutParams = ViewGroup.LayoutParams(size, size)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val padding = context.resources.getDimensionPixelSize(R.dimen.icon_padding)
            setPadding(padding, padding, padding, padding)
            shapeAppearanceModel = ShapeAppearanceModel.builder(
                context,
                R.style.ShapeAppearance_ActivityLauncher_Icon,
                0,
            ).build()
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val iconInfo = filteredIcons[position]
        val icon = iconLoader.getIcon(iconInfo.iconResourceName)
        val drawable = icon.loadInternalDrawable(context) ?: context.packageManager.defaultActivityIcon

        (holder.itemView as ShapeableImageView).setImageDrawable(drawable)
        TooltipCompat.setTooltipText(holder.itemView, iconInfo.iconResourceName)
        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(iconInfo)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.lowercase(Locale.getDefault()) ?: ""
                val filtered = if (query.isEmpty()) {
                    allIcons
                } else {
                    allIcons.filter { it.iconResourceName.lowercase(Locale.getDefault()).contains(query) }
                }

                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredIcons = results?.values as? List<IconInfo> ?: allIcons
                notifyDataSetChanged()
                onFilterListener?.onFilterFinished()
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    fun interface OnFilterListener {
        fun onFilterFinished()
    }

    fun interface OnItemClickListener {
        fun onItemClick(icon: IconInfo)
    }
}
