package com.acentria.benslist.controllers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.ListingDetailsActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.SearchResultsActivity;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.adapters.SavedSearchAdapter;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;


public class SavedSearch extends AbstractController {

	private static SavedSearch instance;

	public static ListView list_view;
	public static int[] menuItems = {R.id.menu_settings};

	public static ArrayList<HashMap<String, String>> fields;
	public static HashMap<String, HashMap<String, String>> searchDatas;
	public static HashMap<String, String> savedSearchData;
	public static List<String> savedSearchMode = new ArrayList<String>(Arrays.asList("ss","find_ss","count_ss"));
	public static SavedSearchAdapter saveSearchAdapter;
	public static LinearLayout main_content;
	public static LinearLayout loading_container;
	public static LinearLayout items_container;
	public static SwipeRefreshLayout swipe_layout;
	public static boolean loading;


	public static SavedSearch getInstance() {
		if ( instance == null ) {
			try {
				instance = new SavedSearch();
			}
			catch(Exception e) {
				Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
			}
			Config.activeInstances.add(instance.getClass().getSimpleName());
		}
		else {
			Utils.restroreInstanceView(instance.getClass().getSimpleName(), Lang.get("title_activity_saved_search"));
		}

		handleMenuItems(menuItems);
		return instance;
	}

	public static void removeInstance(){
		instance = null;
	}

