package com.acentria.benslist.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.MyListing;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.adapters.MyListingAdapter;
import com.acentria.benslist.controllers.MyListings;
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


public class MyListingsFragment extends Fragment {

    private String type_key = "";
    private MyListing myListing;
    public MyListingAdapter adapter;
    private Integer requestSteck = 1;
    private boolean liadingInProgress = false;
    final public Fragment myListingsFragment;

    public MyListingsFragment() {
        myListingsFragment = this;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        Bundle args = getArguments();
        type_key = args.getString("key");

        ProgressBar progressBar = (ProgressBar) Config.context.getLayoutInflater()
                .inflate(R.layout.loading, null);

        final LinearLayout layout = new LinearLayout(getActivity());
        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        layout.setGravity(Gravity.CENTER);
        layout.addView(progressBar);

        this.getMyListings(layout);

        if (Lang.isRtl()) {
//            layout.setRotationY(180);
        }

        return layout;
    }

    /**
     * get recently added listings by listing type
     *
     * @param layout - fragment layout to assign list view to
     */
    private void getMyListings(final LinearLayout layout){

	    /* build request url */
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("type", type_key);
        params.put("sleep", "1");
        params.put("start", ""+requestSteck);
        params.put("account_id", Account.accountData.get("id"));
        params.put("password_hash", Utils.getSPConfig("accountPassword", null));
        final String url = Utils.buildRequestUrl("getMyListings", params, null);

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
                        TextView message = (TextView) Config.context.getLayoutInflater()
                                .inflate(R.layout.info_message, null);

                        message.setText(Lang.get("returned_xml_failed"));
                        layout.removeAllViews();
                        layout.addView(message);
                    }
                    else {
                        NodeList listingNode = doc.getElementsByTagName("items");

                        Element nlE = (Element) listingNode.item(0);
                        NodeList listing_nodes = nlE.getChildNodes();

                        myListing = new MyListing();
                        ArrayList<HashMap<String, String>> listings = myListing.prepareGridListing(listing_nodes, type_key);

                            /* populate list */
                        adapter = new MyListingAdapter(listings, myListingsFragment);
                        OnScrollListener onScrollListener = null;

                        int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
                        int rest_listings = myListing.lastRequestTotalListings - grid_listings_number;

                            /* create list view of listings */
                        final ListView listView = (ListView) Config.context.getLayoutInflater()
                                .inflate(R.layout.listing_list_view, null);

                        LayoutParams params = new LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                LayoutParams.MATCH_PARENT
                        );
                        listView.setLayoutParams(params);
                        //listView.setSelector(R.drawable.blank);

                            /* create footer view for lisings list view */
                        if ( rest_listings > 0 ) {
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
                                        loadNext(preloadButton, footerView, listView);
                                    }
                                });
                            }
                                /* on scroll listener */
                            else {
                                onScrollListener = new OnScrollListener() {
                                    @Override
                                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                                        if ( !liadingInProgress && firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount < myListing.lastRequestTotalListings ) {
                                            liadingInProgress = true;

                                            requestSteck++;

                                            loadNext(preloadButton, footerView, listView);
                                        }
                                    }

                                    @Override
                                    public void onScrollStateChanged(AbsListView view, int scrollState) { }
                                };
                            }

                            listView.addFooterView(footerView);
                        }

                        listView.setAdapter(adapter);

                            /* set listeners */
                        listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));
                        listView.setOnItemClickListener(adapter);

                            /* set empty view */
                        TextView message = (TextView) Config.context.getLayoutInflater()
                                .inflate(R.layout.info_message, null);
                        message.setText(Lang.get("android_no_my_listings"));
                        message.setGravity(Gravity.CENTER);
                        layout.addView(message, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                        listView.setEmptyView(message);

                        //layout.setGravity(Gravity.TOP);
                        layout.addView(listView);
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

    private void loadNext(final Button preloadButton, final View footerView, final ListView listView) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("type", type_key);
        params.put("start", ""+requestSteck);
        params.put("account_id", Account.accountData.get("id"));
        params.put("password_hash", Utils.getSPConfig("accountPassword", null));
        final String url = Utils.buildRequestUrl("getMyListings", params, null);

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
                            adapter.add(myListing.prepareGridListing(listings, type_key));
                            liadingInProgress = false;
                        }

    		    			/* update button text */
                        int grid_listings_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
                        int rest_listings = myListing.lastRequestTotalListings - (grid_listings_number * requestSteck);
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
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Config.RESULT_PAYMENT:
                if ( resultCode == Activity.RESULT_OK ) {
                    HashMap<String, String> listing_hash = (HashMap<String, String>) data.getSerializableExtra("hash");

                    Dialog.simpleWarning(listing_hash.get("success_phrase"));

                    if ( data.hasExtra("success") ) {
                        HashMap<String, String> success = (HashMap<String, String>) data.getSerializableExtra("success");
                        MyListings.updateItem(Integer.parseInt(listing_hash.get("id")), success);
                    }
                    else {
                        Log.d("FD", "My Listings Fragment - no success data received, listview update failed");
                        Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
                    }
                }
                else if (resultCode == Config.RESULT_TRANSACTION_FAILED ) {
                    Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
                    Utils.bugRequest("Payment result error ("+Utils.getSPConfig("domain", "")+")", data.toString());
                }

                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);

                break;
        }
    }
}