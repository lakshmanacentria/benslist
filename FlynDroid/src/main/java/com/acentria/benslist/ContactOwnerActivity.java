package com.acentria.benslist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;


public class ContactOwnerActivity extends AppCompatActivity {
	public static ContactOwnerActivity instance;
	private static Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		Lang.setDirection(this);
		/* get account data from instance */
		intent = getIntent();
		String contactID = intent.getStringExtra("id");
		String listingID = intent.getStringExtra("listing_id");

		/* enable back action */
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		setTitle(Lang.get("android_contact_owner_caption"));
		setContentView(R.layout.activity_contact_owner);
		LinearLayout content = (LinearLayout) findViewById(R.id.activity_contact_owner);
		contactOwner(content, contactID, listingID);
	}

	public void contactOwner(final LinearLayout content, final String contactID, final String listingID) {

		Button send = (Button) content.findViewById(R.id.send);
		send.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean error = false;
				EditText name = (EditText) content.findViewById(R.id.name);
				EditText mail = (EditText) content.findViewById(R.id.mail);
				EditText phone = (EditText) content.findViewById(R.id.phone);
				EditText message = (EditText) content.findViewById(R.id.message);
				if (name.getText().toString().isEmpty()) {
					name.setError(Lang.get("android_dialog_field_is_empty"));
					error = true;
				}
				else {
					name.setError(null);
				}

				Matcher matcher = Pattern.compile("(.+@.+\\.[a-z]+)").matcher(mail.getText().toString());
				if ( !matcher.matches() ) {
					if ( !error ) {
						error = true;
						mail.setError(Lang.get("bad_email"));
					}
				}
				else {
					mail.setError(null);
				}

				if (message.getText().toString().isEmpty()) {
					message.setError(Lang.get("android_dialog_field_is_empty"));
					error = true;
				}
				else {
					message.setError(null);
				}

				if (!error) {
					/* show progressbar */
					final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("loading"));

		    		/* do request */
					AsyncHttpClient client = new AsyncHttpClient();
					client.setTimeout(30000); // set 30 seconds for this task

					/* build request url */
					HashMap<String, String> params = new HashMap<String, String>();
					params.put("id", contactID);
					params.put("listing_id", listingID);
					params.put("name", name.getText().toString());
					params.put("message", message.getText().toString());
					params.put("mail", mail.getText().toString());
					params.put("phone", phone.getText().toString());

					final String url = Utils.buildRequestUrl("contactOwner");
					client.post(url, Utils.toParams(params), new AsyncHttpResponseHandler() {

						@Override
						public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
							// called when response HTTP status is "200 OK"
							try {
								String response = String.valueOf(new String(server_response, "UTF-8"));
								progress.dismiss();
								/* parse response */
								XMLParser parser = new XMLParser();
								Document doc = parser.getDomElement(response, url);

								if (doc == null) {
									Dialog.simpleWarning(Lang.get("returned_xml_failed"));
								}
								else {
									// finish activity
									Intent returnIntent = new Intent();
									returnIntent.putExtra("result","done");
									setResult(Activity.RESULT_OK, returnIntent);
									((Activity) instance).finish();
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
			}
		});
	}

	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

	        case android.R.id.home:
	        	super.onBackPressed();
				return true;
				
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
	@Override
	public void onBackPressed() {
		super.onBackPressed();
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
