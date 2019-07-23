package com.acentria.benslist.controllers;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.Listing;
import com.acentria.benslist.R;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.adapters.ListingItemAdapter;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class Favorites extends AbstractController {

	private static Favorites instance;
	private static final String Title = Lang.get("android_title_activity_favorites");
	
	private static int requestSteck = 1; //steck number (pagination)
	private static int requestTotal = 0; //total availalbe listings
	private static LinearLayout layout;
	public static ListingItemAdapter ListingsAdapter;
	
	public static int[] menuItems = {R.id.menu_settings};
	
	public static Favorites getInstance() {
		if ( instance == null ) {
			try {
				instance = new Favorites();
			}
			catch (Exception e) {
				Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
			}
			Config.activeInstances.add(instance.getClass().getSimpleName());
		}
		else {
			Utils.restroreInstanceView(instance.getClass().getSimpleName(), Title);
		}
		
		handleMenuItems(menuItems);
		
		return instance;
	}
	
	public static void removeInstance(){
		instance = null;
		requestSteck = 1;
		requestTotal = 0;
	}
	
	public Favorites(){
		
		/* set content title */
		Config.context.setTitle(Title);
		
		/* add content view */
		Utils.addContentView(R.layout.view_favorites);
		
		/* get related view */
		LinearLayout content = (LinearLayout) Config.context.getWindow().findViewById(R.id.Favorites);
        layout = (LinearLayout) content.findViewById(R.id.FavoritesContent);

        /* add ad sense */
        Utils.setAdsense(content, "favorites");
		
		/* hide menu */
		Utils.showContent();
		
		String favoriteIDs = Utils.getSPConfig("favoriteIDs", "");
		
		ListingsAdapter = new ListingItemAdapter(new ArrayList<HashMap<String, String>>(), true);
		int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
		int rest_listings = Favorites.requestTotal - grid_listings_number;
		
		/* create list view of listings */
		final ListView listView = (ListView) Config.context.getLayoutInflater()
    			.inflate(R.layout.listing_list_view, null);

    	/* create footer view for lisings list view */
		if ( rest_listings > 0 ) {
			View footerView = (View) Config.context.getLayoutInflater()
	    			.inflate(R.layout.list_view_footer_button, null);
			
	    	final Button preloadButton = (Button) footerView.findViewById(R.id.preload_button);
	    	int set_rest_listings = rest_listings >= grid_listings_number ? grid_listings_number : rest_listings;
	    	String buttonPhrase = Lang.get("android_load_next_number_listings")
	    			.replace("{number}", ""+set_rest_listings); 
	    	preloadButton.setText(buttonPhrase);
	    	
	    	/* preload button listener */
	    	preloadButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
//	            	RecentlyAdded.requestSteck += 1;
//	            	preloadButton.setText(Lang.get("android_loading"));
//	            	loadNextRecentlyAdded(type, ListingsAdapter, preloadButton);
	            }
	        });
	    	
	    	listView.addFooterView(footerView);
		}
    	
    	listView.setAdapter(ListingsAdapter);
    	listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true));
    	listView.setOnItemClickListener(ListingsAdapter);

    	/* set empty view */
    	if ( !favoriteIDs.isEmpty() ) {
    		ProgressBar progressBar = (ProgressBar) Config.context.getLayoutInflater()
    				.inflate(R.layout.loading, null);
    		progressBar.setTag("progress_bar");
    		layout.addView(progressBar);
    		listView.setEmptyView(progressBar);
    	}
    	else {
	    	setEmpty(listView, false);
    	}
    	
    	//layout.addView(listView);
    	layout.addView(listView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		if ( !favoriteIDs.isEmpty() ) {
			/* build request url */
	    	HashMap<String, String> params = new HashMap<String, String>();
			params.put("sleep", "1");
			params.put("start", ""+requestSteck);
			params.put("ids", favoriteIDs);
			final String url = Utils.buildRequestUrl("getFavorites", params, null);
			
			/* do async request */
	    	AsyncHttpClient client = new AsyncHttpClient();
	    	client.get(url, new AsyncHttpResponseHandler() {
	    	
				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						/* parse response */
		    	    	XMLParser parser = new XMLParser();
		    			Document doc = parser.getDomElement(response, url);

		    			setEmpty(listView, true);

		    			if ( doc == null ) {
		    				TextView message = (TextView) Config.context.getLayoutInflater()
		    		    		.inflate(R.layout.info_message, null);

		    	    		message.setText(Lang.get("returned_xml_failed"));
		    				layout.addView(message);
		    			}
		    			else {
			    			NodeList listingNode = doc.getElementsByTagName("items");

			    			Element nlE = (Element) listingNode.item(0);
			    			NodeList listings = nlE.getChildNodes();

			    			/* populate list */
			    			if ( listings.getLength() > 0 ) {
			    				ListingsAdapter.add(Listing.prepareGridListing(listings, null, false, null));
								recountFavorites(Listing.lastRequestTotalListings);
			    			}
		    			}

					} catch (UnsupportedEncodingException e1) {

					}
				}

				@Override
				public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
					// called when response HTTP status is "4XX" (eg. 401, 403, 404)
				}
	    	});
		}
	}

	private void recountFavorites(int count) {
		SwipeMenu.menuData.get(SwipeMenu.favoriteIndex).put("count", count+"");
		SwipeMenu.adapter.notifyDataSetChanged();
	}
	
	private void setEmpty(ListView view, Boolean removeProgress) {
		if ( removeProgress ) {
			layout.removeView(layout.findViewWithTag("progress_bar"));
		}
		
		TextView message = (TextView) Config.context.getLayoutInflater()
    			.inflate(R.layout.info_message, null);
    	message.setText(Lang.get("android_no_favorites"));
    	message.setGravity(Gravity.CENTER);
    	layout.addView(message, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    	view.setEmptyView(message);
	}
}