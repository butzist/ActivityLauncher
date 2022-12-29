package de.szalkowski.activitylauncher;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import org.thirdparty.IconCreator;

import java.util.Objects;

import de.szalkowski.activitylauncher.databinding.DialogEditActivityBinding;

public class ShortcutEditDialogFragment extends DialogFragment {
    private DialogEditActivityBinding binding;
    private MyActivityInfo activity;
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
        binding = DialogEditActivityBinding.inflate(inflater, null, false);

        boolean rooted = this.isRootAllowed();
        boolean asRoot = rooted && requireArguments().getBoolean("as_root", false);
        binding.editTextName.setText(this.activity.name);
        binding.editTextPackage.setText(this.activity.component_name.getPackageName());
        binding.editTextClass.setText(this.activity.component_name.getClassName());
        binding.editTextIcon.setText(this.activity.icon_resource_name);
        binding.checkBoxAsRoot.setChecked(asRoot);
        binding.checkBoxAsRoot.setEnabled(rooted);
        binding.textViewAsRoot.setEnabled(rooted);

        binding.editTextIcon.addTextChangedListener(new TextWatcher() {
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
                binding.iconButton.setImageDrawable(draw_icon);
            }
        });

        binding.editTextName.addTextChangedListener(new TextWatcher() {
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

        binding.iconButton.setImageDrawable(this.activity.icon);
        binding.iconButton.setOnClickListener(v -> {
            IconPickerDialogFragment dialog = new IconPickerDialogFragment();
            dialog.attachIconPickerListener(icon -> {
                binding.editTextIcon.setText(icon);
                Drawable draw_icon = this.loader.getIcon(icon);
                binding.iconButton.setImageDrawable(draw_icon);
            });
            dialog.show(getChildFragmentManager(), "icon picker");
        });

        builder.setTitle(this.activity.name)
                .setView(binding.getRoot())
                .setIcon(this.activity.icon)
                .setPositiveButton(R.string.context_action_shortcut, (dialog, which) -> {
                    ShortcutEditDialogFragment.this.activity.name = binding.editTextName.getText().toString();
                    String component_package = binding.editTextPackage.getText().toString();
                    String component_class = binding.editTextClass.getText().toString();
                    boolean as_root = binding.checkBoxAsRoot.isChecked();
                    ShortcutEditDialogFragment.this.activity.component_name = new ComponentName(component_package, component_class);
                    ShortcutEditDialogFragment.this.activity.icon_resource_name = binding.editTextIcon.getText().toString();
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
                        IconCreator.createLauncherIcon(getActivity(), ShortcutEditDialogFragment.this.activity);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> Objects.requireNonNull(ShortcutEditDialogFragment.this.getDialog()).cancel());

        alertDialog = builder.create();
        return alertDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (binding.editTextName.getText().toString().isEmpty()) {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    private boolean isRootAllowed() {
        return ((MainActivity) requireActivity()).isRootAllowed();
    }
}
