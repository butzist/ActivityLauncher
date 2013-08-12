package de.szalkowski.activitylauncher;

import org.thirdparty.LauncherIconCreator;

import de.szalkowski.activitylauncher.IconPickerDialogFragment.IconPickerListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

public class ShortcutEditDialogFragment extends DialogFragment {
	protected MyActivityInfo activity;
	protected EditText text_name;
	protected EditText text_package;
	protected EditText text_class;
	protected EditText text_icon;
	protected ImageButton image_icon;
	
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
		
		this.text_icon.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				PackageManager pm = getActivity().getPackageManager();
				Drawable draw_icon = IconListAdapter.getIcon(s.toString(), pm);
				ShortcutEditDialogFragment.this.image_icon.setImageDrawable(draw_icon);
			}
		});
		
		this.image_icon = (ImageButton)view.findViewById(R.id.iconButton);
		ShortcutEditDialogFragment.this.image_icon.setImageDrawable(this.activity.icon);
		this.image_icon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				IconPickerDialogFragment dialog = new IconPickerDialogFragment();
				dialog.attachIconPickerListener(new IconPickerListener() {
					@Override
					public void iconPicked(String icon) {
						ShortcutEditDialogFragment.this.text_icon.setText(icon);
						PackageManager pm = getActivity().getPackageManager();
						Drawable draw_icon = IconListAdapter.getIcon(icon, pm);
						ShortcutEditDialogFragment.this.image_icon.setImageDrawable(draw_icon);
					}
				});
				dialog.show(getFragmentManager(),"icon picker");
			}
		});
		
		builder.setTitle(this.activity.name)
				.setView(view)
				.setIcon(this.activity.icon)
				.setPositiveButton(R.string.context_action_shortcut, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ShortcutEditDialogFragment.this.activity.name = ShortcutEditDialogFragment.this.text_name.getText().toString();
						String component_package = ShortcutEditDialogFragment.this.text_package.getText().toString();
						String component_class = ShortcutEditDialogFragment.this.text_class.getText().toString();
						ComponentName component = new ComponentName(component_package,component_class);
						ShortcutEditDialogFragment.this.activity.component_name = component;
						ShortcutEditDialogFragment.this.activity.icon_resource_name = ShortcutEditDialogFragment.this.text_icon.getText().toString();
						PackageManager pm = getActivity().getPackageManager();
						try {
							final String icon_resource_string = ShortcutEditDialogFragment.this.activity.icon_resource_name; 
							final String pack = icon_resource_string.substring(0, icon_resource_string.indexOf(':'));
							final String type = icon_resource_string.substring(icon_resource_string.indexOf(':') + 1, icon_resource_string.indexOf('/'));
							final String name = icon_resource_string.substring(icon_resource_string.indexOf('/') + 1, icon_resource_string.length());
							
							Resources resources = pm.getResourcesForApplication(pack);
							ShortcutEditDialogFragment.this.activity.icon_resource = resources.getIdentifier(name, type, pack);
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
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						 ShortcutEditDialogFragment.this.getDialog().cancel();
					}
				});

		return builder.create();
	}

}
