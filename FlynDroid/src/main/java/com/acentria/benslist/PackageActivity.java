package com.acentria.benslist;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.acentria.benslist.adapters.PackagesItemAdapter;
import com.acentria.benslist.controllers.MyPackages;
import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

/**
 * Created by fed9i on 1/12/16.
 */
public class PackageActivity extends AppCompatActivity {

    private static PackageActivity instance;
    private ArrayList<HashMap<String, String>> plans;
    public static Payment payment;
    private LinearLayout main_container;
    private LinearLayout loading_container;
    private LinearLayout plan_container;
    public static PackagesItemAdapter PackagesAdapter;
    private Button purchase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Lang.setDirection(this);

        instance = this;

        // enable back action
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setTitle(Lang.get("title_activity_select_plan"));
        setContentView(R.layout.view_my_packages);

        /* get related view */
        main_container = (LinearLayout) findViewById(R.id.MyPackages);
        loading_container = (LinearLayout) main_container.findViewById(R.id.progress_bar_custom);
        plan_container = (LinearLayout) main_container.findViewById(R.id.list_view_custom);
        purchase = (Button) main_container.findViewById(R.id.select_plan);

        // set up billing
        payment = new Payment(instance, false, null);


        // get plans data
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("account_id", Account.accountData.get("id"));
        params.put("password_hash", Utils.getSPConfig("accountPassword", null));
        params.put("account_type", Account.accountData.get("type"));

        final String url = Utils.buildRequestUrl("getPackages", params, null);

        /* do async request */
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                // called when response HTTP status is "200 OK"
                try {
                    String xml = String.valueOf(new String(server_response, "UTF-8"));
                    plans = Utils.parseXML(xml, url, instance);

                    // synchronize plans
                    payment.synchronizePlans(plans);

                    if (plans.size() > 0) {
                        //PackageActivity.payment.synchronizePlans(plans);
                        drowPlans();
                    } else {
                        setEmpty();
                        purchase.setVisibility(View.GONE);
                    }

                } catch (UnsupportedEncodingException e1) {}
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
        });

        purchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (PackagesAdapter.selected_id== null) {
                    Dialog.simpleWarning(Lang.get("dialog_no_plan_selected"), instance);
                } else {
                    HashMap<String, String> plan = plans.get(PackagesAdapter.getActivePosition());
                    HashMap<String, String> payment_hash = new HashMap<String, String>();

                    // free plan or available package mode
                    if (plan.get("price").equals("0")) {

                        final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("android_sync_with_server"));

                        // get plans data
                        HashMap<String, String> params = new HashMap<String, String>();
                        params.put("account_id", Account.accountData.get("id"));
                        params.put("password_hash", Utils.getSPConfig("accountPassword", null));

                        params.put("plan_id", plan.get("id"));
                        params.put("service", "purchase");

                        final String url = Utils.buildRequestUrl("upgradePackages", params, null);
                        Log.d("FD", "free update url - " + url);

                        // do async request
                        AsyncHttpClient client = new AsyncHttpClient();
                        client.get(url, new AsyncHttpResponseHandler() {

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
                                // called when response HTTP status is "200 OK"
                                try {
                                    String response = String.valueOf(new String(server_response, "UTF-8"));
                                    progress.dismiss();
                                    Log.d("FD - free mode", response);

                                    // parse response
                                    XMLParser parser = new XMLParser();
                                    Document doc = parser.getDomElement(response, url);

                                    if ( doc == null ) {
                                        Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), instance);
                                    }
                                    else {
                                        NodeList successNode = doc.getElementsByTagName("success");
                                        if (successNode.getLength() > 0) {
                                            Element success = (Element) successNode.item(0);
                                            HashMap<String, String> plan =  Utils.parseHash(success.getChildNodes());
                                            boolean availablePlans = plans.size() > 1 ? true : false;
                                            MyPackages.updatePackage(plan, availablePlans);

                                            Dialog.simpleWarning(Lang.get("listing_plan_purchase"));
                                            instance.finish();
                                        }
                                        else {
                                            Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), instance);
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
                    // paid plan mode
                    else {
                        // apply selected plan data
                        payment_hash.put("service", "purchasePackage");//type service
                        payment_hash.put("item", plan.get("type"));//plan type
                        payment_hash.put("amount", plan.get("price"));//plan price
                        payment_hash.put("title", plan.get("name"));
                        payment_hash.put("id", plan.get("id")); //plan id
                        payment_hash.put("plan", plan.get("id")); //plan id
                        payment_hash.put("featured", "0"); //appearance type
                        payment_hash.put("plan_key", plan.get("key"));
                        payment_hash.put("product_type", plan.containsKey("product_type") ? plan.get("product_type") : "0" );
                        payment_hash.put("subscription", PackagesAdapter.subscription_id);
                        payment_hash.put("success_phrase", Lang.get(Utils.getCacheConfig("listing_auto_approval").equals("1") ? "listing_paid_auto_approved" : "listing_paid_pending"));

                        // start activity
                        Intent intent = new Intent(instance, PurchaseActivity.class);
                        intent.putExtra("hash", payment_hash);
                        startActivityForResult(intent, Config.RESULT_PAYMENT);
                    }
                }

            }
        });
    }

    public void setEmpty() {
        loading_container.setVisibility(View.GONE);
        plan_container.setVisibility(View.VISIBLE);
        plan_container.removeAllViews();
        TextView message = (TextView) Config.context.getLayoutInflater()
                .inflate(R.layout.info_message, null);
        message.setText(Lang.get("android_no_packages"));
        plan_container.setGravity(Gravity.CENTER);
        plan_container.addView(message);
    }

    public void drowPlans() {
        plan_container.removeAllViews();
        loading_container.setVisibility(View.GONE);
        plan_container.setVisibility(View.VISIBLE);

		/* create list view of comments */
//        GridView grid = new GridView(Config.context);
        GridView grid = (GridView) Config.context.getLayoutInflater().inflate(R.layout.grid_view, null);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        grid.setLayoutParams(params);
        if (Config.tabletMode) {
            grid.setNumColumns(2);
        }

        PackagesAdapter = new PackagesItemAdapter(plans, grid, false);
        grid.setClickable(true);
        grid.setAdapter(PackagesAdapter);
        plan_container.addView(grid);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case Config.RESULT_PAYMENT:
                if ( resultCode == RESULT_OK ) {
                    Dialog.simpleWarning(Lang.get("listing_plan_purchase"));
                    if ( data.hasExtra("success") ) {
                        HashMap<String, String> plan = (HashMap<String, String>) data.getSerializableExtra("success");
                        boolean availablePlans = plans.size() > 1 ? true : false;
                        MyPackages.updatePackage(plan, availablePlans);
                        instance.finish();
                    }
                    else {
                        Log.d("FD", "Add Listing Activity - no success data received, listview update failed");
                        Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
                    }
                }
                else if (resultCode == Config.RESULT_TRANSACTION_FAILED ) {
                    Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
                    Utils.bugRequest("Payment result error ("+Utils.getSPConfig("domain", "")+")", data.toString());
                }
                break;

        }
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
