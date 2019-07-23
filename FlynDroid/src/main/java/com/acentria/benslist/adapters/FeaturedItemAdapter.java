package com.acentria.benslist.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
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
import com.acentria.benslist.ListingDetailsActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.ArrayList;
import java.util.HashMap;

public class FeaturedItemAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> listings = new ArrayList<HashMap<String, String>>();
	
	public FeaturedItemAdapter(ArrayList<HashMap<String, String>> insertElements) {
		listings = insertElements;
	}

	@Override
	public void onItemClick(AdapterView<?> gridView, View itemView, int position, long longItemId) {
		Intent intent = new Intent(Config.context, ListingDetailsActivity.class);
		intent.putExtra("id", listings.get(position).get("id"));
		Config.context.startActivity(intent);
	}
	
	private class ViewHolder {
		public LinearLayout listingItem;
		public TextView titleText;
		public TextView priceText;
		public ImageView image;
	}
	
	public void add(ArrayList<HashMap<String, String>> nextListings)
	{
		for (int i = 0; i < nextListings.size(); i++ )
		{
			listings.add(nextListings.get(i));
		}
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
		ViewHolder holder;
		
		if ( view == null ) {			
			view = Config.context.getLayoutInflater().inflate(R.layout.featured, null);
			holder = new ViewHolder();

			holder.titleText = (TextView) view.findViewById(R.id.featured_title);
			holder.priceText = (TextView) view.findViewById(R.id.featured_price);
			holder.image = (ImageView) view.findViewById(R.id.thumbnail_image);
			
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}
						
		HashMap<String, String> listing = listings.get(position);
		
		/* set text values */
		holder.titleText.setText(listing.get("title"));
		holder.priceText.setText(listing.get("price"));

		/* load image to list (AsyncTask) */
		if ( !listing.get("photo").isEmpty() ){
			ImageAware imageAware = new ImageViewAware(holder.image, false);
			Utils.imageLoaderFeatured.displayImage(listing.get("photo"), imageAware, Utils.imageLoaderOptionsFeatured, new ImageLoadingListener() {
				@Override
				public void onLoadingStarted(String imageUri, View view) {}

				@Override
				public void onLoadingFailed(String imageUri, View view, FailReason failReason) {}

				@Override
				public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
					RelativeLayout parent = (RelativeLayout) view.getParent();
//					Animation fadeIn = new AlphaAnimation(0, 1);
//				    fadeIn.setDuration(500);
//				    fadeIn.setFillAfter(true);
					parent.findViewById(R.id.listing_info_container).setVisibility(View.VISIBLE);
				}

				@Override
				public void onLoadingCancelled(String imageUri, View view) {}
			 
			});
		}
		else {
			RelativeLayout parentCont = (RelativeLayout) holder.image.getParent();
			holder.image.setImageResource(R.mipmap.no_image);
			parentCont.findViewById(R.id.listing_info_container).setVisibility(View.VISIBLE);
		}
		
		return view;
	}
}
