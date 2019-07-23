package com.acentria.benslist;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.adapters.ListingItemAdapter;
import com.acentria.benslist.adapters.MapItemAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import cz.msebera.android.httpclient.Header;

public class SearchAroundActivity extends AppCompatActivity
	implements
	OnMyLocationButtonClickListener,
	OnMapReadyCallback,
	ActivityCompat.OnRequestPermissionsResultCallback {

	private static GoogleMap map;
	private static Context context;
	private static SearchAroundActivity instance;

	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
	/**
	 * Flag indicating whether a requested permission has been denied after returning in
	 * {@link #onRequestPermissionsResult(int, String[], int[])}.
	 */
	private boolean mPermissionDenied = false;

	private static LinearLayout listCont;
	private static LinearLayout mapCont;
	private static Button button;

	private static CountDownTimer timer = null;

	private boolean mapMode = true;
	private static HashMap<Marker, HashMap<String, String>> markersContent = new HashMap<Marker, HashMap<String, String>>();

	private static ArrayList<HashMap<String, String>> listings = new ArrayList<HashMap<String, String>>();
	private static ClusterManager<MapItemAdapter> mClusterManager;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Lang.setDirection(this);

		instance = this;
		context = this;

		setTitle(Lang.get("android_title_activity_search_around"));
        setContentView(R.layout.activity_search_around);

        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        listCont = (LinearLayout) findViewById(R.id.listContainer);
        mapCont = (LinearLayout) findViewById(R.id.mapContainer);

        /* get map */
		((SupportMapFragment) instance.getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(instance);
	}

	private static void showList() {
		listCont.removeAllViews();

		if ( listings != null && listings.size() > 0 ) {
			/* create list view of listings */
			final ListView listView = (ListView) instance.getLayoutInflater()
					.inflate(R.layout.listing_list_view, null);

			LayoutParams params = new LayoutParams(
			        LayoutParams.MATCH_PARENT,
			        LayoutParams.MATCH_PARENT
			);
			listView.setLayoutParams(params);

			ListingItemAdapter ListingsAdapter = new ListingItemAdapter(listings, false);

			listView.setAdapter(ListingsAdapter);
	    	listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true));

	    	listView.setOnItemClickListener(ListingsAdapter);

	    	listCont.setGravity(Gravity.TOP);
	    	listCont.addView(listView);
		}
		else {
			TextView message = (TextView) instance.getLayoutInflater()
		    		.inflate(R.layout.info_message, null);

    		message.setText(Lang.get("android_no_listings_found"));
    		listCont.setGravity(Gravity.CENTER);
    		listCont.addView(message);
		}
	}

	private static void showInfo(MapItemAdapter item) {
		View view = instance.getLayoutInflater().inflate(R.layout.map_popup, null);
		final HashMap<String, String> listing = item.getItem();


		/* set text data */
		TextView title = (TextView) view.findViewById(R.id.title);
		title.setText(listing.get("title"));

		TextView price = (TextView) view.findViewById(R.id.price);
		price.setText(listing.get("price"));

		TextView distance = (TextView) view.findViewById(R.id.distance);
		distance.setText(setDistance(listing.get("android_distance")));

		/* set image */
		ImageView thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
		if ( !listing.get("photo").isEmpty() ) {
			Utils.imageLoaderMixed.displayImage(listing.get("photo"), thumbnail, Utils.imageLoaderOptionsMixed);
		}
		else {
			thumbnail.setVisibility(View.GONE);
		}

		/* create dialog */
		AlertDialog.Builder dialogContent = new AlertDialog.Builder(instance);
		dialogContent.setView(view);
		dialogContent.setNegativeButton(Lang.get("android_dialog_cancel"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
		dialogContent.setPositiveButton(Lang.get("android_dialog_details"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(instance, ListingDetailsActivity.class);
				intent.putExtra("id", listing.get("id"));
				instance.startActivity(intent);
			}
		});
		AlertDialog dialogView = dialogContent.create();
		dialogView.show();
	}

	private static void setLocationCenter() {
		final GPSTracker mGPS = new GPSTracker(context);
		if ( mGPS.canGetLocation() ) {
			LatLng position = new LatLng(mGPS.latitude, mGPS.longitude);
			Log.d("FD", position.toString());
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, Listing.getAvailableZoomLevel(map, "14")));
		}
		else {
			Toast.makeText(instance, "Unable to derect your location", Toast.LENGTH_SHORT).show();
		}
	}

	private static void request(CameraPosition camera) {
		listings = new ArrayList<HashMap<String, String>>();

		/* set loading */
		final TextView counter = (TextView) instance.findViewById(R.id.counter);
		counter.setText(Lang.get("android_loading"));

		/* get corner points location */
		LatLngBounds bounds = (LatLngBounds) map.getProjection().getVisibleRegion().latLngBounds;
		LatLng northEast = bounds.northeast;
		LatLng southWest = bounds.southwest;

		/* get current user location */
		double centerLat = camera.target.latitude;
		double centerLng = camera.target.longitude;

		try {
			centerLat = map.getMyLocation().getLatitude();
			centerLng = map.getMyLocation().getLongitude();
		}
		catch (Exception exception) {}

		final String type = Utils.getSPConfig("mapListingType", "");
//		final String type = Utils.getSPConfig("mapListingType", Utils.getCacheConfig("mainListingType"));

		/* build request url */
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", type);
		params.put("centerLat", centerLat+"");
		params.put("centerLng", centerLng+"");
		params.put("northEastLat", northEast.latitude+"");
		params.put("northEastLng", northEast.longitude+"");
		params.put("southWestLat", southWest.latitude+"");
		params.put("southWestLng", southWest.longitude+"");
		final String url = Utils.buildRequestUrl("getListingsByLatLng", params, null);

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
						NodeList listingNodes = nlE.getChildNodes();

						/* populate list */
						if ( listingNodes.getLength() > 0 ) {
							String[] fields = {"loc_latitude", "loc_longitude", "android_distance"};
							listings = Listing.prepareGridListing(listingNodes, type, false, fields);
							setMarkers();

							if (Config.tabletMode) {
								showList();
							}

							counter.setText(Lang.get("android_listings_found").replace("{number}", Listing.lastRequestTotalListings+""));
						}
						else {
							map.clear();
							counter.setText(Lang.get("android_no_listings_found"));
							if (Config.tabletMode) {
								showList();
								listings.clear();
							}
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

	private static void setMarkers() {
		map.clear();
		mClusterManager.clearItems();

		for (HashMap<String, String> entry : listings) {

			double lat = Double.valueOf(entry.get("loc_latitude"));
			double lng = Double.valueOf(entry.get("loc_longitude"));
			MapItemAdapter offsetItem = new MapItemAdapter(lat, lng, entry);

			mClusterManager.addItem(offsetItem);
		}
		mClusterManager.cluster();
	}

	private static void listingTypeDialog(final Context context){
		LayoutParams params = new LayoutParams(
	        LayoutParams.MATCH_PARENT,
	        LayoutParams.MATCH_PARENT
		);

		final RadioGroup group = new RadioGroup(context);
		group.setPadding(Utils.dp2px(5), Utils.dp2px(5), Utils.dp2px(5), Utils.dp2px(5));

		int i = 1;
        RadioButton itemAll = new RadioButton(context);
        itemAll.setText(Lang.get("android_alphabetic_search_all"));
        itemAll.setLayoutParams(params);
        itemAll.setTag("");
        itemAll.setId(i);

        if (Utils.getSPConfig("mapListingType", "").isEmpty()
                || Utils.getSPConfig("mapListingType", null) == null && i == 1 ) {
            itemAll.setChecked(true);
        }
        group.addView(itemAll);
		i++;

		for (Entry<String, HashMap<String, String>> entry : Config.cacheListingTypes.entrySet()) {
//			if ( entry.getValue().get("page").equals("1") ) {
				RadioButton item = new RadioButton(context);
				item.setText(entry.getValue().get("name"));
				item.setLayoutParams(params);
				item.setTag(entry.getKey().toString());
				item.setId(i);

				if (Utils.getSPConfig("mapListingType", "").equals(entry.getKey().toString())) {
					item.setChecked(true);
				}

				group.addView(item);
				i++;
//			}
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(Lang.get("android_menu_listing_type"));
    	dialog.setView(group);

    	/* set listener */
    	dialog.setNegativeButton(Lang.get("android_dialog_cancel"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
    	dialog.setPositiveButton(Lang.get("android_dialog_set"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String selectedKey = (String) group.findViewById(group.getCheckedRadioButtonId()).getTag();
				Utils.setSPConfig("mapListingType", selectedKey);

				request(map.getCameraPosition());
			}
		});

		AlertDialog alert = dialog.create();
		alert.show();
	}

	private static String setDistance(String distance) {
		double dist = Double.parseDouble(distance);
		String unit = Lang.get("android_unit_miles_short");
		String setDistance = distance;
		DecimalFormat dfDecimal = new DecimalFormat("#.#");

		if ( Utils.getSPConfig("distanceUnit", "km").equals("km") ) {
			dist *= 1.60934;

			if ( dist >= 1 ) {
				unit = Lang.get("android_unit_kilometers_short");
				setDistance = dfDecimal.format(dist);
			}
			else {
				unit = Lang.get("android_unit_metres_short");
				setDistance = ""+Math.round(dist/0.1)*100;
			}
		}
		else {
			if ( dist > 0.1 ) {
				setDistance = dfDecimal.format(dist);
			}
			else {
				dist *= 5280;
				unit = Lang.get("android_unit_foots_short");
				setDistance = ""+Math.round(dist/100)*100;
			}
		}

		return Lang.get("android_distance_from_you").replace("{distance}", setDistance).replace("{unit}", unit);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;
		if ( map == null ) {
			return;
		}

		map.getUiSettings().setZoomControlsEnabled(false);

		// set my location
		enableMyLocation();


		map.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
			@Override
			public void onMapClick(LatLng point) {

			}
		});

		mClusterManager = new ClusterManager<MapItemAdapter>(this, map);
		mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MapItemAdapter>() {
			@Override
			public boolean onClusterClick(Cluster<MapItemAdapter> cluster) {
				int zoom_detect = (int) map.getCameraPosition().zoom;
				if(zoom_detect>=20) {
					if(listings.size() > 1) {
						showListingsPopUp(listings);
					}
					/*showList();
					mapMode = false;
					mapCont.setVisibility(View.GONE);
					listCont.setVisibility(View.VISIBLE);*/
				}
				else {
					map.animateCamera(CameraUpdateFactory.newLatLngZoom(
							cluster.getPosition(), (float) Math.floor(map
									.getCameraPosition().zoom + 1)), 300,
							null);
				}
				return true;
			}
		});
		mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MapItemAdapter>() {
			@Override
			public boolean onClusterItemClick(MapItemAdapter item) {
				showInfo(item);
				return true;
			}
		});
		mClusterManager.setRenderer(new MyClusterRenderer(this, map,
				mClusterManager));

		map.setOnMarkerClickListener(mClusterManager);

        /* camera change listener */
		map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener(){
			@Override
			public void onCameraChange(final CameraPosition camera) {
				/* clear timeout */
				if ( timer != null ) {
					timer.cancel();
				}

				/* set timeout 0.5 second */
				timer = new CountDownTimer(500, 500) {
					public void onTick(long millisUntilFinished) {}

					public void onFinish() {
						request(camera);
					}
				};
				timer.start();
			}
		});

        /* set current location to center of the map */
		setLocationCenter();

		button = (Button) this.findViewById(R.id.list_button);
		if ( button != null ) {
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
	            	/* show list */
					if ( mapMode ) {
						mapMode = false;

						showList();

						mapCont.setVisibility(View.GONE);
						listCont.setVisibility(View.VISIBLE);
						button.setText(Lang.get("android_show_map"));
					}
					/* show map */
					else {
						mapMode = true;

						listCont.setVisibility(View.GONE);
						mapCont.setVisibility(View.VISIBLE);
						button.setText(Lang.get("android_show_list"));
					}
				}
			});
		}
		else {
			showList();
		}
	}

	private static void showListingsPopUp(ArrayList<HashMap<String, String>> listingPopup) {
		LinearLayout view = (LinearLayout) instance.getLayoutInflater().inflate(R.layout.map_popup, null);
		view.removeAllViews();

		ListView listView = (ListView) Config.context.getLayoutInflater()
				.inflate(R.layout.listing_list_view, null);

		LayoutParams params = new LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT
		);
		AbsListView.OnScrollListener onScrollListener = null;
		listView.setLayoutParams(params);
		ListingItemAdapter listingsAdapter = new ListingItemAdapter(listingPopup, false);
		listView.setAdapter(listingsAdapter);
		listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));

		listView.setOnItemClickListener(listingsAdapter);

		view.setGravity(Gravity.TOP);
		view.addView(listView);

		/* create dialog */
		AlertDialog.Builder dialogContent = new AlertDialog.Builder(instance);
		dialogContent.setView(view);
		AlertDialog dialogView = dialogContent.create();
		dialogView.show();
	}

	public class MyClusterRenderer extends DefaultClusterRenderer<MapItemAdapter> {

		public MyClusterRenderer(Context context, GoogleMap map, ClusterManager<MapItemAdapter> clusterManager) {
			super(context, map, clusterManager);
		}

		@Override
		protected int getColor(int clusterSize) {
			return Color.parseColor("#"+Integer.toHexString(context.getResources().getColor(R.color.main_color)));
		}

		@Override
		protected boolean shouldRenderAsCluster(Cluster cluster) {
			//start clustering if at least 2 items overlap
			return cluster.getSize() > 1;
		}
	}


	/**
	 * Enables the My Location layer if the fine location permission has been granted.
	 */
	private void enableMyLocation() {
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			// Permission to access the location is missing.
			PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
					Manifest.permission.ACCESS_FINE_LOCATION, true);
		} else if (map != null) {
			// Access to the location has been granted to the app.
			map.setMyLocationEnabled(true);
		}
	}

	@Override
	public boolean onMyLocationButtonClick() {
		Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
		// Return false so that we don't consume the event and the default behavior still occurs
		// (the camera animates to the user's current position).
		return false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
			return;
		}

		if (PermissionUtils.isPermissionGranted(permissions, grantResults,
				Manifest.permission.ACCESS_FINE_LOCATION)) {
			// Enable the my location layer if the permission has been granted.
			enableMyLocation();
		} else {
			// Display the missing permission error dialog when the fragments resume.
			mPermissionDenied = true;
		}
	}

	@Override
	protected void onResumeFragments() {
		super.onResumeFragments();
		if (mPermissionDenied) {
			// Permission was not granted, display error dialog.
			showMissingPermissionError();
			mPermissionDenied = false;
		}
	}

	/**
	 * Displays a dialog with error message explaining that the location permission is missing.
	 */
	private void showMissingPermissionError() {
		PermissionUtils.PermissionDeniedDialog
				.newInstance(true).show(getSupportFragmentManager(), "dialog");
	}
	
	@Override
	public void onBackPressed() {
		if ( mapMode ) {
			super.onBackPressed();
		}
		else {
			mapMode = true;

			listCont.setVisibility(View.GONE);
			mapCont.setVisibility(View.VISIBLE);
			button.setText(Lang.get("android_show_list"));
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_search_around, menu);

		//SearchView searchView = (SearchView) menu.findItem(R.id.search_action).getActionView(); TODO

		Utils.translateMenuItems(menu);
        return true;
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
	        case R.id.map_type:
	            Dialog.mapTypeDialog(instance, map);
	            return true;

	        case R.id.distance_unit:
	            Dialog.distanceUnitDialog(instance);
	            return true;

	        case R.id.listing_type:
	            listingTypeDialog(instance);
	            return true;

	        case android.R.id.home:
	        	super.onBackPressed();
				return true;

	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
}