package com.acentria.benslist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.paypal.android.sdk.payments.PayPalService;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * Purchase item Activity
 * 
 * @author John Freeman
 * 
 */
public class PurchaseActivity extends AppCompatActivity {
	
	final private List<String> products = new ArrayList<String>();
	private HashMap<String, String> purchaseData;
	private Activity activity;
	private Context instance;

	public Intent resultIntent = new Intent();
	
	public PayPalREST paypalREST;
    public PayPalMPL paypalMPL;
	public Payment payment;
	public static boolean subscription = false;;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Lang.setDirection(this);

		setTitle(Lang.get("title_activity_purchase"));
        setContentView(R.layout.activity_purchase);
        
        activity = this;
        instance = this;
        
        // enable back action
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        purchaseData = (HashMap<String, String>) getIntent().getSerializableExtra("hash");
        
        if ( purchaseData == null ) {
        	Dialog.simpleWarning(R.string.no_payment_data);
        	finish();
        }
        
        resultIntent.putExtra("hash", purchaseData);
        
        // build interface
        buildInterface();

        List<String> pp_supported_countries = Utils.string2list(new String[] {"us", "uk"});


		if ( purchaseData.containsKey("subscription") && purchaseData.get("subscription") != null) {
			subscription = true;
		}
		if (!subscription) {
			// paypal
			paypalREST = new PayPalREST(activity, instance, purchaseData);
			paypalMPL = new PayPalMPL(activity, instance, purchaseData);
		}
        // built-in app purchase
        payment = new Payment(activity, true, purchaseData);

		if(!PayPalREST.ppRestActive && !Payment.IabAvailable) {
			Dialog.toast(R.string.paypal_failed);
		}
	}
	
	private void buildInterface() {
        // set title
		TextView title = (TextView) findViewById(R.id.title);
        title.setText(purchaseData.get("title"));

        // set amount
        TextView amount = (TextView) findViewById(R.id.amount);
        
        String price = Utils.getCacheConfig("currency_position").equals("before") 
        		? Utils.getCacheConfig("system_currency") + purchaseData.get("amount")
        		: purchaseData.get("amount") + " " + Utils.getCacheConfig("system_currency");
        		
        amount.setText(price);
	}
	
	public void completePayment(String tracking_id, final String gateway) {
		// save transaction as pending
        Config.pendingTransaction.add(purchaseData);
        
        final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("android_sync_with_server"));
        
        // build request url
		RequestParams post_params = new RequestParams();
        post_params.put("account_id", Account.accountData.get("id"));
        post_params.put("password_hash", Utils.getSPConfig("accountPassword", null));

        post_params.put("payment_item", purchaseData.get("item"));
        post_params.put("payment_title", purchaseData.get("title"));
        post_params.put("payment_id", purchaseData.get("id"));
        post_params.put("payment_plan", purchaseData.get("plan"));
        post_params.put("payment_amount", purchaseData.get("amount"));
		post_params.put("payment_featured", purchaseData.get("featured"));
		post_params.put("payment_service", purchaseData.get("service"));
		if (purchaseData.containsKey("subscription")) {
			post_params.put("payment_subscription", purchaseData.get("subscription"));
		}
		post_params.put("payment_gateway", gateway);
		post_params.put("payment_tracking_id", tracking_id);
		
		final String url = Utils.buildRequestUrl("validateTransaction", null, null);

		/* do async request */
    	AsyncHttpClient client = new AsyncHttpClient();
    	client.setTimeout(60000);//one minute allowed timeout
    	client.post(url, post_params, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					progress.dismiss();

					Log.d("FD - "+gateway, response);

					/* parse response */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if ( doc == null ) {
						Dialog.simpleWarning(Lang.get("returned_xml_failed"), instance);
					}
					else {
						NodeList errorNode = doc.getElementsByTagName("error");

						/* handle errors */
						if ( errorNode.getLength() > 0 ) {
							Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"), instance);
						}
						/* finish this activity and show toast */
						else {
							NodeList successNode = doc.getElementsByTagName("success");
							if ( successNode.getLength() > 0 ) {
								if ( gateway.equals("google") ) {
									payment.consumeTransaction();
								}

								Config.pendingTransaction.remove(purchaseData);

								Element success = (Element) successNode.item(0);
								resultIntent.putExtra("success", Utils.parseHash(success.getChildNodes()));
								Log.d("FD put to success extra", Utils.parseHash(success.getChildNodes()).toString());
								setResult(RESULT_OK, resultIntent);
							}
							else {
								setResult(Config.RESULT_TRANSACTION_FAILED, resultIntent);
							}

							/* set timeout before finish the activity becuase some of services requires some time to be finished */
							CountDownTimer timer = new CountDownTimer(1500, 1500) {
								public void onTick(long millisUntilFinished) {}

								public void onFinish() {
									activity.finish();
								}
							};
							timer.start();
						}
					}

				} catch (UnsupportedEncodingException e1) {

				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
				try {
					String response = String.valueOf(new String(errorResponse, "UTF-8"));

					Log.d("FD - "+gateway, "onFailed - "+response);
//    	    	Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
					progress.dismiss();

					setResult(Config.RESULT_TRANSACTION_FAILED, resultIntent);
					activity.finish();

				} catch (UnsupportedEncodingException e1) {

				}
			}
    	});
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {

		switch(requestCode) {
			case Config.IAP_PURCHASE:
				payment.onResult(requestCode, resultCode, data);
				break;
				
			case Config.PAYPAL_REST_PURCHASE:
                paypalREST.onResult(requestCode, resultCode, data);
				break;

            case Config.PAYPAL_MPL_PURCHASE:
                paypalMPL.onResult(requestCode, resultCode, data);
                break;
				
			default:
				super.onActivityResult(requestCode, resultCode, data);
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
	public void onDestroy() {
		stopService(new Intent(this, PayPalService.class));

		super.onDestroy();

		if ( payment.mService != null ) {
			unbindService(payment.mServiceConn);
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
		