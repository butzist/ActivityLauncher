package de.szalkowski.activitylauncher;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import org.thirdparty.LauncherIconCreator;

import java.util.Objects;

public class ShortcutEditDialogFragment extends DialogFragment {
    private MyActivityInfo activity;
    private EditText text_name;
    private EditText text_package;
    private EditText text_class;
    private EditText text_icon;
    private ImageButton image_icon;
    private CheckBox check_as_root;
    private IconLoader loader;
    private AlertDialog alertDialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ComponentName activity = requireArguments().getParcelable("activity");
        final PackageManager pm = requireActivity().getPackageManager();
        Configuration locale = SettingsUtils.createLocaleConfiguration(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("language", "System Default"));
        this.activity = MyActivityInfo.fromComponentName(pm, activity, locale);
        this.loader = new IconLoader(requireContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_edit_activity, null);

        boolean rooted = this.isRootAllowed();
        boolean asRoot = rooted && requireArguments().getBoolean("as_root", false);

        this.text_name = view.findViewById(R.id.editText_name);
        this.text_name.setText(this.activity.name);
        this.text_package = view.findViewById(R.id.editText_package);
        this.text_package.setText(this.activity.component_name.getPackageName());
        this.text_class = view.findViewById(R.id.editText_class);
        this.text_class.setText(this.activity.component_name.getClassName());
        this.text_icon = view.findViewById(R.id.editText_icon);
        this.text_icon.setText(this.activity.icon_resource_name);
        this.check_as_root = view.findViewById(R.id.checkBox_as_root);
        this.check_as_root.setChecked(asRoot);
        this.check_as_root.setEnabled(rooted);
        var label_as_root = view.findViewById(R.id.textView_as_root);
        label_as_root.setEnabled(rooted);

        this.text_icon.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Drawable draw_icon = ShortcutEditDialogFragment.this.loader.getIcon(s.toString());
                ShortcutEditDialogFragment.this.image_icon.setImageDrawable(draw_icon);
            }
        });

        this.text_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(editable.length() >= 1);
            }
        });

        this.image_icon = view.findViewById(R.id.iconButton);
        this.image_icon.setImageDrawable(this.activity.icon);
        this.image_icon.setOnClickListener(v -> {
            IconPickerDialogFragment dialog = new IconPickerDialogFragment();
            dialog.attachIconPickerListener(icon -> {
                ShortcutEditDialogFragment.this.text_icon.setText(icon);
                Drawable draw_icon = this.loader.getIcon(icon);
                ShortcutEditDialogFragment.this.image_icon.setImageDrawable(draw_icon);
            });
            dialog.show(getChildFragmentManager(), "icon picker");
        });

        builder.setTitle(this.activity.name)
                .setView(view)
                .setIcon(this.activity.icon)
                .setPositiveButton(R.string.context_action_shortcut, (dialog, which) -> {
                    ShortcutEditDialogFragment.this.activity.name = ShortcutEditDialogFragment.this.text_name.getText().toString();
                    String component_package = ShortcutEditDialogFragment.this.text_package.getText().toString();
                    String component_class = ShortcutEditDialogFragment.this.text_class.getText().toString();
                    boolean as_root = ShortcutEditDialogFragment.this.check_as_root.isChecked();
                    ShortcutEditDialogFragment.this.activity.component_name = new ComponentName(component_package, component_class);
                    ShortcutEditDialogFragment.this.activity.icon_resource_name = ShortcutEditDialogFragment.this.text_icon.getText().toString();
                    try {
                        final String icon_resource_string = ShortcutEditDialogFragment.this.activity.icon_resource_name;
                        final String pack = icon_resource_string.substring(0, icon_resource_string.indexOf(':'));
                        final String type = icon_resource_string.substring(icon_resource_string.indexOf(':') + 1, icon_resource_string.indexOf('/'));
                        final String name = icon_resource_string.substring(icon_resource_string.indexOf('/') + 1);

                        Resources resources = pm.getResourcesForApplication(pack);
                        ShortcutEditDialogFragment.this.activity.icon_resource = resources.getIdentifier(name, type, pack);
                        if (ShortcutEditDialogFragment.this.activity.icon_resource != 0) {
                            ShortcutEditDialogFragment.this.activity.icon = ResourcesCompat.getDrawable(resources, ShortcutEditDialogFragment.this.activity.icon_resource, null);
                        } else {
                            ShortcutEditDialogFragment.this.activity.icon = pm.getDefaultActivityIcon();
                            Toast.makeText(getActivity(), R.string.error_invalid_icon_resource, Toast.LENGTH_LONG).show();
                        }
                    } catch (NameNotFoundException e) {
                        ShortcutEditDialogFragment.this.activity.icon = pm.getDefaultActivityIcon();
                        Toast.makeText(getActivity(), R.string.error_invalid_icon_resource, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        ShortcutEditDialogFragment.this.activity.icon = pm.getDefaultActivityIcon();
                        Toast.makeText(getActivity(), R.string.error_invalid_icon_format, Toast.LENGTH_LONG).show();
                    }

                    if (as_root) {
                        RootLauncherIconCreator.createLauncherIcon(getActivity(), ShortcutEditDialogFragment.this.activity);
                    } else {
                        LauncherIconCreator.createLauncherIcon(getActivity(), ShortcutEditDialogFragment.this.activity);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> Objects.requireNonNull(ShortcutEditDialogFragment.this.getDialog()).cancel());

        alertDialog = builder.create();
        return alertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (ShortcutEditDialogFragment.this.text_name.getText().toString().isEmpty()) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    private boolean isRootAllowed() {
        return ((MainActivity) requireActivity()).isRootAllowed();
    }
}
