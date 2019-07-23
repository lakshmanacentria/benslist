package com.acentria.benslist.controllers;


import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Forms;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.SearchAccountActivity;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.adapters.AccountItemAdapter;
import com.acentria.benslist.adapters.ActionbarSpinnerAdapter;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.viewpagerindicator.TabPageIndicator;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

/**
 * AccountType controller differs from the other controllers because it doesb't keep an instance
 * and re-create it each time other account type called from SwipeMenu
 * 
 * @author Freeman
 *
 */
public class AccountType extends AbstractController {

	private static AccountType instance;
	private static String Title;
	
	public static int requestSteck = 1; //steck number (pagination)
	public static int requestTotal = 0; //total availalbe listings
	public static boolean liadingInProgress = false;
	
	private static TabPageIndicator indicator;
	private ArrayList<HashMap<String, String>> TABS;
	
	private static MenuItem menuItem;
	private static ArrayList<String> alphabet;
	private static int filterPosition = 0;
	private static boolean allowListener = false;
	
	private static HashMap<String,String> formData = new HashMap<String,String>();
	
	private static LinearLayout listViewCont;
	private static LinearLayout loadingCont;
	private static View footerView;
	private static ListView listView;
	private static OnScrollListener onScrollListener;
	
	private static String accountType;
	public static AccountItemAdapter AccountAdapter;
	
	public static int[] menuItems = {R.id.menu_settings, R.id.menu_account_alphabet};
	
	public static AccountType getInstance() {
		if ( instance == null ) {
			try {
				instance = new AccountType(false);
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
				instance = new AccountType(true);
				
				Utils.restroreInstanceView(instance.getClass().getSimpleName(), Title);
			}
		}
		
		handleMenuItems(menuItems);
		
		return instance;
	}
	
