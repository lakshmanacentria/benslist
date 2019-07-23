package com.acentria.benslist.controllers;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Forms;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.SearchResultsActivity;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.viewpagerindicator.TabPageIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Search extends AbstractController {

	private static Search instance;
	private static final String Title = Lang.get("android_title_activity_search");
	
	public static int[] menuItems = {R.id.menu_settings, R.id.menu_search};
	
	private static final List<String> TAB_NAMES = new ArrayList<String>();
	private static final List<String> TAB_KEYS = new ArrayList<String>();
	
	private static TabPageIndicator indicator;
	private static int setPosition = 0;
	
	public static Search getInstance() {
		if ( instance == null ) {
			try {
				instance = new Search();
			}
			catch(Exception e) {
				Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
				e.printStackTrace();
			}
			Config.activeInstances.add(instance.getClass().getSimpleName());
		}
		else {
			Utils.restroreInstanceView(instance.getClass().getSimpleName(), Title);
			
			/* set current tab base on previous active controller */
			if ( !Config.prevView.equals("Search") ) {
				int formPosition = 0;
				for (Entry<String, HashMap<String, String>> entry : Config.cacheListingTypes.entrySet()) {
					if ( entry.getValue().get("search").equals("1") && Config.searchForms.containsKey(entry.getKey()) ) {
						if ( SwipeMenu.menuData.get(SwipeMenu.adapter.previousPosition).containsKey("key") ) {
							if ( SwipeMenu.menuData.get(SwipeMenu.adapter.previousPosition).get("key") == entry.getKey() ) {
								indicator.setCurrentItem(formPosition);
							}
						}
						formPosition++;
					}
				}
			}
		}
		
		handleMenuItems(menuItems);
		
		return instance;
	}
	
	public static void removeInstance(){
		instance = null;
		
		TAB_NAMES.clear();
		TAB_KEYS.clear();
		setPosition = 0;
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public Search(){
		
		/* set content title */
		Config.context.setTitle(Title);
		
		/* add content view */
		Utils.addContentView(R.layout.view_search);
		
		/* get related view */
		final LinearLayout layout = (LinearLayout) Config.context.getWindow().findViewById(R.id.Search);

        /* add ad sense */
        Utils.setAdsense(layout, "search");

		/* hide menu */
		Utils.showContent();
		
		/* populate tabs from config */
		int formPosition = 0;
		for (Entry<String, HashMap<String, String>> entry : Config.cacheListingTypes.entrySet()) {
			if ( entry.getValue().get("search").equals("1") && Config.searchForms.containsKey(entry.getKey()) ) {
				TAB_NAMES.add(entry.getValue().get("name"));
				TAB_KEYS.add(entry.getKey().toString());
				
				/* define active tab based on previous active controller */
				if ( SwipeMenu.menuData.get(SwipeMenu.adapter.previousPosition).containsKey("key") ) {
					if ( SwipeMenu.menuData.get(SwipeMenu.adapter.previousPosition).get("key") == entry.getKey() ) {
						setPosition = formPosition;
					}
				}
				
				formPosition++;
			}
		}
		
		if ( TAB_KEYS.size() <= 0 ) {
			layout.removeAllViews();
			
			TextView message = (TextView) Config.context.getLayoutInflater()
	    			.inflate(R.layout.info_message, null);
    		
    		message.setText(Lang.get("android_there_are_not_search"));
			layout.addView(message);
		}
		else {
			/* init adapter */
			FragmentPagerAdapter adapter = new FragmentAdapter(Config.context.getSupportFragmentManager());

	        ViewPager pager = (ViewPager)layout.findViewById(R.id.pagerSearch);
	        pager.setPageMargin(10);
	        pager.setAdapter(adapter);
			if (Lang.isRtl()) {
//				pager.setRotationY(180);
			}
	
	        indicator = (TabPageIndicator)layout.findViewById(R.id.indicator);
	        indicator.setViewPager(pager);
	        indicator.setCurrentItem(setPosition);
	        
	        if ( TAB_KEYS.size() == 1 ) {
	        	indicator.setVisibility(View.GONE);	        	
	        }
		}
	}
	
	private static void buildSearch(LinearLayout container, String key, final HashMap<String,String> formData) {
		LinearLayout layout = (LinearLayout) container.findViewById(R.id.fields_area);
		
    	Forms.buildSearchFields(layout, key, Config.searchForms.get(key), formData, Config.searchFieldItems, Config.context);
    }
	
	class FragmentAdapter extends FragmentPagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	return SearchFragment.newInstance(TAB_KEYS.get(position % TAB_KEYS.size()));
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	return TAB_NAMES.get(position % TAB_NAMES.size()).replace(" ", "\u00A0").toUpperCase(); // converting to non-breaking space
        }

        @Override
        public int getCount() {
        	return TAB_NAMES.size();
        }
        
        @Override
        public void destroyItem(View container, int position, Object object) {
        }
    }
	
	public static class SearchFragment extends Fragment {
	    
	    private String tabKey = "";
	    private HashMap<String,String> formData = new HashMap<String,String>();

	    public static SearchFragment newInstance(String key) {
	    	SearchFragment fragment = new SearchFragment();
	    	fragment.tabKey = key.toString();
	        return fragment;
	    }

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	    }

	    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
	    	final LinearLayout layout = (LinearLayout) Config.context.getLayoutInflater()
		    		.inflate(R.layout.form, null);

	        buildSearch(layout, tabKey, formData);
	        
	        /* submit form listener */
	        Button searchButton = (Button) layout.findViewById(R.id.form_submit);
	        searchButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
                   if ( formData.size() > 0 ) {
		            	Intent intent = new Intent(Config.context, SearchResultsActivity.class);
		    			intent.putExtra("data", formData);
		    			intent.putExtra("type", tabKey);
		    			Config.context.startActivity(intent);
	            	}
	            	else {
	            		Dialog.simpleWarning(Lang.get("android_dialog_empty_search_warning"));
	            	}
	            }
	        });
	        
	        /* reset form listener */
	        Button resetButton = (Button) layout.findViewById(R.id.form_reset);
	        resetButton.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	if ( formData.size() > 0 ) {
	            		LinearLayout fieldsArea = (LinearLayout) layout.findViewById(R.id.fields_area);
	            		fieldsArea.removeAllViews();
	            		formData.clear();
	            		
	            		buildSearch(layout, tabKey, formData);
	            	}
	            }
	        });

			if (Lang.isRtl()) {
//				layout.setRotationY(180);
			}
	        
	        return layout;
	    }

	    @Override
	    public void onSaveInstanceState(Bundle outState) {
	    	super.onSaveInstanceState(outState);
	    }
	}
}