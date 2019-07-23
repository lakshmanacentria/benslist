package com.acentria.benslist.adapters;

import android.graphics.Color;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.controllers.MyPackages;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PackagesItemAdapter extends BaseAdapter  {

	private ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
	public String selected_id = null;
	public String subscription_id = null;
	private List<String> subscriptionList = new ArrayList<String>();
	private GridView gridView;
	private boolean mode;

	public PackagesItemAdapter(ArrayList<HashMap<String, String>> insertElements, GridView list_view, boolean itemMode) {
		items = insertElements;
		gridView = list_view;
		mode = itemMode;
	}

	private class ViewHolder {
		public LinearLayout color;
		public TextView title;
		public TextView type;
		public TextView price;
		public TextView standard;
		public TextView standard_name;
		public TextView standard_name_number;
		public TextView featured;
		public TextView featured_name_number;
		public LinearLayout featured_row;
		public TextView featured_name;
		public TextView expired_plan;
		public TextView expired_plan_name;
		public Button renew;
		public RadioGroup radio_group;
		public RadioButton radio_button;
		public LinearLayout group_info;
		public LinearLayout photo;
		public LinearLayout video;
		public LinearLayout listing_live;
		public LinearLayout subscription_box;
		public View spliter;
		public TextView subscription_desc;
		public CheckBox subscription;
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


	private void onChange(Integer position) {
		if(!items.get(position).get("id").equals(selected_id)) {
			selected_id = items.get(position).get("id");

			if(subscriptionList.contains(selected_id)) {
				subscription_id = selected_id;
			}
			else {
				subscription_id = null;
			}

			for (int i=0; i<getCount(); i++) {
				View ll = gridView.getChildAt(i);
				if (ll != null) {
					RadioGroup rg = (RadioGroup) ll.findViewById(R.id.radio_group);
					rg.clearCheck();
				}
			}
		}
	}

	public void updatePackage(HashMap<String, String> plan) {
		int pos = this.getPosition(plan.get("id"));
		if (pos == -1) {
			items.add(0,plan);
		}
		else {
			items.remove(pos);
			items.add(pos,plan);
		}
		notifyDataSetChanged();

	}

	public int getPosition(String id) {
		int pos = -1;

		for (int position = 0; position < items.size(); position++) {
			HashMap<String, String> item = items.get(position);

			if (item.get("id").equals(id)) {
				pos = position;
			}
		}
		return pos;
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
	public View getView(final int position, View convertView, final ViewGroup parent) {

		View view = convertView;
		final ViewHolder holder;

		if ( view == null ) {
			view = Config.context.getLayoutInflater().inflate(R.layout.plan_packages, null);

			holder = new ViewHolder();

			holder.color = (LinearLayout) view.findViewById(R.id.color);
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.price = (TextView) view.findViewById(R.id.price);
			holder.standard = (TextView) view.findViewById(R.id.standard);
			holder.standard_name = (TextView) view.findViewById(R.id.standard_name);
			holder.standard_name_number = (TextView) view.findViewById(R.id.standard_number);
			holder.featured_row = (LinearLayout) view.findViewById(R.id.featuredRow);
			holder.featured = (TextView) view.findViewById(R.id.featured);
			holder.featured_name = (TextView) view.findViewById(R.id.featured_name);
			holder.featured_name_number = (TextView) view.findViewById(R.id.featured_number);
			holder.expired_plan = (TextView) view.findViewById(R.id.expired_plan);
			holder.expired_plan_name = (TextView) view.findViewById(R.id.expired_plan_name);
			holder.renew = (Button) view.findViewById(R.id.renew);
			holder.radio_group = (RadioGroup) view.findViewById(R.id.radio_group);
			holder.radio_button = (RadioButton) view.findViewById(R.id.plan);
			holder.group_info = (LinearLayout) view.findViewById(R.id.group_info);
			holder.photo = (LinearLayout) view.findViewById(R.id.photo);
			holder.video = (LinearLayout) view.findViewById(R.id.video);
			holder.listing_live = (LinearLayout) view.findViewById(R.id.listing_live);
			holder.spliter = (View) view.findViewById(R.id.spliter);
			holder.subscription_box = (LinearLayout) view.findViewById(R.id.subscription_box);
			holder.subscription_desc = (TextView) view.findViewById(R.id.subscription_desc);
			holder.subscription = (CheckBox) view.findViewById(R.id.subscription);

			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		final HashMap<String, String> item = items.get(position);

		holder.title.setText(item.get("name"));
		holder.price.setText(Utils.buildPrice(item.get("price")));
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

		// check unlim photos
		if (!item.containsKey("image_unlim")) {
			item.put("image_unlim", item.get("image").equals("0") ? "1" : "0");
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

		// check unlim video
		if (!item.containsKey("video_unlim")) {
			item.put("video_unlim", "");
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
		String live_till = item.get("listing_period").equals("0") ? "\u221E" : Lang.get("listing_period_days").replace("{days}", item.get("listing_period"));
		((TextView) holder.listing_live.findViewWithTag("text")).setText(live_till);

		if (mode) {
			// advanced mode
			if ( item.get("advanced_mode").equals("1") ) {
				int standard_remains = item.get("standard_remains").isEmpty() ? 0 : Integer.parseInt(item.get("standard_remains")) > 0 ? Integer.parseInt(item.get("standard_remains")) : 0;
				int featured_remains = item.get("featured_remains").isEmpty() ? 0 : Integer.parseInt(item.get("featured_remains")) > 0 ? Integer.parseInt(item.get("featured_remains")) : 0;

				holder.standard_name.setText(Lang.get("listing_appearance_standard"));
				holder.standard.setText(String.valueOf(standard_remains));
				holder.standard_name_number.setText(Html.fromHtml(Lang.get("of")+ " " +item.get("standard_listings")));

				holder.featured_name.setText(Lang.get("listing_appearance_featured"));
				holder.featured.setText(String.valueOf(featured_remains));
				holder.featured_name_number.setText(Html.fromHtml(Lang.get("of")+ " " +item.get("featured_listings")));

				holder.standard.setVisibility(View.VISIBLE);
				holder.standard_name_number.setVisibility(View.VISIBLE);
				holder.featured_row.setVisibility(View.VISIBLE);
				holder.featured_name_number.setVisibility(View.VISIBLE);
				holder.spliter.setVisibility(View.VISIBLE);
			}
			else {
				String listing_lang;
				if(Integer.parseInt(item.get("listing_number")) > 0) {
					int listings_remains = item.get("listings_remains").isEmpty() ? 0 : Integer.parseInt(item.get("listings_remains")) > 0 ? Integer.parseInt(item.get("listings_remains")) : 0;
					listing_lang = String.valueOf(listings_remains);
					holder.standard_name_number.setText(Html.fromHtml(Lang.get("of")+ " " +item.get("listing_number")));
					holder.standard_name_number.setVisibility(View.VISIBLE);
				}
				else {
					listing_lang = Lang.get("unlimited");
				}
				holder.standard_name.setText(Lang.get("stat_listings"));
				holder.standard.setText(Html.fromHtml(listing_lang));
				holder.standard.setVisibility(View.VISIBLE);
				holder.featured_row.setVisibility(View.GONE);
				holder.spliter.setVisibility(View.GONE);
			}

			String current_time;
			if (item.get("exp_date").equals("unlimited")) {
				current_time = Lang.get("unlimited");
			}
			else {
				long time = Integer.parseInt(item.get("exp_date")) * (long) 1000;
				Date date = new Date(time);
				SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd,yyyy");
				current_time = dateFormat.format(date);
			}
			holder.expired_plan_name.setText(Lang.get("status_expired")+":");
			holder.expired_plan.setText(current_time);
			if (item.get("exp_status").equals("expired")) {
				holder.expired_plan_name.setTextColor(Color.parseColor("#a52323"));
				holder.expired_plan.setTextColor(Color.parseColor("#a52323"));
			}
			holder.expired_plan.setVisibility(View.VISIBLE);
			holder.renew.setVisibility(View.VISIBLE);
			holder.radio_group.setVisibility(View.GONE);
			holder.group_info.setGravity(Gravity.LEFT);
			holder.renew.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					MyPackages.updatePackageRequest(item);
				}
			});
		}
		else {
			holder.renew.setVisibility(View.GONE);
			holder.radio_group.setVisibility(View.VISIBLE);

			if ( item.get("advanced_mode").equals("1") ) {
				String standard_lang =  item.get("standard_listings").equals("0") ? "\u221E" : item.get("standard_listings");
				String featured_lang =  item.get("featured_listings").equals("0") ? "\u221E" : item.get("featured_listings");
				holder.standard_name.setText(Lang.get("listing_appearance_standard"));
				holder.standard.setText(Html.fromHtml("<b>"+standard_lang+"</b>"));
				holder.featured_name.setText(Lang.get("listing_appearance_featured"));
				holder.featured.setText(Html.fromHtml("<b>"+featured_lang+"</b>"));
				holder.spliter.setVisibility(View.VISIBLE);
			}
			else {
				String standard_lang =  item.get("listing_number").equals("0") ? "\u221E" : item.get("listing_number");
				holder.standard_name.setText(Lang.get("stat_listings"));
				holder.standard.setText(Html.fromHtml("<b>"+standard_lang+"</b>"));
				holder.featured_row.setVisibility(View.GONE);
				holder.spliter.setVisibility(View.GONE);
			}
			holder.expired_plan_name.setText(Lang.get("plan_live_for")+":");
			String live_time = item.get("plan_period").equals("0") ? Lang.get("unlimited") : Lang.get("listing_period_days").replace("{days}", item.get("plan_period"));
			holder.expired_plan.setText(live_time);

			if (item.containsKey("subscription") && !item.get("price").equals("0") && item.get("plan_used")==null) {

				if (item.get("subscription").equals("active") && Utils.getCacheConfig("android_inapp_module").equals("1") &&  !Utils.getConfig("customer_domain").isEmpty()) {
					holder.subscription_box.setVisibility(View.VISIBLE);
					holder.subscription_desc.setText(Lang.get("android_subscription"));
					holder.subscription.setText(Lang.get("android_period") + ": " + item.get("period"));

					holder.subscription.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							if (holder.subscription.isChecked()) {
								subscriptionList.add(item.get("id"));
								if(items.get(position).get("id").equals(selected_id)) {
									subscription_id = selected_id;
								}
							} else {
								subscriptionList.remove(item.get("id"));
								if(items.get(position).get("id").equals(selected_id)) {
									subscription_id = null;
								}
							}

						}
					});
				}
			}

			if (item.get("id").equals(selected_id)) {
				holder.radio_button.setChecked(true);
			}
			else {
				holder.radio_button.setChecked(false);
			}
			if (item.get("plan_used")!=null) {
				holder.radio_button.setEnabled(false);
				holder.radio_button.setVisibility(View.GONE);
			}
			else {
				holder.radio_button.setEnabled(true);
				holder.radio_button.setVisibility(View.VISIBLE);

				holder.radio_button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onChange(position);
						holder.radio_button.setChecked(true);
					}
				});

				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						onChange(position);
						holder.radio_button.setChecked(true);
					}
				});
			}
		}

		return view;
	}
}