	public static void removeInstance(){
		requestSteck = 1;
		requestTotal = 0;
		liadingInProgress = false;
		instance = null;
		allowListener = false;
		
		formData.clear();
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public AccountType(boolean clear){
		int itemIndex = SwipeMenu.adapter.currentPosition;
		Title = SwipeMenu.menuData.get(itemIndex).get("name");
		
		accountType = SwipeMenu.menuData.get(itemIndex).get("key");
		
		/* set content title */
		Config.context.setTitle(Title);

		final LinearLayout layout;

		/* clear content view */
		if ( clear ) {
			layout = (LinearLayout) Config.context.getWindow().findViewById(R.id.AccountType);
		}
		/* add content view */
		else {
			Utils.addContentView(R.layout.view_account_type);
			layout = (LinearLayout) Config.context.getWindow().findViewById(R.id.AccountType);
		}

        /* add ad sense */
		if (!Config.activeInstances.contains("AccountType")) {
       		Utils.setAdsense(layout, "account_type");
		}

		/* pupulate alphabet spinner */
		menuItemHandler();

		/* populate tabs */
		TABS = new ArrayList<HashMap<String, String>>();

		HashMap<String, String> tab = new HashMap<String, String>();
		tab.put("key", "alphabetic");
		tab.put("name", Lang.get("android_tab_alphabetic_search"));
		TABS.add(tab);

		if ( Config.searchFormsAccount.containsKey(accountType) ) {
			tab = new HashMap<String, String>();
			tab.put("key", "advanced");
			tab.put("name", Lang.get("android_tab_advanced_search"));
			TABS.add(tab);
		}

		/* init adapter */
		FragmentPagerAdapter adapter = new FragmentAdapter(Config.context.getSupportFragmentManager());

        ViewPager pager = (ViewPager) layout.findViewById(R.id.pagerAccount);
        pager.destroyDrawingCache();
        pager.refreshDrawableState();
        pager.setPageMargin(10);
        pager.setAdapter(adapter);


        indicator = (TabPageIndicator) layout.findViewById(R.id.indicator);
        indicator.setViewPager(pager);
        indicator.setCurrentItem(0);
        indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(final int position) {
            	/* set timeout 0.5 second */
            	CountDownTimer timer = new CountDownTimer(450, 450) {
					public void onTick(long millisUntilFinished) {}

					public void onFinish() {
						if ( position == 1 ) {
		                	menuItem.setVisible(false);
		                }
		                else {
		                	menuItem.setVisible(true);
		                }
					}
				};
				timer.start();
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        if ( !Config.searchFormsAccount.containsKey(accountType) ) {
        	indicator.setVisibility(View.GONE);
        }
        else {
			indicator.setVisibility(View.VISIBLE);
		}
		indicator.notifyDataSetChanged();

		/* hide menu */
		Utils.showContent();
	}
	
	public static void prepareAlphabeticTab(final LinearLayout layout) {
		LinearLayout containerView = (LinearLayout) Config.context.getLayoutInflater()
    			.inflate(R.layout.account_type_alphabetic, null);

		containerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		layout.addView(containerView);

		listViewCont = (LinearLayout) containerView.findViewById(R.id.list_view_cont);
		loadingCont = (LinearLayout) containerView.findViewById(R.id.loading_cont);

    	getAccountsByChar(true);
	}

	public static void buildSearch( LinearLayout layout ) {
		LinearLayout containerView = (LinearLayout) Config.context.getLayoutInflater()
    			.inflate(R.layout.form, null);

		final LinearLayout fieldsArea = (LinearLayout) containerView.findViewById(R.id.fields_area);

		containerView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		layout.addView(containerView);

    	Forms.buildSearchFields(fieldsArea, accountType, Config.searchFormsAccount.get(accountType),
    			formData, Config.searchFieldItemsAccount, Config.context);

    	/* submit form listener */
        Button searchButton = (Button) layout.findViewById(R.id.form_submit);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if ( formData.size() > 0 ) {
	            	Intent intent = new Intent(Config.context, SearchAccountActivity.class);
	    			intent.putExtra("data", formData);
	    			intent.putExtra("type", accountType);
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
            		fieldsArea.removeAllViews();
            		formData.clear();

            		Forms.buildSearchFields(fieldsArea, accountType, Config.searchFormsAccount.get(accountType),
                			formData, Config.searchFieldItemsAccount, Config.context);
            	}
            }
        });
	}

	public static void footerViewHandler() {
		int grid_accounts_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
		int rest_accounts = requestTotal - grid_accounts_number;

		if ( rest_accounts > 0 ) {
			/* create footer view for lisings list view */
			int preloadView = R.layout.list_view_footer_button;
			String buttonPhraseKey = "android_load_next_number_accounts";

			if ( Utils.getSPConfig("preload_method", null).equals("scroll") ) {
				preloadView = R.layout.list_view_footer_loading;
				buttonPhraseKey = "android_loading_next_number_accounts";
			}

			footerView = (View) Config.context.getLayoutInflater()
	    			.inflate(preloadView, null);

	    	final Button preloadButton = (Button) footerView.findViewById(R.id.preload_button);
	    	int set_rest_accounts = rest_accounts >= grid_accounts_number ? grid_accounts_number : rest_accounts;
	    	String buttonPhrase = Lang.get(buttonPhraseKey)
	    			.replace("{number}", ""+set_rest_accounts);
	    	preloadButton.setText(buttonPhrase);

	    	/* preload button listener */
	    	if ( Utils.getSPConfig("preload_method", null).equals("button") ) {
		    	preloadButton.setOnClickListener(new View.OnClickListener() {
		            public void onClick(View v) {
		            	requestSteck += 1;
		            	preloadButton.setText(Lang.get("android_loading"));
		            	loadNextStack(preloadButton);
		            }
		        });
	    	}
	    	/* on scroll listener */
	    	else {
		    	onScrollListener = new OnScrollListener() {
		    		@Override
		    		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		    			if ( !liadingInProgress
		    					&& firstVisibleItem + visibleItemCount == totalItemCount
		    					&& requestTotal > totalItemCount ) {
		    				liadingInProgress = true;
		    				requestSteck += 1;
		    				loadNextStack(preloadButton);
		    			}
		    		}

					@Override
					public void onScrollStateChanged(AbsListView view, int scrollState) { }
				};
	    	}

	    	listView.addFooterView(footerView);
		}
	}

	public static void loadNextStack(final Button preloadButton) {
		HashMap<String, String> params = new HashMap<String, String>();
		if ( filterPosition > 0 ) {
    		params.put("char", alphabet.get(filterPosition));
    	}
		params.put("start", ""+requestSteck);
		params.put("type", accountType);
		final String url = Utils.buildRequestUrl("getAccounts", params, null);

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
						NodeList itemsNode = doc.getElementsByTagName("items");
						NodeList accountNodes = itemsNode.item(0).getChildNodes();

						/* populate list */
						if ( accountNodes.getLength() > 0 )
						{
							AccountAdapter.add(Account.prepareGridAccount(accountNodes, accountType, null));
							requestTotal = Account.lastRequestTotalAccounts;
							liadingInProgress = false;
						}

	    				/* update button text */
						int grid_accounts_number = Integer.parseInt(Utils.getCacheConfig("android_grid_listings_number"));
						int rest_accounts = requestTotal - (grid_accounts_number * requestSteck);
						if ( rest_accounts > 0 )
						{
							int set_rest_accounts = rest_accounts >= grid_accounts_number ? grid_accounts_number : rest_accounts;
							String buttonPhrase = Lang.get("android_load_next_number_accounts")
									.replace("{number}", ""+set_rest_accounts);
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

	public static void getAccountsByChar(boolean first) {
		requestSteck = 1;
		requestTotal = 0;
		liadingInProgress = false;

		/* show progress bar */
		ProgressBar progressBar = (ProgressBar) Config.context.getLayoutInflater()
    			.inflate(R.layout.loading, null);

		progressBar.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		loadingCont.removeAllViews();
		loadingCont.setGravity(Gravity.CENTER);
		loadingCont.addView(progressBar);

		/* change visible containers */
		loadingCont.setVisibility(View.VISIBLE);
		listViewCont.setVisibility(View.GONE);

		/* build request url */
    	HashMap<String, String> params = new HashMap<String, String>();
    	if ( filterPosition > 0 ) {
    		params.put("char", alphabet.get(filterPosition));
    	}
    	if ( first ) {
    		params.put("sleep", "1");
    	}
		params.put("type", accountType);
		final String url = Utils.buildRequestUrl("getAccounts", params, null);

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
						NodeList itemsNode = doc.getElementsByTagName("items");
						NodeList accountNodes = itemsNode.item(0).getChildNodes();

						/* populate list */
						if ( accountNodes.getLength() > 0 ) {
							/* inflate list view */
							AccountAdapter = new AccountItemAdapter(Account.prepareGridAccount(accountNodes, accountType, null));
							requestTotal = Account.lastRequestTotalAccounts;

							/* create list view of accounts */
							listView = (ListView) Config.context.getLayoutInflater()
									.inflate(R.layout.listing_list_view, null);

							footerViewHandler();

							listView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
							listView.setAdapter(AccountAdapter);
							listView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));
							listView.setOnItemClickListener(AccountAdapter);

							listViewCont.removeAllViews();
							listViewCont.addView(listView);

							/* change visible containers */
							loadingCont.setVisibility(View.GONE);
							listViewCont.setVisibility(View.VISIBLE);
						}
						else {
							TextView message = (TextView) Config.context.getLayoutInflater()
									.inflate(R.layout.info_message, null);

							message.setText(filterPosition > 0
									? Lang.get("android_there_are_no_accounts_by_char").replace("{char}", alphabet.get(filterPosition))
									: Lang.get("android_there_are_no_accounts"));

							loadingCont.removeAllViews();
							loadingCont.setGravity(Gravity.CENTER);
							loadingCont.addView(message);
						}

						if ( !allowListener ) {
							allowListener = true;
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
	
	public static void menuItemHandler() {
		menuItem = (MenuItem) Config.menu.findItem(R.id.menu_account_alphabet);
		Spinner menuItemSpinner = (Spinner) menuItem.getActionView();

		alphabet = new ArrayList<String>();
		alphabet.add(Lang.get("android_alphabetic_search_all"));
		if ( Lang.get("android_alphabet").contains(",") ) {
			for ( String entry : Lang.get("android_alphabet").split(",") ) {
				alphabet.add(entry);
			}
		}

		ActionbarSpinnerAdapter spinnerArrayAdapter = new ActionbarSpinnerAdapter(Config.context, alphabet);
		menuItemSpinner.setAdapter(spinnerArrayAdapter);
		menuItemSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
				filterPosition = position;
				if ( allowListener ) {
					getAccountsByChar(false);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
	}
	
	class FragmentAdapter extends FragmentPagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	return AccountFragment.newInstance(TABS.get(position).get("key"));
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	return TABS.get(position).get("name").replace(" ", "\u00A0").toUpperCase(); // converting to non-breaking space
        }

        @Override
        public int getCount() {
        	return TABS.size();
        }
    }
	
	public static class AccountFragment extends Fragment {
	    
	    private String tabKey = "";

	    public static AccountFragment newInstance(String key) {
	    	AccountFragment fragment = new AccountFragment();
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
	    	final LinearLayout layout = new LinearLayout(Config.context);
	    	
	    	if ( tabKey == "alphabetic" ) {
	    		prepareAlphabeticTab(layout);
	    	}
	    	else if ( tabKey == "advanced" && Config.searchFormsAccount.containsKey(accountType) ) {
	    		buildSearch(layout);
	    	}

	

	        return layout;
	    }

	    @Override
	    public void onSaveInstanceState(Bundle outState) {
	    	super.onSaveInstanceState(outState);
	    }
	}
}