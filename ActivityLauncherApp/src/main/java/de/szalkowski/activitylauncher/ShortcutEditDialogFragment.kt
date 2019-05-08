package de.szalkowski.activitylauncher

import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment

import org.thirdparty.LauncherIconCreator

import de.szalkowski.activitylauncher.IconPickerDialogFragment.IconPickerListener

class ShortcutEditDialogFragment : DialogFragment() {
    protected lateinit var activity: MyActivityInfo
    protected lateinit var text_name: EditText
    protected lateinit var text_package: EditText
    protected lateinit var text_class: EditText
    protected lateinit var text_icon: EditText
    protected lateinit var image_icon: ImageButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = arguments!!.getParcelable<Parcelable>("activity") as ComponentName
        this.activity = MyActivityInfo(activity, getActivity()!!.packageManager)

        val builder = AlertDialog.Builder(getActivity())
        val inflater = getActivity()!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_activity, null)

        this.text_name = view.findViewById<View>(R.id.editText_name) as EditText
        this.text_name.setText(this.activity.name)
        this.text_package = view.findViewById<View>(R.id.editText_package) as EditText
        this.text_package.setText(this.activity.componentName.packageName)
        this.text_class = view.findViewById<View>(R.id.editText_class) as EditText
        this.text_class.setText(this.activity.componentName.className)
        this.text_icon = view.findViewById<View>(R.id.editText_icon) as EditText
        this.text_icon.setText(this.activity.iconResouceName)

        this.text_icon.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int,
                                           after: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                val pm = getActivity()!!.packageManager
                val draw_icon = IconListAdapter.getIcon(s.toString(), pm)
                this@ShortcutEditDialogFragment.image_icon.setImageDrawable(draw_icon)
            }
        })

        this.image_icon = view.findViewById<View>(R.id.iconButton) as ImageButton
        this@ShortcutEditDialogFragment.image_icon.setImageDrawable(this.activity.icon)
        this.image_icon.setOnClickListener {
            val dialog = IconPickerDialogFragment()
            dialog.attachIconPickerListener( object: IconPickerListener {
                override fun iconPicked(icon: String) {
                    this@ShortcutEditDialogFragment.text_icon.setText(icon)
                    val pm = getActivity()!!.packageManager
                    val draw_icon = IconListAdapter.getIcon(icon, pm)
                    this@ShortcutEditDialogFragment.image_icon.setImageDrawable(draw_icon)
                }
            })
            dialog.show(fragmentManager!!, "icon picker")
        }

        builder.setTitle(this.activity.name)
                .setView(view)
                .setIcon(this.activity.icon)
                .setPositiveButton(R.string.context_action_shortcut) { dialog, which ->
                    this@ShortcutEditDialogFragment.activity.name = this@ShortcutEditDialogFragment.text_name.text.toString()
                    val component_package = this@ShortcutEditDialogFragment.text_package.text.toString()
                    val component_class = this@ShortcutEditDialogFragment.text_class.text.toString()
                    this@ShortcutEditDialogFragment.activity.componentName = ComponentName(component_package, component_class)
                    this@ShortcutEditDialogFragment.activity.iconResouceName = this@ShortcutEditDialogFragment.text_icon.text.toString()
                    val pm = getActivity()!!.packageManager
                    try {
                        val icon_resource_string = this@ShortcutEditDialogFragment.activity.iconResouceName
                        val pack = icon_resource_string!!.substring(0, icon_resource_string.indexOf(':'))
                        val type = icon_resource_string.substring(icon_resource_string.indexOf(':') + 1, icon_resource_string.indexOf('/'))
                        val name = icon_resource_string.substring(icon_resource_string.indexOf('/') + 1, icon_resource_string.length)

                        val resources = pm.getResourcesForApplication(pack)
                        this@ShortcutEditDialogFragment.activity.icon_resource = resources.getIdentifier(name, type, pack)
                        if (this@ShortcutEditDialogFragment.activity.icon_resource != 0) {
                            this@ShortcutEditDialogFragment.activity.icon = resources.getDrawable(this@ShortcutEditDialogFragment.activity.icon_resource)
                        } else {
                            this@ShortcutEditDialogFragment.activity.icon = pm.defaultActivityIcon
                            Toast.makeText(getActivity(), R.string.error_invalid_icon_resource, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: NameNotFoundException) {
                        this@ShortcutEditDialogFragment.activity.icon = pm.defaultActivityIcon
                        Toast.makeText(getActivity(), R.string.error_invalid_icon_resource, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        this@ShortcutEditDialogFragment.activity.icon = pm.defaultActivityIcon
                        Toast.makeText(getActivity(), R.string.error_invalid_icon_format, Toast.LENGTH_LONG).show()
                    }

                    LauncherIconCreator.createLauncherIcon(getActivity()!!, this@ShortcutEditDialogFragment.activity)
                }
                .setNegativeButton(android.R.string.cancel) { dialog, which -> this@ShortcutEditDialogFragment.dialog.cancel() }

        return builder.create()
    }

}
