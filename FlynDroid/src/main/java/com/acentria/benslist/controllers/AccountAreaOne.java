package com.acentria.benslist.controllers;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
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
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
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
import static com.acentria.benslist.SwipeMenu.populateMenu;


public class AccountAreaOne extends AppCompatActivity {

    private static final String TAG = "AccountAreaOne=> ";
    private static AccountAreaOne instance;
    private static final String Title = Lang.get("android_menu_login");
    private static final ArrayList<HashMap<String, String>> TABS = new ArrayList<HashMap<String, String>>();

    public static int[] menuItems = {R.id.menu_logout, R.id.menu_remove_account};
//	public static int[] menuItems = {R.id.menu_logout};

    public static LinearLayout profileTab;
    public static LinearLayout passwordTab;
    public static MenuItem menu_logout = (MenuItem) Config.menu.findItem(R.id.menu_logout);
    public static MenuItem menu_remove_account = (MenuItem) Config.menu.findItem(R.id.menu_remove_account);
    public static LinearLayout login_form;
    public static LinearLayout profile_layer, ll_charity, ll_charityMainView, ll_foodMainView;
    public static String loginController;

    final public static int PROFILE_IMAGE = 4001;
    public static final int FB_SIGN_IN = 64206;
    public static CallbackManager callbackManager;

    public static boolean loginSS = false;
    //public FacebookConnect fb;

    public static AccountAreaOne getInstance() {
        if (instance == null) {
            try {
                instance = new AccountAreaOne();
            } catch (Exception e) {
                Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
                e.printStackTrace();
            }
            Config.activeInstances.add(instance.getClass().getSimpleName());
        } else {
            Utils.restroreInstanceView(instance.getClass().getSimpleName(), Account.loggedIn ? Lang.get("my_profile") : Title);
        }

       // handleMenuItems(menuItems);

        return instance;
    }

