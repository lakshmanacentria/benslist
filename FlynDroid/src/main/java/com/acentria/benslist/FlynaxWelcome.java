package com.acentria.benslist;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.acentria.benslist.controllers.Home;
import com.acentria.benslist.controllers.MyMessages;
import com.acentria.benslist.controllers.SavedSearch;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;


public class FlynaxWelcome {

    private static final String TAG = "FlynaxWelcome=> ";
    public static ProgressDialog progressDialog;

    public static void showLaunch() {
        Config.context.getSupportActionBar().hide();
        Config.context.setContentView(R.layout.activity_flyndroid);
    }

    public static void animateRefresh() {

        DisplayMetrics displaymetrics = new DisplayMetrics();
        Config.context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int pos = (int) (displaymetrics.scaledDensity * 30 * -1);

        // set animation parameters
        TranslateAnimation anim = new TranslateAnimation(0, 0, 0, pos);
        anim.setDuration(1000);
        anim.setFillAfter(true);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                final LinearLayout run_form = (LinearLayout) Config.context.findViewById(R.id.refresh_form);
                AlphaAnimation fade_in = new AlphaAnimation(0.0f, 1.0f);
                fade_in.setDuration(1000);
                fade_in.setFillAfter(true);
                run_form.startAnimation(fade_in);
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationStart(Animation animation) {
                // TODO Auto-generated method stub
            }
        });

        // start animating the logo
        final ImageView logo = (ImageView) Config.context.findViewById(R.id.welcome_logo);
        logo.startAnimation(anim);

        // set refresh button listener
        final Button go_button = (Button) Config.context.findViewById(R.id.welcome_refresh);
        go_button.setVisibility(View.VISIBLE);
        go_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                go_button.setVisibility(View.GONE);
                checkAvailability(true);
            }
        });
    }

    @SuppressLint("NewApi")
    public static void animateForm() {

        Config.context.getSupportActionBar().hide();
        Config.context.setContentView(R.layout.activity_flyndroid);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        Config.context.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        int pos = (int) (displaymetrics.scaledDensity * 62 * -1);

        // set animation parameters
        TranslateAnimation anim = new TranslateAnimation(0, 0, 0, pos);
        anim.setDuration(1000);
        anim.setStartOffset(2000);
        anim.setFillAfter(true);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                final LinearLayout run_form = (LinearLayout) Config.context.findViewById(R.id.run_form);
                AlphaAnimation fade_in = new AlphaAnimation(0.0f, 1.0f);
                fade_in.setDuration(1000);
                fade_in.setFillAfter(true);
                run_form.startAnimation(fade_in);
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onAnimationStart(Animation animation) {
                // TODO Auto-generated method stub
            }
        });

        // start animating the logo
        final ImageView logo = (ImageView) Config.context.findViewById(R.id.welcome_logo);
        logo.startAnimation(anim);

        // set go button listener
        final Button go_button = (Button) Config.context.findViewById(R.id.welcome_go);
        go_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                /* hide keyboard */
                EditText editText = (EditText) Config.context.findViewById(R.id.domain_name);
                Utils.hideKeyboard(editText);

                welcomeConnect(v);
            }
        });

        final Button demoButton = (Button) Config.context.findViewById(R.id.try_demo);
        demoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Dialog.ListDialog();
            }
        });
    }

    /**
     * Call Home Activity
     **/
    public static void switchToHome() {
        Config.context.setContentView(R.layout.responsive_content_frame);

        Config.contentFrame = (FrameLayout) Config.context.findViewById(R.id.content_frame);

        /* enable back arrow icon */
        ActionBar actionBar = Config.context.getSupportActionBar();
        if (!Config.tabletMode) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setIcon(android.R.color.transparent);
        } else {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
        }
        actionBar.show();

        /* init swipe menu */
        new SwipeMenu();

        /* translate menu items */
        Utils.translateMenuItems(Config.menu);

        /* init home controller */
        Home.getInstance();

        if (Config.pushView != null) {
            if (Config.pushView.equals("message") && Account.loggedIn) {
                MyMessages.switchToMyMessages();
            } else if (Config.pushView.equals("save_search") && Account.loggedIn) {
                SavedSearch.switchToSavedSearch(Config.configIntent);
            }
        }
    }

    /**
     * check application availability
     *
     * @param fromForm - called by click refresh button or from stream
     */
    public static void checkAvailability(boolean fromForm) {
        boolean available = true;

        /* check network availability */
        if (!Utils.isNetworkAvailable()) {
            if (fromForm) {
                Dialog.simpleWarning(R.string.dialog_network_unavailable);
            } else {
                Dialog.welcomeNetworkUnavailable(R.string.dialog_network_unavailable);
            }
            available = false;
        }

        /* check resource availability */
        if (available) {
            final String url = Utils.buildRequestUrl("isPluginAvailable", new HashMap<String, String>(), null);
            Log.e(TAG, "plagin Api" + url);

            AsyncHttpClient client = new AsyncHttpClient();
            client.get(url, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                    // called when response HTTP status is "200 OK"

                    try {
                        String json = new String(server_response);
                        String response = String.valueOf(new String(server_response, "UTF-8"));
                        Log.e(TAG, "PluginapiResponse=> " + response);
                        HashMap<String, String> data = JSONParser.parseJson(response);

                        //						String response = String.valueOf(new String(server_response, "UTF-8"));
                        //						XMLParser parser = new XMLParser();
                        //						Document doc = parser.getDomElement(response, url); // getting DOM element

                        if (data == null || data.isEmpty()) {
                            String customer_domain = Config.context.getString(R.string.customer_domain);
                            if (customer_domain.isEmpty()) {
                                Dialog.simpleWarning(R.string.dialog_plugin_isset);
                                animateForm();
                            } else {
                                Dialog.simpleWarning(R.string.dialog_host_unavailable);
                                animateRefresh();
                            }
                        } else {

                            Config.isHTTPS = data.get("https").equals("true") ? "1" : "";

                            String availability = data.get("available");
                            String versionName = null;
                            try {
                                versionName = Config.context.getPackageManager().getPackageInfo(Config.context.getPackageName(), 0).versionName;
                            } catch (PackageManager.NameNotFoundException e1) {
                                e1.printStackTrace();
                            }

                            int app_version = Config.compireVersion(data.get("app_version"), versionName);
                            int version = Config.compireVersion(data.get("version"), Config.context.getResources().getString(R.string.plugin_min_version));
                            if (app_version == 1) {
                                Dialog.confirmActionApp(R.string.dialog_updated_app, Config.context, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Config.updateVersionApp();
                                    }
                                }, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        FlynaxWelcome.animateRefresh();
                                    }
                                });
                            } else if (availability.equals("1") && version != -1) {
                                Config.initCache(false);
                            } else {
                                Dialog.welcomeHostUnavailable(version == -1 && availability.equals("1") ? R.string.dialog_plugin_need_to_update : R.string.dialog_host_unavailable);
                            }
                        }

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    Dialog.welcomeHostUnavailable(R.string.dialog_host_unavailable);
                }
            });
        } else {
            Button go_button = (Button) Config.context.findViewById(R.id.welcome_refresh);
            go_button.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Try to connect to the server
     **/
    public static void welcomeConnect(View v) {
        // TODO make an item in a property
        EditText editText = (EditText) Config.context.findViewById(R.id.domain_name);
        final String domain = editText.getText().toString().trim();

        if (!Utils.isNetworkAvailable()) {
            Dialog.simpleWarning(R.string.dialog_network_unavailable);
            return;
        }

        if (!Utils.isDomain(domain)) {
            Dialog.simpleWarning(R.string.dialog_invalid_domain);
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("lang_def", "1");
        // check installed plugin /
        final String url = Utils.buildRequestUrl("isPluginAvailable", params, domain);
        Log.e(TAG, "welcomeConnect " + url);

        // TODO move to all-sufficient method
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
                /* Display preload */
                String load = Config.context.getResources().getString(R.string.loading);
                progressDialog = ProgressDialog.show(Config.context, null, load);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                // called when response HTTP status is "200 OK"

                // Error messages
                String title = Config.context.getResources().getString(R.string.home_title_mess);

                try {
                    String response = String.valueOf(new String(server_response, "UTF-8"));
                    Log.e(TAG, "Connect to server Response=> " + response);
//					String json = new String(server_response);
                    HashMap<String, String> data = JSONParser.parseJson(response);

                    Config.isHTTPS = data.get("https").equals("true") ? "1" : "";

                    if (data.containsKey("android_lang")) {
                        String def_lang = data.get("android_lang");
                        Log.e(TAG, "setdef_lang in Prefrence=>" + def_lang);
                        Utils.setSPConfig("select_lang", def_lang);
                    }

                    String availability = data.get("available");
                    String versionName = null;
                    try {
                        versionName = Config.context.getPackageManager().getPackageInfo(Config.context.getPackageName(), 0).versionName;
                    } catch (PackageManager.NameNotFoundException e1) {
                        e1.printStackTrace();
                    }

                    int app_version = Config.compireVersion(data.get("app_version"), versionName);
                    int version = Config.compireVersion(data.get("version"), Config.context.getResources().getString(R.string.plugin_min_version));
                    int phrase_key = version == -1 && availability.equals("1") ? R.string.dialog_plugin_need_to_update : R.string.dialog_plugin_isset;
                    String message = Config.context.getResources().getString(phrase_key);

                    Log.d("FD ve|", data.get("app_version") + " " + versionName);


                    if (app_version == 1) {
                        Dialog.confirmActionApp(R.string.dialog_updated_app, Config.context, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Config.updateVersionApp();
                            }
                        }, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                FlynaxWelcome.animateRefresh();
                                progressDialog.dismiss();
                            }
                        });
                    } else if (availability.equals("1") && version != -1) {
                        Utils.setSPConfig("domain", domain);

                        /* initializa website cache */
                        Config.initCache(true);
                    } else {
                        Dialog.CustomDialog(title, message);
                        progressDialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(Config.context, "Config initialization failed, bug report sent to developers", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();

                    Utils.bugRequest("Config initialization failed (" + Utils.getSPConfig("domain", "") + ")", e.getStackTrace(), domain);

                    return;
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                progressDialog.dismiss();
                Dialog.CustomDialog("Error message", "Can't resolve host !");
            }
        });
    }
}
