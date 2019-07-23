package com.acentria.benslist;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.adapters.ListingItemAdapter;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
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

import cz.msebera.android.httpclient.Header;


public class AccountDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
	public static AccountDetailsActivity instance;
	private static Intent intent;
	
	private static List<String> TAB_NAMES = new ArrayList<String>();
	public static List<String> TAB_KEYS = new ArrayList<String>();
	
	private static SupportMapFragment mapFragment;
	private static GoogleMap map;
	private static Menu pMenu;
	
	private static String accountID;
	private static HashMap<String, String> accountHash;
    public static ViewPager pager;
	
	public static ListingItemAdapter ListingsAdapter;
	
	public static int requestSteck = 1; //steck number (pagination)
	public static int requestTotal = 0; //total availalbe listings
	public static boolean liadingInProgress = false;
	
	private static HashMap<String, String> accountData;
	private static ArrayList<HashMap<String, String>> accountFields;
	private static ArrayList<HashMap<String, String>> accountListings;
	
	private static TabPageIndicator indicator;
	
	private AsyncHttpClient client;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		Lang.setDirection(this);
		/* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        setTitle(Lang.get("android_title_activity_account_details"));
        setContentView(R.layout.activity_account_details);
        
        LinearLayout content = (LinearLayout) findViewById(R.id.account_details);
        /* add ad sense */
        Utils.setAdsense(content, "account_details");

        /* get account data from instance */
        intent = getIntent();
        accountID = intent.getStringExtra("id");
        accountHash = (HashMap<String, String>) intent.getSerializableExtra("accountHash");
        
        /* clear data */
        TAB_NAMES = new ArrayList<String>();
        TAB_KEYS = new ArrayList<String>();
        accountData = new HashMap<String, String>();
        accountFields = new ArrayList<HashMap<String, String>>();
        accountListings = new ArrayList<HashMap<String, String>>();
       
        /* build tabs */
        if ( !accountID.isEmpty() )
        {
        	TAB_NAMES.add(Lang.get("android_tab_caption_details"));
        	TAB_KEYS.add("details");
        	
        	TAB_NAMES.add(Lang.get("android_tab_caption_listings"));
        	TAB_KEYS.add("listings");

        	TAB_NAMES.add(Lang.get("android_tab_caption_map"));
        	TAB_KEYS.add("map");
        	
        	/* init adapter */
    		FragmentPagerAdapter adapter = new FragmentAdapter(getSupportFragmentManager());
    		
            pager = (ViewPager)findViewById(R.id.pager);
            pager.setPageMargin(10);
            pager.setAdapter(adapter);
            pager.setOffscreenPageLimit(3);

            
            indicator = (TabPageIndicator)findViewById(R.id.indicator);
            indicator.setViewPager(pager);
            indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageSelected(final int position) {
                    updateMenu();
                }

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

                @Override
                public void onPageScrollStateChanged(int state) {}
            });
            
            if ( intent.hasExtra("focusOnListings") ) {
            	indicator.setCurrentItem(1);            	
            }
        	
        	/* build request url */
        	HashMap<String, String> params = new HashMap<String, String>();
    		params.put("id", accountID);
    		final String url = Utils.buildRequestUrl("getAccountDetails", params, null);
        	
    		/* do request */
        	client = new AsyncHttpClient();
        	client.get(url, new AsyncHttpResponseHandler() {

				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						prepareDetails(response, url);
	        	    	drawAccount(true, null, pager, null);

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

	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;
	}

	/**
	 * Fragment adapter
	 * 
	 * @author Freeman
	 */
	class FragmentAdapter extends FragmentPagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	return AccountDetailsFragment.newInstance(TAB_KEYS.get(position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	return TAB_NAMES.get(position % TAB_NAMES.size()).replace(" ", "\u00A0").toUpperCase(); // converting to non-breaking space
        }

        @Override
        public int getCount() {
        	return TAB_NAMES.size();
        }
    }
	
	/**
	 * Fragment intance for adapted above
	 * 
	 * @author Freeman
	 */
	public final static class AccountDetailsFragment extends Fragment {
	    
	    private String tabKey;

	    public static AccountDetailsFragment newInstance(String key) {
	    	AccountDetailsFragment fragment = new AccountDetailsFragment();
	    	fragment.tabKey = key.toString();
	        return fragment;
	    }

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	    }

	    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
	    	LinearLayout layout;
	    	
	    	if ( tabKey.equals("map") ) {
	    		layout = (LinearLayout) instance.getLayoutInflater()
	        			.inflate(R.layout.listing_map, null);

				/* create map */
				((SupportMapFragment) instance.getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(instance);
	    	}
	    	else {
		        layout = new LinearLayout(getActivity());
		        layout.setTag(tabKey);
		        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		        layout.setGravity(Gravity.CENTER);
		        
		        if ( tabKey.equals("details") )
		        {
		        	ProgressBar progressBar = (ProgressBar) Config.context.getLayoutInflater()
			    			.inflate(R.layout.loading, null);
			        layout.addView(progressBar);
		        }
	    	}

			if (Lang.isRtl()) {
//				layout.setRotationY(180);
			}
	        
	        return layout;
	    }

	    @Override
	    public void onSaveInstanceState(Bundle outState) {
	    	super.onSaveInstanceState(outState);
	    }
	}
	
	public static void prepareDetails(String response, String url){
    	/* parse xml response */
    	XMLParser parser = new XMLParser();
		Document doc = parser.getDomElement(response, url);
		
		if ( doc == null ) {
			Dialog.simpleWarning(Lang.get("returned_xml_failed"));
		}
		else {
			NodeList accountNode = doc.getElementsByTagName("account");
			
			Element nlE = (Element) accountNode.item(0);
			NodeList accountNodes = nlE.getChildNodes();
			
			for( int i=0; i<accountNodes.getLength(); i++ )
			{
				Element node = (Element) accountNodes.item(i);
				
				/* get title */
				if ( node.getTagName().equals("email") ) {
					accountData.put("email", node.getTextContent());	
				}
				
				/* get location */
				if ( node.getTagName().equals("location") ) {
					accountData.put("latitude", node.getAttribute("latitude"));
					accountData.put("longitude", node.getAttribute("longitude"));
					accountData.put("address", node.getTextContent());
				}
				
				/* get saller fields */
				if ( node.getTagName().equals("fields") ) {
					NodeList sellerFields = node.getChildNodes();
					
					for (int j = 0; j < sellerFields.getLength(); j++) {
						Element fieldNode = (Element) sellerFields.item(j);
						HashMap<String, String> fieldHash = new HashMap<String, String>();
						fieldHash.put("key", fieldNode.getAttribute("key"));
						fieldHash.put("name", fieldNode.getAttribute("name"));
						fieldHash.put("type", fieldNode.getAttribute("type"));
						fieldHash.put("value", fieldNode.getTextContent());
						
						accountFields.add(fieldHash);
					}
				}
				
				/* get seller listings */
				if ( node.getTagName().equals("listings") ) {
					accountListings = Listing.prepareGridListing(node.getChildNodes(), null, false, null);
					requestTotal = Listing.lastRequestTotalListings;
				}
			}
		}
    }
    
    /**
     * draw account details by tab key or on load details
     * 
     * @param onLoad - if true (A): draw two first tabs on account details load, if false (B: then load by tabKey request
     * @param tabKey - tabKey *B
     * @param pager - pager view *A
     * @param layout - tab container layout
     */
    public static void drawAccount(boolean onLoad, String tabKey, ViewPager pager, LinearLayout layout){
    	if ( onLoad ) {
    		drawDetails(pager);
    		drawListings(pager);
    		drawAccountMap(pager);
    	}
    }

    /**
     * drow account details - seller tab container
     * 
     * @param pager - related view
     */
    public static void drawDetails(ViewPager pager){
    	LinearLayout detailsContainer = (LinearLayout) pager.findViewWithTag("details");
    	
    	detailsContainer.removeAllViews();
		detailsContainer.setGravity(Gravity.TOP);
		
		/* inflate details tab */
    	LinearLayout detailsTab = (LinearLayout) instance.getLayoutInflater()
    			.inflate(R.layout.account_details, null);
    	detailsTab.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    	
    	/* set seller thumbnail */
    	if ( accountHash.get("photo") != null && !accountHash.get("photo").isEmpty() ) {
	    	ImageView sellerThumbnail = (ImageView) detailsTab.findViewById(R.id.seller_thumbnail);
	    	Utils.imageLoaderDisc.displayImage(accountHash.get("photo"), sellerThumbnail, Utils.imageLoaderOptionsDisc);
    	}
    	
    	/* set seller name */
    	TextView sellerName = (TextView) detailsTab.findViewById(R.id.name);
    	sellerName.setText(accountHash.get("full_name"));
    	
    	/* seller fields */
        if ( accountFields.size() > 0 )
        {
        	LinearLayout fieldsTable = (LinearLayout) detailsTab.findViewById(R.id.fields_table);
        	int index = 0;
        	
	        for (HashMap<String, String> entry : accountFields) {
        		/* create row view */
        		LinearLayout fieldRow = (LinearLayout) instance.getLayoutInflater()
    	    			.inflate(R.layout.seller_info_field, null);
        		
        		/* set field name */
        		TextView fieldName = (TextView) fieldRow.findViewById(R.id.field_name);
        		fieldName.setText(entry.get("name")+":");
        		
        		TextView fieldValue = (TextView) fieldRow.findViewById(R.id.field_value);
        		
        		if ( entry.get("type").equals("image") ) {
        			fieldValue.setVisibility(View.GONE);
        			ImageView fieldImage = (ImageView) fieldRow.findViewById(R.id.field_image);
        			fieldImage.setVisibility(View.VISIBLE);
        			Utils.imageLoaderDisc.displayImage(entry.get("value"), fieldImage, Utils.imageLoaderOptionsDisc);
        		}
        		else {
	        		/* set field value */
	        		fieldValue.setText(Html.fromHtml(entry.get("value")));
	        		fieldValue.setMovementMethod(LinkMovementMethod.getInstance());
        		}
        		fieldsTable.addView(fieldRow, index);
        		index++;
	        }
        }
    	
    	/* add tab view to container */
    	detailsContainer.addView(detailsTab);
    	
    	/* contact seller listener */
        ImageView iconContact = (ImageView) detailsTab.findViewById(R.id.icon_contact);
        iconContact.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
				if (Account.loggedIn) {
					Intent intent = new Intent(Config.context, MessagesActivity.class);
					intent.putExtra("id", accountID);
					intent.putExtra("data", accountHash);
					intent.putExtra("sendMail", "1");
					intent.putExtra("listing_id", "0");
					Config.context.startActivity(intent);
				}
				else {
					Intent intent = new Intent(Config.context, ContactOwnerActivity.class);
					intent.putExtra("id", accountID);
					intent.putExtra("listing_id", "");
					instance.startActivityForResult(intent, 1104);
					Config.context.startActivity(intent);
				}
            }
        });
        
        /* other listings button listener */
        Button otherListings = (Button) detailsTab.findViewById(R.id.other_listings);
        if ( accountListings.size() > 0 ) {
        	String buttonText = (String) otherListings.getText() + " ("+requestTotal+")";
        	otherListings.setText(buttonText);
        	
	        otherListings.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	indicator.setCurrentItem(1);
	            }
	        });
        }
        else {
        	otherListings.setVisibility(View.GONE);
        }
    }
    
    public static void drawListings(ViewPager pager) {
    	LinearLayout listingsContainer = (LinearLayout) pager.findViewWithTag("listings");
    	
    	if ( accountListings.size() > 0 ) {
	    	ListingsAdapter = new ListingItemAdapter(accountListings, false);
	    	
	    	OnScrollListener onScrollListener = null;
	    	
	    	int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
			int rest_listings = requestTotal - grid_listings_number;
			
			/* create list view of listings */
			final ListView listView = (ListView) instance.getLayoutInflater()
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
				
				final View footerView = (View) instance.getLayoutInflater()
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
	    	
	    	listingsContainer.setGravity(Gravity.TOP);
	    	listingsContainer.addView(listView);
    	}
    	else {
    		TextView message = (TextView) Config.context.getLayoutInflater()
	    			.inflate(R.layout.info_message, null);
    		
    		message.setText(Lang.get("android_no_account_listings"));
    		listingsContainer.addView(message);
    	}
    }
    
    /**
     * drow listing details - map tab container
     * 
     * @param pager - related view
     */
    public static void drawAccountMap(ViewPager pager){

    	if ( map == null ) {
    		return;
    	}
    	
        if ( !accountData.get("latitude").isEmpty() && !accountData.get("longitude").isEmpty() ) {
        	LatLng position = new LatLng(Double.valueOf(accountData.get("latitude")), Double.valueOf(accountData.get("longitude")));
        	MarkerOptions marker = new MarkerOptions().position(position);
        	if ( !accountData.get("address").isEmpty() )
        	{
        		marker.title(accountData.get("address"));
        	}
            map.addMarker(marker);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 
            		Listing.getAvailableZoomLevel(map, Utils.getCacheConfig("android_listing_details_map_zoom"))));
        }
        else {
        	Toast.makeText(instance, Lang.get("android_no_listing_account_found"), Toast.LENGTH_SHORT).show();
        }
    }
    
    public static void loadNextSearchResults(final Button preloadButton, final View footerView, final ListView listView) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		params.put("id", accountID);
		final String url = Utils.buildRequestUrl("getListingsByAccount", params, null);
		
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
							ListingsAdapter.add(Listing.prepareGridListing(listings, null, false, null));
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

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

    @Override
    public void onStop() {
    	client.cancelRequests(instance, true);
		EasyTracker.getInstance(this).activityStop(this);
    	super.onStop();
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case 1104:
				Toast.makeText(instance, Lang.get("android_message_sent"), Toast.LENGTH_SHORT).show();
				break;
		}
	}
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_account_details, menu);
		
		/* set phrases */
		for (int i=0; i<menu.size(); i++) {
			String title = (String) menu.getItem(i).getTitle();
			menu.getItem(i).setTitle(Lang.get(title));
		}
        pMenu = menu;
        return true;
    }
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenu();
		return super.onPrepareOptionsMenu(menu);
	}
	
    private void updateMenu() {
        if (pMenu != null) {
            MenuItem item = pMenu.findItem(R.id.map_type);
            if ( TAB_KEYS.get(pager.getCurrentItem()).equals("map") ) {
                item.setVisible(true);
            }
            else {
                item.setVisible(false);
            }
            ActivityCompat.invalidateOptionsMenu(this.instance);
        }
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        	case R.id.map_type:
        		Dialog.mapTypeDialog(instance, map);
        		return true;
        		
	        case android.R.id.home:
	        	super.onBackPressed();
				return true;
				
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
}
