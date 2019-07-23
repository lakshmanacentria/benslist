package com.acentria.benslist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.paypal.android.MEP.PayPal;
import com.paypal.android.MEP.PayPalActivity;
import com.paypal.android.MEP.PayPalPayment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;

public class PayPalMPL {
    private Activity activity;
    private Context instance;
    private HashMap<String, String> purchaseData;

    private PayPal pp;
    private PayPalPayment payment;
    public static boolean ppMplActive = false;

    public PayPalMPL(Activity ref_activity, Context ref_instance, HashMap<String, String> ref_purchaseData) {
        // return if REST lib is supported
//        if (Config.pp_rest_supported_countries.contains(Config.context.getResources().getConfiguration().locale.getCountry().toLowerCase())) {
//            return;
//        }

        if (Utils.getCacheConfig("android_paypal_mpl_module").equals("0")
                || Utils.getCacheConfig("android_paypal_mpl_app_id").isEmpty()) {
//            Dialog.toast(R.string.paypal_not_configured);
            return;
        }

        if (Utils.getCacheConfig("curl_available").equals("0")) {
            Dialog.toast(R.string.server_curl_failed);
            return;
        }

        activity = ref_activity;
        instance = ref_instance;
        purchaseData = ref_purchaseData;

        // initialize library
        pp = PayPal.getInstance();

        if (pp == null) {
            Integer env_mode = Utils.getCacheConfig("android_paypal_mpl_sandbox").equals("1") ? PayPal.ENV_SANDBOX : PayPal.ENV_LIVE;
            pp = PayPal.initWithAppID(instance, Utils.getCacheConfig("android_paypal_mpl_app_id"), env_mode);
            pp.setLanguage(Locale.getDefault().toString());
            pp.setFeesPayer(PayPal.FEEPAYER_EACHRECEIVER);
        }
        ppMplActive = true;

        // Create a basic PayPal payment
        payment = new PayPalPayment();

        payment.setCurrencyType(Utils.getCacheConfig("android_billing_currency").toUpperCase());

        // Set the recipient for the payment (can be a phone number)
        payment.setRecipient(Utils.getCacheConfig("android_paypal_mpl_account_email"));
        payment.setMerchantName(Utils.getConfig("app_name"));

        payment.setSubtotal(new BigDecimal(purchaseData.get("amount")));
        payment.setMemo(purchaseData.get("title"));

        // Set the payment type--his can be PAYMENT_TYPE_GOODS,
        // PAYMENT_TYPE_SERVICE, PAYMENT_TYPE_PERSONAL, or PAYMENT_TYPE_NONE
        payment.setPaymentType(PayPal.PAYMENT_TYPE_SERVICE);

        // custom button
        Button pay_paypal = (Button) activity.findViewById(R.id.pay_paypal);

        pay_paypal.setVisibility(View.VISIBLE);
        pay_paypal.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d("FD", "Click");
                // Use checkout to create our Intent.
                Intent checkoutIntent = pp.checkout(payment, activity);

                // This will start the library.
                activity.startActivityForResult(checkoutIntent, Config.PAYPAL_MPL_PURCHASE);
            }
        });
    }

    public void onResult(int requestCode, int resultCode, Intent data) {
        if ( resultCode == Activity.RESULT_OK ) {
            if (data.hasExtra(PayPalActivity.EXTRA_PAY_KEY)) {
                Log.d("FD - pay key", data.getStringExtra(PayPalActivity.EXTRA_PAY_KEY));
            }
            if (data.hasExtra(PayPalActivity.EXTRA_PAYMENT_STATUS)) {
                Log.d("FD - pay status", data.getStringExtra(PayPalActivity.EXTRA_PAYMENT_STATUS));
            }

            try {
                ((PurchaseActivity) activity).completePayment(data.getStringExtra(PayPalActivity.EXTRA_PAY_KEY), "paypal_mpl");
            } catch (Exception e) {
                Dialog.simpleWarning(R.string.paypal_failed, activity);
                Log.d("paymentExample", "an extremely unlikely failure occurred: ", e);
            }
        }
        else if ( resultCode == Activity.RESULT_CANCELED ) {
            // user canceled payment
        }
        else if ( resultCode == PayPalActivity.RESULT_FAILURE ) {
            Dialog.simpleWarning(data.getStringExtra(PayPalActivity.EXTRA_ERROR_MESSAGE), activity);
        }
    }
}