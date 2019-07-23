package com.acentria.benslist.adapters;

import android.content.Intent;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.acentria.benslist.AccountDetailsActivity;
import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class AccountItemAdapter extends BaseAdapter implements OnItemClickListener {

	public ArrayList<HashMap<String, String>> accounts = new ArrayList<HashMap<String, String>>();
	
	public AccountItemAdapter(ArrayList<HashMap<String, String>> insertElements) {
		accounts = insertElements;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {
		Intent intent = new Intent(Config.context, AccountDetailsActivity.class);
		intent.putExtra("id", accounts.get(position).get("id"));
		intent.putExtra("accountHash", accounts.get(position));				
		Config.context.startActivity(intent);
	}
	
	private class ViewHolder {
		public RelativeLayout accountItem;
		public TextView nameText;
		public TextView middleText;
		public TextView dateText;
		public TextView listingCount;
		public ImageView image;
		public LinearLayout thumbnail;
	}
	
	/**
	 * add more items to listview
	 * 
	 * @param nextAccounts - next accounts stack
	 */
	public void add(ArrayList<HashMap<String, String>> nextAccounts) {
		for (int i = 0; i < nextAccounts.size(); i++ ) {
			accounts.add(nextAccounts.get(i));
		}
	    notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return accounts.size();
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
			view = Config.context.getLayoutInflater().inflate(R.layout.account, null);
			
			holder = new ViewHolder();
			
			holder.accountItem = (RelativeLayout) view.findViewById(R.id.account_item);
			holder.nameText = (TextView) view.findViewById(R.id.name);
			holder.middleText = (TextView) view.findViewById(R.id.custom_field_1);
			holder.dateText = (TextView) view.findViewById(R.id.date);
			holder.listingCount = (TextView) view.findViewById(R.id.listing_count);
			holder.image = (ImageView) view.findViewById(R.id.thumbnail_image);
			holder.thumbnail = (LinearLayout) view.findViewById(R.id.thumbnail);
			
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}
						
		final HashMap<String, String> account = accounts.get(position);

		/* set text values */
		holder.nameText.setText(account.get("full_name"));
		holder.middleText.setText(Html.fromHtml(account.get("middle_field")));
		//holder.middleText.setMovementMethod(LinkMovementMethod.getInstance()); // it will disable click if listview item
		holder.dateText.setText(account.get("date"));
		
		/* thumbnail manager */
		holder.thumbnail.setVisibility(View.VISIBLE);

		/* load image to list (AsyncTask) */
		if ( !account.get("photo").trim().isEmpty() ){
			Utils.imageLoaderMixed.displayImage(account.get("photo"), holder.image, Utils.imageLoaderOptionsMixed);
		}
		else {
			holder.image.setImageResource(R.mipmap.seller_no_photo);
		}
		
		/* handle listings counter */
		if ( account.get("listings_count").equals("0") ) {
			holder.listingCount.setVisibility(View.GONE);
		}
		else {
			holder.listingCount.setText(account.get("listings_count")+" "+Lang.get("android_listing_count"));
			holder.listingCount.setVisibility(View.VISIBLE);
		}
		return view;
	}
}
