package com.acentria.benslist.controllers;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.chatprocess.MarchentListActivity;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.acentria.benslist.Account;
import com.acentria.benslist.AddListingActivity;
import com.acentria.benslist.Config;
import com.acentria.benslist.CreateAccountActivity;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.EditProfileActivity;
import com.acentria.benslist.GetPushNotification;
import com.acentria.benslist.Image;
import com.acentria.benslist.Lang;
import com.acentria.benslist.LoginActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.ResetPasswordActivity;
import com.acentria.benslist.SendFeedbackActivity;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.viewpagerindicator.TabPageIndicator;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;

import static com.acentria.benslist.AddListingActivity.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;


public class AccountArea extends AbstractController {

    private static final String TAG = "AccountArea=> ";
    private static AccountArea instance;
    private static final String Title = Lang.get("android_menu_login");
    private static final ArrayList<HashMap<String, String>> TABS = new ArrayList<HashMap<String, String>>();

    public static int[] menuItems = {R.id.menu_logout, R.id.menu_remove_account};
//	public static int[] menuItems = {R.id.menu_logout};

    public static LinearLayout profileTab;
    public static LinearLayout passwordTab;
    public static MenuItem menu_logout = (MenuItem) Config.menu.findItem(R.id.menu_logout);
    public static MenuItem menu_remove_account = (MenuItem) Config.menu.findItem(R.id.menu_remove_account);
    public static LinearLayout login_form;
    public static LinearLayout profile_layer;
    public static String loginController;

    final public static int PROFILE_IMAGE = 4001;
    public static final int FB_SIGN_IN = 64206;
    public static CallbackManager callbackManager;

    public static boolean loginSS = false;
    //public FacebookConnect fb;

    public static AccountArea getInstance() {
        if (instance == null) {
            try {
                instance = new AccountArea();
            } catch (Exception e) {
                Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
                e.printStackTrace();
            }
            Config.activeInstances.add(instance.getClass().getSimpleName());
        } else {
            Utils.restroreInstanceView(instance.getClass().getSimpleName(), Account.loggedIn ? Lang.get("my_profile") : Title);
        }

        handleMenuItems(menuItems);

        return instance;
    }

