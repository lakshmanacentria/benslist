package com.acentria.benslist;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.acentria.benslist.controllers.AccountArea;
import com.acentria.benslist.controllers.SavedSearch;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class Account {

	public static int lastRequestTotalAccounts;
	
	public static HashMap<String, String> accountData = new HashMap<String, String>();
	public static ArrayList<HashMap<String, String>> accountFields = new ArrayList<HashMap<String, String>>();
	public static ArrayList<ArrayList<HashMap<String, String>>> accountStats = new ArrayList<ArrayList<HashMap<String, String>>>();
	public static boolean loggedIn = false;
	public static LinkedHashMap<String, HashMap<String,String>> data = new LinkedHashMap<String, HashMap<String,String>>();
	public static String email_field_key = "account_email";
    public static List<String> abilities;
    public static List<String> onMapFields = new ArrayList<String>();
	
	public static void login( String passwordHash, LinearLayout login_form, LinearLayout profile_layer ) {
		Account.loggedIn = true;
		Utils.setSPConfig("accountUsername", accountData.get("username"));
		Utils.setSPConfig("accountPassword", passwordHash);
	
		login_form.setVisibility(View.GONE);
		profile_layer.setVisibility(View.VISIBLE);

		SwipeMenu.menuData.get(SwipeMenu.loginIndex).put("name", accountData.get("full_name"));
		SwipeMenu.adapter.notifyDataSetChanged();

		Config.context.setTitle(Lang.get("my_profile"));
		AccountArea.menu_logout.setVisible(true);
		AccountArea.menu_remove_account.setVisible(true);
		if(AccountArea.profileTab !=null && Account.accountData!=null && Account.accountData.size()>0) {
			Account.populateProfileTab();
		}
	}

	/**
	 * fetch account data and fields array from node list 
	 * 
	 * @param account - account data
	 */
	public static void fetchAccountData( NodeList account ) {
		for( int i=0; i<account.getLength(); i++ ) {
			Element node = (Element) account.item(i);
			if ( node.getTagName().equals("fields") ) {
				NodeList fields = node.getChildNodes();
				
				for ( int j=0; j<fields.getLength(); j++ ) {
					Element field_node = (Element) fields.item(j);
					HashMap<String, String> tmp_field = new HashMap<String, String>();
					tmp_field.put("key", field_node.getAttribute("key"));
					tmp_field.put("type", field_node.getAttribute("type"));
					tmp_field.put("name", Config.convertChars(field_node.getTextContent()));
					accountFields.add(tmp_field);
				}
			}
			else if ( node.getTagName().equals("statistics") ) {
                accountStats = parseStatistics(node);
			}
			else if ( node.getTagName().equals("saved_search")) {
                SavedSearch.parseSavedSearch(node);
			}
			else {
                if ( node.getTagName().equals("abilities") ) {
                    abilities = Utils.string2list(node.getTextContent().split(","));
                }
                else {
                    accountData.put(node.getTagName(), Config.convertChars(node.getTextContent()));
                }
			}
		}
	}

    public static ArrayList<ArrayList<HashMap<String, String>>> parseStatistics(Element node) {
        ArrayList<ArrayList<HashMap<String, String>>> out = new ArrayList<ArrayList<HashMap<String, String>>>();
        NodeList fields = node.getChildNodes();

        for ( int j=0; j<fields.getLength(); j++ ) {
            Element section_node = (Element) fields.item(j);
            ArrayList<HashMap<String, String>> tmp_section = new ArrayList<HashMap<String, String>>();

            NodeList items = section_node.getChildNodes();
            for ( int k=0; k<items.getLength(); k++ ) {
                Element item_node = (Element) items.item(k);

                HashMap<String, String> tmp_item = new HashMap<String, String>();
                tmp_item.put("name", item_node.getTextContent());
                tmp_item.put("number", item_node.getAttribute("number"));
                if ( item_node.hasAttribute("count") ) {
                    tmp_item.put("count", item_node.getAttribute("count"));
                }
                if ( k == 0 ) {
                    tmp_item.put("caption", section_node.getAttribute("name"));
                }

                tmp_section.add(tmp_item);
            }

            out.add(tmp_section);
        }

        return out;
    }
	
	public static void populateProfileTab() {
		// set account details
		if(AccountArea.profileTab!=null) {
			ImageView avatar = (ImageView) AccountArea.profileTab.findViewById(R.id.profileImage);
			if (!accountData.get("photo").isEmpty()) {
				Utils.imageLoaderDisc.displayImage(accountData.get("photo"), avatar, Utils.imageLoaderOptionsDisc);
			} else {
				avatar.setImageResource(R.mipmap.seller_no_photo);
			}

			TextView username_caption = (TextView) AccountArea.profileTab.findViewById(R.id.username);
			username_caption.setText(accountData.get("username"));

			TextView type_name = (TextView) AccountArea.profileTab.findViewById(R.id.type_name);
			type_name.setText(accountData.get("type_name"));

			// build statistics block
			buildStat();
		}
	}

    private static void buildStat() {
        LinearLayout stat_cont = (LinearLayout) AccountArea.profileTab.findViewById(R.id.statistics_cont);
        stat_cont.removeAllViews();

		for (ArrayList<HashMap<String, String>> data : accountStats) {
			if(data.size()>0) {
				buildStatItem(stat_cont, data.get(0).get("caption"), data);
			}
		}
    }

    public static void updateStat() {
        /* prepare GET params */
        final String url = Utils.buildRequestUrl("getAccountStat");

        RequestParams params = new RequestParams();
        params.put("account_id", Account.accountData.get("id"));
        params.put("password_hash", Utils.getSPConfig("accountPassword", null));

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] response) {
				// called when response HTTP status is "200 OK"
				try {
					String server_response = String.valueOf(new String(response, "UTF-8"));
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(server_response, url);

					if ( doc != null ) {
						accountStats = parseStatistics((Element) doc.getElementsByTagName("statistics").item(0));
						if ( Config.activeInstances.contains("AccountArea") ) {
							buildStat();
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
	
	private static void buildStatItem(LinearLayout parent, String caption, ArrayList<HashMap<String, String>> data) {
        if ( data == null || data.size() == 0 ) {
            Log.d("FD", "buildStatItem() failed, no data provided");
            return;
        }

		LinearLayout view = (LinearLayout) Config.context.getLayoutInflater().inflate(R.layout.stat_section, null);
		view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		// set caption
		TextView caption_view = (TextView) view.findViewById(R.id.caption);
		if ( caption == null || caption.isEmpty() ) {
			caption_view.setVisibility(View.GONE);
		}
		else {
			caption_view.setText(Lang.get(caption));
		}
		
		// add item
		LinearLayout container = (LinearLayout) view.findViewById(R.id.container);
		container.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		
		for (int i = 0; i < data.size(); i++ ) {
			HashMap<String, String> item = data.get(i);
			
			LinearLayout stat_item = (LinearLayout) Config.context.getLayoutInflater().inflate(R.layout.stat_item, null);
			LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
			
			int margin = i == data.size()-1 ? 0 : Utils.dp2px(15);
			if(Lang.isRtl()) {
				params.setMargins(margin, 0, 0, 0);
			}
			else {
				params.setMargins(0, 0, margin, 0);
			}
			stat_item.setLayoutParams(params);
			
			// set name
			TextView name_view = (TextView) stat_item.findViewById(R.id.name);
			name_view.setText(Lang.get(item.get("name")));
			
			// set number
			TextView number_view = (TextView) stat_item.findViewById(R.id.number);
			number_view.setText(item.get("number").isEmpty() ? "0" : item.get("number"));
			
			// set count
			if ( item.containsKey("count") && !item.get("count").isEmpty() ) {
				TextView count_view = (TextView) stat_item.findViewById(R.id.counter);
				count_view.setVisibility(View.VISIBLE);
				count_view.setText(item.get("count"));
			}
			
			container.addView(stat_item);
		}
		
		data.clear();
		parent.addView(view);
	}

	/**
	 * prepare listing in grid, parse xml and populate listing fields
	 * 
	 * @param accounts - listings node list
	 * @param type - listing type key
	 * @param aFields - additional fields to parse
	 * @return
	 */
    public static ArrayList<HashMap<String, String>> prepareGridAccount(NodeList accounts, String type, String[] aFields){
		ArrayList<HashMap<String, String>> accountsOut = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> tmpFields;
		
		for( int i=0; i<accounts.getLength(); i++ ) {
			Element account = (Element) accounts.item(i);
			
			if ( account.getTagName().equals("statistic") ) {
				NodeList stats = account.getChildNodes();
				for ( int j = 0; j < stats.getLength(); j++ ) {
					Element tag = (Element) stats.item(j);
					if ( tag.getTagName().equals("total") )	{
						lastRequestTotalAccounts = tag.getTextContent().isEmpty() ? 0 : Integer.parseInt(tag.getTextContent());
					}
				}
			}
			else {
				//NodeList fields = listing.getChildNodes();
				tmpFields = new HashMap<String, String>();//clear steck 
				
				/* convert fields from nodes to array */
				tmpFields.put("id", Utils.getNodeByName(account, "id"));
				tmpFields.put("photo", Utils.getNodeByName(account, "photo"));
				tmpFields.put("listings_count", Utils.getNodeByName(account, "listings_count"));
				tmpFields.put("date", Utils.getNodeByName(account, "date"));
				tmpFields.put("full_name", Utils.getNodeByName(account, "full_name"));
				tmpFields.put("middle_field", Utils.getNodeByName(account, "middle_field"));
				
				/* additional fields handler */
				if ( aFields != null && aFields.length > 0 ) {
					for (int j = 0; j < aFields.length; j++) {
						tmpFields.put(aFields[j], Utils.getNodeByName(account, aFields[j]));
					}
				}
	
				accountsOut.add(i, tmpFields);
			}
		}
		
		return accountsOut;
	}
    
    public static void logout() {
    	if ( !Account.loggedIn )
    		return;
    	
    	SwipeMenu.menuData.get(SwipeMenu.loginIndex).put("name", Lang.get("android_menu_login"));
      	SwipeMenu.adapter.notifyDataSetChanged();
      	 
		// unregistr push notification
		GetPushNotification.regNotification(Account.accountData.get("id"), false);

      	Account.loggedIn = false;
      	Account.accountData.clear();
      	Account.accountFields.clear();
      	Account.accountStats.clear();
      	 
      	Utils.setSPConfig("accountUsername", "");
      	Utils.setSPConfig("accountPassword", "");
    }
    
    public static LinkedHashMap<String, HashMap<String, String>> parseJsonForm(JSONObject object,
																		   HashMap<String, ArrayList<HashMap<String, String>>> items) {
		LinkedHashMap<String, HashMap<String, String>> out = new LinkedHashMap<String, HashMap<String, String>>();


		try {
            JSONObject fields = object.getJSONObject("form");

			Iterator<String> keysItr = fields.keys();
			while(keysItr.hasNext()) {
				HashMap<String, String> tmpField = new HashMap<String, String>();
				String key = keysItr.next();
				Object value = fields.get(key);

				JSONObject jsonObj = (JSONObject) value;
				tmpField.put("key", key);
				tmpField.put("type", jsonObj.getString("type"));
				tmpField.put("required", jsonObj.getString("required"));
				tmpField.put("multilingual", jsonObj.getString("multilingual"));
				tmpField.put("name", jsonObj.getString("name"));
				tmpField.put("current", jsonObj.getString("current").equals("null") || jsonObj.getString("current").isEmpty() ? "" : jsonObj.getString("current"));
				tmpField.put("default", jsonObj.getString("default").equals("null") || jsonObj.getString("default").isEmpty() ? "" : jsonObj.getString("default"));
				tmpField.put("data", jsonObj.getString("data"));

				/* on map fields*/
				if ( !jsonObj.isNull("map") && jsonObj.getString("map").equals("1") ) {
					onMapFields.add(key);
				}

				try {
					String jsonItems = jsonObj.getString("values");
					if(key.equals("availability")) {
						parseAvailability(items, tmpField, jsonItems);
					}
					else if(key.equals("escort_rates")) {
						parseEscortRates(items, tmpField, jsonItems);
					}
					else {

						if (JSONParser.isJson(jsonItems)) {
							tmpField.put("values", "__data"); // the data for this field stored in value_data variable under related key

                            ArrayList<HashMap<String, String>> tmpValues = new ArrayList<HashMap<String, String>>();
                            tmpValues = JSONParser.parseJsontoArrayList(jsonItems);

                            if ( tmpField.get("type").equals("select") ) {
                                HashMap<String, String> field_name = new HashMap<String, String>();
                                field_name.put("name", Lang.get("android_select_field").replace("{field}", tmpField.get("name")));
                                field_name.put("key", "");
                                tmpValues.add(0, field_name);
                            }
                            items.put(key, tmpValues);
						}
						else {
							tmpField.put("values", jsonItems);
						}
					}
				} catch (Exception e) {
					//e.printStackTrace();
					tmpField.put("values", jsonObj.getString("values"));
				}
				out.put(key, tmpField);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return out;
	}

	public static LinkedHashMap<String, HashMap<String, String>> parseForm(NodeList fields, 
			HashMap<String, ArrayList<HashMap<String, String>>> items) {
		
		if ( fields.getLength() <= 0 )
			return null;
		
		LinkedHashMap<String, HashMap<String, String>> out = new LinkedHashMap<String, HashMap<String, String>>();

		for ( int i = 0; i < fields.getLength(); i++ ) {
			HashMap<String, String> tmpField = new HashMap<String, String>();
			
			Element field = (Element) fields.item(i);
			String fieldKey = Utils.getNodeByName(field, "key");
			
			tmpField.put("key", fieldKey);
			tmpField.put("type", Utils.getNodeByName(field, "type"));
			tmpField.put("required", Utils.getNodeByName(field, "required"));
			tmpField.put("multilingual", Utils.getNodeByName(field, "multilingual"));
			tmpField.put("name", Utils.getNodeByName(field, "name"));
			tmpField.put("current", Utils.getNodeByName(field, "current"));
			tmpField.put("default", Utils.getNodeByName(field, "default"));
			tmpField.put("data", Utils.getNodeByName(field, "data"));
			
             /* on map fields*/
            if ( Utils.getNodeByName(field, "map").equals("1") ) {
                onMapFields.add(fieldKey);
            }

			// values
			NodeList valuesNode = field.getElementsByTagName("values");

			if(fieldKey.equals("availability")) {
				parseAvailability(items, tmpField, valuesNode);
			}
			else {
				if ( valuesNode.item(0).hasChildNodes() ) { // * because this line returns true even if the node hasn't child notes, I think CDATA is guilty
					try {
						tmpField.put("values", "__data"); // the data for this field stored in value_data variable under related key

						ArrayList<HashMap<String, String>> tmpValues = new ArrayList<HashMap<String, String>>();

						if ( tmpField.get("type").equals("select") ) {
							HashMap<String, String> field_name = new HashMap<String, String>();
							field_name.put("name", Lang.get("android_select_field").replace("{field}", tmpField.get("name")));
							field_name.put("key", "");
							tmpValues.add(field_name);
						}

						NodeList values = (NodeList) valuesNode.item(0).getChildNodes();
						for ( int j = 0; j < values.getLength(); j++ ) {
							HashMap<String, String> tmpValue = new HashMap<String, String>();

							Element value = (Element) values.item(j); // this line breaks the app, see comment marked * above
							tmpValue.put("key", Utils.getNodeByName(value, "key"));
							tmpValue.put("name", Utils.getNodeByName(value, "name"));
							if (Utils.getNodeByName(value, "default").equals("1") ){
								tmpField.put("default", Utils.getNodeByName(value, "default"));
							}
							tmpValues.add(tmpValue);
						}
						HashMap<String, String> tmpValue = new HashMap<String, String>();
						tmpValue.put("key", "*cust0m*");
						tmpValue.put("name", Lang.get("android_custom"));
						tmpValues.add(tmpValue);

						items.put(fieldKey, tmpValues);
					} catch (Exception e) {
						//e.printStackTrace();
						tmpField.put("values", Utils.getNodeByName(field, "values"));
					}
				}
				else {
					tmpField.put("values", Utils.getNodeByName(field, "values"));
				}
			}

			if(fieldKey.equals("escort_rates")) {
				// values
				NodeList datasNode = field.getElementsByTagName("data");
				if (datasNode.item(0).hasChildNodes()) { // * because this line returns true even if the node hasn't child notes, I think CDATA is guilty
					try {
						tmpField.put("datas", "__data"); // the data for this field stored in value_data variable under related key

						ArrayList<HashMap<String, String>> tmpDatas = new ArrayList<HashMap<String, String>>();

						HashMap<String, String> field_name = new HashMap<String, String>();
						field_name.put("name", Lang.get("android_select_field").replace("{field}", tmpField.get("name")));
						field_name.put("key", "");
						tmpDatas.add(field_name);

						NodeList values = (NodeList) datasNode.item(0).getChildNodes();
						for (int j = 0; j < values.getLength(); j++) {
							HashMap<String, String> tmpValue = new HashMap<String, String>();

							Element value = (Element) values.item(j); // this line breaks the app, see comment marked * above
							tmpValue.put("key", Utils.getNodeByName(value, "key"));
							tmpValue.put("name", Utils.getNodeByName(value, "name"));
							if (Utils.getNodeByName(value, "default").equals("1")) {
								tmpField.put("default", Utils.getNodeByName(value, "default"));
							}
							tmpDatas.add(tmpValue);
						}
						items.put("rates_value", tmpDatas);
					} catch (Exception e) {
						//e.printStackTrace();
//						tmpField.put("values", Utils.getNodeByName(field, "values"));
					}
				}
			}
			
			out.put(fieldKey, tmpField);
		}
		
		return out;
	}

	private static void parseAvailability(HashMap<String, ArrayList<HashMap<String, String>>> items, HashMap<String, String> tmpField, String jsonItems) {
		try {
			if (JSONParser.isJson(jsonItems)) {
				tmpField.put("values", "__data"); // the data for this field stored in value_data variable under related key

				JSONObject jsObj = new JSONObject( jsonItems );

				if(!jsObj.isNull("days")) {
					ArrayList<HashMap<String, String>> tmpDays = new ArrayList<HashMap<String, String>>();
					tmpDays = JSONParser.parseJsontoArrayList(jsObj.getString("days"));
					items.put("availability_days", tmpDays);
				}

				if(!jsObj.isNull("time_range")) {
					ArrayList<HashMap<String, String>> tmpRange = new ArrayList<HashMap<String, String>>();
					tmpRange = JSONParser.parseJsontoArrayList(jsObj.getString("time_range"));

					HashMap<String, String> field_name = new HashMap<String, String>();
					field_name.put("name", "N/A");
					field_name.put("key", "");
					tmpRange.add(0, field_name);
					items.put("availability_time_range", tmpRange);
				}
			}
			else {
				tmpField.put("values", jsonItems);
			}
		} catch (Exception e) {
		}
	}

	private static void parseEscortRates(HashMap<String, ArrayList<HashMap<String, String>>> items, HashMap<String, String> tmpField, String jsonItems) {
		try {
			if (JSONParser.isJson(jsonItems)) {
				tmpField.put("values", "__data"); // the data for this field stored in value_data variable under related key

				JSONObject jsObj = new JSONObject( jsonItems );

				if(!jsObj.isNull("values")) {
					ArrayList<HashMap<String, String>> tmp = new ArrayList<HashMap<String, String>>();
					tmp = JSONParser.parseJsontoArrayList(jsObj.getString("values"));

					HashMap<String, String> field_name = new HashMap<String, String>();
					field_name.put("name", "N/A");
					field_name.put("key", "");
					tmp.add(0, field_name);
					items.put(tmpField.get("key"), tmp);
				}

				if(!jsObj.isNull("currency")) {
					ArrayList<HashMap<String, String>> tmpCurrency = new ArrayList<HashMap<String, String>>();
					tmpCurrency = JSONParser.parseJsontoArrayList(jsObj.getString("currency"));
					items.put("rates_value", tmpCurrency);
				}
			}
			else {
				tmpField.put("values", jsonItems);
			}
		} catch (Exception e) {
		}
	}

	private static void parseAvailability(HashMap<String, ArrayList<HashMap<String, String>>> items, HashMap<String, String> tmpField, NodeList valuesNode) {
		if ( valuesNode.item(0).hasChildNodes() ) { // * because this line returns true even if the node hasn't child notes, I think CDATA is guilty
			try {
				tmpField.put("values", "__data"); // the data for this field stored in value_data variable under related key

				NodeList values = (NodeList) valuesNode.item(0).getChildNodes();
				for ( int j = 0; j < values.getLength(); j++ ) {

					Element value = (Element) values.item(j); // this line breaks the app, see comment marked * above
					String fieldKey = "availability_"+value.getTagName();

					ArrayList<HashMap<String, String>> tmpValues = new ArrayList<HashMap<String, String>>();

					if(value.getTagName().equals("time_range")) {
						HashMap<String, String> field_name = new HashMap<String, String>();
						field_name.put("name", "N/A");
						field_name.put("key", "");
						tmpValues.add(field_name);
					}

					NodeList itemNodes = value.getChildNodes();
					for ( int jj = 0; jj < itemNodes.getLength(); jj++ ) {
						Element item = (Element) itemNodes.item(jj);
						HashMap<String, String> tmpValue = new HashMap<String, String>();

						tmpValue.put("key", Utils.getNodeByName(item, "key"));
						tmpValue.put("name", Utils.getNodeByName(item, "name"));
						tmpValues.add(tmpValue);
					}

					items.put(fieldKey, tmpValues);
				}
			} catch (Exception e) {
				//e.printStackTrace();
//				tmpField.put("values", Utils.getNodeByName(field, "values"));
			}
		}

	}
	
	public static void requestAccountForm(String typeKey, final Context context, final LinearLayout profileFields, final LinearLayout accountFields, 
			final HashMap<String,String> formData, final HashMap<String, View> fieldViews) {
		
		/* get form data - build request url */
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put("type", typeKey);
		params.put("id", accountData.get("id"));
		final String url = Utils.buildRequestUrl("getProfileForm", params, null);

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
						Dialog.simpleWarning(Lang.get("returned_xml_failed"), Config.context);
						((Activity) context).finish();
					}
					else {
						accountFields.removeAllViews();

						NodeList fields = doc.getElementsByTagName("items").item(0).getChildNodes();
						if ( fields.getLength() > 0 ) {
							HashMap<String, ArrayList<HashMap<String, String>>> items = new HashMap<String, ArrayList<HashMap<String, String>>>();
							data = Account.parseForm(fields, items);

							Forms.buildSubmitFields(accountFields, data, formData, items, context, EditProfileActivity.actionbarSpinner, fieldViews, false);

							// add email field to the array to allow validator validate it
							if ( Utils.getCacheConfig("account_edit_email_confirmation").equals("0") ) {
								HashMap<String, String> email_field = new HashMap<String, String>();
								email_field.put("key", email_field_key);
								email_field.put("type", "text");
								email_field.put("required", "1");
								email_field.put("multilingual", "0");
								email_field.put("name", Lang.get("hint_email"));
								email_field.put("current", Account.accountData.get("mail"));
								email_field.put("data", "isEmail");
								data.put(email_field_key, email_field);
							}
						}
						else {
							TextView message = (TextView) Config.context.getLayoutInflater()
									.inflate(R.layout.info_message, null);

							message.setText(Lang.get("selected_account_type_no_fields"));
							accountFields.addView(message);
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
}