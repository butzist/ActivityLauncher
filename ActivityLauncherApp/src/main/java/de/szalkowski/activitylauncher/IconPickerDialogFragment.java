package de.szalkowski.activitylauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class IconPickerDialogFragment extends DialogFragment implements IconListAsyncProvider.Listener<IconListAdapter> {
	public interface IconPickerListener {
		public void iconPicked(String icon);		
	}
	
	private GridView grid;
	private IconPickerListener listener = null;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		IconListAsyncProvider provider = new IconListAsyncProvider(this.getActivity(), this);
		provider.execute();
	}
	
	public void attachIconPickerListener(IconPickerListener listener) {
		this.listener = listener;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.icon_picker, null);
		
		this.grid = (GridView)view;
		this.grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> view, View item, int index,
					long id) {
				if(IconPickerDialogFragment.this.listener != null) {
					IconPickerDialogFragment.this.listener.iconPicked(view.getAdapter().getItem(index).toString());
					IconPickerDialogFragment.this.getDialog().dismiss();
				}
			}
		});
				
		builder.setTitle(R.string.title_dialog_icon_picker)
		.setView(view)
		.setNegativeButton(android.R.string.cancel, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				IconPickerDialogFragment.this.getDialog().cancel();
			}
		});
		
		return builder.create();
	}

	@Override
	public void onProviderFininshed(AsyncProvider<IconListAdapter> task,
			IconListAdapter value) {
		try {
			this.grid.setAdapter(value);
		} catch (Exception e) {
			Toast.makeText(this.getActivity(), R.string.error_icons, Toast.LENGTH_SHORT).show();
		}
	}
}
