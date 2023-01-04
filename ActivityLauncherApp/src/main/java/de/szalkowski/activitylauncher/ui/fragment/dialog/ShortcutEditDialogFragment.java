package de.szalkowski.activitylauncher.ui.fragment.dialog;

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

import de.szalkowski.activitylauncher.util.IconLoader;
import de.szalkowski.activitylauncher.R;
import de.szalkowski.activitylauncher.util.SettingsUtils;
import de.szalkowski.activitylauncher.databinding.DialogEditActivityBinding;
import de.szalkowski.activitylauncher.object.MyActivityInfo;
import de.szalkowski.activitylauncher.ui.activity.MainActivity;

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
        Configuration locale = SettingsUtils.createLocaleConfiguration(PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("language", "System Default"));
        this.activity = MyActivityInfo.fromComponentName(pm, activity, locale);
        this.loader = new IconLoader(requireContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = (LayoutInflater) requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = DialogEditActivityBinding.inflate(inflater, null, false);

        boolean rooted = this.isRootAllowed();
        boolean asRoot = rooted && requireArguments().getBoolean("as_root", false);
        binding.editTextName.setText(this.activity.getName());
        binding.editTextPackage.setText(this.activity.getComponentName().getPackageName());
        binding.editTextClass.setText(this.activity.getComponentName().getClassName());
        binding.editTextIcon.setText(this.activity.getIconResourceName());
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

        binding.iconButton.setImageDrawable(this.activity.getIcon());
        binding.iconButton.setOnClickListener(v -> {
            IconPickerDialogFragment dialog = new IconPickerDialogFragment();
            dialog.attachIconPickerListener(icon -> {
                binding.editTextIcon.setText(icon);
                Drawable draw_icon = this.loader.getIcon(icon);
                binding.iconButton.setImageDrawable(draw_icon);
            });
            dialog.show(getChildFragmentManager(), "icon picker");
        });

        builder.setTitle(this.activity.getName())
                .setView(binding.getRoot())
                .setIcon(this.activity.getIcon())
                .setPositiveButton(R.string.context_action_shortcut, (dialog, which) -> {
                    ShortcutEditDialogFragment.this.activity.setName(binding.editTextName.getText().toString());
                    String component_package = binding.editTextPackage.getText().toString();
                    String component_class = binding.editTextClass.getText().toString();
                    boolean as_root = binding.checkBoxAsRoot.isChecked();
                    ShortcutEditDialogFragment.this.activity.setComponentName(new ComponentName(component_package, component_class));
                    ShortcutEditDialogFragment.this.activity.setIconResourceName(binding.editTextIcon.getText().toString());
                    try {
                        final String icon_resource_string = ShortcutEditDialogFragment.this.activity.getIconResourceName();
                        final String pack = icon_resource_string.substring(0, icon_resource_string.indexOf(':'));
                        final String type = icon_resource_string.substring(icon_resource_string.indexOf(':') + 1, icon_resource_string.indexOf('/'));
                        final String name = icon_resource_string.substring(icon_resource_string.indexOf('/') + 1);

                        Resources resources = pm.getResourcesForApplication(pack);
                        ShortcutEditDialogFragment.this.activity.setIconResource(resources.getIdentifier(name, type, pack));
                        if (ShortcutEditDialogFragment.this.activity.getIconResource() != 0) {
                            ShortcutEditDialogFragment.this.activity.setIcon(ResourcesCompat.getDrawable(resources, ShortcutEditDialogFragment.this.activity.getIconResource(), null));
                        } else {
                            ShortcutEditDialogFragment.this.activity.setIcon(pm.getDefaultActivityIcon());
                            Toast.makeText(getActivity(), R.string.error_invalid_icon_resource, Toast.LENGTH_LONG).show();
                        }
                    } catch (NameNotFoundException e) {
                        ShortcutEditDialogFragment.this.activity.setIcon(pm.getDefaultActivityIcon());
                        Toast.makeText(getActivity(), R.string.error_invalid_icon_resource, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        ShortcutEditDialogFragment.this.activity.setIcon(pm.getDefaultActivityIcon());
                        Toast.makeText(getActivity(), R.string.error_invalid_icon_format, Toast.LENGTH_LONG).show();
                    }

                    if (as_root) {
                        IconCreator.createRootLauncherIcon(getActivity(), ShortcutEditDialogFragment.this.activity);
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
