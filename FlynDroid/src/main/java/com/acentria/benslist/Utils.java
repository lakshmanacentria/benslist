package com.acentria.benslist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.acentria.benslist.adapters.SpinnerAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.utils.L;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

import static com.facebook.GraphRequest.TAG;

public class Utils {
	private static final String TAG="Utils";
	private static SharedPreferences SPSettings = PreferenceManager.getDefaultSharedPreferences(Config.context);
	
	public static ImageLoader imageLoaderDisc = ImageLoader.getInstance();// image loader (cache on disc)
	public static DisplayImageOptions imageLoaderOptionsDisc;// image display image options (cache on disc)
	
	public static ImageLoader imageLoaderMixed = ImageLoader.getInstance();// image loader (cache on disc and in memory)
	public static DisplayImageOptions imageLoaderOptionsMixed;// image display image options (cache on disc and in memory)
	
	public static ImageLoader imageLoaderFeatured = ImageLoader.getInstance();// featured loader (cache in memory)
	public static DisplayImageOptions imageLoaderOptionsFeatured;// featured display image options (cache in memory)
	
	private static int menuTranslateAttempts = 0;
	private static int menuVisibilityAttempts = 0;
	
	private static String requestedTabKey = null;
	private static String requestedControllerKey = null;
	
	public static void initImageLoader() {
		/* imageLoader cache condigs (cache on disc) */
		ImageLoaderConfiguration configDisc = new ImageLoaderConfiguration.Builder(Config.context)
			.threadPriority(Thread.NORM_PRIORITY - 2)
			.discCacheSize(50 * 1024 * 1024) // 50 Mb
			.discCacheFileNameGenerator(new Md5FileNameGenerator())
			.denyCacheImageMultipleSizesInMemory()
			.defaultDisplayImageOptions(DisplayImageOptions.createSimple())
			.tasksProcessingOrder(QueueProcessingType.LIFO)
			.build();
		
		imageLoaderDisc.init(configDisc);
		
		imageLoaderOptionsDisc = new DisplayImageOptions.Builder()
			.showStubImage(R.mipmap.blank)
			.showImageForEmptyUri(R.mipmap.no_image)
			.cacheOnDisc(true)
			.build();
		
		/* imageLoader cache condigs (cache on disc and in memory) */
		ImageLoaderConfiguration configMixed = new ImageLoaderConfiguration.Builder(Config.context)
			.threadPriority(Thread.NORM_PRIORITY - 2)
			.discCacheSize(50 * 1024 * 1024) // 50 Mb
			.memoryCacheSize(20 * 1024 * 1024) // 20 Mb
			.discCacheFileNameGenerator(new Md5FileNameGenerator())
			.denyCacheImageMultipleSizesInMemory()
			.defaultDisplayImageOptions(DisplayImageOptions.createSimple())
			.tasksProcessingOrder(QueueProcessingType.LIFO)
			.build();
		
		imageLoaderMixed.init(configMixed);
		
		imageLoaderOptionsMixed = new DisplayImageOptions.Builder()
			.showStubImage(R.mipmap.blank)
			.showImageForEmptyUri(R.mipmap.no_image)
			.imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
			.cacheInMemory(true)
			.cacheOnDisc(true)
			.build();
		
		/* imageLoader cache condigs (cache on disc and in memory) */
		ImageLoaderConfiguration configFeatured = new ImageLoaderConfiguration.Builder(Config.context)
			.threadPriority(Thread.NORM_PRIORITY - 2)
			.memoryCacheSize(20 * 1024 * 1024) // 20 Mb
			.memoryCache(new LruMemoryCache(20 * 1024 * 1024))
			.defaultDisplayImageOptions(DisplayImageOptions.createSimple())
			.tasksProcessingOrder(QueueProcessingType.LIFO)
			.build();
		
		imageLoaderFeatured.init(configFeatured);
		
		imageLoaderOptionsFeatured = new DisplayImageOptions.Builder()
			.showStubImage(R.mipmap.blank)
			.showImageForEmptyUri(R.mipmap.no_image)
			.cacheInMemory(true)
			.build();
		
		L.disableLogging();
	}

	public static boolean isOnline(Context context) {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connManager.getActiveNetworkInfo();
		if (info != null)
			if (info.isConnected()) {
				return true;
			} else {
				return false;
			}
		else
			return false;
	}

	
	/**
	 * check string for domain entry
	 * 
	 * @param domain
	 * @return regexp matches
	 */
	public static boolean isDomain( String domain ) {
		Pattern patt = Pattern.compile("[\\w-\\.]+\\.[a-zA-Z]+([\\w-/]+)?");
		Matcher matcher = patt.matcher(domain);

        return matcher.matches();
	}
	
