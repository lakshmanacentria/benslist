package com.acentria.benslist;


import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

import com.acentria.benslist.adapters.KeywordSearchAdapter;
import com.acentria.benslist.adapters.SwipeMenuAdapter;
import com.acentria.benslist.controllers.SavedSearch;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class SwipeMenu extends SlidingActivity {
	
	public static ArrayList<HashMap<String, String>> menuData;
	public static ListView menuListView;
	public static SwipeMenuAdapter adapter;
	
	public static int loginIndex = 2; // maybe weak idea but easy to handle
	public static int favoriteIndex = 0;
	public static int accountItems = 0; // the number of menu items related to the account area
	
	final public static String CON = "controller";
	final public static String ACT = "activity";
	final public static String AA = "account-area-item";
	
	public static AutoCompleteTextView keywordSearchField;
	
	/**
	 * Menu object
	 */
	public static SlidingMenu menu;

	public SwipeMenu(){
		// configure the SlidingMenu
		//Config.context.setBehindContentView(R.layout.menu); // already set in FlynDroid

		//menu = new SlidingMenu(Config.context);
		if (Lang.isRtl()) {
			menu.setMode(SlidingMenu.RIGHT);
		}
		menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
		menu.setShadowWidth(25);
		menu.setShadowDrawable(Lang.isRtl() ? R.drawable.shadow_right : R.drawable.shadow);
		//menu.setBehindOffset(150);
		menu.setBehindWidth(SwipeMenu.getBehindOfset());
		//menu.setBehindOffsetRes();
		menu.setFadeDegree(0.35f);
		//menu.attachToActivity(Config.context, SlidingMenu.SLIDING_WINDOW);
		//menu.setMenu(R.layout.menu);

		/* hide keyboard on menu close */
		keywordSearchField = (AutoCompleteTextView)Config.context.findViewById(R.id.sm_keyword_search);
		menu.setOnClosedListener(new SlidingMenu.OnClosedListener(){
			@Override
			public void onClosed() {
    			Utils.hideKeyboard(keywordSearchField);
			}
		});

		// populate menu list view
		populateMenu();
	}

	public static void populateMenu(){
		menuData = new ArrayList<HashMap<String, String>>();

		/* colculate favorites count */
		String favoriteCount = null;
		
		if ( Utils.getSPConfig("favoriteIDs", "").contains(",") ) {
			favoriteCount = "" + Utils.getSPConfig("favoriteIDs", "").split(",").length;
		}
		else if ( !Utils.getSPConfig("favoriteIDs", "").isEmpty() ) {
			favoriteCount = "1";
		}

		/* calcualte new listings count */
		String messagesCount = null;
		if (Account.loggedIn && Account.accountData.get("new_messages") != null) {
			messagesCount = Integer.parseInt(Account.accountData.get("new_messages")) > 0 ? Account.accountData.get("new_messages") : null;
		}

		/* calculate new listings count */
		String newListingsCount = null;
		if ( !Utils.getCacheConfig("countNewListingsData").equals("empty")
				&& Integer.parseInt(Utils.getCacheConfig("countNewListingsData")) > 0 ) {
			newListingsCount = Utils.getCacheConfig("countNewListingsData");
			newListingsCount = Integer.parseInt(newListingsCount) > 99 ? "99+" : newListingsCount;
		}

		/* calcualte new saved search count */
		String savedSearchCount = null;
		if (Account.loggedIn && SavedSearch.getSSPreference(2, "count") != null && !SavedSearch.getSSPreference(2, "count").isEmpty()) {
			savedSearchCount = Integer.parseInt(SavedSearch.getSSPreference(2, "count")) > 0 ? SavedSearch.getSSPreference(2, "count") : null;
		}
		
		String login_phrase = Account.loggedIn ? Account.accountData.get("full_name") : Lang.get("android_menu_login");
        String home_icon = Utils.getConfig("customer_domain").isEmpty() ? "sm_item_fl" : "sm_item_home";
		
        // populate menu (system items)
        String[][] menu = new String[][] {
        	new String[] {CON, null, Lang.get("android_title_activity_home"), home_icon, "Home", null},
        	new String[] {CON, null, Lang.get("android_menu_user_area_caption"), "divider", null, null},
        	new String[] {CON, null, login_phrase, "sm_item_user", "AccountArea", null},
        	new String[] {ACT, null, Lang.get("title_activity_add_listing"), "sm_item_add_listing", "AddListingActivity", null},
        	new String[] {CON, AA, Lang.get("title_activity_my_listings"), "sm_item_my_listings", "MyListings", null},
        	new String[] {CON, AA, Lang.get("title_activity_my_packages"), "sm_item_packages", "MyPackages", null},
        	new String[] {CON, AA, Lang.get("title_activity_my_messages"), "sm_item_my_messages", "MyMessages", messagesCount},
        	new String[] {CON, AA, Lang.get("title_activity_saved_search"), "sm_item_saved_search", "SavedSearch", savedSearchCount},
        	new String[] {CON, null, Lang.get("android_title_activity_favorites"), "sm_item_favorites", "Favorites", favoriteCount},
        	new String[] {CON, null, Lang.get("android_menu_browse_caption"), "divider", null, null},
        	new String[] {CON, null, Lang.get("android_title_activity_recently_added"), "sm_item_recently_added", "RecentlyAdded", newListingsCount},
        	new String[] {ACT, null, Lang.get("android_title_activity_search_around"), "sm_item_sbd", "SearchAroundActivity", null},
        };
        
        for (int i = 0; i < menu.length; i++) {
            // creating new HashMap
            HashMap<String, String> map = new HashMap<String, String>();

            map.put("type", menu[i][0]);
            map.put("account", menu[i][1]);
            map.put("name", menu[i][2]);
            map.put("icon", menu[i][3]);
            map.put("controller", menu[i][4]);
            map.put("count", menu[i][5]);
 
            menuData.add(map);
            
            // increase counter
            if ( menu[i][1] != null ) {
            	accountItems++;
            }
            
            // set favorites index
            if ( menu[i][4] != null && menu[i][4].equals("Favorites") ) {
            	favoriteIndex = i;
            }
        }
        
        /* add listing types to the menu */
        if ( Config.cacheListingTypes.size() > 0 ) {
        	ArrayList<String> listingTypeIcons = (ArrayList<String>) Utils.string2list(Config.context.getResources().getStringArray(R.array.list_listing_types_icons));
        	
	        for (Entry<String, HashMap<String, String>> entry : Config.cacheListingTypes.entrySet()) {
//				if ( entry.getValue().get("page").equals("1") ) {
					String setIcon = listingTypeIcons.indexOf(entry.getValue().get("icon")) >= 0 
							? "sm_item_"+ entry.getValue().get("icon")
							: "sm_item_default";
							
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("type", CON);
					map.put("account", null);
					map.put("name", entry.getValue().get("name"));
					map.put("icon", setIcon);
					map.put("controller", "ListingType");
					map.put("key", entry.getKey());	
					map.put("count", null);
					menuData.add(map);
//				}
	        }
		}
        
        /* add account types to the menu */
        if ( Config.cacheAccountTypes.size() > 0 ) {
        	HashMap<String, String> divider = new HashMap<String, String>();
        	divider.put("type", CON);
        	divider.put("account", null);
        	divider.put("name", Lang.get("android_menu_accounts_caption"));
        	divider.put("icon", "divider");
        	divider.put("controller", null);
        	divider.put("count", null);
			menuData.add(divider);
			
			for (Entry<String, HashMap<String, String>> entry : Config.cacheAccountTypes.entrySet()) {
				if ( !entry.getValue().get("page").equals("1") )
					continue;
				
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("type", CON);
				map.put("account", null);
				map.put("name", entry.getValue().get("name"));
				map.put("icon", "sm_item_dealers");
				map.put("controller", "AccountType");
				map.put("key", entry.getKey());	
				map.put("count", null);
				menuData.add(map);
	        }
        }

        menuListView = (ListView) Config.context.findViewById(R.id.sm_menu_view);
 
        adapter = new SwipeMenuAdapter(menuData);
        menuListView.setAdapter(adapter);
        menuListView.setOnItemClickListener(adapter);
        
        /* set hint to keyword search */
        keywordSearchField.setHint(Lang.get("android_keyword_search"));
		if (Lang.isRtl()) {
			keywordSearchField.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		}

        /* handle autocomplete search */
        final KeywordSearchAdapter adapter = new KeywordSearchAdapter(Config.context);
        adapter.setNotifyOnChange(true);
        keywordSearchField.setAdapter(adapter);
        keywordSearchField.setOnItemClickListener(adapter);
        keywordSearchField.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable query) {
				adapter.timerRetrieveResults(query.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        keywordSearchField.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				keywordSearchField.showDropDown();
			}
		});
	}
	
	public static void performClick(int position) {
		menuListView.setSelection(position);
		menuListView.performItemClick(adapter.getView(position, null, null), position, position);
	}

	public static void changeBehindOffset(){
		SwipeMenu.menu.setBehindWidth(SwipeMenu.getBehindOfset());
	}

	public static int getBehindOfset(){
		DisplayMetrics displaymetrics = new DisplayMetrics();
		Config.context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		Display display = ((WindowManager) Config.context.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        int workWidth = display.getOrientation() == 0
        		? displaymetrics.widthPixels
        		: displaymetrics.heightPixels;
        int behindOffset = (int) (workWidth * 0.8);
        
		return behindOffset;
	}
}
