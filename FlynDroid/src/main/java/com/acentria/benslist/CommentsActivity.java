package com.acentria.benslist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.acentria.benslist.adapters.CommentItemAdapter;
import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

/**
 * Created by FED9I on 09.01.2015.
 */
public class CommentsActivity extends AppCompatActivity {
    public static CommentsActivity instance;
    private static Intent intent;

    private static String ldListingID;
    private static String ldAccountID;
    public static CommentItemAdapter CommentsAdapter;
    private static LinearLayout layout;

    public static int lastRequestTotalComments;
    public static int requestSteck = 1; //steck number (pagination)
    public static int requestTotal = 0; //total availalbe items
    public static boolean loadingInProgress = false;
    public static ArrayList<HashMap<String, String>> comments = new ArrayList<HashMap<String, String>>();
    private static Menu actionBarMenu;
    private static Boolean restart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        Lang.setDirection(this);
        setTitle(Lang.get("android_title_activity_comments"));
        setContentView(R.layout.activity_comments);

        /* get listing data from instance */
        intent = getIntent();
        ldListingID = intent.getStringExtra("listing_id");
        ldAccountID = intent.getStringExtra("account_id");
        comments = new ArrayList<HashMap<String, String>>();
        requestSteck = 1;
        loadingInProgress = false;
        restart = false;

        /* enable back action */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        LinearLayout content = (LinearLayout) findViewById(R.id.activity_comments);

        /* add ad sense */
        Utils.setAdsense(content, "comments");

        layout = (LinearLayout) content.findViewById(R.id.comments);

        /* build request url */
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("listing_id", ldListingID);
        params.put("account_id", ldAccountID);
        final String url = Utils.buildRequestUrl("getComments", params, null);

