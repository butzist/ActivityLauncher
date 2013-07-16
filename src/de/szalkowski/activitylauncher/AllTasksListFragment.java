package de.szalkowski.activitylauncher;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

public class AllTasksListFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.frament_all_list, container);

		ExpandableListView list = (ExpandableListView) view.findViewById(R.id.expandableListView1);
		list.setAdapter(new AllTasksListAdapter(this.getActivity()));
		
		return view;
	}
	
}
