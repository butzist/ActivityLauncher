package de.szalkowski.activitylauncher.presentation.intent

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.databinding.DialogEditIntentBinding
import de.szalkowski.activitylauncher.databinding.ItemIntentCategoryBinding
import de.szalkowski.activitylauncher.databinding.ItemIntentExtraBinding
import de.szalkowski.activitylauncher.domain.intent.ExtraDef
import de.szalkowski.activitylauncher.domain.intent.ExtraType
import de.szalkowski.activitylauncher.domain.intent.IntentDef
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditIntentDialogFragment : DialogFragment() {
    private val viewModel: EditIntentViewModel by viewModels()
    private var _binding: DialogEditIntentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditIntentBinding.inflate(layoutInflater)

        val initialIntentDef = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_INTENT_DEF, IntentDef::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable<IntentDef>(ARG_INTENT_DEF)
        } ?: IntentDef()
        viewModel.init(initialIntentDef)

        setupPrefilledAdapters()
        setupListeners()
        observeViewModel()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_dialog_edit_intent)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(R.string.action_clear, null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        setFragmentResult(REQUEST_KEY, bundleOf(RESULT_INTENT_DEF to viewModel.intentDef.value))
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? AlertDialog
        val okButton = dialog?.getButton(DialogInterface.BUTTON_POSITIVE)
        val clearButton = dialog?.getButton(DialogInterface.BUTTON_NEUTRAL)

        clearButton?.setOnClickListener {
            binding.root.clearFocus()
            viewModel.clear()
        }

        lifecycleScope.launch {
            viewModel.isIntentValid.collect { isValid ->
                okButton?.isEnabled = isValid
            }
        }
    }

    private fun setupPrefilledAdapters() {
        val actions = listOf(
            Intent.ACTION_VIEW,
            Intent.ACTION_SEND,
            Intent.ACTION_EDIT,
            Intent.ACTION_MAIN,
            Intent.ACTION_PICK,
            Intent.ACTION_GET_CONTENT,
        )
        binding.atvAction.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, actions))

        val mimeTypes = listOf(
            "text/plain",
            "image/*",
            "video/*",
            "audio/*",
            "application/pdf",
            "application/octet-stream",
        )
        binding.atvMimeType.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mimeTypes))
    }

    private fun setupListeners() {
        binding.atvAction.doAfterTextChanged { viewModel.updateAction(it.toString()) }
        binding.tiData.doAfterTextChanged { viewModel.updateData(it.toString()) }
        binding.atvMimeType.doAfterTextChanged { viewModel.updateMimeType(it.toString()) }

        binding.btAddCategory.setOnClickListener { viewModel.addCategory() }
        binding.btAddExtra.setOnClickListener { showAddExtraMenu() }
    }

    private fun showAddExtraMenu() {
        val popup = PopupMenu(requireContext(), binding.btAddExtra)
        ExtraType.entries.forEach { type ->
            popup.menu.add(type.name)
        }
        popup.setOnMenuItemClickListener { item ->
            viewModel.addExtra(ExtraType.valueOf(item.title.toString()))
            true
        }
        popup.show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.intentDef.collect { intentDef ->
                if (!binding.atvAction.isFocused && binding.atvAction.text.toString() != (intentDef.action ?: "")) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        binding.atvAction.setText(intentDef.action, false)
                    } else {
                        binding.atvAction.setText(intentDef.action)
                    }
                }
                if (!binding.tiData.isFocused && binding.tiData.text.toString() != (intentDef.data ?: "")) {
                    binding.tiData.setText(intentDef.data)
                }
                if (!binding.atvMimeType.isFocused && binding.atvMimeType.text.toString() != (intentDef.mimeType ?: "")) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        binding.atvMimeType.setText(intentDef.mimeType, false)
                    } else {
                        binding.atvMimeType.setText(intentDef.mimeType)
                    }
                }

                updateCategories(intentDef.categories)
                updateExtras(intentDef.extras)
            }
        }
    }

    private fun updateCategories(categories: List<String>) {
        if (binding.llCategories.childCount != categories.size) {
            binding.llCategories.removeAllViews()
            categories.forEachIndexed { index, category ->
                val itemBinding = ItemIntentCategoryBinding.inflate(
                    layoutInflater,
                    binding.llCategories,
                    true,
                )
                itemBinding.atvCategory.setText(category)

                fun validateCategory() {
                    val currentText = itemBinding.atvCategory.text.toString()
                    val isDuplicate = currentText.isNotEmpty() && categories.count { it == currentText } > 1
                    itemBinding.tilCategory.error = when {
                        currentText.isEmpty() -> getString(R.string.error_field_required)
                        isDuplicate -> getString(R.string.error_duplicate_category)
                        else -> null
                    }
                }

                validateCategory()

                val commonCategories = listOf(
                    Intent.CATEGORY_DEFAULT,
                    Intent.CATEGORY_BROWSABLE,
                    Intent.CATEGORY_LAUNCHER,
                    Intent.CATEGORY_HOME,
                    Intent.CATEGORY_PREFERENCE,
                )
                itemBinding.atvCategory.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        commonCategories,
                    ),
                )

                itemBinding.atvCategory.doAfterTextChanged {
                    if (itemBinding.atvCategory.hasFocus()) {
                        viewModel.updateCategory(index, it.toString())
                        validateCategory()
                    }
                }
                itemBinding.ibRemoveCategory.setOnClickListener { viewModel.removeCategory(index) }
            }
        } else {
            for (i in 0 until categories.size) {
                val view = binding.llCategories.getChildAt(i)
                val itemBinding = ItemIntentCategoryBinding.bind(view)
                if (!itemBinding.atvCategory.isFocused && itemBinding.atvCategory.text.toString() != categories[i]) {
                    itemBinding.atvCategory.setText(categories[i])
                }

                val currentText = itemBinding.atvCategory.text.toString()
                val isDuplicate = currentText.isNotEmpty() && categories.count { it == currentText } > 1
                itemBinding.tilCategory.error = when {
                    currentText.isEmpty() -> getString(R.string.error_field_required)
                    isDuplicate -> getString(R.string.error_duplicate_category)
                    else -> null
                }
            }
        }
    }

    private fun updateExtras(extras: List<ExtraDef>) {
        // Check if we need to rebuild the list (size changed or types changed)
        val needsRebuild = binding.llExtras.childCount != extras.size || extras.indices.any { i ->
            val view = binding.llExtras.getChildAt(i)
            (view.tag as? ExtraType) != extras[i].type
        }

        if (needsRebuild) {
            binding.llExtras.removeAllViews()
            extras.forEachIndexed { index, extra ->
                val itemBinding = ItemIntentExtraBinding.inflate(
                    layoutInflater,
                    binding.llExtras,
                    true,
                )
                itemBinding.root.tag = extra.type
                itemBinding.tiExtraKey.setText(extra.key)
                itemBinding.tiExtraValue.setText(extra.value)

                val hintRes = when (extra.type) {
                    ExtraType.STRING -> R.string.hint_extra_value_string
                    ExtraType.INT -> R.string.hint_extra_value_int
                    ExtraType.LONG -> R.string.hint_extra_value_long
                    ExtraType.FLOAT -> R.string.hint_extra_value_float
                    ExtraType.DOUBLE -> R.string.hint_extra_value_double
                    ExtraType.BOOLEAN -> R.string.hint_extra_value_boolean
                }
                itemBinding.tilExtraValue.placeholderText = getString(hintRes)

                fun validate() {
                    val key = itemBinding.tiExtraKey.text.toString()
                    val isDuplicateKey = key.isNotEmpty() && extras.count { it.key == key } > 1
                    itemBinding.tilExtraKey.error = when {
                        key.isEmpty() -> getString(R.string.error_field_required)
                        isDuplicateKey -> getString(R.string.error_duplicate_extra_key)
                        else -> null
                    }

                    val value = itemBinding.tiExtraValue.text.toString()
                    if (extra.type.isValid(value)) {
                        itemBinding.tilExtraValue.error = null
                    } else {
                        itemBinding.tilExtraValue.error = when (extra.type) {
                            ExtraType.INT -> getString(R.string.error_invalid_int)
                            ExtraType.LONG -> getString(R.string.error_invalid_long)
                            ExtraType.FLOAT -> getString(R.string.error_invalid_float)
                            ExtraType.DOUBLE -> getString(R.string.error_invalid_double)
                            ExtraType.BOOLEAN -> getString(R.string.error_invalid_boolean)
                            else -> null
                        }
                    }
                }

                validate()

                itemBinding.tiExtraKey.doAfterTextChanged {
                    if (itemBinding.tiExtraKey.hasFocus()) {
                        val latestExtra = viewModel.intentDef.value.extras.getOrNull(index) ?: return@doAfterTextChanged
                        viewModel.updateExtra(index, latestExtra.copy(key = it.toString()))
                    }
                }
                itemBinding.tiExtraValue.doAfterTextChanged {
                    if (itemBinding.tiExtraValue.hasFocus()) {
                        val latestExtra = viewModel.intentDef.value.extras.getOrNull(index) ?: return@doAfterTextChanged
                        viewModel.updateExtra(index, latestExtra.copy(value = it.toString()))
                        validate()
                    }
                }
                itemBinding.ibRemoveExtra.setOnClickListener { viewModel.removeExtra(index) }
            }
        } else {
            for (i in 0 until extras.size) {
                val view = binding.llExtras.getChildAt(i)
                val itemBinding = ItemIntentExtraBinding.bind(view)
                val extra = extras[i]

                if (!itemBinding.tiExtraKey.isFocused && itemBinding.tiExtraKey.text.toString() != extra.key) {
                    itemBinding.tiExtraKey.setText(extra.key)
                }
                if (!itemBinding.tiExtraValue.isFocused && (itemBinding.tiExtraValue.text.toString() != extra.value)) {
                    itemBinding.tiExtraValue.setText(extra.value)
                }

                val key = itemBinding.tiExtraKey.text.toString()
                val isDuplicateKey = key.isNotEmpty() && extras.count { it.key == key } > 1
                itemBinding.tilExtraKey.error = when {
                    key.isEmpty() -> getString(R.string.error_field_required)
                    isDuplicateKey -> getString(R.string.error_duplicate_extra_key)
                    else -> null
                }

                // Re-validate in case validation state changed elsewhere
                val value = itemBinding.tiExtraValue.text.toString()
                if (extra.type.isValid(value)) {
                    itemBinding.tilExtraValue.error = null
                } else if (itemBinding.tilExtraValue.error == null) {
                    itemBinding.tilExtraValue.error = when (extra.type) {
                        ExtraType.INT -> getString(R.string.error_invalid_int)
                        ExtraType.LONG -> getString(R.string.error_invalid_long)
                        ExtraType.FLOAT -> getString(R.string.error_invalid_float)
                        ExtraType.DOUBLE -> getString(R.string.error_invalid_double)
                        ExtraType.BOOLEAN -> getString(R.string.error_invalid_boolean)
                        else -> null
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "edit_intent_request"
        const val RESULT_INTENT_DEF = "result_intent_def"
        private const val ARG_INTENT_DEF = "arg_intent_def"

        fun newInstance(intentDef: IntentDef): EditIntentDialogFragment {
            return EditIntentDialogFragment().apply {
                arguments = bundleOf(ARG_INTENT_DEF to intentDef)
            }
        }
    }
}
