package com.acentria.benslist;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.vending.billing.IInAppBillingService;
import com.paypal.android.sdk.payments.PaymentActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Payment {
	
	public IInAppBillingService mService;
	public ServiceConnection mServiceConn;
	
	private Activity activity;
	public static Boolean IabAvailable = false;
	private Boolean buildButton = false;
	private HashMap<String, String> hash = null;
	private JSONObject transactionData;
	
	public Payment(Activity ref_activity, Boolean ref_buildButton, HashMap<String, String> ref_hash) {
		activity = ref_activity;
		buildButton = ref_buildButton;
		hash = ref_hash;

		if ( !Utils.getCacheConfig("android_inapp_module").equals("1") ) {
			Log.d("FD", "IAB MODULE: module disabled by administrator");
			return; // disable module
		}

        if ( Utils.getConfig("customer_domain").isEmpty() ) {
            return; // disable module
        }

		if ( hash != null && hash.containsKey("subscription") && hash.get("subscription") == null && hash.containsKey("product_type") && hash.get("product_type").equals("subs")
				&& !PayPalMPL.ppMplActive && !PayPalREST.ppRestActive) {
			Log.d("FD", "IAB MODULE: no subsss, exit");
			Dialog.toast(R.string.subs_failed);
			IabAvailable = true;
			return; // disable module
		}

		if ( hash != null && hash.containsKey("subscription")  && hash.containsKey("product_type") && hash.get("product_type").equals("subs") && hash.get("subscription") == null
				&& ( PayPalMPL.ppMplActive || PayPalREST.ppRestActive )) {
			Log.d("FD", "IAB MODULE: no subs, exit");
            return; // disable module
        }

		if ( hash != null && (!hash.containsKey("plan_key") || (hash.containsKey("plan_key") && hash.get("plan_key") != null && hash.get("plan_key").isEmpty())) ) {
			Log.d("FD", "IAB MODULE: no plan key specified, exit");
			return; // disable module
		}

		IabAvailable = true;
		
		// setup connection
		mServiceConn = new ServiceConnection() {

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				mService = IInAppBillingService.Stub.asInterface(service);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				mService = null;
			}
		};

		Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
		serviceIntent.setPackage("com.android.vending");
        activity.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        
		setup();
	}
	
	public void setup() {
		if ( buildButton && hash != null ) {
			Log.d("FDs", hash.toString());
			drowButton();
		}
	}
	
	public Boolean isAvailalbe() {
		return IabAvailable;
	}
	
	private void drowButton() {
		Button pay_google = (Button) activity.findViewById(R.id.pay_google);
		pay_google.setVisibility(View.VISIBLE);
		
		final String plan_key = hash.get("plan_key");

		pay_google.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if ( getPurchase(plan_key) != null ) {
					Dialog.simpleWarning(Lang.get("dialog_unable_proceed_iap_product"), activity);
					return;
				}
				
				try {
					String in_app_type = "inapp";
					if (PurchaseActivity.subscription) {
						in_app_type = "subs";
					}
					Bundle buyIntentBundle = mService.getBuyIntent(3, activity.getPackageName(), plan_key, in_app_type, "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
					PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
					activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
							Config.IAP_PURCHASE, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
					
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				//removeAll();
			}
		});
	}
	
	public void onResult(int requestCode, int resultCode, Intent data) {
		
		if ( resultCode == Activity.RESULT_OK ) {
			String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
			String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
			
			try {
				Log.d("FD", purchaseData);
				Log.d("FD", dataSignature);
				transactionData = new JSONObject(purchaseData);
				
				((PurchaseActivity) activity).completePayment(purchaseData+"|||"+dataSignature, "google");
			}
			catch (JSONException e) {
				e.printStackTrace();
				Dialog.simpleWarning(R.string.iab_purchase_error, activity);
			}
	    }
	    else if ( resultCode == Activity.RESULT_CANCELED ) {
	        // user canceled payment
	    }
	    else if ( resultCode == PaymentActivity.RESULT_EXTRAS_INVALID ) {
	        Dialog.simpleWarning(R.string.iab_purchase_error, activity);
	    }
	}
	
	public void consumeTransaction() {
		try {
			mService.consumePurchase(3, activity.getPackageName(), transactionData.getString("purchaseToken"));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void synchronizePlans(ArrayList<HashMap<String, String>> plans) {

		// do not synchronize plans if iap module disabled
		if ( !IabAvailable )
			return;

		if ( plans.size() == 0 ) {
			Log.d("FD", "IAB MODULE: plan synchronization, no initial plans received");
			return;
		}

		ArrayList<String> plan_keys = new ArrayList<String>();
		ArrayList<String> plan_sub_keys = new ArrayList<String>();

		for ( HashMap<String, String> plan : plans ) {
			if ( (plan.containsKey("price") && Double.parseDouble(plan.get("price")) == 0)
				|| (plan.containsKey("package_id") && !plan.get("listings_remains").isEmpty() && Integer.parseInt(plan.get("listings_remains")) > 0) )
				continue;

			if (plan.containsKey("subscription") && plan.get("subscription").equals("active")) {
				plan_sub_keys.add(plan.get("key"));
			}
			else {
				plan_keys.add(plan.get("key"));
			}
		}
		
		if (plan_keys.size() == 0 && plan_sub_keys.size() == 0) {
			Log.d("FD", "IAB MODULE: plan synchronization, no paied plans found");
			return;
		}

		HashMap<String, JSONObject> product_list = new HashMap<String, JSONObject>();

		// get paid item
		Bundle querySkus = new Bundle();
		querySkus.putStringArrayList("ITEM_ID_LIST", plan_keys);
		try {
			Bundle skuDetails = mService.getSkuDetails(3, activity.getPackageName(), "inapp", querySkus);
			int response = skuDetails.getInt("RESPONSE_CODE");

			if ( response == 0 ) {
				ArrayList<String> response_list = skuDetails.getStringArrayList("DETAILS_LIST");
				
				for (String this_response : response_list) {
					JSONObject object = new JSONObject(this_response);
					product_list.put(object.getString("productId"), object);
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
            e.printStackTrace();
            IabAvailable = false;
            Log.d("FD", "IAB MODULE: plan synchronization, mService is NULL");
        }

        // get paid item
		Bundle querySkusub = new Bundle();
		querySkusub.putStringArrayList("ITEM_ID_LIST", plan_sub_keys);
		try {
			Bundle skuDetails = mService.getSkuDetails(3, activity.getPackageName(), "subs", querySkusub);
			int response = skuDetails.getInt("RESPONSE_CODE");

			if ( response == 0 ) {
				ArrayList<String> response_list = skuDetails.getStringArrayList("DETAILS_LIST");

				for (String this_response : response_list) {
					JSONObject object = new JSONObject(this_response);
					product_list.put(object.getString("productId"), object);
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
            e.printStackTrace();
            IabAvailable = false;
            Log.d("FD", "IAB MODULE: plan synchronization, mService is NULL");
        }

		if ( product_list.size() == 0 ) {
			IabAvailable = false;
			Log.d("FD", "IAB MODULE: plan synchronization, no IAP products received");
			return;
		}

		Iterator<HashMap<String, String>> iterator = plans.iterator();
		while (iterator.hasNext()) {
			HashMap<String, String> next = iterator.next();
			if ( plan_keys.contains(next.get("key")) || plan_sub_keys.contains(next.get("key")) ) {
				if ( !product_list.containsKey(next.get("key")) ) {
					iterator.remove();	
				}
				else {
					//next.put("product_type", product_list.get(next.get("key")).getString("type").toString());

					try {
						String product_type  = product_list.get(next.get("key")).getString("type");
						next.put("product_type", product_type);
					} catch (JSONException e) {
						e.printStackTrace();
					}

					try {
						next.put("price", convertPrice(product_list.get(next.get("key")).getString("price_amount_micros")));

					} catch (JSONException e) {
						e.printStackTrace();
					}
//					if ( Integer.parseInt(next.get("listings_remains")) == 0 ) {
//						next.put("name", next.get("name_original") + " ("+ sku.getPrice().replaceAll("\\.00", "") +")");
//					}
				}
			}
		}
	}

	private JSONObject getPurchase(String planKey) {
		JSONObject purchase = null;
		
		if ( planKey == null ) {
			return purchase;
		}
		
		try {
			String in_app_type = "inapp";
			if (PurchaseActivity.subscription) {
				in_app_type = "subs";
			}
			Bundle ownedItems = mService.getPurchases(3, activity.getPackageName(), in_app_type, null);
			int response = ownedItems.getInt("RESPONSE_CODE");
			
			if ( response == 0 ) {
				ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");

			   for (int i = 0; i < purchaseDataList.size(); ++i) {
			      String purchaseData = purchaseDataList.get(i);
			      Log.d("FD", purchaseData);
			      JSONObject json = new JSONObject(purchaseData);
			      
			      if ( json.getString("productId").equals(planKey) ) {
			    	  purchase = json;
			    	  break;
			      }
			   }
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return purchase;
	}
	
	private void removeAll() {
		try {
			String in_app_type = "inapp";
			if (PurchaseActivity.subscription) {
				in_app_type = "subs";
			}
			Bundle ownedItems = mService.getPurchases(3, activity.getPackageName(), in_app_type, null);
			int response = ownedItems.getInt("RESPONSE_CODE");
			
			if (response == 0) {
			   ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
			   
			   String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");
			   
			   for (int i = 0; i < purchaseDataList.size(); ++i) {
			      String purchaseData = purchaseDataList.get(i);
			      JSONObject purchaseJSON = new JSONObject(purchaseData);

		    	try {
		  			mService.consumePurchase(3, activity.getPackageName(), purchaseJSON.getString("purchaseToken"));
		  		} catch (RemoteException e) {
		  			// TODO Auto-generated catch block
		  			e.printStackTrace();
		  		} catch (JSONException e) {
		  			// TODO Auto-generated catch block
		  			e.printStackTrace();
		  		}
			   }
			}
			
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			Log.d("FD", "simple exception");
			e.printStackTrace();
		}
	}
	
	private String convertPrice(String micros) {
		Double price = ((double) Integer.parseInt(micros)) / 1000000;
		return price.toString().replaceAll("\\.0$", "");
	}
}