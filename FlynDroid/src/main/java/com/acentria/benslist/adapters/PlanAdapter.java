package com.acentria.benslist.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class PlanAdapter extends BaseAdapter implements Checkable {

	private ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
	private String selected_id = null;
	private String selected_type = "standard";
	private ListView list_view;
	
	public PlanAdapter(ListView listView, ArrayList<HashMap<String, String>> insertElements, String plan_id, Boolean is_fetured) {
		this.list_view = listView;
		this.items = insertElements;
		
		if ( plan_id != null && !plan_id.isEmpty() ) {
			selected_id = plan_id;
		}
		
		if ( is_fetured ) {
			selected_type = "featured";
		}
	}
	
	private class ViewHolder {
		public LinearLayout color;
		public TextView title;
		public TextView type;
		public TextView price;
		public TextView description;
		public TextView notice_text;
		public LinearLayout photo;
		public LinearLayout video;
		public LinearLayout listing_live;
		public LinearLayout radio_group;
		public RadioButton radio_standard;
		public RadioButton radio_featured;
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

	@Override
	public boolean isEnabled(int position) {
		return false;
	}
	
	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		
		View view = convertView;
		final ViewHolder holder;
		
		if ( view == null ) {
			view = Config.context.getLayoutInflater().inflate(R.layout.plan, null);
			
			holder = new ViewHolder();
			
			holder.color = (LinearLayout) view.findViewById(R.id.color);
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.type = (TextView) view.findViewById(R.id.type);
			holder.price = (TextView) view.findViewById(R.id.price);
			holder.description = (TextView) view.findViewById(R.id.description);
			holder.notice_text = (TextView) view.findViewById(R.id.notice_text);
			holder.photo = (LinearLayout) view.findViewById(R.id.photo);
			holder.video = (LinearLayout) view.findViewById(R.id.video);
			holder.listing_live = (LinearLayout) view.findViewById(R.id.listing_live);
			holder.radio_group = (LinearLayout) view.findViewById(R.id.radio_group);
			holder.radio_standard = (RadioButton) holder.radio_group.findViewWithTag("standard");
			holder.radio_featured = (RadioButton) holder.radio_group.findViewWithTag("featured");
			
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		final HashMap<String, String> item = items.get(position);
		
		holder.title.setText(item.get("name"));
		holder.type.setText(Lang.get("plan_"+item.get("type")));
		holder.price.setText(Utils.buildPrice(item.get("price")));
		
		// description
//		if ( item.containsKey("des") && !item.get("des").isEmpty() ) {
//			holder.description.setText(item.get("des"));
//			holder.description.setVisibility(View.VISIBLE);
//		}
//		else {
//			holder.description.setVisibility(View.GONE);
//		}
		
		// color
		if ( item.containsKey("color") && !item.get("color").isEmpty() ) {
			try {
				holder.color.setBackgroundColor(Color.parseColor("#"+item.get("color")));
			} catch (Exception e) {
				e.printStackTrace();
				holder.color.setBackgroundColor(Config.context.getResources().getColor(R.color.plan_default_color));
			}
		}
		else {
			holder.color.setBackgroundColor(Config.context.getResources().getColor(R.color.plan_default_color));
		}
		
		// photos
		if ( item.get("image").equals("0") && item.get("image_unlim").equals("0") ) {
			holder.photo.setVisibility(View.GONE);
		}
		else {
			String photos = item.get("image_unlim").equals("0") ? item.get("image") : "\u221E";
			
			holder.photo.setVisibility(View.VISIBLE);
			((TextView) holder.photo.findViewWithTag("text")).setText(photos);
		}
		
		// video
		if ( item.get("video").equals("0") && item.get("video_unlim").equals("0") ) {
			holder.video.setVisibility(View.GONE);
		}
		else {
			String video = item.get("video_unlim").equals("0") ? item.get("video") : "\u221E";
			
			holder.video.setVisibility(View.VISIBLE);
			((TextView) holder.video.findViewWithTag("text")).setText(video);
		}
		
		// live till
		String live_till = item.get("listing_period").equals("0") ? "\u221E" : item.get("listing_period");
		((TextView) holder.listing_live.findViewWithTag("text")).setText(live_till);
		
		// radio
		//holder.radio_group.clearCheck();
		holder.radio_group.setVisibility(View.VISIBLE);
		holder.notice_text.setVisibility(View.GONE);
		
		int standard_visible = View.VISIBLE;
		String standard_tail = "";
		Boolean standard_enabled = true;
		Boolean standard_checked = false;
		
		int featured_visible = View.VISIBLE;
		String featured_tail = "";
		Boolean featured_enabled = true;
		Boolean featured_checked = false;
		
		final boolean is_enabled = Integer.parseInt(item.get("limit")) > 0 && item.get("using").equals("0") ? false : true;
		
		// advanced mode
		if ( item.get("advanced_mode").equals("1") ) {
			/*** STANDARD ***/
			// phrase
			if ( !item.get("standard_listings").equals("0") ) {
				standard_tail = item.get("standard_listings");
				if ( !item.get("listings_remains").isEmpty() ) {
					standard_tail = item.get("standard_remains").isEmpty() ? Lang.get("used_up") : item.get("standard_remains");
				}
			}
			standard_tail = standard_tail.isEmpty() ? "" : " ("+ standard_tail +")";
			
			// status
			standard_enabled = !item.get("package_id").isEmpty() && item.get("standard_remains").isEmpty() && !item.get("standard_listings").equals("0") ? false : true;
			
			// checked
			standard_checked = item.get("id").equals(selected_id) && selected_type.equals("standard") ? true : false;
			
			/*** FEATURED ***/
			// phrase
			if ( !item.get("featured_listings").equals("0") ) {
				featured_tail = item.get("featured_listings");
				if ( !item.get("listings_remains").isEmpty() ) {
					featured_tail = item.get("featured_remains").isEmpty() ? Lang.get("used_up") : item.get("featured_remains");
				}
			}
			featured_tail = featured_tail.isEmpty() ? "" : " ("+ featured_tail +")";
			
			// status
			featured_enabled = !item.get("package_id").isEmpty() && item.get("featured_remains").isEmpty() && !item.get("featured_listings").equals("0") ? false : true;
			
			// checked
			featured_checked = item.get("id").equals(selected_id) && selected_type.equals("featured") ? true : false;
		}
		// default mode
		else {
			// standard
			if ( item.get("featured").equals("0") ) {
				featured_visible = View.GONE;
				standard_checked = item.get("id").equals(selected_id) && is_enabled ? true : false;
			}
			// featured
			else {
				standard_visible = View.GONE;
				featured_checked = item.get("id").equals(selected_id) && is_enabled ? true : false;
			}
			
			if ( !is_enabled ) {
				holder.radio_group.setVisibility(View.GONE);
				holder.notice_text.setVisibility(View.VISIBLE);
			}
			
			standard_enabled = featured_enabled = is_enabled;
		}
		
		holder.radio_standard.setVisibility(standard_visible);
		holder.radio_standard.setText(Lang.get("standard_listing")+standard_tail);
		holder.radio_standard.setEnabled(standard_enabled);
		holder.radio_standard.setChecked(standard_checked);
		
		holder.radio_featured.setVisibility(featured_visible);
		holder.radio_featured.setText(Lang.get("featured_listing")+featured_tail);
		holder.radio_featured.setEnabled(featured_enabled);
		holder.radio_featured.setChecked(featured_checked);
		
		// radio click listener
//		holder.radio_group.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//			
//			@Override
//			public void onCheckedChanged(RadioGroup group, int checkedId) {
//				onChange(position, group, checkedId);
//			}
//		});
		
		return view;
	}
	
	private void onChange(Integer position, RadioGroup group, int checkedId) {
		if ( group.getCheckedRadioButtonId() < 0 )
			return;
		
		// reset all radio buttons in list view
		for (int i=0; i<getCount(); i++) {
			LinearLayout ll = (LinearLayout) ((ListView) list_view).getChildAt(i);
			if  ( ll != null && i != position ) {
				RadioGroup rg = (RadioGroup) ll.findViewById(R.id.radio_group);
				rg.clearCheck();
			}
		}

		// select radio
		RadioButton radio = (RadioButton) group.findViewById(checkedId); 
//		radio.setChecked(true);

		selected_type = (String) radio.getTag();
		selected_id = items.get(position).get("id");
		
		Log.d("FD", selected_id);
		Log.d("FD", selected_type);
	}
	
	public String getId() {
		return this.selected_id;
	}
	
	public HashMap<String, String> getPlan() {
		HashMap<String, String> plan = null;
		
		for (int i=0; i<getCount(); i++) {
			if ( items.get(i).get("id").equals(selected_id) ) {
				plan = items.get(i);
				break;
			}
		}
		
		return plan;
	}
	
	public Integer getActivePosition() {
		int position = 0;
		for (int i=0; i<getCount(); i++) {
			if ( items.get(i).get("id").equals(selected_id) ) {
				position = i;
				break;
			}
		}
		
		return position;
	}

	@Override
	public void setChecked(boolean checked) {
		// TODO Auto-generated method stub
		Log.d("FD", "chcked");
	}

	@Override
	public boolean isChecked() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void toggle() {
		// TODO Auto-generated method stub
		
	}
}
