package com.acentria.benslist.adapters;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.acentria.benslist.CategoryActivity;
import com.acentria.benslist.Config;
import com.acentria.benslist.R;

import java.util.ArrayList;
import java.util.HashMap;

public class CategoryAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> categories = new ArrayList<HashMap<String, String>>();
	private String ListingType;
	
	public CategoryAdapter(ArrayList<HashMap<String, String>> passedCategories, String type) {
		categories = passedCategories;
		ListingType = type;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {
		Intent intent = new Intent(Config.context, CategoryActivity.class);
		intent.putExtra("categoryHash", categories.get(position));
		intent.putExtra("type", ListingType);
		Config.context.startActivity(intent);
	}
	
	private class ViewHolder {
		public LinearLayout categoryItem;
		public TextView nameText;
		public TextView countText;
	}
	
	/**
	 * add more items to listview
	 * 
	 * @param nextListings - next listings stack
	 */
	public void add(ArrayList<HashMap<String, String>> nextListings)
	{
		for (int i = 0; i < nextListings.size(); i++) {
			categories.add(nextListings.get(i));
		}
	    notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return categories.size();
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
		final ViewHolder holder;
		
		if ( view == null ){
			
			view = Config.context.getLayoutInflater().inflate(R.layout.category, null);
			
			holder = new ViewHolder();
			
			holder.categoryItem = (LinearLayout) view.findViewById(R.id.category_item);
			holder.nameText = (TextView) view.findViewById(R.id.name);
			holder.countText = (TextView) view.findViewById(R.id.counter);
			
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}
						
		final HashMap<String, String> category = categories.get(position);
		
		/* set text values */
		holder.nameText.setText(category.get("name"));
		holder.countText.setText("("+category.get("count")+")");
		
		if ( Integer.parseInt(category.get("count")) <= 0 ) {
			holder.nameText.setTextAppearance(Config.context, R.style.category_empty);
		}
		else {
			holder.nameText.setTextAppearance(Config.context, R.style.category);
		}
		
		return view;
	}
}