	/**
	 * parse XML of standard items structure
	 * 
	 * @param xml - xml content
	 * @param url - xml feed url to write logs for (optional)
	 * 
	 * @return hashmap of data
	 */
	public static ArrayList<HashMap<String, String>> parseXML(String xml, String url, Context instance) {
		instance = instance == null ? Config.context : instance;
		
		ArrayList<HashMap<String, String>> out = null;
		
		/* parse response */
    	XMLParser parser = new XMLParser();
		Document doc = parser.getDomElement(xml, url);
		
		if ( doc == null ) {
			Dialog.simpleWarning(Lang.get("returned_xml_failed"), instance);
		}
		else {
			Element parentNode = (Element) doc.getElementsByTagName("items").item(0);
			NodeList nodes = (NodeList) parentNode.getChildNodes();
			
			out = parseStack(nodes);
		}
		
		return out;
	}
	
	public static ArrayList<HashMap<String, String>> parseStack(NodeList nodes) {
		ArrayList<HashMap<String, String>> out = new ArrayList<HashMap<String, String>>();
		
		for (int a = 0; a < nodes.getLength(); a++) {
			NodeList tags = (NodeList) nodes.item(a).getChildNodes();
			out.add(parseHash(tags));
		}
		
		return out;
	}
	
	public static HashMap<String, String> parseHash(NodeList tags) {
		HashMap<String, String> tmpTag = new HashMap<String, String>(); 
		
		for (int b = 0; b < tags.getLength(); b++) {
			Element tag = (Element) tags.item(b);
			tmpTag.put(tag.getTagName(), Config.convertChars(tag.getTextContent()));
		}
		
		return tmpTag;
	}
	
	/**
	 * add the first item to spinner items
	 * 
	 * @param items - items array list to add new first item to
	 * @param key - phrase key which will be added as the first item 
	 * 
	 * @return hashmap of data
	 */
	public static void addFirstItem(ArrayList<HashMap<String, String>> items, String key) {
		HashMap<String, String> type_first = new HashMap<String, String>();
		type_first.put("key", "");
		type_first.put("name", Lang.get(key));
		items.add(0, type_first);
	}
	
	/**
	 * set spinner to loading state
	 * 
	 * @param spinner - our spinner 
	 * 
	 * @return hashmap of data
	 */
	public static void setSpinnerLoading(Spinner spinner) {
		ArrayList<HashMap<String, String>> loading = new ArrayList<HashMap<String, String>>();
		addFirstItem(loading, "loading");
		((SpinnerAdapter) spinner.getAdapter()).updateItems(loading);
	}
	
	/**
	 * check for network availability
	 * 
	 * @return
	 */
	public static boolean isNetworkAvailable() {
		ConnectivityManager cm =
			(ConnectivityManager) Config.context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}
	
	/**
	 * get application (system) config
	 * 
	 * @param name - config key
	 * @return config value
	 */
	public static String getConfig(String name){
        return (String) Config.context.getResources().getText(Config.context.getResources().getIdentifier(name, "string", Config.context.getPackageName()));
	}
	
	/**
	 * get config from connected website (cached)
	 * 
	 * @param key - config key
	 * @return
	 */
	public static String getCacheConfig(String key){
        return Config.cacheConfig.get(key);
	}
	
	public static String getSPConfig(String key, String defValue){
        return SPSettings.getString(key, defValue);
	}
	
	public static void setSPConfig(String key, String value){
		SharedPreferences.Editor editor = SPSettings.edit();
		editor.putString(key, value);
		editor.commit();
	}
	
