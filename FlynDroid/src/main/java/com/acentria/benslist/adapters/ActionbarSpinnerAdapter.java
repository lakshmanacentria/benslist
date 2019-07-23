package com.acentria.benslist.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.acentria.benslist.Config;

import java.util.ArrayList;
 
public class ActionbarSpinnerAdapter extends ArrayAdapter<String> {

	private ArrayList<String> items;
	
	public ActionbarSpinnerAdapter(Context context, ArrayList<String> itemsArray) {
		super(context, android.R.layout.simple_dropdown_item_1line, itemsArray);
		items = itemsArray;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return getCustomView(position, convertView, parent);
	}

	public View getCustomView(int position, View convertView, ViewGroup parent) {
		View item;
		if ( convertView == null ) {
			item = Config.context.getLayoutInflater()
				.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
		}
		else {
			item = convertView;
		}
		
		TextView label = (TextView) item.findViewById(android.R.id.text1);
		String item_color = "#e5e5e5";
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			item_color = "#515151";
		}
		label.setTextColor(Color.parseColor(item_color));
		label.setText(items.get(position));
		
		return item;
	}
}