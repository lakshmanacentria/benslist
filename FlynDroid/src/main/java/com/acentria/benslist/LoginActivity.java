package com.acentria.benslist;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.facebook.FacebookSdk;
import com.acentria.benslist.controllers.AccountArea;
import com.google.analytics.tracking.android.EasyTracker;

public class LoginActivity extends AppCompatActivity {
    private static LoginActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;

        FacebookSdk.sdkInitialize(this);
        Lang.setDirection(this);
        setTitle(Lang.get("android_login"));
        setContentView(R.layout.activity_login);

        /* enable back action */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);


        AccountArea.loginController = "login";

        LinearLayout layout = (LinearLayout) findViewById(R.id.login_form);
        AccountArea.loginForm(layout, this);



    }

    public static void confirmLogin() {

        /* login user */
        Account.loggedIn = true;
        Utils.setSPConfig("accountUsername", Account.accountData.get("username"));
        Utils.setSPConfig("accountPassword", Account.accountData.get("password"));

        SwipeMenu.menuData.get(SwipeMenu.loginIndex).put("name", Account.accountData.get("full_name"));
        SwipeMenu.adapter.notifyDataSetChanged();

        if (Config.activeInstances.contains("AccountArea")) {
            Config.context.setTitle(Lang.get("my_profile"));
            AccountArea.menu_logout.setVisible(true);
            AccountArea.menu_remove_account.setVisible(true);
            if (AccountArea.profileTab != null) {
                Account.populateProfileTab();
            }
            AccountArea.login_form.setVisibility(View.GONE);
            AccountArea.profile_layer.setVisibility(View.VISIBLE);
        }



        GetPushNotification.regNotification(Account.accountData.get("id"), true);

        instance.setResult(Activity.RESULT_OK);
        instance.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }
}