        /* do async request */
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                // called when response HTTP status is "200 OK"
                try {
                    String response = String.valueOf(new String(server_response, "UTF-8"));
                    /* parse response */

                    JSONObject json =  new JSONObject(response);

                    layout.removeAllViews();

                    if (json.isNull("items")) {
                        TextView message = (TextView) Config.context.getLayoutInflater()
                                .inflate(R.layout.info_message, null);

                        message.setText(Lang.get("returned_xml_failed"));
                        layout.addView(message);
                    }
                    else {
                        ArrayList<HashMap<String, String>> tmpComments = prepareComments(response);
                        //	 populate list
                        if ( tmpComments!=null && tmpComments.size() > 0 ) {
                            CommentsAdapter = new CommentItemAdapter(tmpComments);
                            requestTotal = CommentsActivity.lastRequestTotalComments;

                            AbsListView.OnScrollListener onScrollListener = null;

                            int grid_items_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
                            int rest_items = requestTotal - (grid_items_number * requestSteck);

                            /* create list view of comments */
                            final ListView listView = (ListView) getLayoutInflater()
                                    .inflate(R.layout.list_view, null);

                            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                            );
                            listView.setLayoutParams(params);

                            /* create footer view for lisings list view */
                            if ( rest_items > 0 )
                            {
                                int preloadView = R.layout.list_view_footer_button;
                                String buttonPhraseKey = "android_load_next_number_accounts";

                                if ( Utils.getSPConfig("preload_method", null).equals("scroll") ) {
                                    preloadView = R.layout.list_view_footer_loading;
                                    buttonPhraseKey = "android_loading_next_number_accounts";
                                }

                                final View footerView = (View) getLayoutInflater()
                                        .inflate(preloadView, null);

                                final Button preloadButton = (Button) footerView.findViewById(R.id.preload_button);
                                int set_rest_items = rest_items >= grid_items_number ? grid_items_number : rest_items;
                                String buttonPhrase = Lang.get(buttonPhraseKey)
                                        .replace("{number}", ""+set_rest_items);
                                preloadButton.setText(buttonPhrase);

                                /* preload button listener */
                                if ( Utils.getSPConfig("preload_method", null).equals("button") ) {
                                    preloadButton.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            requestSteck += 1;
                                            preloadButton.setText(Lang.get("android_loading"));
                                            loadNextComments(preloadButton, footerView, listView);
                                        }
                                    });
                                }
                                /* on scroll listener */
                                else {
                                    onScrollListener = new AbsListView.OnScrollListener() {
                                        @Override
                                        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                                            if ( !loadingInProgress && firstVisibleItem + visibleItemCount == totalItemCount ) {
                                                loadingInProgress = true;
                                                requestSteck += 1;
                                                loadNextComments(preloadButton, footerView, listView);
                                            }
                                        }

                                        @Override
                                        public void onScrollStateChanged(AbsListView view, int scrollState) { }
                                    };
                                }
                                listView.addFooterView(footerView);
                            }

                            listView.setAdapter(CommentsAdapter);
                            listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));

                            /* set empty view */
                            if ( !comments.isEmpty() ) {
                                ProgressBar progressBar = (ProgressBar) Config.context.getLayoutInflater()
                                        .inflate(R.layout.loading, null);
                                progressBar.setTag("progress_bar");
                                layout.addView(progressBar);
                                listView.setEmptyView(progressBar);
                            }
                            else {
                                setEmpty(listView, false);
                            }
                            layout.setGravity(Gravity.TOP);
                            layout.addView(listView);
                        }
                    }

                } catch (UnsupportedEncodingException e1) {

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        });
    }

    public static void loadNextComments(final Button preloadButton, final View footerView, final ListView listView) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("listing_id", "" + ldListingID);
        params.put("account_id", "" + ldAccountID);
        params.put("start", "" + requestSteck);

        final String url = Utils.buildRequestUrl("getComments", params, null);

		/* do async request */
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                // called when response HTTP status is "200 OK"
                try {
                    String response = String.valueOf(new String(server_response, "UTF-8"));
                    /* parse response */
                    JSONObject json =  new JSONObject(response);

                    if (json.isNull("items")) {
                        Dialog.simpleWarning(Lang.get("returned_xml_failed"));
                    }
                    else {
                        ArrayList<HashMap<String, String>> tmpComments = prepareComments(response);
                        //	 populate list
                        if ( tmpComments!=null && tmpComments.size() > 0 ) {
                            CommentsAdapter = new CommentItemAdapter(tmpComments);
                            CommentsAdapter.add(tmpComments, false);
                            requestTotal = CommentsActivity.lastRequestTotalComments;
                            loadingInProgress = false;
                        }

                        /* update button text */
                        int grid_items_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
                        int rest_items = requestTotal - (grid_items_number * requestSteck);
                        if ( rest_items > 0 )
                        {
                            int set_rest_items = rest_items >= grid_items_number ? grid_items_number : rest_items;
                            String buttonPhrase = Lang.get("android_load_next_number_accounts")
                                    .replace("{number}", "" + set_rest_items);
                            preloadButton.setText(buttonPhrase);
                        }
                        else
                        {
                            listView.removeFooterView(footerView);
                        }
                    }

                } catch (UnsupportedEncodingException e1) {

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        });
    }

    /**
     * prepare comments
     *
     * @param comments - comments data
     */
    public static ArrayList<HashMap<String, String>> prepareComments(String string){
        JSONObject json = null;
        try {

            json = new JSONObject( string );
            if(!json.isNull("count")) {
                lastRequestTotalComments = json.getString("count").isEmpty() ? 0 : Integer.parseInt(json.getString("count"));
            }
            if(!json.isNull("items")) {
                comments = JSONParser.parseJsontoArrayList(json.getString("items"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

//       ArrayList<HashMap<String, String>> commentsOut = new ArrayList<HashMap<String, String>>();
//       HashMap<String, String> tmpFields;
//
//        for ( int i=0; i<commentsNode.getLength(); i++ )
//        {
//           Element comment = (Element) commentsNode.item(i);
//           if ( comment.getTagName().equals("count") ) {
//               lastRequestTotalComments = comment.getTextContent().isEmpty() ? 0 : Integer.parseInt(comment.getTextContent());
//           }
//           else {
//               tmpFields = new HashMap<String, String>();
//               //clear steck
//               /* convert fields from nodes to array */
//               tmpFields.put("title", comment.getAttribute("title"));
//               tmpFields.put("status", comment.getAttribute("status"));
//               tmpFields.put("author", comment.getAttribute("author"));
//               tmpFields.put("rating", comment.getAttribute("rating"));
//               tmpFields.put("date", comment.getAttribute("date"));
//               tmpFields.put("description", comment.getTextContent());
//               tmpFields.put("use_html", "0");
//
//               commentsOut.add(tmpFields);
//           }
//        }
//        comments = commentsOut;
        return comments;
    }

    /**
     * add comment
     *
     * @param comment - comment data
     */
    public static void addComment(  HashMap<String, String> comment ) {
        ArrayList<HashMap<String, String>> comment_array = new ArrayList<HashMap<String, String>>();
        comment.put("listing_id", ldListingID.toString());
        if ( Account.loggedIn ) {
            String account_id = Account.accountData.get("id");
            comment.put("account_id", account_id.toString());
        }
        String now = DateFormat.getDateInstance().format(new Date());
        comment.put("date", now);
        comment_array.add(comment);
        CommentsAdapter.add(comment_array, true);

        //  do async request
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(30000); // set 30 seconds for this task
        final String url = Utils.buildRequestUrl("addComment", comment, null);
        client.post(url, Utils.toParams(comment), new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                // called when response HTTP status is "200 OK"
                try {
                    String response = String.valueOf(new String(server_response, "UTF-8"));
                    // parse response
                    XMLParser parser = new XMLParser();
                    Document doc = parser.getDomElement(response, url);

                    if ( doc == null ) {
                        layout.removeViewAt(comments.size());
                        comments.remove(comments.size()-1);
                        Dialog.simpleWarning(Lang.get("returned_xml_failed"), instance);
                    }
                    else {
                        NodeList errorNode = doc.getElementsByTagName("error");
                        // handle errors
                        if ( errorNode.getLength() > 0 ) {
                            layout.removeViewAt(comments.size());
                            comments.remove(comments.size()-1);
                            Element error = (Element) errorNode.item(0);
                            Dialog.simpleWarning(Lang.get(error.getTextContent()), instance);

                        }
                        else {
                            if ( Utils.getCacheConfig("comment_auto_approval").equals("1") ) {
                                Dialog.toast("comment_added", instance);
                            }
                            else {
                                Dialog.toast("comment_added_approval", instance);
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

    /*
    * set if empty comments
    */
    private void setEmpty(ListView view, Boolean removeProgress) {
        if ( removeProgress ) {
            layout.removeView(layout.findViewWithTag("progress_bar"));
        }

        TextView message = (TextView) Config.context.getLayoutInflater()
                .inflate(R.layout.info_message, null);
        message.setText(Lang.get("android_no_comments_exist"));
        message.setGravity(Gravity.CENTER);
        layout.addView(message, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.setEmptyView(message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_comments, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_add_comment);
        if ( Utils.getCacheConfig("comments_login_post").equals("1") && Account.loggedIn == false) {
            item.setVisible(false);
        }
        else {
            item.setVisible(true);
        }
        actionBarMenu = menu;

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_comment:
                Context context = instance;
                Listing.showCommentDialog(context, true);
                return true;

            case android.R.id.home:
                super.onBackPressed();
                LinearLayout commentsContent = (LinearLayout) ListingDetailsActivity.instance.findViewById(R.id.comments);

                if ( !comments.isEmpty() && comments.size() <= 5 ) {
                    Listing.populateComments(commentsContent, comments);
                }
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