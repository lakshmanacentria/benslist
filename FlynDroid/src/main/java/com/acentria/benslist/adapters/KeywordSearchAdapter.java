package com.acentria.benslist.adapters;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.ListingDetailsActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

public class KeywordSearchAdapter extends ArrayAdapter<String> implements OnItemClickListener {

	private static ArrayList<HashMap<String, String>> items;
	private static CountDownTimer timer = null;
	static KeywordSearchAdapter self;
	private static String lastRequest = "";
	
	static AsyncHttpClient client;
	
	public KeywordSearchAdapter(Context context) {
		super(context, R.layout.keyword_search_item);
		items = new ArrayList<HashMap<String, String>>();
		self = this;
	}
	
	@Override
    public int getCount() {
        return items.size();
    }
	
	@Override
	public String getItem(int index) {
	    return items.get(index).get("listing_title");
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View item;
		if ( convertView == null ) {
			item = Config.context.getLayoutInflater()
				.inflate(R.layout.keyword_search_item, parent, false);
		}
		else {
			item = convertView;
		}
		
		TextView label = (TextView) item.findViewById(R.id.name);
		
		Pattern pattern = Pattern.compile("("+lastRequest.replace(" ", "|")+"?)(?=[^>]*(<|$))", Pattern.CASE_INSENSITIVE);
		Matcher m = pattern.matcher(items.get(position).get("listing_title"));
		label.setText(Html.fromHtml(m.replaceAll("<b color=\"red\">$1</b>")));
		
		return item;
	}
	
	public void timerRetrieveResults(final String query) {
		/* clear timeout */
		if ( timer != null ) {
			timer.cancel();
		}
		
		/* set timeout 1 second */
		timer = new CountDownTimer(1000, 1000) {
			public void onTick(long millisUntilFinished) {}
		
			public void onFinish() {
				retrieveResults(query);
			}
		};
		timer.start();
	}
	
	public void retrieveResults(String query) {
		query = query.trim();
		
		if ( !lastRequest.equals(query) && !query.isEmpty()) {
			lastRequest = query;
			
			if ( client != null ) {
				client.cancelRequests(Config.context, true);
			}
			
			/* clear data */
			items.clear();
			
			/* build request url */
	    	HashMap<String, String> params = new HashMap<String, String>();
			params.put("query", query);
			final String url = Utils.buildRequestUrl("keywordSearch", params, null);
	
			client = new AsyncHttpClient();
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

							HashMap<String, String> tmpFields;

							/* populate list */
							if ( listings.getLength() > 0 )	{
								for( int i=0; i<listings.getLength(); i++ ) {
									Element listing = (Element) listings.item(i);

									//NodeList fields = listing.getChildNodes();
									tmpFields = new HashMap<String, String>();//clear steck

									/* convert fields from nodes to array */
									tmpFields.put("id", Utils.getNodeByName(listing, "id"));
									tmpFields.put("photo", Utils.getNodeByName(listing, "main_photo"));
									tmpFields.put("listing_title", Utils.getNodeByName(listing, "listing_title").isEmpty() ? "No listing title defined" : Utils.getNodeByName(listing, "listing_title"));
									items.add(tmpFields);
								}

								self.notifyDataSetChanged();
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

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
		Intent intent = new Intent(Config.context, ListingDetailsActivity.class);
		intent.putExtra("id", items.get(position).get("id"));
		Config.context.startActivity(intent);
		
		SwipeMenu.keywordSearchField.setText(lastRequest);
		SwipeMenu.keywordSearchField.setSelection(lastRequest.length());
	}
}