	public SavedSearch(){
		/* set content title */
		Config.context.setTitle(Lang.get("title_activity_saved_search"));

		/* add content view */
		Utils.addContentView(R.layout.view_saved_search);
		
		/* get related view */
		main_content = (LinearLayout)  Config.context.findViewById(R.id.SavedSearch);
		loading_container = (LinearLayout) main_content.findViewById(R.id.progress_bar_custom);
		items_container = (LinearLayout) main_content.findViewById(R.id.list_view_custom);
		swipe_layout = (SwipeRefreshLayout) main_content.findViewById(R.id.list_view_swipe);

		swipe_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if(!loading) {
					loading = true;
					showContent();
					swipe_layout.setRefreshing(false);
				}
			}
		});

		showContent();

		/* hide menu */
		Utils.showContent();
	}


	public static void showContent() {
		fields = new ArrayList<HashMap<String, String>>();
		searchDatas = new HashMap<String, HashMap<String, String>>();

		// get plans data
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));

		final String url = Utils.buildRequestUrl("getMySaveSearch", params, null);

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

					if (doc == null) {
						main_content.removeViewAt(0);
						TextView message = (TextView) Config.context.getLayoutInflater()
								.inflate(R.layout.info_message, null);

						message.setText(Lang.get("returned_xml_failed"));
						main_content.addView(message);
					} else {
						NodeList itemsNode = doc.getElementsByTagName("items");
						Element nlE = (Element) itemsNode.item(0);
						NodeList item = nlE.getChildNodes();
						if (item.getLength() > 0) {
							parseItems(item);
							showSavedSearch();
						} else {
							setEmpty();
						}
						loading = false;
						swipe_layout.setRefreshing(false);
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

	/* display items */
	public static void showSavedSearch() {
		loading_container.setVisibility(View.GONE);
		swipe_layout.setVisibility(View.VISIBLE);
		items_container.removeAllViews();
		items_container.setGravity(Gravity.TOP);
		/* populate list */
		list_view = (ListView) Config.context.getLayoutInflater()
				.inflate(R.layout.list_view, null);

		saveSearchAdapter = new SavedSearchAdapter();

		list_view.setAdapter(saveSearchAdapter);
		list_view.setOnItemClickListener(saveSearchAdapter);

		items_container.addView(list_view);
	}

	/* set empty content */
	public static void setEmpty() {
		loading_container.setVisibility(View.GONE);
		swipe_layout.setVisibility(View.VISIBLE);
		items_container.removeAllViews();
		TextView message = (TextView) Config.context.getLayoutInflater()
				.inflate(R.layout.info_message, null);
		message.setText(Lang.get("android_no_save_search"));
		items_container.setGravity(Gravity.CENTER);
		items_container.addView(message);
	}

	/* parse xml */
	public static void parseItems(NodeList itemsNodes){

		HashMap<String, String> itemHash;

		for( int i=0; i<itemsNodes.getLength(); i++ )
		{
			itemHash = new HashMap<String, String>();//clear steck
			Element item = (Element) itemsNodes.item(i);
			NodeList formFields = item.getChildNodes();

			ArrayList<HashMap<String, String>> itemArray = new ArrayList<HashMap<String, String>>();
			/* convert data from nodes to array */
			for (int c = 0; c < formFields.getLength(); c++) {
				Element field = (Element) formFields.item(c);

				if(field.getTagName().equals("fields")) {
					NodeList itemFields = field.getChildNodes();
					HashMap<String,String> saveItem = new HashMap<String, String>();
					for (int d = 0; d < itemFields.getLength(); d++) {
						Element itemF = (Element) itemFields.item(d);
						saveItem.put(itemF.getTagName(), itemF.getTextContent());
					}
					searchDatas.put(itemHash.get("id"), saveItem);
				}
				else {
					itemHash.put(field.getTagName(), field.getTextContent());
				}

			}
			fields.add(itemHash);
		}
		Log.d("fd", searchDatas.toString());
	}

	// save saved search in preference
	public static void saveSSPreference(Context context, int modeInt, String key, String value){
		String mode = SavedSearch.savedSearchMode.get(modeInt);

		SharedPreferences settings = context.getSharedPreferences("saved_search", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(mode+key, value);
		editor.commit();
	}


	/* parse xml */
	public static void parseSavedSearch(Element itemsNodes){
		savedSearchData = new HashMap<String, String>();

		NodeList items = itemsNodes.getChildNodes();
		for ( int j=0; j<items.getLength(); j++ ) {
			Element itemEl = (Element) items.item(j);
			savedSearchData.put(Utils.getNodeByName(itemEl, "id"), Utils.getNodeByName(itemEl, "matches"));
			if(AccountArea.loginSS) {
				clearSSItem(Utils.getNodeByName(itemEl, "id"));
				saveSSPreference(Config.context, 0, Utils.getNodeByName(itemEl, "id"), Utils.getNodeByName(itemEl, "matches"));
			}


		}
	}

	public static void switchToSavedSearch(Intent intent) {
		String ssType = intent.getStringExtra("type");
		String ssID = intent.getStringExtra("id");
		HashMap<String, String> ssData = Utils.json2hash(intent.getStringExtra("data"));

		updateSSCounter();

		if (!Config.currentView.equals("SavedSearch")) {
			Config.prevView = Config.currentView;
			Config.currentView = "SavedSearch";
			SwipeMenu.adapter.previousPosition = SwipeMenu.adapter.currentPosition;
			SwipeMenu.adapter.currentPosition = SwipeMenu.adapter.getPositionByController("SavedSearch");

			SwipeMenu.adapter.notifyDataSetChanged();

			SavedSearch.getInstance();
			Config.pushView = "";
		}

		if(getSSPreference(2, ssID) != null && getSSPreference(2, ssID).equals("1")) {
			List<String> ss_new = Utils.string2list(getSSPreference(1, ssID).split(","));
			Intent intentDet = new Intent(Config.context, ListingDetailsActivity.class);
			intentDet.putExtra("id", ss_new.get(ss_new.size()-1));
			Config.context.startActivity(intentDet);
		}
		else {
			Intent search_intent = new Intent(Config.context, SearchResultsActivity.class);
			search_intent.putExtra("type", ssType);
			search_intent.putExtra("id", ssID);
			if(SavedSearch.getSSPreference(2, ssID) != null && !SavedSearch.getSSPreference(2, ssID).isEmpty()) {
				search_intent.putExtra("find_ids", SavedSearch.getSSPreference(1, ssID));
			}
			Config.context.startActivity(search_intent);
		}

		clearSSItem(ssID);
	}

	// get saved search in preference
	public static String getSSPreference(int modeInt, String key){
		String mode = SavedSearch.savedSearchMode.get(modeInt);
		SharedPreferences sharedPref = Config.context.getSharedPreferences("saved_search", Context.MODE_PRIVATE);
		String restoredVal = sharedPref.getString(mode+key, null)!=null ? sharedPref.getString(mode+key, null) : "";
		return restoredVal;
	}


	public static void updateSSCounter(){

		if(savedSearchData!=null && !savedSearchData.isEmpty()) {
			for ( String key : savedSearchData.keySet() ) {
				if(getSSPreference(1, key) != null && !getSSPreference(1, key).isEmpty()) {
					List<String> ss_old = Utils.string2list(getSSPreference(0, key).split(","));
					List<String> ss_new = Utils.string2list(getSSPreference(1, key).split(","));
					int count = 0;
					for (int i = 0; i < ss_new.size(); i++)
					{
						if(!ss_old.contains(ss_new.get(i))) {
							count++;
						}
					}
					if(count>0) {
						saveSSPreference(Config.context, 2, key, count+"");
					}
				}
				updateCounter();
			}
		}
	}

	public static void updateCounter(){
		if(savedSearchData!=null && !savedSearchData.isEmpty()) {
			int allSScount = 0;
			for (String key : savedSearchData.keySet()) {
				int count = 0;
				if (getSSPreference(2, key) != null && !getSSPreference(2, key).isEmpty()) {
					count = Integer.parseInt(getSSPreference(2, key));
					allSScount +=count;
 				}
			}
			if (Config.activeInstances.contains("SavedSearch") && SavedSearch.list_view != null) {
				SavedSearch.list_view.post(new Runnable() {
					@Override
					public void run() {
						if (SavedSearch.saveSearchAdapter != null) {
							SavedSearch.saveSearchAdapter.notifyDataSetChanged();
						}
					}
				});
			}

			saveSSPreference(Config.context, 2, "count", allSScount+"");
			if(SwipeMenu.menuData!=null) {
				String all_counts = allSScount > 0 ? allSScount+"" : null;
				SwipeMenu.menuData.get(SwipeMenu.adapter.getPositionByController("SavedSearch")).put("count", all_counts);
				SwipeMenu.menuListView.post(new Runnable() {
					@Override
					public void run() {
						if (SwipeMenu.adapter != null) {
							SwipeMenu.adapter.notifyDataSetChanged();
						}
					}
				});
			}
		}
	}

	public static void clearSSItem(String id){
		saveSSPreference(Config.context, 0, id, getSSPreference(1, id));
		saveSSPreference(Config.context, 1, id, "");
		saveSSPreference(Config.context, 2, id, "");
		updateSSCounter();
	}
}
