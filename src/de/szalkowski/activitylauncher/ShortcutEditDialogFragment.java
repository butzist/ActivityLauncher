package de.szalkowski.activitylauncher;

import org.thirdparty.LauncherIconCreator;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class ShortcutEditDialogFragment extends DialogFragment {
	protected MyActivityInfo activity;
	protected EditText text_name;
	protected EditText text_package;
	protected EditText text_class;
	protected EditText text_icon;
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		ComponentName activity = (ComponentName)getArguments().getParcelable("activity");
		this.activity = new MyActivityInfo(activity, getActivity().getPackageManager());
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.dialog_edit_activity, null);
		
		this.text_name = (EditText)view.findViewById(R.id.editText_name);
		this.text_name.setText(this.activity.name);
		this.text_package = (EditText)view.findViewById(R.id.editText_package);
		this.text_package.setText(this.activity.component_name.getPackageName());
		this.text_class = (EditText)view.findViewById(R.id.editText_class);
		this.text_class.setText(this.activity.component_name.getClassName());
		this.text_icon = (EditText)view.findViewById(R.id.editText_icon);
		this.text_icon.setText(this.activity.icon_resource_name);
		
		builder.setTitle(this.activity.name)
				.setView(view)
				.setIcon(this.activity.icon)
				.setPositiveButton(R.string.context_action_shortcut, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ShortcutEditDialogFragment.this.activity.name = ShortcutEditDialogFragment.this.text_name.getText().toString();
						String component_package = ShortcutEditDialogFragment.this.text_package.getText().toString();
						String component_class = ShortcutEditDialogFragment.this.text_class.getText().toString();
						ComponentName component = new ComponentName(component_package,component_class);
						ShortcutEditDialogFragment.this.activity.component_name = component;
						ShortcutEditDialogFragment.this.activity.icon_resource_name = ShortcutEditDialogFragment.this.text_icon.getText().toString();
						PackageManager pm = getActivity().getPackageManager();
						Resources resources;
						try {
							resources = pm.getResourcesForApplication(component_package);
							ShortcutEditDialogFragment.this.activity.icon_resource = resources.getIdentifier(ShortcutEditDialogFragment.this.activity.icon_resource_name, null, null);
							if(ShortcutEditDialogFragment.this.activity.icon_resource != 0) {
								ShortcutEditDialogFragment.this.activity.icon = (BitmapDrawable)resources.getDrawable(ShortcutEditDialogFragment.this.activity.icon_resource);
							} else {
								ShortcutEditDialogFragment.this.activity.icon = (BitmapDrawable)pm.getDefaultActivityIcon();
								Toast.makeText(getActivity(), R.string.error_invalid_icon_resource, Toast.LENGTH_LONG).show();
							}
						} catch (NameNotFoundException e) {
							ShortcutEditDialogFragment.this.activity.icon = (BitmapDrawable)pm.getDefaultActivityIcon();
							Toast.makeText(getActivity(), R.string.error_invalid_icon_resource, Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							ShortcutEditDialogFragment.this.activity.icon = (BitmapDrawable)pm.getDefaultActivityIcon();
							Toast.makeText(getActivity(), R.string.error_invalid_icon_format, Toast.LENGTH_LONG).show();
						}
						
						
						LauncherIconCreator.createLauncherIcon(getActivity(), ShortcutEditDialogFragment.this.activity);
					}
				})
				.setNegativeButton(android.R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						 ShortcutEditDialogFragment.this.getDialog().cancel();
					}
				});

		return builder.create();
	}

}