	/**
	 * build request string with GET parameters
	 * 
	 * @param method - request item name, corresponding to connect plugin
	 * @param params - hashmap parameters, like key => value
	 * @param default_domain - domain name to do request to, set null to use default domain 
	 * @return url to the connect plugin
	 */
	public static String buildRequestUrl(String method, HashMap<String, String> params, String default_domain) {
		String add_string = "?item="+ method;
		add_string += "&lang="+ Lang.getSystemLang();
		add_string += "&json="+ Config.context.getResources().getString(R.string.json);

		if (params != null)
		{
			add_string += "&";
			
			Iterator<String> iterator = params.keySet().iterator();
			while(iterator.hasNext()) {
			    String key = (String) iterator.next();
			    String value = "";
			    
				try {
					if ( params.get(key) != null ) {
						value = URLEncoder.encode(params.get(key).toString(), "utf-8");
					}
					add_string += key+"="+value+"&";
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		String domain = default_domain != null ? default_domain : Utils.getSPConfig("domain", null);
		String http = Config.isHTTPS!=null && !Config.isHTTPS.isEmpty() ? "https" : "http";
		String url = http + "://"+ domain.replaceAll("(/+)$", "") +'/'+ Utils.getConfig("pluginBridgePath") + add_string;
		Log.d("FD", url);
		return (String) url;
	}
	
	/**
	 * build request string without any parameters
	 * 
	 * @param method - request item name, corresponding to connect plugin 
	 * @return url to the connect plugin
	 */
	public static String buildRequestUrl(String method) {
		String add_string = "?item="+ method;
		add_string += "&lang="+ Lang.getSystemLang();
		add_string += "&json="+ Config.context.getResources().getString(R.string.json);

		String domain = Utils.getSPConfig("domain", null);
		String http = Config.isHTTPS!=null && !Config.isHTTPS.isEmpty() ? "https" : "http";
		String url = http + "://"+ domain.replaceAll("(/+)$", "") +'/'+ Utils.getConfig("pluginBridgePath") + add_string;
		Log.d("FD", url);
		return (String) url;
	}

	public static void restroreInstanceView(String className, String Title) {
		/* hide previus view */
		if ( Config.prevView != null ) {
			int prevId = Config.context.getResources().getIdentifier(Config.prevView, "id", Config.context.getPackageName());
			Config.context.getWindow().findViewById(prevId).setVisibility(View.GONE);
		}
		
		/* show current/requested view */
		int viewId = Config.context.getResources().getIdentifier(className, "id", Config.context.getPackageName());
		Log.e(TAG,"classename"+className+" title=> "+Title);
		Config.context.getWindow().findViewById(viewId).setVisibility(View.VISIBLE);
		
		ActionBar actionBar = Config.context.getSupportActionBar();
		actionBar.show();
		
		/* set title */
		Config.context.setTitle(Title);
		
		/* hide swipe menu */
		new CountDownTimer(200, 200) {
			public void onTick(long millisUntilFinished) {}
		
			public void onFinish() {
				SwipeMenu.menu.showContent();
			}
		}.start();
	}
	
	public static void removeContentView(String className) {
		int prevId = Config.context.getResources().getIdentifier(className, "id", Config.context.getPackageName());
		((ViewManager) Config.context.findViewById(prevId).getParent()).removeView(Config.context.findViewById(prevId));		
	}
	
	/**
	 * add controller view 
	 *
	 * @param view
	 */
	public static View addContentView(int view) {
		if ( Config.prevView != null ) {
			int prevId = Config.context.getResources().getIdentifier(Config.prevView, "id", Config.context.getPackageName());
			Config.context.getWindow().findViewById(prevId).setVisibility(View.GONE);
		}
		
		LayoutInflater inflater = Config.context.getLayoutInflater();
		View inflate_view = inflater.inflate(view, null);
		if ( Config.tabletMode ) {

			Config.contentFrame.addView(
					inflate_view,
				new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
			);
		}
		else {
			Config.context.addContentView(
				inflate_view,
				new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
			);
		}
		return inflate_view;
	}
	
	/**
	 * show content with 0.25 seconds delay
	 */
	public static void showContent() {
		if ( !Config.tabletMode ) {
			/* hide swipe menu */
			new CountDownTimer(250, 250) {
				public void onTick(long millisUntilFinished) {
					// nothing to do
				}
			
				public void onFinish() {
					SwipeMenu.menu.showContent();
				}
			}.start();
		}
	}
	
	/**
	 * calls all running controllers which contains onOrientationChange method
	 * 
	 * @param orientation
	 */
	public static void onOrientationChangeHandler( int orientation ) {
		for ( int i = 0; i < Config.activeInstances.size(); i++ ) {
			String className = Config.activeInstances.get(i);
			
			try {
				/* invoke onOrientationChange method of the requested class */
				Config.orientation = orientation;
				Class.forName("com.acentria.benslist.controllers."+className).getMethod("onOrientationChange").invoke(className);
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
	}

	/**
	 * converts dpi to px
	 * 
	 * @param dp
	 * @return int - value in pixels
	 */
	public static int dp2px(int dp) {
		return (int)((dp *  Config.context.getResources().getDisplayMetrics().density) + 0.5);
	}
	
	/**
	 * add item to set string
	 * 
	 * @param set - string to add item to
	 * @param value
	 * @return
	 */
	public static String addToSet(String set, String value) {
		set += set.isEmpty() ? value : ","+value;
		
		return set;
	}
	
	/**
	 * hide keyboard in main context
	 * 
	 * @param view - currently active view
	 */
	public static void hideKeyboard(View view) {
		InputMethodManager imm = (InputMethodManager)Config.context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}
	
	/**
	 * hide keyboard in specified context
	 * 
	 * @param view - currently active view
	 * @param context - current context
	 */
	public static void hideKeyboard(View view, Context context) {
		if ( view == null )
			return;
		
		InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}


	
	/**
	 * remove item from set string
	 * 
	 * @param set - string to remove item from
	 * @param value
	 * @return
	 */
	public static String removeFromSet(String set, String value) {
		String out = "";
		
		if ( set.contains(",") ) {
			String[] elements = set.split(",");
			out = "";
			
			for (int i = 0; i < elements.length; i++) {
				if ( !elements[i].equals(value) ) {
					out += elements[i] + ",";
				}
			}

			out = out.substring(0, out.length()-1);
		}
		
		return out;
	}
	
	public static List<String> string2list(String[] string) {
		List<String> list = new ArrayList<String>();
		for ( String entry : string ) {
			list.add(entry);
		}
		return list;
	}
	
	/**
	 * convert arraylist with hashmap to list
	 * 
	 * @param array
	 * @return
	 */
	public static List<String> hash2list(ArrayList<HashMap<String, String>> array) {
		List<String> list = new ArrayList<String>();
		for (HashMap<String, String> entry : array) {
			list.add(entry.get("name"));
		}
		return list;
	}

	/**
	 * convert string(json) to hash map
	 *
	 * @param string
	 * @return
	 */
	public static HashMap<String, String> json2hash(String array) {
		HashMap<String, String> hashMap = new HashMap<String, String>();

		// Object value = data.get("data");
		JSONObject json = null;
		try {
			json = new JSONObject( array );
			Iterator<String> keysItr = json.keys();
			while(keysItr.hasNext()) {
				String key = keysItr.next();
				Object value = json.get(key);
				hashMap.put(key, value.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return hashMap;
	}
	
	/**
	 * convert linked hashmap with to arraylist
	 * 
	 * @param array
	 * @return
	 */
	public static ArrayList<HashMap<String, String>> linkedHM2arratList(LinkedHashMap<String, HashMap<String, String>> array) {
		ArrayList<HashMap<String, String>> map = new ArrayList<HashMap<String, String>>();
		 for (Entry<String, HashMap<String, String>> entry : array.entrySet()) {
			map.add(entry.getValue());
		}
		 Log.d("FD", map.toString());
		return map;
	}
	
	public static void adaptTabs(String[][] from, ArrayList<HashMap<String, String>> to) {
		if ( from.length <= 0 )
			return;
	
		for (int i = 0; i < from.length; i++) {
			HashMap<String, String> tmp = new HashMap<String, String>();
			tmp.put("key", from[i][0]);
			tmp.put("name", from[i][1]);
			to.add(tmp);
		}
	}
	
	public static String getNodeByName(Element item, String name) {
    	NodeList node = item.getElementsByTagName(name);
    	return node.getLength() > 0 ? Config.convertChars(node.item(0).getTextContent()) : "";
    }
	
	public static void switchController(String name) {
		String className = name;

//		try {
//			/* save current view */
//			Config.prevView = Config.currentView;
//			Config.currentView = className;
//			
//			/* set current menu position as current */
//			previousPosition = currentPosition;
//			currentPosition = position;
//
//			notifyDataSetChanged();
//			
//			/* invoke getInstance method of the requested class */
//			Class.forName("com.acentria.benslist.controllers."+className).getMethod("getInstance").invoke(className);
//		}
//		catch (ClassNotFoundException exception) {
//			Context context = Config.context.getApplicationContext();
//    		CharSequence text = "No related class found for: "+data.get(position).get("name");
//    		Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	public static void getMenuItemInfo( String name ) {
		
	}
	
	public static void translateMenuItems(final Menu menu) {
		if ( menuTranslateAttempts > 25 ) {
			menuTranslateAttempts = 0;
			return;
		}
		
		if ( menu == null ) {
			Log.d("..................", "Menu unavailable");
			/* menu isn't available now, let's try again in 300 miliseconds */
			new CountDownTimer(300, 300) {
    			public void onTick(long millisUntilFinished) {}
    		
    			public void onFinish() {
    				menuTranslateAttempts++;
    				translateMenuItems(menu);
    			}
    		}.start();
		}
		else {
			for (int i=0; i<menu.size(); i++) {
				String title = (String) menu.getItem(i).getTitle();
				menu.getItem(i).setTitle(Lang.get(title));
			}
		}
	}
	
	public static void bugRequest(String subject, String body) {
		sendToLab("Android Debug: "+subject, body);
	}
	
	public static void bugRequest(String subject, StackTraceElement[] stackTrace, String message) {
		String stack = message+"\r\n";
		for (int i = 0; i<stackTrace.length; i++) {
			stack += stackTrace[i]+"\r\n";
		}
		sendToLab("Android Debug: "+subject+" ("+ getSPConfig("domain", "") +")", stack);
	}

	public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(

			"[a-zA-Z0-9+._%-+]{1,256}" +
					"@." +
					"[a-zA-Z0-9][a-zA-Z0-9-]{0,64}" +
					"(" +
					"." +
					"[a-zA-Z0-9][a-zA-Z0-9-]{0,25}" +
					")+"
	);



	public static void isAlertBox(final Activity mcontext, String title, String msg) {
		AlertDialog.Builder builder1 = new AlertDialog.Builder(mcontext);
		builder1.setTitle(title);
		builder1.setMessage(msg);
		builder1.setCancelable(false);

		builder1.setPositiveButton(
				"OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						mcontext.onBackPressed();
					}
				});


		AlertDialog alert11 = builder1.create();
		alert11.show();
	}

	public static boolean checkEmail(String email) {


		return EMAIL_ADDRESS_PATTERN.matcher(email).matches();
	}
	
	public static void sendEmail(String subject, String body, String from, String to) {
		/* build request url */
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put("subject", subject);
		params.put("body", body);
		params.put("from", from);
		params.put("to", to);
		String url = Utils.buildRequestUrl("sendEmail", params, null);
//		Log.d("FD",subject );
//		Log.d("FD",body );
		/* do async request */
    	AsyncHttpClient client = new AsyncHttpClient();
    	client.get(url, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int i, Header[] headers, byte[] bytes) {

			}

			@Override
			public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

			}

//			@Override
//    	    public void onSuccess(String response) {
//    	    	//TODO
//    	    }
    	});
	}
	
	public static void sendToLab(String subject, String body) {
		sendEmail(subject, body, "acentria@gmail.com", "android@acentria.com");
	}
	
	public static void changeMenuItemVisibility(final Menu menu, final int menuItemID, final boolean visibility) {
		if ( menuItemID <= 0 )
			return;
		
		if ( menuVisibilityAttempts > 5 ) {
			Log.d("..................", "Menu visibility can't be changed, requested menu unavilable");
			menuVisibilityAttempts = 0;
			return;
		}
		
		if ( menu == null ) {
			new CountDownTimer(300, 300) {
    			public void onTick(long millisUntilFinished) {}
    		
    			public void onFinish() {
    				menuVisibilityAttempts++;
    				changeMenuItemVisibility(menu, menuItemID, visibility);
    			}
    		}.start();
		}
		else {
			MenuItem menuItem = (MenuItem) menu.findItem(menuItemID);
			menuItem.setVisible(visibility);
			
			menuVisibilityAttempts = 0;
		}
	}
	
	public static String md5Java(String message){
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
           
            //converting byte array to Hexadecimal String
           StringBuilder sb = new StringBuilder(2*hash.length);
           for(byte b : hash){
               sb.append(String.format("%02x", b&0xff));
           }
          
           digest = sb.toString();
          
        } catch (UnsupportedEncodingException ex) {
        	digest = "Unable to hash the string";
        } catch (NoSuchAlgorithmException ex) {
        	digest = "Unable to hash the string";
        }
        return digest;
    }
	
	public static RequestParams toParams( HashMap<String, String> map ) {
		RequestParams params = new RequestParams();
		if ( map.size() > 0 ) {
			for ( Entry<String, String> entry : map.entrySet() ) {
				params.put(entry.getKey(), entry.getValue());
			}
		}
		
		return params;
	}
	
	public static int getFlagResources( String langCode ) {
		String flag_name = langCode.equals("do") ? "dom" : langCode; // we can't use do as name of file :(
		return Config.context.getResources().getIdentifier(flag_name, "mipmap", Config.context.getPackageName());
	}
	
	public static Drawable getFlag( String langCode ) {
		return Config.context.getResources().getDrawable(getFlagResources(langCode));
	}
	
	public static DisplayMetrics getScreenSize() {
		DisplayMetrics metrics = new DisplayMetrics();
		Config.context.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		return metrics;
	}
	
	public static int getScreenWidth() {
		return ((DisplayMetrics) getScreenSize()).widthPixels;
	}
	
	public static int getScreenHeight() {
		return ((DisplayMetrics) getScreenSize()).heightPixels;
	}
	
	public static String getYouTubeID(String url) {
		String pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|watch\\?v%3D|%2Fvideos%2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";

		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(url);

		return matcher.find() ? matcher.group() : null;
	}
	
	public static Fragment findFragmentByPosition(int position, Context instance, ViewPager pager, FragmentPagerAdapter adapter) {
		instance = instance == null ? Config.context : instance;
		
	    return ((FragmentActivity) instance).getSupportFragmentManager().findFragmentByTag(
	            "android:switcher:" + pager.getId() + ":" + adapter.getItemId(position));
	}
	
	/**
	 * request tab to be opened on controller start
	 * 
	 * @param array
	 * @return
	 */
	public static void requestTab(String contollerKey, String tabKey) {
		requestedTabKey = tabKey;
		requestedControllerKey = contollerKey;
	}
	
	/**
	 * clear tab request
	 * 
	 * @param array
	 * @return
	 */
	public static void clearTab() {
		requestedTabKey = null;
		requestedControllerKey = null;
	}
	
	/**
	 * request tab to be opened on controller start
	 * 
	 * @param array
	 * @return
	 */
	public static String getTabRequest(String controller) {
		return requestedControllerKey != null && requestedControllerKey.equals(controller) ? requestedTabKey : null;
	}
	
	public static String buildPrice(String price) {
		if ( price.isEmpty() || price.equals("0") ) {
			return Lang.get("free");
		}
		else {
			return Utils.getCacheConfig("currency_position").equals("before") ? Utils.getCacheConfig("system_currency") + price : price +" "+ Utils.getCacheConfig("system_currency");
		}
	}
	
	public static String buildTitleForOrder(String type, String id) {
		return Lang.get("order_item_"+type).replace("{id}", id);
	}
	
	public static void setMargins(View view, int l, int t, int r, int b) {
		if ( view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams ) {
			ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
			p.setMargins(l, t, r, b);
			view.requestLayout();
		}
	}

    public static void setAdsense(View view, String page) {
        HashMap ads = null;

        for (int b = 0; b <  Config.adsenses.size(); b++) {
            if ( Config.adsenses.get(b).get("page").equals(page) ) {
                ads = (HashMap) Config.adsenses.get(b);
            }
        }

        if( ads != null ) {
            String code = (String) ads.get("code");
            String side = (String) ads.get("side");
            AdView mAdView = new AdView(Config.context);
            mAdView.setAdSize(AdSize.BANNER);
            mAdView.setAdUnitId(code);

            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

            LinearLayout ads_box = (LinearLayout) Config.context.getLayoutInflater()
                    .inflate(R.layout.adsense, null);

            ads_box.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            ads_box.addView(mAdView);

            if ( view instanceof LinearLayout ) {
                LinearLayout layout = (LinearLayout) view;
                if (side.equals("top")) {
                    if ( page.equals("listing_details") || page.equals("account_details") || page.equals("account_type") || page.equals("recently_added") || page.equals("search") ) {
                        ads_box.setGravity(Gravity.CENTER_HORIZONTAL);
                        layout.addView(ads_box, 1);
                    }
                    else {
                        ads_box.setGravity(Gravity.CENTER_HORIZONTAL);
                        layout.addView(ads_box, 0);
                    }
                }
                else {
                    layout.addView(ads_box);
                }
            }
            else if ( view instanceof RelativeLayout ) {
                RelativeLayout layout = (RelativeLayout) view;

                if (side.equals("top")) {
                    layout.addView(ads_box);
                }
                else {
                    if ( page.equals("home") ) {
                        LinearLayout banner = (LinearLayout) layout.findViewById(R.id.bottom_adsense);
                        banner.addView(ads_box);
                    }
                    else {
                        layout.addView(ads_box);
                    }
                }
            }
        }
	}
}