package com.acentria.benslist;

import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

public class GetPushNotification  {

    public static final String PROPERTY_REG_ID = "123";
    private static String PROPERTY_APP_VERSION;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static String SENDER_ID;
    static GoogleCloudMessaging gcm;
    static String regid;
    static String accountId;
    static String phone_id;
    static boolean status;

    public static void regNotification( String account_id, boolean statusSend) {

        PROPERTY_APP_VERSION = Config.context.getResources().getString(R.string.plugin_min_version);
        SENDER_ID = Utils.getCacheConfig("android_google_id");
        accountId = account_id;
        status = statusSend;
        phone_id = Settings.Secure.getString(Config.context.getContentResolver(), Settings.Secure.ANDROID_ID);

        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(Config.context);
            regid = getRegistrationId();
            if (!status) {
                sendRegistrationIdToBackend();
            } else {
                if (regid.isEmpty()) {
                    registerInBackground();
                } else {
                    sendRegistrationIdToBackend();
                }
            }
        } else {
            Log.d("FD", "No valid Google Play Services APK found.");
        }

    }


    private static String getRegistrationId() {
//        final SharedPreferences prefs = getGCMPreferences(context);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Config.context.getApplicationContext());
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.d("fd", "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            Log.d("fd", "App version changed.");
            return "";
        }
        return registrationId;
    }

    private static int getAppVersion() {
        try {
            PackageInfo packageInfo = Config.context.getPackageManager()
                    .getPackageInfo(Config.context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    private static void registerInBackground() {
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... params)
            {
                String msg = "";

                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(Config.context);
                    }

                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // Persist the registration ID - no need to register again.
                    storeRegistrationId(regid);
                    if (!regid.isEmpty()) {
                        TimeUnit.SECONDS.sleep(2);
                        sendRegistrationIdToBackend();
                    }
                }
                catch (IOException ex)
                {
                    Log.d("FD", "Error");
                    msg = "Error :" + ex.getMessage();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected void onPostExecute(Object result)
            { //to do here
            };
        }.execute(null, null, null);

    }
    private static void sendRegistrationIdToBackend() {
        // Your implementation here.
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", regid);
        params.put("account_id", accountId);
        params.put("phone_id", phone_id);
        params.put("status", status ? "1" : "0" );

        final String url = Utils.buildRequestUrl("registrNotification", params, null);
        /* do request */
        AsyncHttpClient client = null;
        if (Looper.myLooper() == null) {
            client = new SyncHttpClient();
        }
        else {
            client = new AsyncHttpClient();
        }
        client.get(url, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                // called when response HTTP status is "200 OK"
                try {
                    String response = String.valueOf(new String(server_response, "UTF-8"));
                } catch (UnsupportedEncodingException e1) {

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        });
    }

    private static void storeRegistrationId(String regId) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Config.context.getApplicationContext());
        int appVersion = getAppVersion();
        Log.d("fd", "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
	
	private static boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(Config.context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, Config.context,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.d("fd", "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private static void finish() {
    }
}
