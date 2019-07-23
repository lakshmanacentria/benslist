package com.acentria.benslist.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.Utils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;


/**
* Extends class DialogPreference for display custom dialog
* 
**/
public class ChangeSiteDialog extends DialogPreference {

    public ChangeSiteDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogMessage(Lang.get("are_you_sure"));
        setNegativeButtonText(Lang.get("android_dialog_cancel"));
        setPositiveButtonText(Lang.get("android_dialog_ok"));     
    }
    
    public void onClick(DialogInterface dialog, int which){
    	if( which == -1 ){
    		dialog.dismiss();
			this.setDialogMessage(Lang.get("are_you_sure"));
			this.setNegativeButtonText(Lang.get("android_dialog_cancel"));
			this.setPositiveButtonText(Lang.get("android_dialog_ok"));
	    	if ( this.getKey().equals("pref_reset_domain") ) {
	    		/* logout user */
				Account.logout();

		    	Utils.setSPConfig("domain", null);
	    		Utils.setSPConfig("FlynDroidCache", null);
	    		Utils.setSPConfig("LastCacheUpdateTime", "0");
	    		Utils.setSPConfig("favoriteIDs", "");

	    		Config.clearCacheData();

				Config.restartApp();
	    	}
	    	else if ( this.getKey().equals("clear_cache") )
	    	{
	    		HashMap<String, String> params = new HashMap<String, String>();
	    			
	    		if ( Utils.getSPConfig("newCountDate", null) != null ) {
	    			params.put("countDate", Utils.getSPConfig("newCountDate", ""));
	    			
	    			/*TimeZone timeZone = Calendar.getInstance().getTimeZone();
	    			params.put("timeZone", timeZone.getID());*/
	    		}
	    		
	    		params.put("tablet", Config.tabletMode ? "1" : "0");
	    	    final String url = Utils.buildRequestUrl("getCache", params, null);
	    	    
	    	    AsyncHttpClient client = new AsyncHttpClient();
	    		client.get(url, new AsyncHttpResponseHandler() {
	 
					@Override
					public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
						// called when response HTTP status is "200 OK"
						try {
							String xml = String.valueOf(new String(server_response, "UTF-8"));
							Config.clearCacheData();

							Utils.setSPConfig("FlynDroidCache", xml);
							Config.parseCacheData(xml, false, url);

	//	    	    	    Utils.translateMenuItems(Config.menu);

							/* save last cache update time */
							Long tsLong = System.currentTimeMillis()/1000;
							Utils.setSPConfig("LastCacheUpdateTime", tsLong.toString());

							Toast.makeText(Config.context, Lang.get("android_pref_toast_cache_updated"), Toast.LENGTH_SHORT).show();
							Config.restartApp();

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
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        persistBoolean(positiveResult);
    }
}
