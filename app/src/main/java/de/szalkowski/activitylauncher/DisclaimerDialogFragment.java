package de.szalkowski.activitylauncher;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class DisclaimerDialogFragment extends DialogFragment {
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.title_dialog_disclaimer)
				.setMessage(R.string.dialog_disclaimer)
				.setPositiveButton(android.R.string.yes, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences.Editor editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
						editor.putBoolean("disclaimer_accepted", true);
						editor.commit();
					}
				})
				.setNegativeButton(android.R.string.cancel, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						SharedPreferences.Editor editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
						editor.putBoolean("disclaimer_accepted", false);
						editor.commit();
						getActivity().finish();
					}
				});

		return builder.create();
	}
}
