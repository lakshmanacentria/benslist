package com.acentria.benslist;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.acentria.benslist.adapters.CategoryAdapter;
import com.acentria.benslist.adapters.ListingItemAdapter;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;


public class CategoryActivity extends AppCompatActivity {

	private static final String TAG = "CategoryActivity=> ";
	private static String ListingType;
	private static HashMap<String, String> categoryHash;
	
	public static int requestSteck = 1; //steck number (pagination)
	public static int requestTotal = 0; //total availalbe listings
	public static boolean liadingInProgress = false;
	
	public static ListingItemAdapter ListingsAdapter;
	
	private static String sortingField = Utils.getSPConfig("sortingFieldCategory", "");
	private static ProgressDialog progressDialog;
	
	public static SlidingMenu rightMenu;
	
	private static Menu actionBarMenu;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Lang.setDirection(this);

		Intent intent = getIntent();
		categoryHash = (HashMap<String, String>) intent.getSerializableExtra("categoryHash");
		ListingType = intent.getStringExtra("type");
		
		setTitle(categoryHash.get("name"));
		Log.e(TAG, "Title"+categoryHash.get("name"));
        setContentView(R.layout.activity_category);
        
        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

        LinearLayout containerCat = (LinearLayout) findViewById(R.id.categoryContainer);

        /* add adsense */
        Utils.setAdsense(containerCat, "category");

        final LinearLayout containerView = (LinearLayout) findViewById(R.id.container);
        
        /* clean sorting fields */
        Listing.sortingFields.clear();

