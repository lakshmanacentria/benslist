package com.acentria.benslist;

import android.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.acentria.benslist.adapters.ListingItemAdapter;
import com.acentria.benslist.controllers.SavedSearch;
import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class SearchResultsActivity extends AppCompatActivity {

	public static int requestSteck = 1; //steck number (pagination)
	public static int requestTotal = 0; //total availalbe listings
	public static boolean liadingInProgress = false;
	
	public static SearchResultsActivity instance;
	
	public static HashMap<String,String> formData;
	public static String ssID;
	public static String find_ssID;
	public static String listingType;
	public static String load_url;

	public static ListingItemAdapter ListingsAdapter;
	
	private static String sortingField = Utils.getSPConfig("sortingFieldSearch", "");
	private static ProgressDialog progressDialog;
	private static Menu actionBarMenu;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Lang.setDirection(this);

		instance = this;
		
		setTitle(Lang.get("android_title_activity_search_results"));
        setContentView(R.layout.activity_search_results);
        
        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        LinearLayout content = (LinearLayout) findViewById(R.id.activity_search_resulst);
        final LinearLayout layout = (LinearLayout) content.findViewById(R.id.SearchResulst);

        /* add ad sense */
        Utils.setAdsense(content, "search_results");
        
        /* clean sorting fields */
        Listing.sortingFields.clear();
        
        /* get passed data */
        Intent intent = getIntent();
        formData = (HashMap<String, String>) intent.getSerializableExtra("data");
        ssID = intent.getStringExtra("id");
		find_ssID = intent.getStringExtra("find_ids")!=null ? intent.getStringExtra("find_ids") : "";
        listingType = intent.getStringExtra("type");

        /* build request url */
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		params.put("type", listingType);
		params.put("sort", sortingField);


		load_url = "searchResults";
		if(ssID!=null && !ssID.isEmpty()) {
			params.put("id", ssID);
			params.put("find_ids", find_ssID);
			load_url = "runSaveSearch";
		}
		else if(formData!=null && !formData.isEmpty()) {
			params.put("data", formData.toString());
		}
		Log.d("FD", params.toString());
		Log.d("FD", load_url.toString());
		final String url = Utils.buildRequestUrl(load_url, params, null);

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

	    			layout.removeAllViews();

	    			if ( doc == null ) {
	    				TextView message = (TextView) getLayoutInflater()
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
		    				/* prepare listings */
		    				ListingsAdapter = new ListingItemAdapter(Listing.prepareGridListing(listings, null, false, null), false);
		    				requestTotal = Listing.lastRequestTotalListings;

		    				OnScrollListener onScrollListener = null;

		    				setTitle(Lang.get("android_title_activity_search_results")+" ("+requestTotal+")");

		    				int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
		    				int rest_listings = requestTotal - grid_listings_number;

		    				/* create list view of listings */
		    				final ListView listView = (ListView) getLayoutInflater()
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
					    	            	loadNextSearchResults(preloadButton, footerView, listView);
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
					    	    				loadNextSearchResults(preloadButton, footerView, listView);
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

			    	    	layout.setGravity(Gravity.TOP);
			    	    	layout.addView(listView);
							updateMenuItems();
		    			}
		    			/* display no listings message */
		    			else {
							emptySeach(layout);
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
	
	public static void loadNextSearchResults(final Button preloadButton, final View footerView, final ListView listView) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		params.put("type", listingType);
		params.put("sort", sortingField);

		if(ssID!=null && !ssID.isEmpty()) {
			params.put("id", ssID);
			params.put("find_ids", find_ssID);
			load_url = "runSaveSearch";
		}
		else if(formData!=null && !formData.isEmpty()) {
			params.put("data", formData.toString().replace(" ", ""));
		}

		final String url = Utils.buildRequestUrl(load_url, params, null);
		
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

		    			Element nlE = (Element) listingNode.item(0);
		    			NodeList listings = nlE.getChildNodes();

		    			/* populate list */
		    			if ( listings.getLength() > 0 )
		    			{
		    				ListingsAdapter.add(Listing.prepareGridListing(listings, listingType, false, null));
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
			    	    			.replace("{number}", ""+set_rest_listings);
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

	
	public static void emptySeach(LinearLayout layout) {
		/* hide sorting item icon */
		if ( actionBarMenu != null ) {
			MenuItem menuIcon = (MenuItem) actionBarMenu.findItem(R.id.menu_sorting);
			if ( menuIcon != null )
				menuIcon.setVisible(false);

			MenuItem menuSaveIcon = (MenuItem) actionBarMenu.findItem(R.id.menu_save_search);
			if ( menuSaveIcon != null )
				menuSaveIcon.setVisible(false);
		}

		LinearLayout save_layout = (LinearLayout) Config.context.getLayoutInflater()
				.inflate(R.layout.save_search_button, null);
		save_layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		/* show message */
		TextView messages = (TextView) save_layout.findViewById(R.id.message);
		messages.setText(Lang.get("android_there_no_listings_found"));

//		LinearLayout save_search_box = (LinearLayout) save_layout.findViewById(R.id.save_search_box);
//		save_search_box.setVisibility(Account.loggedIn ? View.VISIBLE : View.GONE);

		Button save_search = (Button) save_layout.findViewById(R.id.save_the_search);
		save_search.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				saveSearchAction();
			}
		});


		layout.addView(save_layout);
	}

	public static void saveSearchAction() {

		AlertDialog.Builder builder = new AlertDialog.Builder(instance);

		builder.setTitle(Lang.get("title_activity_saved_search"));
		builder.setMessage(Lang.get(Account.loggedIn ? "android_saved_search_confirm" : "android_login_use"));
		builder.setNegativeButton(Lang.get("android_dialog_cancel"), null);
		builder.setPositiveButton(Lang.get("android_dialog_ok"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if(Account.loggedIn) {
					saveSearch();
				}
				else {
					Intent report_intent = new Intent(instance, LoginActivity.class);
					instance.startActivityForResult(report_intent, 82);
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public static void saveSearch() {

		if(Account.loggedIn) {
			final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("loading"));
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("account_id", Account.accountData.get("id"));
			params.put("password_hash", Utils.getSPConfig("accountPassword", null));
			params.put("data", formData != null ? formData.toString().replace(" ", "") : "");
			params.put("type", listingType);
			final String url = Utils.buildRequestUrl("saveSearch", params, null);

			/* do async request */
			AsyncHttpClient client = new AsyncHttpClient();
			client.get(url, new AsyncHttpResponseHandler() {

				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						progress.dismiss();
								/* parse response */
						XMLParser parser = new XMLParser();
						Document doc = parser.getDomElement(response, url);

						if ( doc == null ) {
							Dialog.simpleWarning(Lang.get("returned_xml_failed"), instance);
						}
						else {
							NodeList successNode = doc.getElementsByTagName("success");
							if ( successNode.getLength() > 0 ) {
								Element success = (Element) successNode.item(0);
								SavedSearch.savedSearchData.put(Utils.getNodeByName(success, "id"), "");

								Dialog.simpleWarning(Utils.getNodeByName(success, "item"), instance);
							}
							else {
								NodeList errorNode = doc.getElementsByTagName("error");
								Element  error = (Element) errorNode.item(0);
								Dialog.simpleWarning( error.getTextContent(), instance);
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



	public static void sortListings() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		params.put("data", formData.toString().replace(" ", ""));
		params.put("type", listingType);
		params.put("sort", sortingField);
		final String url = Utils.buildRequestUrl("searchResults", params, null);
		
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
							ListingsAdapter.listings = Listing.prepareGridListing(listings, listingType, false, null);
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

		finish();
	}

	public void updateMenuItems() {
		/* hide sorting item icon */
		if ( actionBarMenu != null ) {
			MenuItem menuIcon = (MenuItem) actionBarMenu.findItem(R.id.menu_sorting);
			if ( menuIcon != null )
				menuIcon.setVisible(true);
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		clearData();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_search_results, menu);
        return true;
    }
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		actionBarMenu = menu;

		MenuItem menuSaveIcon = (MenuItem) menu.findItem(R.id.menu_save_search);
		menuSaveIcon.setTitle(Lang.get(menuSaveIcon.getTitle().toString()));
		menuSaveIcon.setVisible(true);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if ( resultCode == RESULT_OK ) {
			saveSearch();
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        	case R.id.menu_sorting:
	        	final Context context = this;
	        	OnClickListener listener = new DialogInterface.OnClickListener() {
	    			public void onClick(DialogInterface dialog, int which) {
	    				sortingField = Utils.getSPConfig("sortingFieldSearch", "");
	    				requestSteck = 1;
	    				progressDialog = ProgressDialog.show(context, null, Lang.get("android_loading"));
	    				
	    				sortListings();
	    			}
	    		};
	        	
	        	Dialog.sortingDialog(this, listener, sortingField, Listing.sortingFields, "sortingFieldSearch");
	        	
	        	return true;

			case R.id.menu_save_search:
				saveSearchAction();

				return true;
	            
	        case android.R.id.home:
	        	super.onBackPressed();
	        	clearData();
				return true;
				
	        default:
	            return super.onOptionsItemSelected(item);
        }
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
}