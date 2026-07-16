package de.szalkowski.activitylauncher.presentation.common

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.ScaleGestureDetector
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.core.util.getLauncherLargeIconSize
import de.szalkowski.activitylauncher.core.util.toBitmap
import de.szalkowski.activitylauncher.databinding.DialogCropIconBinding
import de.szalkowski.activitylauncher.domain.model.ActivityIcon
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CropIconDialogFragment : DialogFragment() {
    private var _binding: DialogCropIconBinding? = null
    private val binding get() = _binding!!

    private var listener: CropListener? = null
    private var imageUri: Uri? = null

    private val matrix = Matrix()
    private val savedMatrix = Matrix()

    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2

        fun newInstance(uri: Uri): CropIconDialogFragment {
            return CropIconDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("uri", uri)
                }
            }
        }
    }

    fun setCropListener(listener: CropListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        imageUri = arguments?.let { BundleCompat.getParcelable(it, "uri", Uri::class.java) }
        _binding = DialogCropIconBinding.inflate(layoutInflater)

        setupGestures()

        imageUri?.let { uri ->
            binding.ivPhoto.setImageURI(uri)
            binding.ivPhoto.post {
                centerImage()
            }
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.title_dialog_crop_icon)
        builder.setView(binding.root)
        builder.setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
            val cropped = cropImage()
            listener?.onCropped(cropped)
        }
        builder.setNegativeButton(android.R.string.cancel, null)

        return builder.create()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        val scaleDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val scaleFactor = detector.scaleFactor
                    matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                    binding.ivPhoto.imageMatrix = matrix
                    return true
                }
            },
        )

        binding.ivPhoto.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)

            when (event.action and android.view.MotionEvent.ACTION_MASK) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    start.set(event.x, event.y)
                    mode = DRAG
                }
                android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = spacing(event)
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        midPoint(mid, event)
                        mode = ZOOM
                    }
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        matrix.set(savedMatrix)
                        matrix.postTranslate(event.x - start.x, event.y - start.y)
                    } else if (mode == ZOOM) {
                        val newDist = spacing(event)
                        if (newDist > 10f) {
                            matrix.set(savedMatrix)
                            val scale = newDist / oldDist
                            matrix.postScale(scale, scale, mid.x, mid.y)
                        }
                    }
                }
            }
            binding.ivPhoto.imageMatrix = matrix
            true
        }
    }

    private fun centerImage() {
        val drawable = binding.ivPhoto.drawable ?: return
        val fullBitmap = drawable.toBitmap()
        val viewWidth = binding.ivPhoto.width.toFloat()
        val viewHeight = binding.ivPhoto.height.toFloat()
        val drawableWidth = fullBitmap.width.toFloat()
        val drawableHeight = fullBitmap.height.toFloat()

        val scale = if ((viewWidth / drawableWidth) < (viewHeight / drawableHeight)) {
            viewWidth / drawableWidth
        } else {
            viewHeight / drawableHeight
        }

        matrix.setScale(scale, scale)
        matrix.postTranslate((viewWidth - drawableWidth * scale) / 2f, (viewHeight - drawableHeight * scale) / 2f)
        binding.ivPhoto.imageMatrix = matrix
    }

    private fun spacing(event: android.view.MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    private fun midPoint(point: PointF, event: android.view.MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun cropImage(): ActivityIcon {
        val drawable = binding.ivPhoto.drawable ?: return ActivityIcon.BitmapIcon(createBitmap(1, 1, Bitmap.Config.ARGB_8888), false)
        val fullBitmap = drawable.toBitmap()

        val viewport = binding.vViewport
        val viewportWidth = viewport.width.toFloat()
        val viewportHeight = viewport.height.toFloat()

        val photo = binding.ivPhoto
        // Use coordinates relative to the common parent
        val viewportX = viewport.x
        val viewportY = viewport.y
        val photoX = photo.x
        val photoY = photo.y

        // Adaptive icons need a 108dp area where 72dp is the safe zone.
        // Ratio is 108/72 = 1.5
        val adaptiveSizeFactor = 1.5f
        val adaptiveWidth = viewportWidth * adaptiveSizeFactor
        val adaptiveHeight = viewportHeight * adaptiveSizeFactor

        val left = viewportX - (adaptiveWidth - viewportWidth) / 2f
        val top = viewportY - (adaptiveHeight - viewportHeight) / 2f
        val right = left + adaptiveWidth
        val bottom = top + adaptiveHeight

        val inverse = Matrix()
        matrix.invert(inverse)

        val pts = floatArrayOf(
            left - photoX,
            top - photoY,
            right - photoX,
            bottom - photoY,
        )
        inverse.mapPoints(pts)

        val cropRectLeft = pts[0].roundToInt().coerceIn(0, fullBitmap.width - 1)
        val cropRectTop = pts[1].roundToInt().coerceIn(0, fullBitmap.height - 1)
        val cropRectRight = pts[2].roundToInt().coerceIn(cropRectLeft + 1, fullBitmap.width)
        val cropRectBottom = pts[3].roundToInt().coerceIn(cropRectTop + 1, fullBitmap.height)

        val cropRectWidth = cropRectRight - cropRectLeft
        val cropRectHeight = cropRectBottom - cropRectTop

        val cropped = Bitmap.createBitmap(
            fullBitmap,
            cropRectLeft,
            cropRectTop,
            cropRectWidth,
            cropRectHeight,
        )

        val targetSize = (requireContext().getLauncherLargeIconSize() * adaptiveSizeFactor).toInt()
        val scaled = cropped.scale(targetSize, targetSize, true)
        return ActivityIcon.BitmapIcon(scaled, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun interface CropListener {
        fun onCropped(icon: ActivityIcon)
    }
}
