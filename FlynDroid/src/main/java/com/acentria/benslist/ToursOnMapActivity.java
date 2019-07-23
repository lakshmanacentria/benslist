package com.acentria.benslist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class ToursOnMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static ToursOnMapActivity instance;
    private static Intent intent;
    private static ArrayList<HashMap<String, String>> tours;
    private static GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        setTitle(Lang.get("android_tours_on_map"));

        setContentView(R.layout.activity_tours_on_map);

         /* enable back action */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);


        /* get account data from instance */
        intent = getIntent();
        tours = (ArrayList<HashMap<String, String>>) intent.getSerializableExtra("data");

        ((SupportMapFragment) instance.getSupportFragmentManager().findFragmentById(R.id.map_tours)).getMapAsync(instance);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        /* create map */
        if ( map == null ) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (HashMap<String, String> entry : tours) {

            double lat = Double.valueOf(entry.get("latitude"));
            double lng = Double.valueOf(entry.get("longitude"));
            LatLng position = new LatLng(lat, lng);

            MarkerOptions marker = new MarkerOptions().position(position);
            if ( !entry.get("location").isEmpty() )
            {
                marker.title(entry.get("location"));
            }
            builder.include(marker.getPosition());
            map.addMarker(marker);

        }
        int zoom = Listing.getAvailableZoomLevel(map, Utils.getCacheConfig("android_listing_details_map_zoom"));
        LatLngBounds bounds = builder.build();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, zoom));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
