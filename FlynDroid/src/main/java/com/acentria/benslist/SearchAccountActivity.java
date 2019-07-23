package com.acentria.benslist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.acentria.benslist.adapters.AccountItemAdapter;
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

public class SearchAccountActivity extends AppCompatActivity {

	public static int requestSteck = 1; //steck number (pagination)
	public static int requestTotal = 0; //total availalbe items
	public static boolean liadingInProgress = false;
	
	public static HashMap<String,String> formData;
	public static String accountType;
	
	public static AccountItemAdapter AccountAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Lang.setDirection(this);

		setTitle(Lang.get("android_title_activity_search_accounts"));
        setContentView(R.layout.activity_search_account);
        
        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        /* ad sense*/
        LinearLayout content = (LinearLayout) findViewById(R.id.SearchResulstAccount);
        Utils.setAdsense(content, "search_accounts");

        final LinearLayout layout = (LinearLayout) findViewById(R.id.SearchResulst);
        
        /* get passed data */
        Intent intent = getIntent();
        formData = (HashMap<String, String>) intent.getSerializableExtra("data");
        accountType = intent.getStringExtra("type");

        /* build request url */
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		params.put("data", formData.toString().replace(" ", ""));
		params.put("type", accountType);
		final String url = Utils.buildRequestUrl("searchAccounts", params, null);

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
						Element nlE = (Element) doc.getElementsByTagName("items").item(0);
						NodeList accountNodes = nlE.getChildNodes();

						/* populate list */
						if ( accountNodes.getLength() > 0 )
						{
							AccountAdapter = new AccountItemAdapter(Account.prepareGridAccount(accountNodes, accountType, null));
							requestTotal = Account.lastRequestTotalAccounts;

							OnScrollListener onScrollListener = null;

							setTitle(Lang.get("android_title_activity_search_accounts")+" ("+requestTotal+")");

							int grid_items_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
							int rest_items = requestTotal - (grid_items_number * requestSteck);

							/* create list view of accounts */
							final ListView listView = (ListView) getLayoutInflater()
									.inflate(R.layout.listing_list_view, null);

							LayoutParams params = new LayoutParams(
									LayoutParams.MATCH_PARENT,
									LayoutParams.MATCH_PARENT
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

							listView.setAdapter(AccountAdapter);
							listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));

							listView.setOnItemClickListener(AccountAdapter);

							layout.setGravity(Gravity.TOP);
							layout.addView(listView);
						}
						/* display no accounts message */
						else
						{
							TextView message = (TextView) getLayoutInflater()
								.inflate(R.layout.info_message, null);

							message.setText(Lang.get("android_there_no_accounts_found"));
							layout.addView(message);
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
	
	public static void loadNextStack(final Button preloadButton, final View footerView, final ListView listView) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("start", ""+requestSteck);
		params.put("data", formData.toString().replace(" ", ""));
		params.put("type", accountType);
		final String url = Utils.buildRequestUrl("searchAccounts", params, null);
		
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
						Element nlE = (Element) doc.getElementsByTagName("items").item(0);
						NodeList accountNodes = nlE.getChildNodes();

						/* populate list */
						if ( accountNodes.getLength() > 0 )
						{
							AccountAdapter.add(Account.prepareGridAccount(accountNodes, accountType, null));
							requestTotal = Account.lastRequestTotalAccounts;
							liadingInProgress = false;
						}

						/* update button text */
						int grid_items_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
						int rest_items = requestTotal - (grid_items_number * requestSteck);
						if ( rest_items > 0 )
						{
							int set_rest_items = rest_items >= grid_items_number ? grid_items_number : rest_items;
							String buttonPhrase = Lang.get("android_load_next_number_accounts")
									.replace("{number}", ""+set_rest_items);
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

	public void clearData() {
		requestSteck = 1;
		requestTotal = 0;
		liadingInProgress = false;

		finish();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		clearData();
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
	        case R.id.menu_settings:
	            FlynMenu.menuItemSetting();
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