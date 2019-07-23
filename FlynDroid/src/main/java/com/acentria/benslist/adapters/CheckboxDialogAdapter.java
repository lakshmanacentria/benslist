package com.acentria.benslist.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CheckboxDialogAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
	private List<String> current = new ArrayList<String>();
	
	public CheckboxDialogAdapter(ArrayList<HashMap<String, String>> insertElements, List<String> currentRef) {
		items = insertElements;
		current = currentRef;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {
		CheckBox field = (CheckBox) itemView.findViewById(R.id.checkbox);
		if ( field.isChecked() ) {
			field.setChecked(false);
            current.remove(getItemKey(position));
		}
		else {
			field.setChecked(true);
            current.add(getItemKey(position));
		}
	}
	
	private class ViewHolder {
		public TextView text;
		public CheckBox field;
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

    public String getItemKey(int position){
        HashMap<String, String> item = items.get(position);
        String key = item.get("key");
        return key;
    }

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		
		View view = convertView;
		ViewHolder holder;
		
		if ( view == null ) {			
			view = Config.context.getLayoutInflater()
					.inflate(R.layout.checkbox_listview_item, null);
			
			holder = new ViewHolder();
			holder.text = (TextView) view.findViewById(R.id.name);
			holder.field = (CheckBox) view.findViewById(R.id.checkbox);
			
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
		if ( current.indexOf(item.get("key")) >= 0 ) {
			holder.field.setChecked(true);
		}
		else {
			holder.field.setChecked(false);
		}
		
		return view;
	}
}