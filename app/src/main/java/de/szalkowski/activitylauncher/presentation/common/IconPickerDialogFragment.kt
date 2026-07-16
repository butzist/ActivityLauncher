package de.szalkowski.activitylauncher.presentation.common

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.IconPickerBinding
import javax.inject.Inject

@AndroidEntryPoint
class IconPickerDialogFragment : DialogFragment(), AsyncProvider.Listener<IconListAdapter> {
    @Inject
    internal lateinit var iconListAsyncProviderFactory: IconListAsyncProvider.IconListAsyncProviderFactory

    private var listener: IconPickerListener? = null
    private var _binding: IconPickerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog)

        val provider = iconListAsyncProviderFactory.create(this)
        provider.execute(lifecycleScope)
    }

    fun attachIconPickerListener(listener: IconPickerListener) {
        this.listener = listener
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val height = (resources.displayMetrics.heightPixels * 0.9).toInt()
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = IconPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.tiSearch.doAfterTextChanged { text ->
            (binding.rvIcons.adapter as? IconListAdapter)?.filter?.filter(text)
        }

        binding.rvIcons.background = CheckerboardDrawable()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onProviderFinished(task: AsyncProvider<IconListAdapter>?, value: IconListAdapter) {
        try {
            value.onItemClickListener = IconListAdapter.OnItemClickListener { iconInfo ->
                listener?.iconPicked(iconInfo.iconResourceName)
                requireDialog().dismiss()
            }
            value.onFilterListener = IconListAdapter.OnFilterListener {
                updateStatus(value)
            }

            val iconSize = resources.getDimensionPixelSize(R.dimen.icon_size)
            val padding = resources.getDimensionPixelSize(R.dimen.icon_padding)
            val totalIconWidth = iconSize + (padding * 2)

            binding.rvIcons.post {
                val width = binding.rvIcons.width
                if (width > 0) {
                    val spanCount = (width / totalIconWidth).coerceAtLeast(1)
                    binding.rvIcons.layoutManager = GridLayoutManager(requireContext(), spanCount)
                }
            }

            binding.rvIcons.layoutManager = GridLayoutManager(requireContext(), 6) // Fallback
            binding.rvIcons.adapter = value
            updateStatus(value)

            binding.progressCircular.visibility = View.GONE
            binding.searchContainer.visibility = View.VISIBLE
            binding.tvStatus.visibility = View.VISIBLE
            binding.rvIcons.visibility = View.VISIBLE
        } catch (_: Exception) {
            Toast.makeText(this.activity, R.string.error_icons, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatus(adapter: IconListAdapter) {
        binding.tvStatus.text = getString(
            R.string.filter_count_format,
            adapter.filteredCount,
            adapter.totalCount,
        )
    }

    fun interface IconPickerListener {
        fun iconPicked(icon: String)
    }

    private class CheckerboardDrawable : Drawable() {
        private val paint = Paint()
        private val size = 20f // pixels

        override fun draw(canvas: Canvas) {
            val width = bounds.width()
            val height = bounds.height()

            for (y in 0 until (height / size).toInt() + 1) {
                for (x in 0 until (width / size).toInt() + 1) {
                    paint.color = if ((x + y) % 2 == 0) 0xFF888888.toInt() else 0xFF666666.toInt()
                    canvas.drawRect(x * size, y * size, (x + 1) * size, (y + 1) * size, paint)
                }
            }
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        @Suppress("DEPRECATION")
        override fun getOpacity(): Int = PixelFormat.OPAQUE
    }
}