        /* load sub-categories */
        if ( categoryHash.get("sub_categories").equals("1") ) {
        	/* set right menu */
        	rightMenu = new SlidingMenu(this);
            
        	if ( Config.tabletMode ) {
        		rightMenu.setSlidingEnabled(false);
        	}
        	else {
	            rightMenu.setMode(Lang.isRtl() ? SlidingMenu.LEFT : SlidingMenu.RIGHT);
	            rightMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
	            rightMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
	            rightMenu.setShadowWidth(25);
				rightMenu.setShadowDrawable(Lang.isRtl() ? R.drawable.shadow_right : R.drawable.shadow);
	            rightMenu.setBehindWidth(SwipeMenu.getBehindOfset());
	            rightMenu.setFadeDegree(0.35f);
	            rightMenu.setMenu(R.layout.menu_right);
        	}
            
        	/* build request url */
        	HashMap<String, String> scParams = new HashMap<String, String>();
        	scParams.put("type", ListingType);
        	scParams.put("parent", categoryHash.get("id"));
    		final String scUrl = Utils.buildRequestUrl("getCategories", scParams, null);
			Log.e(TAG, "ApiUrl"+scUrl);


			/* do async request */
        	AsyncHttpClient scClient = new AsyncHttpClient();
        	scClient.get(scUrl, new AsyncHttpResponseHandler() {
        	
				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						Log.e(TAG, "response=> "+response);
						/* parse response */
						XMLParser parser = new XMLParser();
						Document doc = parser.getDomElement(response, scUrl);

						if ( doc == null ) {
							Dialog.simpleWarning(Lang.get("returned_xml_failed"));
						}
						else {
							NodeList categoryNode = doc.getElementsByTagName("items");
							NodeList categoryNodes = categoryNode.item(0).getChildNodes();

							LinearLayout rightContainer = (LinearLayout) findViewById(R.id.menu_right_container);

							/* populate list */
							if ( categoryNodes.getLength() > 0 )
							{
								/* create list view of listings */
								GridView gridView = (GridView) Config.context.getLayoutInflater()
										.inflate(R.layout.categories_grid, null);

								LayoutParams params = new LayoutParams(
										LayoutParams.MATCH_PARENT,
										LayoutParams.MATCH_PARENT
								);
								gridView.setLayoutParams(params);

								CategoryAdapter categoryAdapter = new CategoryAdapter(Categories.parse(categoryNodes), ListingType);
								gridView.setAdapter(categoryAdapter);
								gridView.setOnItemClickListener(categoryAdapter);

								rightContainer.addView(gridView);
							}
							/* display no categories message */
							else
							{
								TextView message = (TextView) Config.context.getLayoutInflater()
										.inflate(R.layout.info_message, null);

								message.setText(Lang.get("android_there_are_no_categories"));
								rightContainer.addView(message);
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
        else {
        	if ( Config.tabletMode ) {
        		FrameLayout categoriesMenuCont = (FrameLayout) findViewById(R.id.category_menu_frame);
        		categoriesMenuCont.setVisibility(View.GONE);
        	}
        }
        
        /* build content */
        if ( Integer.parseInt(categoryHash.get("count")) > 0 ) {

        	/* build request url */
	    	HashMap<String, String> params = new HashMap<String, String>();
			params.put("start", ""+requestSteck);
			params.put("id", categoryHash.get("id"));
			params.put("type", ListingType);
			params.put("sort", sortingField);
			final String url = Utils.buildRequestUrl("getListingsByCategory", params, null);
			Log.e(TAG, "api getlistingByCategory=> "+url);


			/* do async request */
	    	AsyncHttpClient client = new AsyncHttpClient();
	    	client.get(url, new AsyncHttpResponseHandler() {
	    	
				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						Log.e(TAG, "response getlistingByCategory=> "+response);
						/* parse response */
						XMLParser parser = new XMLParser();
						Document doc = parser.getDomElement(response, url);

						containerView.removeAllViews();

						if ( doc == null ) {
							TextView message = (TextView) Config.context.getLayoutInflater()
								.inflate(R.layout.info_message, null);

							message.setText(Lang.get("returned_xml_failed"));
							containerView.addView(message);
						}
						else {
							NodeList listingNode = doc.getElementsByTagName("items");
							NodeList listings = listingNode.item(0).getChildNodes();

							/* populate list */
							if ( listings.getLength() > 0 )
							{
								/* prepare listings */
								ListingsAdapter = new ListingItemAdapter(Listing.prepareGridListing(listings, ListingType, false, null), false);
								requestTotal = Listing.lastRequestTotalListings;

								OnScrollListener onScrollListener = null;

								int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
								int rest_listings = requestTotal - grid_listings_number;

								/* create list view of listings */
								final ListView listView = (ListView) Config.context.getLayoutInflater()
										.inflate(R.layout.listing_list_view, null);

								LayoutParams params = new LayoutParams(
										LayoutParams.MATCH_PARENT,
										LayoutParams.MATCH_PARENT
								);
								listView.setLayoutParams(params);

								/* create footer view for lisings list view */
								if ( rest_listings > 0 )
								{
									int preloadView = R.layout.list_view_footer_button;
									String buttonPhraseKey = "android_load_next_number_listings";

									if ( Utils.getSPConfig("preload_method", null).equals("scroll") ) {
										preloadView = R.layout.list_view_footer_loading;
										buttonPhraseKey = "android_loading_next_number_listings";
									}

									final View footerView = (View) getLayoutInflater()
											.inflate(preloadView, null);

									final Button preloadButton = (Button) footerView.findViewById(R.id.preload_button);
									int set_rest_listings = rest_listings >= grid_listings_number ? grid_listings_number : rest_listings;
									String buttonPhrase = Lang.get(buttonPhraseKey)
											.replace("{number}", ""+set_rest_listings);
									preloadButton.setText(buttonPhrase);

									/* preload button listener */
									if ( Utils.getSPConfig("preload_method", null).equals("button") ) {
										preloadButton.setOnClickListener(new View.OnClickListener() {
											public void onClick(View v) {
												requestSteck += 1;
												preloadButton.setText(Lang.get("android_loading"));
												loadNextStack(preloadButton, footerView, listView);
											}
										});
									}
									/* on scroll listener */
									else {
										onScrollListener = new OnScrollListener() {
											@Override
											public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
												if ( !liadingInProgress && firstVisibleItem + visibleItemCount == totalItemCount ) {
													liadingInProgress = true;
													requestSteck += 1;
													loadNextStack(preloadButton, footerView, listView);
												}
											}

											@Override
											public void onScrollStateChanged(AbsListView view, int scrollState) { }
										};
									}

									listView.addFooterView(footerView);
								}

								listView.setAdapter(ListingsAdapter);
								listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));

								listView.setOnItemClickListener(ListingsAdapter);

								containerView.setGravity(Gravity.TOP);
								containerView.addView(listView);
							}
							/* display no listings message */
							else
							{
								/* hide sorting item icon */
								Utils.changeMenuItemVisibility(actionBarMenu, R.id.menu_sorting, false);

								/* show no listings message */
								TextView message = (TextView) Config.context.getLayoutInflater()
									.inflate(R.layout.info_message, null);

								message.setText(Lang.get("android_there_are_no_listings_in_category"));
								containerView.addView(message);
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
        else {
        	TextView message = (TextView) Config.context.getLayoutInflater()
	    		.inflate(R.layout.info_message, null);
    		
    		message.setText(Lang.get("android_there_are_no_listings_in_category"));
    		
    		containerView.removeViewAt(0);
    		containerView.addView(message);
        }
	}

	public static void loadNextStack(final Button preloadButton, final View footerView, final ListView listView) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		params.put("id", categoryHash.get("id"));
		params.put("type", ListingType);
		params.put("sort", sortingField);
		final String url = Utils.buildRequestUrl("getListingsByCategory", params, null);
		
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

					if ( doc == null ) {
						Dialog.simpleWarning(Lang.get("returned_xml_failed"));
					}
					else {
						NodeList listingNode = doc.getElementsByTagName("items");
						NodeList listings = listingNode.item(0).getChildNodes();

						/* populate list */
						if ( listings.getLength() > 0 )
						{
							ListingsAdapter.add(Listing.prepareGridListing(listings, ListingType, false, null));
							requestTotal = Listing.lastRequestTotalListings;
							liadingInProgress = false;
						}

						/* update button text */
						int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
						int rest_listings = requestTotal - (grid_listings_number * requestSteck);
						if ( rest_listings > 0 )
						{
							int set_rest_listings = rest_listings >= grid_listings_number ? grid_listings_number : rest_listings;
							String buttonPhrase = Lang.get("android_load_next_number_listings")
									.replace("{number}", "" + set_rest_listings);
							preloadButton.setText(buttonPhrase);
						}
						else
						{
							listView.removeFooterView(footerView);
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
	
	public static void sortListings() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		params.put("id", categoryHash.get("id"));
		params.put("type", ListingType);
		params.put("sort", sortingField);
		final String url = Utils.buildRequestUrl("getListingsByCategory", params, null);
		Log.d(".............", url);
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

	    			if ( doc == null ) {
	    				Dialog.simpleWarning(Lang.get("returned_xml_failed"));
	    			}
	    			else {
		    			NodeList listingNode = doc.getElementsByTagName("items");
		    			NodeList listings = listingNode.item(0).getChildNodes();

		    			if ( listings.getLength() > 0 ) {
		    				ListingsAdapter.listings = Listing.prepareGridListing(listings, ListingType, false, null);
		    				ListingsAdapter.notifyDataSetChanged();

		    				progressDialog.dismiss();
		    			}
		    			else {
		    				// error message here
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

	public void clearData() {
		requestSteck = 1;
		requestTotal = 0;
		liadingInProgress = false;
	}
	
	@Override
	public void onBackPressed() {
		if ( rightMenu != null ) {
			if ( rightMenu.isMenuShowing() ) {
				rightMenu.showContent();
			}
			else {
				super.onBackPressed();
        	}
		}
		else {
			super.onBackPressed();
		}
		clearData();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_category, menu);
        return true;
    }
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		actionBarMenu = menu;
		
		if ( categoryHash.get("sub_categories").equals("1") && !Config.tabletMode ) {
			MenuItem menuIcon = (MenuItem) menu.findItem(R.id.menu_category_settings);
			menuIcon.setVisible(true);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this);
	}
		
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
	        case R.id.menu_category_settings:
	        	if ( rightMenu != null ) {
		        	if ( rightMenu.isMenuShowing() ) {
		        		rightMenu.showContent();
		        	}
		        	else {
		        		rightMenu.showMenu();
		        	}
	        	}
	        	else {
	        		rightMenu.showMenu();
	        	}
	            return true;
	        
	        case R.id.menu_sorting:
	        	final Context context = this;
	        	OnClickListener listener = new DialogInterface.OnClickListener() {
	    			public void onClick(DialogInterface dialog, int which) {
	    				sortingField = Utils.getSPConfig("sortingFieldCategory", "");
	    				requestSteck = 1;
	    				progressDialog = ProgressDialog.show(context, null, Lang.get("android_loading"));
	    				
	    				clearData();
	    				
	    				sortListings();
	    			}
	    		};
	        	
	        	Dialog.sortingDialog(this, listener, sortingField, Listing.sortingFields, "sortingFieldCategory");
	        	
	        	return true;
	        
	        case android.R.id.home:
	        	super.onBackPressed();
	        	clearData();
				return true;
				
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
}