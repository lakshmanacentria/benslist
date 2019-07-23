package com.acentria.benslist.controllers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.GPSTracker;
import com.acentria.benslist.Lang;
import com.acentria.benslist.Listing;
import com.acentria.benslist.R;
import com.acentria.benslist.SearchAroundActivity;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.adapters.FeaturedItemAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;


public class Home extends AbstractController implements OnMapReadyCallback {
	
	private static Home instance;
	private static String Title;
	
	public static ImageLoader imageLoader = ImageLoader.getInstance();
	public static DisplayImageOptions imageDisplayOptions;

	public static RelativeLayout layout;
	public static FeaturedItemAdapter FeaturedAdapter;
	public static GridView gridview;
	public static int requestSteck = 1; //steck number (pagination)
	public static int requestTotal = 0; //total availalbe listings
	public static boolean loadingInProgress = false;
	public static GoogleMap map;

	public static int[] menuItems = {R.id.menu_settings, R.id.menu_about_app, R.id.menu_search, R.id.menu_send_feedback};
	
	public static Home getInstance() {
		if ( instance == null ) {
			try {
				instance = new Home();
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
	}
	
	public Home(){
		/* set content title */
		Title = Lang.get("android_title_activity_flyn_droid");
		Config.context.setTitle(Title);

		/* set content view */
		if ( Config.tabletMode ) {
			RelativeLayout content = (RelativeLayout) Config.context.getLayoutInflater()
		    		.inflate(R.layout.view_home, null);
			Config.contentFrame.addView(content);
		}
		else {
			Config.context.setContentView(R.layout.view_home);
		}

		/* get related view */
		layout = (RelativeLayout) Config.context.getWindow().findViewById(R.id.Home);

		/* initialize the image loaders */
		Utils.initImageLoader();

		showHomeListings();
		
        // set ad sense
        Utils.setAdsense(layout, "home");

		((SupportMapFragment) Config.context.getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
	}


	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;

		requestRead();
	}

	public  void requestRead() {
		String[] PERMISSIONS = {
			Manifest.permission.ACCESS_FINE_LOCATION,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_NETWORK_STATE,
			Manifest.permission.ACCESS_WIFI_STATE,
			Manifest.permission.VIBRATE,
			Manifest.permission.READ_PHONE_STATE,
			Manifest.permission.GET_TASKS,
			Manifest.permission.CAMERA,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			NETWORK_PROVIDER,
			GPS_PROVIDER
		};

		if (!hasPermissions(Config.context, PERMISSIONS)) {

			ActivityCompat.requestPermissions(Config.context,
					PERMISSIONS,
					2222);
		} else {
			updateMapHost(true);
		}
	}
	private  boolean hasPermissions(Context context, String... permissions) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
			for (String permission : permissions) {
				if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
					return false;
				}
			}
		}
		return true;
	}
	public static void updateMapHost(final boolean myLoc) {

		if ( map == null ) {
			return;
		}

		final LinearLayout around_me = (LinearLayout) Config.context.findViewById(R.id.around_me);
		ViewTreeObserver vto = around_me.getViewTreeObserver();
		vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			public boolean onPreDraw() {
				int height;
				around_me.getViewTreeObserver().removeOnPreDrawListener(this);
				height = around_me.getMeasuredHeight();

				/* set default option */
				Double lat = 40.915582;
				Double lon = -73.832241;
				LatLng position = new LatLng(lat, lon);
				int zoom = 14;

				map.getUiSettings().setAllGesturesEnabled(false);
				if (myLoc) {
					final GPSTracker mGPS = new GPSTracker(Config.context);
					if (mGPS.canGetLocation() && mGPS.latitude != 0.0) {
						position = new LatLng(mGPS.latitude, mGPS.longitude);
						zoom = Listing.getAvailableZoomLevel(map, "14");
					}
				}

				map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
				map.setPadding(0, 0, 0, height);
				final LatLng finalPosition = position;
				map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
					@Override
					public void onMapClick(LatLng point) {
						Intent intent = new Intent(Config.context, SearchAroundActivity.class);
						intent.putExtra("lat", finalPosition.latitude);
						intent.putExtra("long", finalPosition.longitude);
						Config.context.startActivity(intent);
					}
				});

				return true;
			}
		});
	}

	public void showHomeListings() {
		if ( Config.featuredListings.size() > 0 ) {
			requestSteck = 1;
			requestTotal = 0;
			/* create adapter */
			FeaturedAdapter = new FeaturedItemAdapter(Config.featuredListings);

			/* create grid view */
			gridview = (GridView) layout.findViewById(R.id.featured);
			final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) layout.findViewById(R.id.swiperefresh);
			final LinearLayout footerGridView = (LinearLayout) layout.findViewById(R.id.home_footer_view);

			requestTotal = Config.lastRequestTotalHomeListings;

			AbsListView.OnScrollListener onScrollListener = null;

			int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
			final int rest_listings = requestTotal - grid_listings_number;

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
				String buttonPhrase = Lang.get(buttonPhraseKey)
						.replace("{number}", ""+set_rest_listings);
				preloadButton.setText(buttonPhrase);


				if ( Utils.getSPConfig("preload_method", null).equals("button") ) {
					preloadButton.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							loadNextHomeListings(preloadButton, footerView, footerGridView);
						}
					});
				}
				/* on scroll listener */
				onScrollListener = new AbsListView.OnScrollListener() {
					@Override
					public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
						if ( !loadingInProgress && firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount < requestTotal) {
							loadingInProgress = true;
							requestSteck += 1;
							if ( !Utils.getSPConfig("preload_method", null).equals("button") ) {
								loadNextHomeListings(preloadButton, footerView, footerGridView);
							}
							footerGridView.setVisibility(View.VISIBLE);
						}
					}

					@Override
					public void onScrollStateChanged(AbsListView view, int scrollState) { }
				};

				footerGridView.removeAllViews();
				footerGridView.addView(footerView);
			}
			swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					refreshLayout(swipeLayout);
				}
			});

			gridview.setAdapter(FeaturedAdapter);
			gridview.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));
			if(Config.tabletMode) {
				gridview.setNumColumns(4);
			}
			else {
				gridview.setNumColumns(2);
			}

			gridview.setOnItemClickListener(FeaturedAdapter);
		}
		else {
			TextView message = (TextView) Config.context.getLayoutInflater()
					.inflate(R.layout.info_message, null);

			message.setText(Lang.get("android_no_featured_listings"));

			LinearLayout container = (LinearLayout) layout.findViewById(R.id.featured_container);
			container.removeAllViews();
			container.setGravity(Gravity.CENTER);
			container.addView(message);
		}
	}

	public static void loadNextHomeListings(final Button preloadButton, final View footerView, final LinearLayout footerGridView) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		final String url = Utils.buildRequestUrl("getHomeListings", params, null);

		/* do async request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {

					footerGridView.setVisibility(View.GONE);

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
						for (int a = 0; a < listings.getLength(); a++) {
							Element tag = (Element) listings.item(a);
							Config.parseHomeListings(tag);
						}

						/* populate list */
						if ( listings.getLength() > 0 )
						{
							requestTotal = Config.lastRequestTotalHomeListings;
							loadingInProgress = false;
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

	/*refresh home listings*/
	public static void refreshLayout(final SwipeRefreshLayout swipeLayout) {
		requestSteck = 1;
		requestTotal = 0;
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", "0");
		final String url = Utils.buildRequestUrl("getHomeListings", params, null);

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
						Config.featuredListings.clear();
						NodeList listingNode = doc.getElementsByTagName("items");
						NodeList listings = listingNode.item(0).getChildNodes();
						for (int a = 0; a < listings.getLength(); a++) {
							Element tag = (Element) listings.item(a);
							Config.parseHomeListings(tag);
						}

						/* populate list */
						requestTotal = Config.lastRequestTotalHomeListings;
						loadingInProgress = false;
						swipeLayout.setRefreshing(false);
						if(FeaturedAdapter!=null) {
							FeaturedAdapter.notifyDataSetChanged();
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

	public static void onOrientationChange() {
		if ( Config.tabletMode ) {
			return;
		}
		Configuration config = Config.context.getResources().getConfiguration();


		/* get Home view */
		RelativeLayout view = (RelativeLayout) Config.context.getWindow().findViewById(R.id.Home);
		
		/* handle map host */
		RelativeLayout mapHost = (RelativeLayout) view.findViewById(R.id.mapHost);
		mapHost.setVisibility(Config.orientation == Configuration.ORIENTATION_PORTRAIT ? View.VISIBLE : View.GONE);

		gridview.setNumColumns(Config.orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4);

		/* handle grid view shadow */
		LinearLayout shadow = (LinearLayout) view.findViewById(R.id.featured_shadow);
		shadow.setVisibility(Config.orientation == Configuration.ORIENTATION_PORTRAIT ? View.VISIBLE : View.GONE);
	}
}