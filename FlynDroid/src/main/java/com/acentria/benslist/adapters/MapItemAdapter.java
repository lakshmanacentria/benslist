package com.acentria.benslist.adapters;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.util.HashMap;

/**
 * Created by fed9i on 8/23/16.
 */
public class MapItemAdapter implements ClusterItem {
    private final LatLng mPosition;
    private HashMap<String, String> info;

    public MapItemAdapter(double lat, double lng, HashMap<String, String> entry) {
        mPosition = new LatLng(lat, lng);
        info = entry;
    }

    public HashMap<String, String> getItem() {
        return info;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }
}
