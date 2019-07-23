package com.acentria.benslist.controllers;

import android.view.Gravity;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.acentria.benslist.Categories;
import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.adapters.CategoryAdapter;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

/**
 * ListingType controller differs from the other controllers because it doesb't keep an instance
 * and re-create it each time other listing type called from SwipeMenu
 * 
 * @author Freeman
 *
 */
public class ListingType extends AbstractController {

	private static ListingType instance;
	private static String Title;
	
	public static int requestSteck = 1; //steck number (pagination)
	public static int requestTotal = 0; //total availalbe listings
	public static CategoryAdapter CategoryAdapter;
	
	public static int[] menuItems = {R.id.menu_settings, R.id.menu_search};
	
	public static ListingType getInstance() {
		if ( instance == null ) {
			try {
				instance = new ListingType(false);
			}
			catch(Exception e) {
				Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
			}
			Config.activeInstances.add(instance.getClass().getSimpleName());
		}
		else {
			if ( SwipeMenu.adapter.currentPosition == SwipeMenu.adapter.previousPosition ) {
				SwipeMenu.menu.showContent();
			}
			else {
				/* remove exists instance */
				removeInstance();
				
				/* start new instance */
				instance = new ListingType(true);
				
				Utils.restroreInstanceView(instance.getClass().getSimpleName(), Title);
			}
		}
		
		handleMenuItems(menuItems);
		
		return instance;
	}
	
	public static void removeInstance(){
		instance = null;
		
		requestSteck = 1;
		requestTotal = 0;
	}
	
	public ListingType(boolean clear){
		int itemIndex = SwipeMenu.adapter.currentPosition;
		Title = SwipeMenu.menuData.get(itemIndex).get("name");
		
		final String listingType = SwipeMenu.menuData.get(itemIndex).get("key");
		
		/* set content title */
		Config.context.setTitle(Title);
		
		/* clear content view */
		if ( clear ) {
            LinearLayout currentView = (LinearLayout) Config.context.findViewById(R.id.ListingType);
			currentView.removeAllViews();
		}
		/* add content view */
		else {
			Utils.addContentView(R.layout.view_listing_type);
		}
		
        LinearLayout container = (LinearLayout) Config.context.getWindow().findViewById(R.id.ListingType);

		/* get LinearLayout view */
        final LinearLayout layout = new LinearLayout(Config.context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
		layout.setGravity(Gravity.CENTER);
        layout.setOrientation(LinearLayout.VERTICAL);
		
		/* add progress bar */
		ProgressBar progressBar = (ProgressBar) Config.context.getLayoutInflater()
    			.inflate(R.layout.loading, null);
		layout.addView(progressBar);
        container.addView(layout);
		
		/* build request url */
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put("sleep", "1");
		params.put("type", listingType);
		params.put("parent", "0");
		final String url = Utils.buildRequestUrl("getCategories", params, null);
		
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

					layout.removeViewAt(0);

					if ( doc == null ) {
						TextView message = (TextView) Config.context.getLayoutInflater()
								.inflate(R.layout.info_message, null);

						message.setText(Lang.get("returned_xml_failed"));
						layout.addView(message);
					}
					else {
						NodeList categoryNode = doc.getElementsByTagName("items");
						NodeList categoryNodes = categoryNode.item(0).getChildNodes();

						/* populate list */
						if ( categoryNodes.getLength() > 0 )
						{
							layout.setGravity(Gravity.TOP);

							/* create list view of listings */
							GridView gridView = (GridView) Config.context.getLayoutInflater()
									.inflate(R.layout.categories_grid, null);

							LayoutParams params = new LayoutParams(
									LayoutParams.MATCH_PARENT,
									LayoutParams.MATCH_PARENT
							);
							gridView.setLayoutParams(params);

							CategoryAdapter = new CategoryAdapter(Categories.parse(categoryNodes), listingType);
							gridView.setAdapter(CategoryAdapter);
							gridView.setOnItemClickListener(CategoryAdapter);

							layout.addView(gridView);
						}
						/* display no categories message */
						else
						{
							TextView message = (TextView) Config.context.getLayoutInflater()
									.inflate(R.layout.info_message, null);

							message.setText(Lang.get("android_there_are_no_categories"));
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

		/* add adsense */
        Utils.setAdsense(container, "category");

		/* hide menu */
		Utils.showContent();
	}
}