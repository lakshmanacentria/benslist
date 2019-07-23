package com.acentria.benslist.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.acentria.benslist.R;
import com.acentria.benslist.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class SortingAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
	private Context instance;
	private String sortingVarName;
	
	public SortingAdapter(ArrayList<HashMap<String, String>> insertElements, Context context, String SPVariable) {
		items = insertElements;
		instance = context;
		sortingVarName = SPVariable;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {
		String currentTag = Utils.getSPConfig(sortingVarName, null);

		if ( currentTag != null ) {
			RadioButton current = (RadioButton) listView.findViewWithTag(currentTag);
			if ( current != null ) {
				current.setChecked(false);
			}
		}
		
		RadioButton button = (RadioButton) itemView.findViewById(R.id.radio);
		Utils.setSPConfig(sortingVarName, button.getTag().toString());
		button.setChecked(true);
	}
	
	private class ViewHolder {
		public TextView text;
		public RadioButton field;
	}
	
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		
		View view = convertView;
		ViewHolder holder;
		
		if ( view == null ) {			
			view = ((Activity) instance).getLayoutInflater()
					.inflate(R.layout.sorting_list_view_item, null);
			
			holder = new ViewHolder();
			holder.text = (TextView) view.findViewById(R.id.name);
			holder.field = (RadioButton) view.findViewById(R.id.radio);
			
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		HashMap<String, String> item = items.get(position);
		
		/* set data */
		holder.text.setText(item.get("name"));
		holder.field.setTag(item.get("key"));
		
		/* set current item */
		String currentTag = Utils.getSPConfig(sortingVarName, null);

		if ( currentTag != null && item.get("key").equals(currentTag) ) {
			holder.field.setChecked(true);
		}
		else {
			holder.field.setChecked(false);
		}
		
		return view;
	}
}