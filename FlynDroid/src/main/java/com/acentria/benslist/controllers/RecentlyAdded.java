package com.acentria.benslist.controllers;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
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
import com.viewpagerindicator.TabPageIndicator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import cz.msebera.android.httpclient.Header;


public class RecentlyAdded extends AbstractController {

	private static RecentlyAdded instance;
	private static final String Title = Lang.get("android_title_activity_recently_added");

	public static int[] menuItems = {};

	private static final List<String> TAB_NAMES = new ArrayList<String>();
	private static final List<String> TAB_KEYS = new ArrayList<String>();

	public static RecentlyAdded getInstance() {
		if ( instance == null ) {
			try {
				instance = new RecentlyAdded();
			}
			catch(Exception e) {
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
		TAB_NAMES.clear();
		TAB_KEYS.clear();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public RecentlyAdded(){
		/* set content title */
		Config.context.setTitle(Title);

		/* add content view */
		Utils.addContentView(R.layout.view_recently_added);

		/* get related view */
		final LinearLayout layout = (LinearLayout) Config.context.findViewById(R.id.RecentlyAdded);

        //set ad sense
        Utils.setAdsense(layout, "recently_added");

		/* hide menu */
		Utils.showContent();

		/* populate tabs from config */
		for (Entry<String, HashMap<String, String>> entry : Config.cacheListingTypes.entrySet()) {
			TAB_NAMES.add(entry.getValue().get("name"));
			TAB_KEYS.add(entry.getKey().toString());
		}

		if ( TAB_KEYS.size() <= 0 ) {
			layout.removeAllViews();

			TextView message = (TextView) Config.context.getLayoutInflater()
	    			.inflate(R.layout.info_message, null);

    		message.setText(Lang.get("android_there_are_not_listing_types"));
			layout.addView(message);
		}
		else {
			/* init adapter */
			FragmentPagerAdapter adapter = new FragmentAdapter(Config.context.getSupportFragmentManager());

	        ViewPager pager = (ViewPager)layout.findViewById(R.id.pager);
	        pager.setPageMargin(10);
	        pager.setAdapter(adapter);
	        //pager.setOffscreenPageLimit(3);


	        TabPageIndicator indicator = (TabPageIndicator)layout.findViewById(R.id.indicator);
	        indicator.setViewPager(pager);

	        /* set current date as last check date */
	        long unixTime = System.currentTimeMillis() / 1000L;
			Utils.setSPConfig("newCountDate", unixTime+"");
			
			/* clear swipe menu counter for recently added item */
			SwipeMenu.menuData.get(1).put("count", null);
	    	SwipeMenu.adapter.notifyDataSetChanged();
	    	
	    	if ( TAB_KEYS.size() == 1 ) {
	        	indicator.setVisibility(View.GONE);	        	
	        }
		}
	}
	
	class FragmentAdapter extends FragmentPagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	return ListingsGridFragment.newInstance(TAB_KEYS.get(position % TAB_KEYS.size()));
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	return TAB_NAMES.get(position % TAB_NAMES.size()).replace(" ", "\u00A0").toUpperCase(); // converting to non-breaking space
        }

        @Override
        public int getCount() {
        	return TAB_NAMES.size();
        }
        
        @Override
        public void destroyItem(View container, int position, Object object) {
        }
    }
	
	public final static class ListingsGridFragment extends Fragment {
	    
		private String type_key = "";
	    public ListingItemAdapter ListingsAdapter;
	    private Integer requestSteck = 1;
	    private boolean liadingInProgress = false;
	    private Integer lastTotal = 0;

	    public static ListingsGridFragment newInstance(String key) {
	    	ListingsGridFragment fragment = new ListingsGridFragment();
	    	fragment.type_key = key.toString();
	        return fragment;
	    }

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	    }

	    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
	    	ProgressBar progressBar = (ProgressBar) Config.context.getLayoutInflater()
	    			.inflate(R.layout.loading, null);
	    	
	        LinearLayout layout = new LinearLayout(getActivity());
	        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	        layout.setGravity(Gravity.CENTER);
	        layout.addView(progressBar);

	        getRecentlyAdded(layout);

		
	        return layout;
	    }
	    
