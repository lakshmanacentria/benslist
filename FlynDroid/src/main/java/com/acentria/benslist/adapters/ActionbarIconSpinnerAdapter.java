package com.acentria.benslist.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;

import java.util.ArrayList;
import java.util.HashMap;
 
public class ActionbarIconSpinnerAdapter extends BaseAdapter implements OnItemSelectedListener {

	private ArrayList<HashMap<String,String>> items;
	private LinearLayout layout;
	private Spinner spinner;
	
	public ActionbarIconSpinnerAdapter(Context context, LinearLayout layoutRef, ArrayList<HashMap<String,String>> itemsArray, Spinner refSpinner) {
		items = itemsArray;
		layout = layoutRef;
		spinner = refSpinner;
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
				.inflate(R.layout.item_list_icon, parent, false);
		}
		else {
			item = convertView;
		}
		
		ImageView icon = (ImageView) item.findViewById(R.id.icon);
		icon.setImageResource(Utils.getFlagResources(items.get(position).get("key")));
		
		return item;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public void selectDefault() {
		int index = 0;
		for ( HashMap<String, String> m_lang : items ) {
			if ( m_lang.get("key").equals(Lang.getSystemLang()) ) {
				spinner.setSelection(index);
			}
			index++;
		}
	}
	
	@Override
	public void onItemSelected(AdapterView<?> spinnerView, View itemView, int position, long arg3) {
		for (int i = 0; i < layout.getChildCount(); i++) {
			String tag = (String) layout.getChildAt(i).getTag();
			if ( tag != null && tag.equals("multilingual_"+items.get(position).get("key")) ) {
				layout.getChildAt(i).setVisibility(View.VISIBLE);
			}
			else if ( tag != null && tag.contains("multilingual_") ) {
				layout.getChildAt(i).setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {}
}