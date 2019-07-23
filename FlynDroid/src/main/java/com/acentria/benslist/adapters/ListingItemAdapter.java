package com.acentria.benslist.adapters;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.Listing;
import com.acentria.benslist.ListingDetailsActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.controllers.Favorites;

import java.util.ArrayList;
import java.util.HashMap;

public class ListingItemAdapter extends BaseAdapter implements OnItemClickListener {

	public ArrayList<HashMap<String, String>> listings = new ArrayList<HashMap<String, String>>();
	public boolean favoritesRemoveMode = false;//remove item from list view on favorite icon click 
	
	public ListingItemAdapter(ArrayList<HashMap<String, String>> insertElements, boolean favMode) {
		listings = insertElements;
		favoritesRemoveMode = favMode;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {
//		if ( listings.get(position).get("page_allowed").equals("1") ) {
			ViewHolder holder = (ViewHolder) itemView.getTag();
			holder.favorite.setTag("favorite_"+listings.get(position).get("id"));
			
			Intent intent = new Intent(Config.context, ListingDetailsActivity.class);
			intent.putExtra("id", listings.get(position).get("id"));
			intent.putExtra("listingHash", listings.get(position));
			intent.putExtra("removeID", favoritesRemoveMode ? position+"" : null);//pass the position to have possabilities remove the listings if user unset listigns from favorites				
			Config.context.startActivity(intent);
//		}
	}
	
	private class ViewHolder {
		public LinearLayout divider;
		public TextView dividerText;
		public LinearLayout listingItem;
		public TextView titleText;
		public TextView middleText;
		public TextView priceText;
		public TextView photoCount;
		public ImageView image;
		public ImageView favorite;
		public RelativeLayout thumbnail;
	}
	
	/**
	 * add more items to listview
	 * 
	 * @param nextListings - next listings stack
	 */
	public void add(ArrayList<HashMap<String, String>> nextListings) {
		for (int i = 0; i < nextListings.size(); i++ ) {
			listings.add(nextListings.get(i));
		}
	    notifyDataSetChanged();
	}
	
	/**
	 * add one entry
	 * 
	 * @param nextListings - next listings stack
	 */
	public void addEntry(HashMap<String, String> entry) {
		entry.put("date_diff", "");
		listings.add(entry);
	    notifyDataSetChanged();
	}
	
	public void removeEntry(HashMap<String, String> entry) {
		for (HashMap<String, String> listing : listings) {
			if ( listing.get("id").equals(entry.get("id")) ) {
				listings.remove(listing);
				notifyDataSetChanged();
				break;
			}
		}
	}
	
	public void remove(int position) {
		listings.remove(position);
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return listings.size();
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
			
			view = Config.context.getLayoutInflater().inflate(R.layout.listing, null);
			
			holder = new ViewHolder();
			
			holder.divider = (LinearLayout) view.findViewById(R.id.divider);
			holder.dividerText = (TextView) view.findViewById(R.id.divider_text);
			holder.listingItem = (LinearLayout) view.findViewById(R.id.listing_item);
			holder.titleText = (TextView) view.findViewById(R.id.title);
			holder.middleText = (TextView) view.findViewById(R.id.custom_field_1);
			holder.priceText = (TextView) view.findViewById(R.id.price);
			holder.photoCount = (TextView) view.findViewById(R.id.pic_count);
			holder.image = (ImageView) view.findViewById(R.id.thumbnail_image);
			holder.thumbnail = (RelativeLayout) view.findViewById(R.id.thumbnail);
			holder.favorite = (ImageView) view.findViewById(R.id.favorite);
			
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}
						
		final HashMap<String, String> listing = listings.get(position);
		
		/* set text values */
		holder.titleText.setText(listing.get("title"));
		holder.middleText.setText(listing.get("middle_field"));
		holder.priceText.setText(listing.get("price"));

		/* featured status handler */
		if ( listing.get("featured").equals("0") ) {
			holder.listingItem.setBackgroundResource(R.drawable.listing_border);
		}
		else {
			holder.listingItem.setBackgroundResource(R.drawable.listing_border_featured);
		}

		/* thumbnail manager */
		if ( listing.get("photo_allowed").equals("1") )
		{
			holder.thumbnail.setVisibility(View.VISIBLE);
			
			/* load image to list (AsyncTask) */
			if ( !listing.get("photos_count").isEmpty() ){
				Utils.imageLoaderMixed.displayImage(listing.get("photo"), holder.image, Utils.imageLoaderOptionsMixed);
				//holder.image.setImageResource(R.drawable.no_image);
			}
			else {
				holder.image.setImageResource(R.mipmap.no_image);
			}
			
			/* handle listing counter */
			if ( listing.get("photos_count").equals("0") ) {
				holder.photoCount.setVisibility(View.GONE);
			}
			else {
				holder.photoCount.setText(listing.get("photos_count"));
				holder.photoCount.setVisibility(View.VISIBLE);
			}
		}
		else
		{
			holder.thumbnail.setVisibility(View.GONE);
			holder.photoCount.setVisibility(View.GONE);
		}
		
		/* favorites icon handler */
		if ( Utils.getSPConfig("favoriteIDs", "").indexOf(listing.get("id")) >= 0 ) {
			holder.favorite.setBackgroundResource(R.mipmap.icon_like_active);
		}
		else {
			holder.favorite.setBackgroundResource(R.mipmap.icon_like);
		}
		
		holder.favorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	String favoriteIDs = Utils.getSPConfig("favoriteIDs", "");

            	if ( favoritesRemoveMode ) {
            		favoriteIDs = Utils.removeFromSet(favoriteIDs, listing.get("id"));
            		Listing.countFavorites("remove");
            		
            		remove(position);
            	}
            	else {
	            	if ( favoriteIDs.length() > 0 && favoriteIDs.indexOf(listing.get("id")) >= 0 ) {
	            		favoriteIDs = Utils.removeFromSet(favoriteIDs, listing.get("id"));
	            		holder.favorite.setBackgroundResource(R.mipmap.icon_like);
	            		
	            		Listing.countFavorites("remove");
	            		
	            		/* remove favorite from favorites if controller available */
	            		if ( Config.activeInstances.contains("Favorites") ) {
	            			if ( Favorites.ListingsAdapter != null ) {
	            				Favorites.ListingsAdapter.removeEntry(listing);
	            			}
	            		}
	            	}
	            	else {
	            		favoriteIDs = Utils.addToSet(favoriteIDs, listing.get("id"));
	            		holder.favorite.setBackgroundResource(R.mipmap.icon_like_active);
	            		
	            		Listing.countFavorites("add");
	            		
	            		/* add favorite to favorites if controller available */
	            		if ( Config.activeInstances.contains("Favorites") ) {
	            			if ( Favorites.ListingsAdapter != null ) {
	            				Favorites.ListingsAdapter.addEntry(listing);
	            			}
	            			else {
	            				Config.activeInstances.remove("Favorites");
	            				Favorites.removeInstance();
	            				Utils.removeContentView("Favorites");
	            			}
	            		}
	            	}
            	}

            	Utils.setSPConfig("favoriteIDs", favoriteIDs);
            }
        });
		
		/* divider layout handler */
		if ( !listing.get("date_diff").isEmpty() ){
			holder.dividerText.setText(listing.get("date_diff"));
			holder.divider.setVisibility(View.VISIBLE);
		}
		else {
			holder.divider.setVisibility(View.GONE);
		}
		
		return view;
	}
}
