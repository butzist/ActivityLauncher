package de.szalkowski.activitylauncher;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class IconPickerFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.icon_picker, null);
		
		GridView grid = (GridView)view;
		grid.setAdapter(new IconListAdapter(getActivity()));
		grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> view, View item, int index,
					long id) {
				Toast.makeText(getActivity(), view.getAdapter().getItem(index).toString(), Toast.LENGTH_SHORT).show();				
			}
		});
		
		return view;
	}	

}
