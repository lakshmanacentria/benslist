package com.acentria.benslist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.HashMap;

public class PayPalREST {

    private PayPalConfiguration config;
    private Activity activity;
    private Context instance;
    private HashMap<String, String> purchaseData;
    public static boolean ppRestActive = false;
    private String TAG = "PayPalREST=> ";

    public PayPalREST(Activity ref_activity, Context ref_instance, HashMap<String, String> ref_purchaseData) {
        // return if REST lib is not supported
//        if (!Config.pp_rest_supported_countries.contains(Config.context.getResources().getConfiguration().locale.getCountry().toLowerCase())) {
//            return;
//        }

        if (Utils.getCacheConfig("android_paypal_module").equals("0")
                || Utils.getCacheConfig("android_paypal_client_id").isEmpty()) {
            return;
        }

        if (Utils.getCacheConfig("curl_available").equals("0")) {
            Dialog.toast(R.string.server_curl_failed);
            return;
        }
        ppRestActive = true;
        activity = ref_activity;
        instance = ref_instance;
        purchaseData = ref_purchaseData;

        Button pay_paypal = (Button) activity.findViewById(R.id.pay_paypal);

        pay_paypal.setVisibility(View.VISIBLE);
        pay_paypal.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.e(TAG + "FD=>", purchaseData.toString());
                PayPalPayment payment = new PayPalPayment(new BigDecimal(purchaseData.get("amount")), Utils.getCacheConfig("android_billing_currency"),
                        purchaseData.get("title"),
                        PayPalPayment.PAYMENT_INTENT_SALE);

                Intent intent = new Intent(activity, PaymentActivity.class);
                intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                activity.startActivityForResult(intent, Config.PAYPAL_REST_PURCHASE);
            }
        });

        String env_mode = Utils.getCacheConfig("android_paypal_sandbox").equals("1") ? PayPalConfiguration.ENVIRONMENT_SANDBOX : PayPalConfiguration.ENVIRONMENT_PRODUCTION;

        config = new PayPalConfiguration()
                .environment(env_mode)
                .clientId(Utils.getCacheConfig("android_paypal_client_id"));

        //config.acceptCreditCards(false);

        Intent intent = new Intent(activity, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);

        activity.startService(intent);
    }

    public void onResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
            if (confirm != null) {
                try {
                    Log.d("FD | PAYPAL", confirm.toJSONObject().toString(4));
                    JSONObject response_json = new JSONObject(confirm.toJSONObject().get("response").toString());

                    ((PurchaseActivity) activity).completePayment(response_json.get("id").toString(), "paypal_rest");
                } catch (JSONException e) {
                    Dialog.simpleWarning(R.string.paypal_failed, activity);
                    Log.d("paymentExample", "an extremely unlikely failure occurred: ", e);
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // user canceled payment
        } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
            Dialog.simpleWarning(R.string.paypal_failed, activity);
        }
    }
}