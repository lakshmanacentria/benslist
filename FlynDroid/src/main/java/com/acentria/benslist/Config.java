package com.acentria.benslist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public final class Config {

	public static FlynDroid context = null;
	public static Activity mainActivity = null;
	
	public static Intent configIntent = null;
	public static boolean tabletMode = false;
	public static List<String> activeInstances = new ArrayList<String>();
	public static FrameLayout contentFrame;
	
	public static String currentView = "Home";
	public static String prevView = "";
	public static String pushView = null;
	public static int orientation = Configuration.ORIENTATION_PORTRAIT;
	public static Menu menu;
	
	public static HashMap<String, HashMap<String, String>> cacheLanguages = new HashMap<String, HashMap<String, String>>();
	public static ArrayList<HashMap<String, String>> cacheLanguagesWebs = new ArrayList<HashMap<String, String>>();
	public static String cacheLanguagesWebsDefault = "";
	public static List<String> cacheLangCodes = new ArrayList<String>();
	public static List<String> cacheLangNames = new ArrayList<String>();
	public static HashMap<String, String> cacheConfig = new HashMap<String, String>();
	public static HashMap<String, String> cacheLang = new HashMap<String, String>();
	public static LinkedHashMap<String, HashMap<String, String>> cacheListingTypes = new LinkedHashMap<String, HashMap<String, String>>();
	public static LinkedHashMap<String, HashMap<String, String>> cacheAccountTypes = new LinkedHashMap<String, HashMap<String, String>>();
	public static ArrayList<HashMap<String, String>> featuredListings = new ArrayList<HashMap<String, String>>();
	public static HashMap<String, ArrayList<HashMap<String, String>>> searchForms = new HashMap<String, ArrayList<HashMap<String, String>>>();
	public static HashMap<String, ArrayList<HashMap<String, String>>> searchFieldItems = new HashMap<String, ArrayList<HashMap<String, String>>>();
	public static HashMap<String, ArrayList<HashMap<String, String>>> searchFormsAccount = new HashMap<String, ArrayList<HashMap<String, String>>>();
	public static HashMap<String, ArrayList<HashMap<String, String>>> searchFieldItemsAccount = new HashMap<String, ArrayList<HashMap<String, String>>>();
    public static ArrayList<HashMap<String, String>> adsenses = new ArrayList<HashMap<String, String>>();
    public static ArrayList<HashMap<String, String>> reportBroken = new ArrayList<HashMap<String, String>>();
	public static int lastRequestTotalHomeListings;
	public static String isHTTPS;

	public static ArrayList<HashMap<String, String>> pendingTransaction = new ArrayList<HashMap<String, String>>();
	
	public static final int ACTIONBAR_TIMEOUT = 600; // 0.6 seconds delay before hide the action bar menu item, SHOULD BE REMOVED IN NEW VERSION TODO
	
	public static final int RESULT_PAYMENT = 101;
	public static final int RESULT_TRANSACTION_FAILED = 103; // activity for result faild
	public static final int IAP_PURCHASE = 104;
	public static final int PAYPAL_REST_PURCHASE = 105;
    public static final int PAYPAL_MPL_PURCHASE = 107;
	public static final int RESULT_SELECT_PLAN = 106;

    public static String categoryFieldKey = "Category_ID";

    // list of countries supported by PayPal REST lib
    public static List<String> pp_rest_supported_countries;

	public static void initCache(final boolean login) {
		clearCacheData();

        pp_rest_supported_countries = Utils.string2list(new String[] {"us", "uk"});
		
		String xml = Utils.getSPConfig("FlynDroidCache", null);
		int lastCacheUpdateTime = Integer.parseInt(Utils.getSPConfig("LastCacheUpdateTime", "0"));
		Long currentTime = System.currentTimeMillis()/1000;
		xml = null;
		
		if ( xml == null || currentTime > lastCacheUpdateTime + 3600 ) {
			HashMap<String, String> params = new HashMap<String, String>();
			if ( Utils.getSPConfig("newCountDate", null) != null ) {
				params.put("countDate", Utils.getSPConfig("newCountDate", ""));
				
				/*TimeZone timeZone = Calendar.getInstance().getTimeZone();
				params.put("timeZone", timeZone.getID());*/
			}
			/* request username  */
			if ( Utils.getSPConfig("accountUsername", null) != null ) {
				params.put("username", Utils.getSPConfig("accountUsername", null));
				params.put("passwordHash", Utils.getSPConfig("accountPassword", null));
			}
			params.put("tablet", Config.tabletMode ? "1" : "0");
	    	final String url = Utils.buildRequestUrl("getCache", params, null);

	    	AsyncHttpClient client = new AsyncHttpClient();
			client.get(url, new AsyncHttpResponseHandler() {
				
				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						if ( login ) {
							FlynaxWelcome.progressDialog.dismiss();
						}
						Utils.setSPConfig("FlynDroidCache", response);
						parseCacheData(response, true, url);

						/* return if parseCacheData failed */
						if ( cacheConfig.size() <= 0 ) {
							return;
						}

						/* save last cache update time */
						Long tsLong = System.currentTimeMillis()/1000;
						Utils.setSPConfig("LastCacheUpdateTime", tsLong.toString());

					} catch (UnsupportedEncodingException e1) {

					}
				}

				@Override
				public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
					// called when response HTTP status is "4XX" (eg. 401, 403, 404)
				}
	    	});
		}
		else {
			parseCacheData(xml, true, "");
		}
	}
	
	public static void parseCacheData(String cacheXml, Boolean switchToHome, String url ) {
		/* parse xml */
		XMLParser parser = new XMLParser();
		Document xmlContent = parser.getDomElement(cacheXml, url);

		NodeList nl = xmlContent.getElementsByTagName("cache");
		
		if ( nl.getLength() <= 0 ) {
			Dialog.simpleWarning(R.string.dialog_cache_faild);
			FlynaxWelcome.animateForm();
			return;
		}
		
		Element nlElement = (Element) nl.item(0);
		NodeList tags = nlElement.getChildNodes();

		for (int a = 0; a < tags.getLength(); a++) {
			Element tag = (Element) tags.item(a);
			
			/* fetch client configs */
			if ( tag.getTagName().equals("configs") )
			{
				NodeList nodes = tag.getChildNodes();
				for (int b = 0; b < nodes.getLength(); b++) {
					Element node = (Element) nodes.item(b);
					cacheConfig.put(node.getAttribute("key"), node.getTextContent());
				}
				
				/* modify SP config */
				if ( Utils.getSPConfig("preload_method", null) == null ) {
					Utils.setSPConfig("preload_method", Utils.getCacheConfig("android_preload_method"));
				}
			}
			/* fetch languages codes/phrases */
			else if ( tag.getTagName().equals("langsweb") )
			{
				/* fetch codes */
				NodeList nodes = tag.getChildNodes();
				for (int b = 0; b < nodes.getLength(); b++) {
					Element node = (Element) nodes.item(b);
					HashMap<String, String> tmpLang = new HashMap<String, String>();
					tmpLang.put("key", node.getAttribute("code")); // I put code as key to keep standards
					tmpLang.put("name", node.getAttribute("name"));
					tmpLang.put("default", node.getAttribute("default"));
					if (tmpLang.get("default").equals("1")) {
						cacheLanguagesWebsDefault = node.getAttribute("code");
					}
					cacheLanguagesWebs.add(tmpLang);
				}
			}
			
			/* fetch languages codes/phrases */
			else if ( tag.getTagName().equals("langs") )
			{
				/* fetch codes */
				NodeList nodes = tag.getChildNodes();
				for (int b = 0; b < nodes.getLength(); b++) {
					Element node = (Element) nodes.item(b);
					cacheLangCodes.add(node.getAttribute("code"));
					cacheLangNames.add(node.getAttribute("name"));
					
					HashMap<String, String> tmpLang = new HashMap<String, String>();
					tmpLang.put("key", node.getAttribute("code")); // I put code as key to keep standards 
					tmpLang.put("name", node.getAttribute("name"));
					tmpLang.put("dir", node.hasAttribute("dir") ? node.getAttribute("dir") : "");
					cacheLanguages.put(node.getAttribute("code"), tmpLang);
				}
				
				/* define system lang */
				String langCode = Lang.getSystemLang();
				
				/* fetch phrases */
				for (int b = 0; b < nodes.getLength(); b++) {
					Element node = (Element) nodes.item(b);
					if ( node.getAttribute("code").equals(langCode) )
					{
						NodeList phrases = node.getChildNodes();
						for (int c = 0; c < phrases.getLength(); c++) {
							Element phrase = (Element) phrases.item(c);
							cacheLang.put(phrase.getAttribute("key"), phrase.getTextContent());
						}
					}
				}
			}
			
			/* fetch listing types */
			else if ( tag.getTagName().equals("listing_types") )
			{
				/* fetch codes */
				NodeList nodes = tag.getChildNodes();
				for (int b = 0; b < nodes.getLength(); b++) {
					Element node = (Element) nodes.item(b);
					HashMap <String, String> typeConfig = new HashMap<String, String>();
					typeConfig.put("photo", node.getAttribute("photo"));
					typeConfig.put("video", node.getAttribute("video"));
//					typeConfig.put("page", node.getAttribute("page"));
					typeConfig.put("search", node.getAttribute("search"));
					typeConfig.put("icon", node.getAttribute("icon"));
					typeConfig.put("admin", node.getAttribute("admin"));
					typeConfig.put("name", convertChars(cacheLang.get("listing_types+name+"+node.getAttribute("key"))));
					typeConfig.put("key", node.getAttribute("key")); // looks like duplicate key entry, but it is necessary to have key as map value sometimes
					cacheListingTypes.put(node.getAttribute("key"), typeConfig);
				}
			}
			
			/* fetch account types */
			else if ( tag.getTagName().equals("account_types") )
			{
				/* fetch codes */
				NodeList nodes = tag.getChildNodes();
				for (int b = 0; b < nodes.getLength(); b++) {
					Element node = (Element) nodes.item(b);
					HashMap <String, String> typeConfig = new HashMap<String, String>();
					typeConfig.put("own_location", node.getAttribute("own_location"));
					typeConfig.put("page", node.getAttribute("page"));
					typeConfig.put("name", cacheLang.get("account_types+name+"+node.getAttribute("key")));
					typeConfig.put("key", node.getAttribute("key"));
					cacheAccountTypes.put(node.getAttribute("key"), typeConfig);
				}
			}
			
			/* fetch featured listings */
			else if ( tag.getTagName().equals("featured") )
			{
				parseHomeListings(tag);
			}
			/* fetch featured listings count*/
			else if ( tag.getTagName().equals("featured_count") )
			{
				parseHomeListings(tag);
			}

			/* fetch listing search forms */
			else if ( tag.getTagName().equals("search_forms") )
			{
				NodeList searchFormNodes = tag.getChildNodes();
				for (int b = 0; b < searchFormNodes.getLength(); b++) {
					Element formNode = (Element) searchFormNodes.item(b);
					NodeList formFields = formNode.getChildNodes();

					ArrayList<HashMap<String, String>> form = new ArrayList<HashMap<String, String>>();
					
					for (int c = 0; c < formFields.getLength(); c++) {
						Element field = (Element) formFields.item(c);
						HashMap<String, String> fieldHash = new HashMap<String, String>();
						fieldHash.put("name", field.getAttribute("name"));
						fieldHash.put("type", field.getAttribute("type"));
						fieldHash.put("key", field.getAttribute("key"));
						fieldHash.put("data", field.getAttribute("data"));
						
						NodeList fieldItem = field.getChildNodes();
						if ( fieldItem.getLength() > 0 && !searchFieldItems.containsKey(field.getAttribute("key")) ) { 
							fieldHash.put("values", "1");
						
							ArrayList<HashMap<String, String>> itemArray = new ArrayList<HashMap<String, String>>();
							
							if ( field.getAttribute("type").equals("select") ) {
								HashMap<String, String> itemDefault = new HashMap<String, String>();
								itemDefault.put("name", Lang.get("android_any_field").replace("{field}", field.getAttribute("name")));
								itemDefault.put("key", "");
								itemArray.add(itemDefault);
							}
							
							for (int d = 0; d < fieldItem.getLength(); d++) {
								Element item = (Element) fieldItem.item(d);
								HashMap<String, String> itemHash = new HashMap<String, String>();
								itemHash.put("name", item.getAttribute("name"));
								itemHash.put("key", item.getAttribute("key"));
								
								if ( item.hasAttribute("margin") ) {
									itemHash.put("margin", item.getAttribute("margin"));	
								}
								
								itemArray.add(itemHash);
							}
							
							searchFieldItems.put(field.getAttribute("key"), itemArray);
						}
						
						form.add(fieldHash);
					}
					
					searchForms.put(formNode.getAttribute("type"), form);
				}
			}
			
			/* fetch account search forms */
			else if ( tag.getTagName().equals("account_search_forms") )
			{
				NodeList searchFormNodes = tag.getChildNodes();
				for (int b = 0; b < searchFormNodes.getLength(); b++) {
					Element formNode = (Element) searchFormNodes.item(b);
					NodeList formFields = formNode.getChildNodes();

					ArrayList<HashMap<String, String>> form = new ArrayList<HashMap<String, String>>();
					
					for (int c = 0; c < formFields.getLength(); c++) {
						Element field = (Element) formFields.item(c);
						HashMap<String, String> fieldHash = new HashMap<String, String>();
						fieldHash.put("name", field.getAttribute("name"));
						fieldHash.put("type", field.getAttribute("type"));
						fieldHash.put("key", field.getAttribute("key"));
						fieldHash.put("data", field.getAttribute("data"));
						
						NodeList fieldItem = field.getChildNodes();
						if ( fieldItem.getLength() > 0 && !searchFieldItemsAccount.containsKey(field.getAttribute("key")) ) { 
							fieldHash.put("values", "1");
						
							ArrayList<HashMap<String, String>> itemArray = new ArrayList<HashMap<String, String>>();
							
							if ( field.getAttribute("type").equals("select") ) {
								HashMap<String, String> itemDefault = new HashMap<String, String>();
								itemDefault.put("name", Lang.get("android_any_field").replace("{field}", field.getAttribute("name")));
								itemDefault.put("key", "");
								itemArray.add(itemDefault);
							}
							
							for (int d = 0; d < fieldItem.getLength(); d++) {
								Element item = (Element) fieldItem.item(d);
								HashMap<String, String> itemHash = new HashMap<String, String>();
								itemHash.put("name", item.getAttribute("name"));
								itemHash.put("key", item.getAttribute("key"));
								
								if ( item.hasAttribute("margin") ) {
									itemHash.put("margin", item.getAttribute("margin"));	
								}
								
								itemArray.add(itemHash);
							}
							
							searchFieldItemsAccount.put(field.getAttribute("key"), itemArray);
						}
						
						form.add(fieldHash);
					}
					
					searchFormsAccount.put(formNode.getAttribute("type"), form);
				}
			}
			
            /* fetch ad sense */
            else if ( tag.getTagName().equals("adsenses") )
            {
                NodeList nodeAdsenses = tag.getChildNodes();
                for (int b = 0; b < nodeAdsenses.getLength(); b++) {
                    Element adsenseNode = (Element) nodeAdsenses.item(b);

                    HashMap<String, String> fieldHash = new HashMap<String, String>();
                    fieldHash.put("id", adsenseNode.getAttribute("id"));
                    fieldHash.put("page", adsenseNode.getAttribute("page"));
                    fieldHash.put("side", adsenseNode.getAttribute("side"));
                    fieldHash.put("code", adsenseNode.getTextContent());
                    adsenses.add( fieldHash);
                }
            }
			/* fetch report broken items */
            else if ( tag.getTagName().equals("report_broken") )
            {
                NodeList nodeReport = tag.getChildNodes();
                for (int b = 0; b < nodeReport.getLength(); b++) {
                    Element reportNode = (Element) nodeReport.item(b);
                    HashMap<String, String> fieldHash = new HashMap<String, String>();
                    fieldHash.put("key", reportNode.getAttribute("key"));
					fieldHash.put("name", reportNode.getTextContent());
					reportBroken.add( fieldHash);
                }
            }

			/* fetch user login status */
			else if ( tag.getTagName().equals("account") ) {
				Account.fetchAccountData(tag.getChildNodes());
				Account.loggedIn = true;
			}
		}
		
		if (switchToHome) {
			FlynaxWelcome.switchToHome();
		}

		Lang.setDirection(Config.context);
	}


	public static void parseHomeListings(Element tag){

		/* fetch featured listings */
		if ( tag.getTagName().equals("featured") )
		{
			NodeList listings = tag.getChildNodes();
			for (int b = 0; b < listings.getLength(); b++) {
				Element listingElement = (Element) listings.item(b);
				NodeList listingFields = listingElement.getChildNodes();
				HashMap<String, String> listing = new HashMap<String, String>();

				listing.put("id", listingFields.item(0).getTextContent());
				listing.put("photo", listingFields.item(1).getTextContent());
				listing.put("price", listingFields.item(2).getTextContent());
				listing.put("title", convertChars(listingFields.item(3).getTextContent()));

				featuredListings.add(listing);
			}
		}
		/* fetch featured listings count*/
		else if ( tag.getTagName().equals("featured_count") )
		{
			lastRequestTotalHomeListings = tag.getTextContent()!=null && !tag.getTextContent().isEmpty() ? Integer.parseInt(tag.getTextContent()) : 0 ;
		}
	}

	/**
	 * convert chars
	 * &amp; -> &
	 * &lt;	-> <
	 * &gt; -> >
	 */

	public static String convertChars(String value){
		Spanned new_value = Html.fromHtml(value.replaceAll("&amp;", "&"));
		return new_value.toString();
	}

	/**
	 * compare version
	 * ver = get param from plugin
	 * vers = main version
	 */
	public static int compireVersion(String ver, String vers){

		if (ver.isEmpty()) {
			return  -1;
		}

		Version a = new Version(ver);
		Version b = new Version(vers);

		return a.compareTo(b);
	}

	/**
	 *  send link on update
	**/
	public static void updateVersionApp() {
		Intent intent = new Intent(Intent.ACTION_VIEW , Uri.parse("market://details?id=com.acentria.benslist"));
		Config.context.startActivity(intent);
	}

	/**
	 *  restart app on action
	**/
	public static void restartApp() {
		Intent restart = Config.context.getBaseContext().getPackageManager()
				.getLaunchIntentForPackage( Config.context.getBaseContext().getPackageName() );
		restart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		Config.context.startActivity(restart);
	}
	
	/**
	 * clear cache data 
	 */
	public static void clearCacheData(){
		/* remove created instances */
    	for (int i = 0; i < Config.activeInstances.size(); i++)
    	{
    		String className = Config.activeInstances.get(i);

    		try {
				/* invoke removeInstance method of the requested class */
				Class.forName("com.acentria.benslist.controllers."+className).getMethod("removeInstance").invoke(className);
			}
			catch (ClassNotFoundException exception) {
				Context context = Config.context.getApplicationContext();
	    		CharSequence text = "Can't remove instance of class: "+className;
	    		Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
		
    	/* clear variables */
		activeInstances = new ArrayList<String>();
		
		currentView = "Home";
		prevView = "";

		cacheLanguagesWebsDefault = "";
		cacheLanguagesWebs = new ArrayList<HashMap<String, String>>();
		cacheLanguages = new HashMap<String, HashMap<String, String>>();
		cacheLangCodes = new ArrayList<String>();
		cacheLangNames = new ArrayList<String>();
		cacheConfig = new HashMap<String, String>();
		cacheLang = new HashMap<String, String>(); // language phrases
		cacheListingTypes = new LinkedHashMap<String, HashMap<String, String>>();
		featuredListings = new ArrayList<HashMap<String, String>>();
		searchForms.clear();
		searchFieldItems.clear();
		searchFormsAccount.clear();
		searchFieldItemsAccount.clear();
		cacheAccountTypes.clear();
		adsenses.clear();
		reportBroken.clear();

		SwipeMenu.accountItems = 0;
	}

	public static void changeLanguage(String newLang) {
		Utils.setSPConfig("select_lang", newLang);
		
		String xml = Utils.getSPConfig("FlynDroidCache", null);
		
		XMLParser parser = new XMLParser();
		Document xmlContent = parser.getDomElement(xml, "");
		
		NodeList nl = xmlContent.getElementsByTagName("cache");
		
		if ( nl.getLength() <= 0 ) {
			Log.d("FD", "ERROR: No xml received after language change");
			return;
		}
		
		//cacheLang.clear();
		
		Element nlElement = (Element) nl.item(0);
		NodeList tags = nlElement.getChildNodes();

		for (int a = 0; a < tags.getLength(); a++) {
			Element tag = (Element) tags.item(a);
			
			if ( tag.getTagName().equals("langs") ) {
				/* fetch codes */
				NodeList nodes = tag.getChildNodes();
				
				/* fetch phrases */
				for (int b = 0; b < nodes.getLength(); b++) {
					Element node = (Element) nodes.item(b);
					if ( node.getAttribute("code").equals(newLang) ) {
						NodeList phrases = node.getChildNodes();
						for (int c = 0; c < phrases.getLength(); c++) {
							Element phrase = (Element) phrases.item(c);
							cacheLang.put(phrase.getAttribute("key"), phrase.getTextContent());
						}
					}
				}
				
				break;
			}
		}
	}
}