    public static void removeInstance() {
        instance = null;
        TABS.clear();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public AccountArea() {
        loginController = "AccountArea";
        /* set content title */
        Config.context.setTitle(Account.loggedIn ? Lang.get("my_profile") : Title);
        FacebookSdk.sdkInitialize(Config.context);
        /* add content view */
        Utils.addContentView(R.layout.view_account_area);

        /* get main controller view */
        View view = Config.context.findViewById(R.id.AccountArea);

        /* hide menu */
        Utils.showContent();

        /* handle layers */
        login_form = (LinearLayout) view.findViewById(R.id.login_form);
        profile_layer = (LinearLayout) view.findViewById(R.id.profile_layer);

        if (Account.loggedIn) {
            login_form.setVisibility(View.GONE);
            profile_layer.setVisibility(View.VISIBLE);

            menu_logout.setVisible(true);
            menu_remove_account.setVisible(true);
            Log.e(TAG, "user profile");
        } else {
            loginForm(login_form, Config.context);
            Log.e(TAG, "userNot login");
        }

        /* start pager */
        String[][] tabs = {
                {"profile", Lang.get("prifile_tab_name")},
                {"password", Lang.get("password_tab_name")}
        };
        Utils.adaptTabs(tabs, TABS);

        FragmentPagerAdapter adapter = new FragmentAdapter(Config.context.getSupportFragmentManager());

        ViewPager pager = (ViewPager) profile_layer.findViewById(R.id.pagerProfile);
        pager.setPageMargin(10);
        pager.setAdapter(adapter);

        TabPageIndicator indicator = (TabPageIndicator) profile_layer.findViewById(R.id.indicator);
        indicator.setViewPager(pager);
    }

    public static void requestRead(ImageView edit_avatar, AsyncHttpResponseHandler success) {
        String[] PERMISSIONS = {android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!hasPermissions(Config.context, PERMISSIONS)) {

            ActivityCompat.requestPermissions(Config.context,
                    PERMISSIONS,
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Image.openGallery(PROFILE_IMAGE, Config.context, edit_avatar, "uploadProfileImage", success);
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void loginForm(final LinearLayout login_form, final Context context) {

        /* create an account link handler */
        // make it underlined
        TextView registration_link = (TextView) login_form.findViewById(R.id.registration);
        registration_link.setPaintFlags(registration_link.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        registration_link.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Config.context, CreateAccountActivity.class);
                Config.context.startActivity(intent);
            }
        });

        /* reset password link handler */
        TextView reset_link = (TextView) login_form.findViewById(R.id.reset_password);
        reset_link.setPaintFlags(reset_link.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        reset_link.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Config.context, ResetPasswordActivity.class);
                Config.context.startActivity(intent);
            }
        });

        /* fix the password field font */
        final EditText password = (EditText) login_form.findViewById(R.id.password);
        password.setTypeface(Typeface.DEFAULT);

        if (Utils.getCacheConfig("account_login_mode").equals("email")) {
            EditText username = (EditText) login_form.findViewById(R.id.username);
            username.setHint(Lang.get("android_hint_email"));
        }

        /* login button handler */
        Button login_button = (Button) login_form.findViewById(R.id.login);
        login_button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean error = false;

                /* validate fields */
                EditText username = (EditText) login_form.findViewById(R.id.username);
                if (username.getText().toString().isEmpty()) {
                    username.setError(Lang.get("no_field_value"));
                    error = true;
                }

                if (password.getText().toString().isEmpty()) {
                    password.setError(Lang.get("no_field_value"));
                    error = true;
                }

                if (!error) {
                    final String passwordHash = password.getText().toString();

                    /* show progressbar */
                    final ProgressDialog login_progress = ProgressDialog.show(Config.context, null, Lang.get("loading"));

                    /* build request url */
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("username", username.getText().toString());
                    params.put("password", passwordHash);
                    final String url = Utils.buildRequestUrl("loginAttempt", params, null);

                    /* do request */
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.get(url, new AsyncHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                            // called when response HTTP status is "200 OK"
                            try {
                                String response = String.valueOf(new String(server_response, "UTF-8"));
                                login_progress.dismiss();
                                /* parse xml response */
                                XMLParser parser = new XMLParser();
                                Document doc = parser.getDomElement(response, url);

                                if (doc == null) {
                                    Dialog.simpleWarning(Lang.get("returned_xml_failed"), context);
                                } else {
                                    NodeList accountNode = doc.getElementsByTagName("account");
                                    Element element = (Element) accountNode.item(0);

                                    /* list of status to offer "Contact Us" page */
                                    String status = Utils.getNodeByName(element, "status");
                                    String[] tmp_status = {"approval", "pending", "trash"};
                                    List<String> contact_status = Utils.string2list(tmp_status);

                                    /* error */
                                    if (status.equals("error")) {
                                        Dialog.simpleWarning(Lang.get("dialog_login_error"), context);
                                        password.setText("");
                                    } else if (status.equals("incomplete")) {
                                        Dialog.simpleWarning(Lang.get("dialog_login_" + status + "_info"), context);
                                    }
                                    /* status matched contact us message logic */
                                    else if (contact_status.indexOf(status) >= 0) {
                                        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(Config.context, SendFeedbackActivity.class);
                                                intent.putExtra("selection", "contact_us");
                                                Config.context.startActivity(intent);
                                            }
                                        };

                                        Dialog.CustomDialog(Lang.get("dialog_login_" + status), Lang.get("dialog_login_" + status + "_info"), context, listener);
                                    }

                                    /* do login if account */
                                    if (!Utils.getNodeByName(element, "id").isEmpty()) {
                                        /* hide keyboard */
                                        Utils.hideKeyboard(login_form.findFocus());

                                        confirmLogin(accountNode);
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
            }
        });

        /* fb button handler */
        String facebook_id = Config.context.getString(R.string.app_id);
        if (Utils.getCacheConfig("facebookConnect_plugin").equals("1") && !facebook_id.isEmpty()) {
            LoginButton fbLogin = (LoginButton) login_form.findViewById(R.id.fbLogin);
            fbLogin.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);

            /* set related items visible */
            fbLogin.setVisibility(View.VISIBLE);
            login_form.findViewWithTag("fbview").setVisibility(View.VISIBLE);

            callbackManager = CallbackManager.Factory.create();

            fbLogin.setReadPermissions(Arrays.asList("public_profile", "email"));
            fbLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

                @Override
                public void onSuccess(LoginResult loginResult) {
                    GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                            new GraphRequest.GraphJSONObjectCallback() {

                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {
                                    // Application code
									/*Boolean ver = Boolean.parseBoolean(object.optString("verified"));
									if ( !ver ) {
										Dialog.simpleWarning(Lang.get("facebook_not_verified"), context);
									}
									else */
                                    if (object != null) {
                                        final HashMap<String, String> formData = new HashMap<String, String>();
                                        formData.put("username", object.optString("name"));
                                        formData.put("password", "will-be-generated");
                                        formData.put("email", object.optString("email"));
                                        formData.put("account_type", "will-be-set");
                                        formData.put("fb_id", object.optString("id"));
                                        formData.put("first_name", object.optString("first_name"));
                                        formData.put("last_name", object.optString("last_name"));

                                        /* show progressbar */
                                        final ProgressDialog progress = ProgressDialog.show(Config.context, null, Lang.get("loading"));

                                        /* do request */
                                        AsyncHttpClient client = new AsyncHttpClient();
                                        client.setTimeout(30000); // set 30 seconds for this task

                                        final String url = Utils.buildRequestUrl("createAccount");
                                        client.post(url, Utils.toParams(formData), new AsyncHttpResponseHandler() {

                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                                                // called when response HTTP status is "200 OK"
                                                try {
                                                    String response = String.valueOf(new String(server_response, "UTF-8"));
                                                    /* hide progressbar */
                                                    progress.dismiss();

                                                    /* parse xml response */
                                                    XMLParser parser = new XMLParser();
                                                    Document doc = parser.getDomElement(response, url);

                                                    if (doc == null) {
                                                        Dialog.simpleWarning(Lang.get("returned_xml_failed"), context);
                                                    } else {
                                                        NodeList errorsNode = doc.getElementsByTagName("errors");

                                                        /* handle errors */
                                                        if (errorsNode.getLength() > 0) {
                                                            Element element = (Element) errorsNode.item(0);
                                                            NodeList errors = element.getChildNodes();

                                                            if (errors.getLength() > 0) {
                                                                Element error = (Element) errors.item(0);
                                                                String key_error = error.getTextContent();
                                                                if (key_error.equals("fb_email_exists")) {
                                                                    checkFbPassword(formData, context);
                                                                } else {
                                                                    Dialog.simpleWarning(Lang.get(key_error), context);
                                                                }
                                                                LoginManager.getInstance().logOut();
                                                            }
                                                        }
                                                        /* process login */
                                                        else {
                                                            NodeList accountNode = doc.getElementsByTagName("account");
                                                            confirmLogin(accountNode);
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
                                    } else {
                                        Log.d("FD", "FB connect no user");
                                    }
                                }
                            });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "id,name,email,gender,first_name,last_name,verified,birthday");
                    request.setParameters(parameters);
                    request.executeAsync();

                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onError(FacebookException e) {

                }
            });
        }
    }

    public static void confirmLogin(NodeList accountNode) {
        loginSS = true;

        Element element = (Element) accountNode.item(0);
        NodeList account = element.getChildNodes();
        /* fetch account data */
        Account.fetchAccountData(account);

        if (loginController.equals("AddListing")) {
            AddListingActivity.loginAddListing();
        } else if (loginController.equals("login")) {
            LoginActivity.confirmLogin();
        } else {
            /* login user */
            LinearLayout login_form = (LinearLayout) Config.context.findViewById(R.id.login_form);
            LinearLayout profile_layer = (LinearLayout) Config.context.findViewById(R.id.profile_layer);
            Account.login(Account.accountData.get("password"), login_form, profile_layer);

            GetPushNotification.regNotification(Account.accountData.get("id"), true);
        }
    }

    public static void checkFbPassword(final HashMap<String, String> formData, final Context context) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Config.context);

        View view = Config.context.getLayoutInflater().inflate(R.layout.facebook_password, null);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(Utils.dp2px(35), Utils.dp2px(35), Utils.dp2px(35), 0);
        view.setLayoutParams(params);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(view);
        final EditText fb_password = (EditText) view.findViewById(R.id.fb_password);
        fb_password.setHint(Lang.get("android_hint_password"));

        // set dialog message
        alertDialogBuilder
                .setTitle(Lang.get("android_password_reset_here"))
                .setCancelable(false)
                .setPositiveButton(Lang.get("android_dialog_ok"), null)
                .setNegativeButton(Lang.get("android_dialog_cancel"), null);

        // create alert dialog
        final AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean error = false;
                if (fb_password.getText().toString().isEmpty()) {
                    fb_password.setError(Lang.get("no_field_value"));
                    error = true;
                } else {
                    fb_password.setError(null);
                    error = false;
                }

                if (error == false) {

                    formData.put("fb_password", fb_password.getText().toString());

                    /* show progressbar */
                    final ProgressDialog progress = ProgressDialog.show(Config.context, null, Lang.get("loading"));

                    /* do request */
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.setTimeout(30000); // set 30 seconds for this task

                    final String url = Utils.buildRequestUrl("createAccount");
                    client.post(url, Utils.toParams(formData), new AsyncHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                            // called when response HTTP status is "200 OK"
                            try {
                                String response = String.valueOf(new String(server_response, "UTF-8"));
                                /* hide progressbar */
                                progress.dismiss();

                                /* parse xml response */
                                XMLParser parser = new XMLParser();
                                Document doc = parser.getDomElement(response, url);

                                if (doc == null) {
                                    Dialog.simpleWarning(Lang.get("returned_xml_failed"), context);
                                } else {
                                    NodeList errorsNode = doc.getElementsByTagName("errors");

                                    /* handle errors */
                                    if (errorsNode.getLength() > 0) {
                                        Element element = (Element) errorsNode.item(0);
                                        NodeList errors = element.getChildNodes();

                                        if (errors.getLength() > 0) {
                                            Element error = (Element) errors.item(0);
                                            String key_error = error.getTextContent();
                                            Dialog.simpleWarning(Lang.get(key_error), context);
                                        }
                                    }
                                    /* process login */
                                    else {
                                        NodeList accountNode = doc.getElementsByTagName("account");
                                        confirmLogin(accountNode);
                                        alertDialog.dismiss();
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
            }

        });
    }

    public static void logout() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Config.context.setTitle(Title);

                login_form.setVisibility(View.VISIBLE);
                profile_layer.setVisibility(View.GONE);

                GetPushNotification.regNotification(Account.accountData.get("id"), false);
                Log.e(TAG, "logout" + Account.accountData.get("id"));

                Account.logout();
                Dialog.toast("logout_completed");

                menu_logout.setVisible(false);
                menu_remove_account.setVisible(false);

                if (Config.activeInstances.contains("MyListings")) {
                    Config.activeInstances.remove("MyListings");
                    MyListings.removeInstance();

                    Utils.removeContentView("MyListings");
                }
                loginForm(login_form, Config.context);

                LoginManager.getInstance().logOut();
            }
        };

        Dialog.CustomDialog(Lang.get("logout"), Lang.get("confirm_logout"), null, listener);
    }


    class FragmentAdapter extends FragmentPagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ProfileFragment.newInstance(TABS.get(position).get("key"));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TABS.get(position).get("name").replace(" ", "\u00A0").toUpperCase(); // converting to non-breaking space
        }

        @Override
        public int getCount() {
            return TABS.size();
        }

        @Override
        public void destroyItem(View container, int position, Object object) {
        }
    }

    public final static class ProfileFragment extends Fragment {

        private static final String TAG = "ProfileFragment TAG Adapter=> ";
        private String tabKey = "";

        public static ProfileFragment newInstance(String key) {
            ProfileFragment fragment = new ProfileFragment();
            fragment.tabKey = key.toString();
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @SuppressLint("LongLogTag")
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View layout = null;
            if (tabKey.equals("profile")) {
                layout = createPrifileTab();
                Log.e(TAG, "createProfilelayout" + tabKey);
            } else if (tabKey.equals("password")) {
                layout = createPasswordTab();
                Log.e(TAG, "passwordlayout" + tabKey);
            }

            return layout;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }
    }

    private static LinearLayout createPrifileTab() {
        profileTab = (LinearLayout) Config.context.getLayoutInflater()
                .inflate(R.layout.profile_tab, null);

        profileTab.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));



        /* profile image handler */
        final ImageView edit_avatar = (ImageView) profileTab.findViewById(R.id.profileImage);
        edit_avatar.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AsyncHttpResponseHandler success = new AsyncHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                        // called when response HTTP status is "200 OK"
                        try {
                            String response = String.valueOf(new String(server_response, "UTF-8"));
                            /* parse xml response */
                            XMLParser parser = new XMLParser();
                            Document doc = parser.getDomElement(response, "uploadProfileImage request");

                            if (doc == null) {
                                Dialog.simpleWarning(Lang.get("returned_xml_failed"));
                            } else {
                                NodeList errorNode = doc.getElementsByTagName("error");

                                /* handle errors */
                                if (errorNode.getLength() > 0) {
                                    Element error = (Element) errorNode.item(0);
                                    Dialog.simpleWarning(Lang.get(error.getTextContent()));

                                    if (!Account.accountData.get("photo").isEmpty()) {
                                        Utils.imageLoaderDisc.displayImage(Account.accountData.get("photo"), edit_avatar, Utils.imageLoaderOptionsDisc);
                                    } else {
                                        edit_avatar.setImageBitmap(null);
                                    }
                                }
                                /* seve uploaded profile image name */
                                else {
                                    NodeList accountNode = doc.getElementsByTagName("account");

                                    if (accountNode.getLength() > 0) {
                                        Element account = (Element) accountNode.item(0);
                                        Account.accountData.put("photo", Lang.get(account.getTextContent()));
                                    }
                                }
                            }

                        } catch (UnsupportedEncodingException e1) {

                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                    }
                };
                requestRead(edit_avatar, success);
            }
        });

        /* edit profile button handler */
        Button edit_profile = (Button) profileTab.findViewById(R.id.editProfile);
        edit_profile.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent intent = new Intent(Config.context, EditProfileActivity.class);
                Config.context.startActivity(intent);
            }
        });

        Button btn_chat = profileTab.findViewById(R.id.btn_chat);
        btn_chat.setText("Chat");
        btn_chat.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "chat window");
                Config.context.startActivity(new Intent(Config.context, MarchentListActivity.class));
            }
        });


        if (Account.loggedIn) {
            Account.populateProfileTab();
        }

        return profileTab;
    }

    private static LinearLayout createPasswordTab() {
        passwordTab = (LinearLayout) Config.context.getLayoutInflater()
                .inflate(R.layout.password_tab, null);

        passwordTab.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        /* change password handler */
        final EditText current_password = (EditText) passwordTab.findViewById(R.id.curren_password);
        current_password.setTypeface(Typeface.DEFAULT);

        final EditText new_password = (EditText) passwordTab.findViewById(R.id.new_password);
        new_password.setTypeface(Typeface.DEFAULT);

        final EditText repeat_password = (EditText) passwordTab.findViewById(R.id.repeat_password);
        repeat_password.setTypeface(Typeface.DEFAULT);

        Button password_submit = (Button) passwordTab.findViewById(R.id.password_submit);
        password_submit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                boolean error = false;


                /* new password strange rules */
                final String new_pass = new_password.getText().toString();
                Matcher matcher = Pattern.compile("((?=.*\\d)(?=.*[a-z]).{5,20})").matcher(new_pass);
                if (!matcher.matches() && !error) {
                    error = true;
                    new_password.setError(Lang.get("password_weak"));
                    new_password.requestFocus();
                }

                if (!error && !new_pass.equals(repeat_password.getText().toString())) {
                    error = true;
                    repeat_password.setError(Lang.get("password_does_not_match"));
                    repeat_password.requestFocus();
                }

                if (!error) {
                    /* show progress dialog */
                    final ProgressDialog progress = ProgressDialog.show(Config.context, null, Lang.get("loading"));

                    /* get form data - build request url */
                    HashMap<String, String> params = new HashMap<String, String>();
                    //params.put("type", typeKey);
                    params.put("account_id", Account.accountData.get("id"));
                    params.put("password_hash", current_password.getText().toString());
                    params.put("new_password_hash", new_pass);
                    final String url = Utils.buildRequestUrl("changePassword", params, null);

                    /* do async request */
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.get(url, new AsyncHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                            // called when response HTTP status is "200 OK"
                            try {
                                String response = String.valueOf(new String(server_response, "UTF-8"));
                                progress.dismiss();

                                /* parse response */
                                XMLParser parser = new XMLParser();
                                Document doc = parser.getDomElement(response, url);

                                if (doc == null) {
                                    Dialog.simpleWarning(Lang.get("returned_xml_failed"), Config.context);
                                } else {
                                    NodeList errorNode = doc.getElementsByTagName("error");

                                    /* handle errors */
                                    if (errorNode.getLength() > 0) {
                                        Element error = (Element) errorNode.item(0);
                                        Dialog.simpleWarning(Lang.get(error.getTextContent()));
                                    }
                                    /* finish this activity and show toast */
                                    else {
                                        NodeList successNode = doc.getElementsByTagName("success");
                                        if (successNode.getLength() > 0) {
                                            Element node_password = (Element) successNode.item(0);
                                            String new_password_hashe = node_password.getTextContent();

                                            Toast.makeText(Config.context, Lang.get("password_changed"), Toast.LENGTH_LONG).show();
                                            Utils.setSPConfig("accountPassword", new_password_hashe);

                                            /* clear fields */
                                            current_password.setText("");
                                            new_password.setText("");
                                            repeat_password.setText("");

                                            /* hide keyboard */
                                            Utils.hideKeyboard(passwordTab.findFocus());
                                        } else {
                                            Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"));
                                        }
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
            }
        });

        return passwordTab;
    }

    // remove account
    public static void removeAccount() {

        AlertDialog.Builder builder = new AlertDialog.Builder(Config.context);
        builder.setTitle(Lang.get("android_remove_account"));
        LayoutInflater content = LayoutInflater.from(Config.context);

        LinearLayout remove_layout = (LinearLayout) content.inflate(R.layout.remove_account, null);
        builder.setView(remove_layout);

        final EditText password = (EditText) remove_layout.findViewById(R.id.password);

        builder.setMessage(Lang.get("android_remove_account_desc"));
        builder.setNeutralButton(Lang.get("android_dialog_cancel"), null);
        builder.setPositiveButton(Lang.get("android_remove_account"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();


        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pass_text = password.getText().toString();

                if (!pass_text.isEmpty()) {
                    final ProgressDialog progress = ProgressDialog.show(Config.context, null, Lang.get("android_loading"));
                    /* build request url */
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("account_id", Account.accountData.get("id"));
                    params.put("password_hash", Utils.getSPConfig("accountPassword", null));
                    params.put("password_confirm", pass_text);

                    final String url = Utils.buildRequestUrl("deleteAccount", params, null);
                    Log.e(TAG, "delete account api url" + url);

                    /* do request */
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.get(url, new AsyncHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                            // called when response HTTP status is "200 OK"
                            try {
                                progress.dismiss();
                                String response = String.valueOf(new String(server_response, "UTF-8"));
                                // parse response
                                XMLParser parser = new XMLParser();
                                Document doc = parser.getDomElement(response, url);

                                if (doc == null) {
                                    Dialog.simpleWarning(Lang.get("returned_xml_failed"));
                                } else {
                                    NodeList successNode = doc.getElementsByTagName("success");
                                    if (successNode.getLength() > 0) {
                                        Element success = (Element) successNode.item(0);
                                        Config.context.setTitle(Title);

                                        login_form.setVisibility(View.VISIBLE);
                                        profile_layer.setVisibility(View.GONE);

                                        Account.logout();

                                        Dialog.toast(success.getTextContent());

                                        menu_logout.setVisible(false);
                                        menu_remove_account.setVisible(false);

                                        if (Config.activeInstances.contains("MyListings")) {
                                            Config.activeInstances.remove("MyListings");
                                            MyListings.removeInstance();

                                            Utils.removeContentView("MyListings");
                                        }
                                        loginForm(login_form, Config.context);

                                        LoginManager.getInstance().logOut();
                                        dialog.dismiss();
                                    } else {
                                        NodeList errorNode = doc.getElementsByTagName("error");

                                        if (errorNode.getLength() > 0) {
                                            // handle errors
                                            Element error = (Element) errorNode.item(0);
                                            Dialog.simpleWarning(Lang.get(error.getTextContent()));
                                        }
                                    }
                                }

                            } catch (UnsupportedEncodingException e1) {

                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                            // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                            progress.dismiss();
                        }
                    });
                } else {
                    password.setError(Lang.get("no_field_value"));
                }
                password.setText("");
            }
        });
    }


}