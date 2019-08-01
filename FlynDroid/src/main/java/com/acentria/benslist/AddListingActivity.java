package com.acentria.benslist;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.acentria.benslist.adapters.ActionbarIconSpinnerAdapter;
import com.acentria.benslist.controllers.AccountArea;
import com.acentria.benslist.controllers.MyListings;
import com.google.analytics.tracking.android.EasyTracker;

import java.util.HashMap;

/**
 * Add/Edit Listing Activity, works with AddListing class
 *
 * @author John Freeman
 */
public class AddListingActivity extends AppCompatActivity {

    public final static int IMAGE_RETURN_CODE = 214;
    public final static int RESULT_ACCEPT = 215;
    public final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 216;
    public static AddListingActivity instance;
    public static MenuItem actionbarSpinner;

    public static AddListing addListing;

    public static Payment payment;
    public static Intent intent;
    public static LinearLayout login_form;
    public static LinearLayout category_options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Lang.setDirection(this);
        instance = this;

        intent = getIntent();
        FacebookSdk.sdkInitialize(instance);

        setTitle(Lang.get(intent.getStringExtra("id") == null ? "title_activity_add_listing" : "title_activity_edit_listing"));
        setContentView(R.layout.activity_add_listing);

        /* enable back action */
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        login_form = (LinearLayout) findViewById(R.id.login_form);
        category_options = (LinearLayout) findViewById(R.id.category_options);

        // set up billing
        payment = new Payment(instance, false, null);

        if (!Account.loggedIn) {
            AccountArea.loginController = "AddListing";
            AccountArea.loginForm(login_form, instance);
            login_form.setVisibility(View.VISIBLE);
            category_options.setVisibility(View.GONE);

        } else {
            setAddListing();
        }
    }

    public static void requestRead() {
        String[] PERMISSIONS = {android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!hasPermissions(AddListingActivity.instance, PERMISSIONS)) {

            ActivityCompat.requestPermissions(AddListingActivity.instance,
                    PERMISSIONS,
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Image.openGallery(AddListingActivity.IMAGE_RETURN_CODE, instance, null, null, null);
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

    public static void loginAddListing() {

        /* login user */
        Account.loggedIn = true;
        Utils.setSPConfig("accountUsername", Account.accountData.get("username"));
        Utils.setSPConfig("accountPassword", Account.accountData.get("password"));

        login_form.setVisibility(View.GONE);
        category_options.setVisibility(View.VISIBLE);

        SwipeMenu.menuData.get(SwipeMenu.loginIndex).put("name", Account.accountData.get("full_name"));
        SwipeMenu.adapter.notifyDataSetChanged();

        if (Config.activeInstances.contains("AccountArea")) {
            Config.context.setTitle(Lang.get("my_profile"));
            AccountArea.menu_logout.setVisible(true);
            AccountArea.menu_remove_account.setVisible(true);
            Account.populateProfileTab();
            AccountArea.login_form.setVisibility(View.GONE);
            AccountArea.profile_layer.setVisibility(View.VISIBLE);
        }




        GetPushNotification.regNotification(Account.accountData.get("id"), true);
        /* initiate */
        addListing = new AddListing(instance, intent);
    }

    public static void setAddListing() {
        login_form.setVisibility(View.GONE);
        category_options.setVisibility(View.VISIBLE);

        /* initiate */
        addListing = new AddListing(instance, intent);

        /* set timeout */
        CountDownTimer timer = new CountDownTimer(Config.ACTIONBAR_TIMEOUT, Config.ACTIONBAR_TIMEOUT) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                actionbarSpinner.setVisible(false);
            }
        };
        timer.start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case IMAGE_RETURN_CODE:
                Log.d("FD", resultCode + "" + data);
                Image.manageSelectedImage(resultCode, data, instance);
                break;

            case Config.RESULT_PAYMENT:
                if (resultCode == RESULT_OK) {
                    HashMap<String, String> listing_hash = (HashMap<String, String>) data.getSerializableExtra("hash");

                    Dialog.simpleWarning(listing_hash.get("success_phrase"));

                    if (data.hasExtra("success")) {
                        HashMap<String, String> success = (HashMap<String, String>) data.getSerializableExtra("success");
                        MyListings.updateItem(Integer.parseInt(listing_hash.get("id")), success);
                    } else {
                        Log.d("FD", "Add Listing Activity - no success data received, listview update failed");
                        Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
                    }
                } else if (resultCode == Config.RESULT_TRANSACTION_FAILED) {
                    Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
                    Utils.bugRequest("Payment result error (" + Utils.getSPConfig("domain", "") + ")", data.toString());
                }
                break;

            case AccountArea.FB_SIGN_IN:
                AccountArea.callbackManager.onActivityResult(requestCode, resultCode, data);


                break;
            case Config.RESULT_SELECT_PLAN:
                if (resultCode == RESULT_OK) {
                    addListing.selectPlan(
                            Integer.parseInt(data.getStringExtra("selected_id")),
                            data.getStringExtra("selected_type"),
                            data.getIntExtra("selected_position", 0)
                    );
                }
                break;

            case RESULT_ACCEPT:
                if (resultCode == RESULT_OK) {
                    Forms.confirmAccept(data.getStringExtra("key"));
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Account.loggedIn) {

            getMenuInflater().inflate(R.menu.multilingual, menu);

            Utils.translateMenuItems(menu);

            MenuItem spinnerItem = menu.findItem(R.id.multilingual);
            Spinner spinner = (Spinner) spinnerItem.getActionView();

            actionbarSpinner = spinnerItem;

            ActionbarIconSpinnerAdapter adapter = new ActionbarIconSpinnerAdapter(instance, addListing.fields_area, Config.cacheLanguagesWebs, spinner);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(adapter);
            adapter.selectDefault();
        }

        return true;
    }

    /**
     * onRequestPermissionsResult
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//				readFile();
                Image.openGallery(AddListingActivity.IMAGE_RETURN_CODE, instance, null, null, null);
            } else {
                // Permission Denied
                Toast.makeText(AddListingActivity.instance, "Permission Denied", Toast.LENGTH_LONG).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:

                if (addListing != null && addListing.selected_category_id > 0) {
                    Dialog.confirmAction(Lang.get("dialog_discard_listing"), this, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //instance.onBackPressed();
                            addListing = null;
                            instance.finish();
                        }
                    }, null);
                } else {
                    super.onBackPressed();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (addListing != null && addListing.selected_category_id > 0) {
                Dialog.confirmAction(Lang.get("dialog_discard_listing"), this, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //instance.onBackPressed();
                        addListing = null;
                        instance.finish();
                    }
                }, null);
            } else {
                super.onKeyDown(keyCode, event);
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (payment != null && payment.mService != null) {
            unbindService(payment.mServiceConn);
        }
    }
}