package com.acentria.benslist;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;


public class ReportListingActivity extends AppCompatActivity {
	private ReportListingActivity instance;
	private View main_container;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		Lang.setDirection(this);
		setTitle(Lang.get("android_title_report_listing"));
        setContentView(R.layout.activity_report_listing);
        
        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

		Intent intent = getIntent();
		final String listing_id = intent.getStringExtra("id");

		main_container = findViewById(R.id.main_report);

		final EditText reportMessage = (EditText) main_container.findViewById(R.id.report_description);
		final RadioGroup radioGroup = (RadioGroup) main_container.findViewById(R.id.report_list);
		Button reportSend = (Button) main_container.findViewById(R.id.send_report);


		for (HashMap<String, String> item :  Config.reportBroken) {
			RadioButton radio = new RadioButton(this);
			radio.setText(item.get("name"));
			radio.setTag(item.get("key"));
			radioGroup.addView(radio);
		}

		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// checkedId is the RadioButton selected
				RadioButton button = (RadioButton) group.findViewById(checkedId);
				String key = button.getTag().toString();
				reportMessage.setVisibility(key.equals("custom") ? View.VISIBLE : View.GONE);
			}
		});

		reportSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				if(radioGroup.getCheckedRadioButtonId()>0) {
					View radioButton = radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
					boolean error = false;
					String report_value = radioButton.getTag().toString();
					String report_text = reportMessage.getText().toString();
					if(report_value.equals("custom") && report_text.isEmpty()) {
						error = true;
						reportMessage.requestFocus();
						reportMessage.setError(Lang.get("android_dialog_field_is_empty"));
					}

					if(!error) {
						HashMap<String,String> data = new HashMap<>();
						data.put("key", report_value);
						data.put("message", report_text);
						data.put("listing_id", listing_id);
						data.put("account_id", Account.loggedIn ? Account.accountData.get("id") : "0");
						sendReportListing(data);
					}
				}
				else {
					Dialog.simpleWarning(Lang.get("dialog_selector_is_empty"), instance);
				}
			}
		});
	}

	// send report listing to server
	private void sendReportListing(HashMap<String, String> data) {

		final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("loading"));

		/* hide keyboard */
		Utils.hideKeyboard(main_container.findFocus(), instance);
		/* do request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.setTimeout(30000); // set 30 seconds for this task

		final String url = Utils.buildRequestUrl("sendReportBroken");
		client.post(url, Utils.toParams(data), new AsyncHttpResponseHandler() {

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

					if ( doc == null ) {
						Dialog.simpleWarning(Lang.get("returned_xml_failed"), instance);
					}
					else {
						NodeList errorsNode = doc.getElementsByTagName("errors");

						/* handle errors */
						if ( errorsNode.getLength() > 0 ) {
							Element error = (Element) errorsNode.item(0);
							Dialog.simpleWarning(Lang.get(error.getTextContent()), instance);
						}
						else {
							NodeList successNode = doc.getElementsByTagName("success");
							Element success = (Element) successNode.item(0);
							Dialog.toast(success.getTextContent(), instance);
							instance.finish();
						}
					}

				} catch (UnsupportedEncodingException e1) {
					progress.dismiss();
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
				// called when response HTTP status is "4XX" (eg. 401, 403, 404)
				progress.dismiss();
			}
		});

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