	    /**
		 * get recently added listings by listing type
		 * 
		 * @param layout - fragment layout to assign list view to
		 * @param type - listing type key
		 */
		public void getRecentlyAdded(final LinearLayout layout){
			
	    	/* build request url */
	    	HashMap<String, String> params = new HashMap<String, String>();
			params.put("type", type_key);
			params.put("sleep", "1");
			params.put("start", ""+requestSteck);
			final String url = Utils.buildRequestUrl("getRecentlyAdded", params, null);
			
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
							TextView message = (TextView) Config.context.getLayoutInflater()
								.inflate(R.layout.info_message, null);

							message.setText(Lang.get("returned_xml_failed"));
							layout.addView(message);
						}
						else {
							NodeList listingNode = doc.getElementsByTagName("items");

							Element nlE = (Element) listingNode.item(0);
							NodeList listings = nlE.getChildNodes();

							layout.removeViewAt(0);

							/* populate list */
							if ( listings.getLength() > 0 ) {
								OnScrollListener onScrollListener = null;

								ListingsAdapter = new ListingItemAdapter(Listing.prepareGridListing(listings, type_key, true, null), false);

								lastTotal = Listing.lastRequestTotalListings;
								int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
								int rest_listings = lastTotal - grid_listings_number;

								/* create list view of listings */
								final ListView listView = (ListView) Config.context.getLayoutInflater()
										.inflate(R.layout.listing_list_view, null);

								LayoutParams params = new LayoutParams(
										LayoutParams.MATCH_PARENT,
										LayoutParams.MATCH_PARENT
								);
								listView.setLayoutParams(params);

//								if ( Config.cacheListingTypes.get(type_key).get("page").equals("0") ) {
//									listView.setSelector(R.mipmap.blank);
//								}

								/* create footer view for lisings list view */
								if ( rest_listings > 0 )
								{
									int preloadView = R.layout.list_view_footer_button;
									String buttonPhraseKey = "android_load_next_number_listings";

									if ( Utils.getSPConfig("preload_method", null).equals("scroll") ) {
										preloadView = R.layout.list_view_footer_loading;
										buttonPhraseKey = "android_loading_next_number_listings";
									}

									final View footerView = (View) Config.context.getLayoutInflater()
											.inflate(preloadView, null);

									final Button preloadButton = (Button) footerView.findViewById(R.id.preload_button);
									int set_rest_listings = rest_listings >= grid_listings_number ? grid_listings_number : rest_listings;
									String buttonPhrase = Lang.get(buttonPhraseKey).replace("{number}", ""+set_rest_listings);
									preloadButton.setText(buttonPhrase);

									/* preload button listener */
									if ( Utils.getSPConfig("preload_method", null).equals("button") ) {
										preloadButton.setOnClickListener(new View.OnClickListener() {
											public void onClick(View v) {
												requestSteck++;

												preloadButton.setText(Lang.get("android_loading"));
												loadNextRecentlyAdded(preloadButton, footerView, listView);
											}
										});
									}
									/* on scroll listener */
									else {
										onScrollListener = new OnScrollListener() {
											@Override
											public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
												if ( !liadingInProgress && firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount < lastTotal ) {
													liadingInProgress = true;

													requestSteck++;

													loadNextRecentlyAdded(preloadButton, footerView, listView);
												}
											}

											@Override
											public void onScrollStateChanged(AbsListView view, int scrollState) { }
										};
									}

									listView.addFooterView(footerView);
								}

								listView.setAdapter(ListingsAdapter);

								/* set listeners */
								listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));
								listView.setOnItemClickListener(ListingsAdapter);

								layout.setGravity(Gravity.TOP);
								layout.addView(listView);
							}
							/* display no listings message */
							else
							{
								TextView message = (TextView) Config.context.getLayoutInflater()
										.inflate(R.layout.info_message, null);

								message.setText(Lang.get("android_there_are_no_listings"));
								layout.addView(message);
							}
						}

					} catch (UnsupportedEncodingException e1) {

					}
				}

				@Override
				public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
					layout.removeViewAt(0);

	    	    	TextView message = (TextView) Config.context.getLayoutInflater()
			    			.inflate(R.layout.info_message, null);

		    		message.setText(Lang.get("android_http_request_faild"));
					layout.addView(message);
				}
	    	});
		}
	    
		public void loadNextRecentlyAdded(final Button preloadButton, final View footerView, final ListView listView) {
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("type", type_key);
			params.put("start", ""+requestSteck);
			final String url = Utils.buildRequestUrl("getRecentlyAdded", params, null);

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
							if ( listings.getLength() > 0 ) {
								ListingsAdapter.add(Listing.prepareGridListing(listings, type_key, true, null));
								liadingInProgress = false;
							}

							/* update button text */
							int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
							int rest_listings = lastTotal - (grid_listings_number * requestSteck);
							if ( rest_listings > 0 ) {
								String buttonPhraseKey = "android_load_next_number_listings";
								if ( Utils.getSPConfig("preload_method", null).equals("scroll") ) {
									buttonPhraseKey = "android_loading_next_number_listings";
								}

								int set_rest_listings = rest_listings >= grid_listings_number ? grid_listings_number : rest_listings;
								String buttonPhrase = Lang.get(buttonPhraseKey)
										.replace("{number}", ""+set_rest_listings);
								preloadButton.setText(buttonPhrase);
							}
							else {
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

	    @Override
	    public void onSaveInstanceState(Bundle outState) {
	    	super.onSaveInstanceState(outState);
	    }
	}
}