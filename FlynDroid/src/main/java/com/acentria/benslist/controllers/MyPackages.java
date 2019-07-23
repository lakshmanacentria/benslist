package com.acentria.benslist.controllers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.PackageActivity;
import com.acentria.benslist.PurchaseActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.adapters.PackagesItemAdapter;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class MyPackages extends AbstractController {

	private static MyPackages instance;

	public static int[] menuItems = {};
	public static ArrayList<HashMap<String, String>> plans;
	public static boolean availablePlans = false;
	private LinearLayout main_container;
	private static LinearLayout loading_container;
	private static LinearLayout plan_container;
	private static Button add_packages;
	private static PackagesItemAdapter PackagesAdapter;

	public static MyPackages getInstance() {
		if ( instance == null ) {
			try {
				instance = new MyPackages();
			}
			catch (Exception e) {
				Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
                e.printStackTrace();
			}
			Config.activeInstances.add(instance.getClass().getSimpleName());
		}
		else {
			Utils.restroreInstanceView(instance.getClass().getSimpleName(), Lang.get("title_activity_my_packages"));
			showMyPackages();
		}
		
		handleMenuItems(menuItems);

		return instance;
	}
	
	public static void removeInstance(){
		instance = null;
	}
	
	public MyPackages() {
		
		/* set content title */
		Config.context.setTitle(Lang.get("title_activity_my_packages"));
	
		/* add content view */
		Utils.addContentView(R.layout.view_my_packages);

		/* hide menu */
		Utils.showContent();

		/* get related view */
		main_container = (LinearLayout) Config.context.findViewById(R.id.MyPackages);
		loading_container = (LinearLayout) main_container.findViewById(R.id.progress_bar_custom);
		plan_container = (LinearLayout) main_container.findViewById(R.id.list_view_custom);
		add_packages = (Button) Config.context.findViewById(R.id.select_plan);
		add_packages.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Config.context, PackageActivity.class);
				Config.context.startActivity(intent);
			}
		});
		add_packages.setText(Lang.get("android_purchase_new"));

		showMyPackages();
	}

	public static void showMyPackages() {
		availablePlans = false;
		// get plans data
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		params.put("account_type", Account.accountData.get("type"));

		final String url = Utils.buildRequestUrl("getMyPackages", params, null);
		Log.d("FD", url.toString());
		/* do async request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					plans = Utils.parseXML(response, url, Config.context);

					checkAvailablePlans();

					if (plans.size() > 0) {
						drowPlans();
					} else {
						setEmpty();
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

	public static void setEmpty() {
		loading_container.setVisibility(View.GONE);
		plan_container.setVisibility(View.VISIBLE);
		plan_container.removeAllViews();
		TextView message = (TextView) Config.context.getLayoutInflater()
				.inflate(R.layout.info_message, null);
		message.setText(Lang.get("android_no_packages"));
		plan_container.setGravity(Gravity.CENTER);
		plan_container.addView(message);
	}
	public static void checkAvailablePlans() {
		for (int i = 0; i < plans.size(); i++) {
			if(plans.get(i).containsKey("available_plan")) {
				if(plans.get(i).get("available_plan").equals("1")) {
					availablePlans = true;
				}
				plans.remove(i);
			}
		}
	}

	public static void drowPlans() {
		plan_container.removeAllViews();
		loading_container.setVisibility(View.GONE);
		plan_container.setVisibility(View.VISIBLE);

		/* create list view of comments */
		GridView grid = new GridView(Config.context);
		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
		);
		grid.setLayoutParams(params);
		if (Config.tabletMode) {
			grid.setNumColumns(2);
		}
		PackagesAdapter = new PackagesItemAdapter(plans, grid, true);
		grid.setAdapter(PackagesAdapter);

		plan_container.addView(grid);
		if(availablePlans==false) {
			add_packages.setVisibility(View.GONE);
		}
	}

	public static void updatePackage(HashMap<String, String> plan, boolean availableButton) {
		if(plans.size() > 0) {
			PackagesAdapter.updatePackage(plan);
		}
		else {
			showMyPackages();
		}
		availablePlans = availableButton;
		if(availablePlans==false) {
			add_packages.setVisibility(View.GONE);
		}
	}

	public static void updatePackageRequest(HashMap<String, String> plan) {

		if (plan.get("price").equals("0")) {
			final ProgressDialog progress = ProgressDialog.show(Config.context, null, Lang.get("android_sync_with_server"));
			// get plans data
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("account_id", Account.accountData.get("id"));
			params.put("password_hash", Utils.getSPConfig("accountPassword", null));

			params.put("package_id", plan.get("id"));
			params.put("plan_id", plan.get("plan_id"));
			params.put("service", "upgrade");

			final String url = Utils.buildRequestUrl("upgradePackages", params, null);
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
							Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), Config.context);
						}
						else {
							NodeList successNode = doc.getElementsByTagName("success");
							if (successNode.getLength() > 0) {
								Element success = (Element) successNode.item(0);
								HashMap<String, String> plan =  Utils.parseHash(success.getChildNodes());

								MyPackages.updatePackage(plan, availablePlans);

								Dialog.simpleWarning(Lang.get("listing_plan_upgraded"));
							}
							else {
								Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), Config.context);
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
		else {
			HashMap<String, String> payment_hash = new HashMap<String, String>();
			// apply selected plan data
			payment_hash.put("service", "upgradePackage");//type service
			payment_hash.put("item", plan.get("type"));//plan type
			payment_hash.put("amount", plan.get("price"));//plan price
			payment_hash.put("title", plan.get("name"));
			payment_hash.put("id", plan.get("id")); //id
			payment_hash.put("plan", plan.get("plan_id")); //plan id
			payment_hash.put("featured", "0"); //appearance type
			payment_hash.put("plan_key", plan.get("key"));
			payment_hash.put("success_phrase", Lang.get(Utils.getCacheConfig("listing_auto_approval").equals("1") ? "listing_paid_auto_approved" : "listing_paid_pending"));

			Intent intent = new Intent(Config.context, PurchaseActivity.class);
			intent.putExtra("hash", payment_hash);
			Config.context.startActivityForResult(intent, Config.RESULT_PAYMENT);
		}
	}
}