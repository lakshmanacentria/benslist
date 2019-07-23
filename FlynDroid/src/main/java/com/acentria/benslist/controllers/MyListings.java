package com.acentria.benslist.controllers;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.fragments.MyListingsFragment;
import com.viewpagerindicator.TabPageIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class MyListings extends AbstractController {

	private static MyListings instance;
	private static final String Title = Lang.get("title_activity_my_listings");

	public static int[] menuItems = {};
	
	private static final ArrayList<String> TAB_NAMES = new ArrayList<String>();
	public static final ArrayList<String> TAB_KEYS = new ArrayList<String>();
	
	public static FragmentPagerAdapter adapter;
	public static ViewPager pager;
	
	public static MyListings getInstance() {
		if ( instance == null ) {
			try {
				instance = new MyListings();
			}
			catch (Exception e) {
				Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
                e.printStackTrace();
			}
			Config.activeInstances.add(instance.getClass().getSimpleName());
		}
		else {
			Utils.restroreInstanceView(instance.getClass().getSimpleName(), Title);
		}
		
		handleMenuItems(menuItems);
		
		return instance;
	}
	
	public static void removeInstance(){
		// remove saved fragments


		FragmentManager manager = Config.context.getSupportFragmentManager();
		FragmentTransaction trans = manager.beginTransaction();

		for ( int i = 0; i < TAB_NAMES.size(); i++ ) {
			Fragment frg = Utils.findFragmentByPosition(i, Config.context, pager, adapter);
			if ( frg != null ) {
				trans.remove(frg);
			}
		}

		pager.removeAllViews();
		pager = null;
		
		adapter = null;
		
		TAB_NAMES.clear();
		TAB_KEYS.clear();
		
		instance = null;

		trans.commitAllowingStateLoss();
		manager.popBackStack();
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public MyListings() {
		
		/* set content title */
		Config.context.setTitle(Title);
	
		/* add content view */
		Utils.addContentView(R.layout.view_my_listings);
		
		/* get related view */
		final LinearLayout layout = (LinearLayout) Config.context.findViewById(R.id.MyListings);
		
		/* hide menu */
		Utils.showContent();

		/* populate tabs from config */
		for (Entry<String, HashMap<String, String>> entry : Config.cacheListingTypes.entrySet()) {
			if ( Account.abilities.indexOf(entry.getKey()) >= 0 ) {
				TAB_NAMES.add(entry.getValue().get("name"));
				TAB_KEYS.add(entry.getKey().toString());
			}
		}

		if ( TAB_KEYS.size() <= 0 ) {
			layout.removeAllViews();
			
			TextView message = (TextView) Config.context.getLayoutInflater()
	    			.inflate(R.layout.info_message, null);
    		
    		message.setText(Lang.get("android_there_are_not_listing_types"));
    		layout.setGravity(Gravity.CENTER);
			layout.addView(message);
		}
		else {
			/* init adapter */
			adapter = new FragmentAdapter(Config.context.getSupportFragmentManager());
	
	        pager = (ViewPager) layout.findViewById(R.id.my_listings_pager);
	        pager.setPageMargin(10);
	        pager.setAdapter(adapter);
//	        pager.setOffscreenPageLimit(5);


	        TabPageIndicator indicator = (TabPageIndicator) layout.findViewById(R.id.my_listings_indicator);
	        indicator.setViewPager(pager);
	        
	    	if ( TAB_KEYS.size() == 1 ) {
	        	indicator.setVisibility(View.GONE);	        	
	        }
	    	
	    	// set current tab by request
	    	if ( Utils.getTabRequest("MyListings") != null ) {
	        	int index = TAB_KEYS.indexOf(Utils.getTabRequest("MyListings"));
	        	if ( index > 0 ) {
	        		pager.setCurrentItem(index);
	        		Utils.clearTab();
	        	}
	        }
		}
	}
	
	class FragmentAdapter extends FragmentPagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment my_fragment = new MyListingsFragment();
            Bundle args = new Bundle();
            args.putString("key", TAB_KEYS.get(position));
            my_fragment.setArguments(args);
        	return my_fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	return TAB_NAMES.get(position).replace(" ", "\u00A0").toUpperCase(); // converting to non-break space
        }

        @Override
        public int getCount() {
        	return TAB_NAMES.size();
        }
    }
	
	public static void manageItem(String mode, Integer listing_id, HashMap<String, String> listing) {
		int position = -1;

        Log.d("FD - manageItem", listing.toString());

		String key = listing.get("listing_type");
		
		if ( key == null )
			return;

        // update account statistics section (it's a good place for this because manageItem() calls every time listings stats changed)
        Account.updateStat();

		// add listing to my listings grid
		listing.put("photo_allowed", Config.cacheListingTypes.get(key).get("photo"));//photo allowed by listing type
//		listing.put("page_allowed", Config.cacheListingTypes.get(key).get("page"));//own page allowed by listing type
		
		for ( int i = 0; i < TAB_KEYS.size(); i++ ) {
			if ( TAB_KEYS.get(i).equals(key) ) {
				position = i;
				break;
			}
		}

		if ( position >= 0 && Config.activeInstances.contains("MyListings") ) {
			try {
				MyListingsFragment fragment = (MyListingsFragment) Utils.findFragmentByPosition(position, null, pager, adapter);
				if ( mode.equals("add") ) {
					fragment.adapter.addFirst(listing);
				}
				else if ( mode.equals("update") ) {
					fragment.adapter.updateOne(listing_id+"", listing);
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			
			pager.setCurrentItem(position);
		}
	}
	
	public static void addItem(HashMap<String, String> listing) {
		manageItem("add", null, listing);
	}
	
	public static void updateItem(Integer listing_id, HashMap<String, String> listing) {
		manageItem("update", listing_id, listing);
	}
}