    public static void removeInstance() {
        instance = null;
        TABS.clear();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public AccountAreaOne() {
        loginController = "AccountAreaOne";
        //


        //
        /* set content title */
       // Config.context.setTitle(Account.loggedIn ? Lang.get("my_profile") : Title);
      //  FacebookSdk.sdkInitialize(Config.context);
        /* add content view */
        Utils.addContentView(R.layout.view_account_area2);

        /* get main controller view */
        View view = Config.context.findViewById(R.id.AccountArea2);

        /* hide menu */
        Utils.showContent();

        /* handle layers */

        profile_layer = (LinearLayout) view.findViewById(R.id.profile_layer);
        ll_charity = (LinearLayout) view.findViewById(R.id.ll_charity);
        ll_charityMainView = (LinearLayout) view.findViewById(R.id.ll_charityMainView);
        ll_foodMainView = (LinearLayout) view.findViewById(R.id.ll_foodMainView);


        Log.e(TAG, "CharityShow ");
        ll_foodMainView.setVisibility(View.GONE);
        ll_charityMainView.setVisibility(View.VISIBLE);
//        charityFrom(ll_charityMainView, Config.context);
        profile_layer.setVisibility(View.GONE);
        menu_logout.setVisible(true);
        menu_remove_account.setVisible(true);
      /*  if (Config.loginStatus .equalsIgnoreCase("charity")) {



            *//*Clear session*//*
        } else if (Config.loginStatus .equalsIgnoreCase("food")) {


            //  profile_layer.setVisibility(View.GONE);
            ll_charityMainView.setVisibility(View.GONE);
            ll_foodMainView.setVisibility(View.VISIBLE);
            Log.e(TAG, "Food Show ");
            *//*Clear session*//*
        }*/
       /*
        if (Account.loggedIn) {

            if (Config.loginStatus .equalsIgnoreCase("charity")) {
                Log.e(TAG, "CharityShow ");
                ll_foodMainView.setVisibility(View.GONE);
                profile_layer.setVisibility(View.GONE);
                ll_charityMainView.setVisibility(View.VISIBLE);
                charityFrom(ll_charityMainView, Config.context);


                *//*Clear session*//*
            } else if (Config.loginStatus .equalsIgnoreCase("food")) {


              //  profile_layer.setVisibility(View.GONE);
                ll_charityMainView.setVisibility(View.GONE);
                ll_foodMainView.setVisibility(View.VISIBLE);
                Log.e(TAG, "Food Show ");
                *//*Clear session*//*
            } else {
                ll_foodMainView.setVisibility(View.GONE);
                ll_charityMainView.setVisibility(View.GONE);
                profile_layer.setVisibility(View.VISIBLE);
                Log.e(TAG, "others Show ");
            }
            //
            menu_logout.setVisible(true);
            menu_remove_account.setVisible(true);
            Log.e(TAG, "user profile");
        } else {
            loginForm(login_form, Config.context);
            Log.e(TAG, "userNot login");
        }*/

        /* start pager */
        String[][] tabs = {
                {"profile", Lang.get("prifile_tab_name")},
                {"password", Lang.get("password_tab_name")}

        };
       /* Utils.adaptTabs(tabs, TABS);

        FragmentPagerAdapter adapter = new FragmentAdapter(Config.context.getSupportFragmentManager());

        ViewPager pager = (ViewPager) profile_layer.findViewById(R.id.pagerProfile);
        pager.setPageMargin(10);
        pager.setAdapter(adapter);

        TabPageIndicator indicator = (TabPageIndicator) profile_layer.findViewById(R.id.indicator);
        indicator.setViewPager(pager);*/
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }







    public static void logout() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Config.context.setTitle(Title);

              //  login_form.setVisibility(View.VISIBLE);
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
                //loginForm(login_form, Config.context);



                LoginManager.getInstance().logOut();
            }
        };

        Dialog.CustomDialog(Lang.get("logout"), Lang.get("confirm_logout"), null, listener);
    }




    public final static class CharityCreateFragment extends Fragment {

        private static final String TAG = "CharityCreateFragment TAG Adapter=> ";
        private String tabKey = "";

        public static CharityCreateFragment newInstance(String key) {
            CharityCreateFragment fragment = new CharityCreateFragment();
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
            if (tabKey.equals("tab1")) {
                layout = createCharityTab();
                Log.e(TAG, "tab1" + tabKey);
            } else if (tabKey.equals("tab2")) {
                layout = donateCharityTab();
                Log.e(TAG, "tab2" + tabKey);
            }

            return layout;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
        }
    }

    private static LinearLayout createCharityTab() {
        profileTab = (LinearLayout) Config.context.getLayoutInflater()
                .inflate(R.layout.fragment_create_charity_layout, null);

        profileTab.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));



        /* profile image handler */
        //final ImageView edit_avatar = (ImageView) profileTab.findViewById(R.id.profileImage);


        return profileTab;
    }

    private static LinearLayout donateCharityTab() {
        passwordTab = (LinearLayout) Config.context.getLayoutInflater()
                .inflate(R.layout.donate_charity_tab, null);

        passwordTab.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        /* change password handler */
        ///final EditText current_password = (EditText) passwordTab.findViewById(R.id.curren_password);


        return passwordTab;
    }


    ///

    // remove account



    //////////////------------------------------------------------laxman---------------------------------------------

//    private void charityFrom(final LinearLayout charity_from, final Context context) {
//        Config.context.setTitle("Charity");
//        TextView tv_CharityCreate = charity_from.findViewById(R.id.tv_CharityCreate);
//        TextView tv_DonateCharity = charity_from.findViewById(R.id.tv_DonateCharity);
//
//        tv_CharityCreate.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                charity_from.findViewById(R.id.ll_donateCharity).setVisibility(View.GONE);
//                charity_from.findViewById(R.id.ll_charity).setVisibility(View.VISIBLE);
//
//                charity_from.findViewById(R.id.dividerCharity).setBackgroundColor(Color.parseColor("#10B660")); // .setVisibility(View.GONE);
//                charity_from.findViewById(R.id.dividerDonate).setBackgroundColor(Color.parseColor("#7C7A7A"));//greem
//                setDataCreateCharity(charity_from);
//            }
//        });
//        tv_DonateCharity.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                charity_from.findViewById(R.id.ll_donateCharity).setVisibility(View.VISIBLE);
//                charity_from.findViewById(R.id.ll_charity).setVisibility(View.GONE);
//
//                charity_from.findViewById(R.id.dividerCharity).setBackgroundColor(Color.parseColor("#7C7A7A")); // .setVisibility(View.GONE);
//                charity_from.findViewById(R.id.dividerDonate).setBackgroundColor(Color.parseColor("#10B660"));
//
//
//                setDataDonateCharity(charity_from);
//
//            }
//        });
//
//    }
//
//
//    private void setDataCreateCharity(LinearLayout charity_from) {
//
//    }
//
//    private void setDataDonateCharity(LinearLayout charity_from) {
//        TextView tvTest = (TextView) charity_from.findViewById(R.id.tv_test);
//        tvTest.setText("fdasf");
//
//    }


}