package com.acentria.benslist;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;


public class ResetPasswordActivity extends AppCompatActivity {
	
	private static ResetPasswordActivity instance;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Lang.setDirection(this);

		instance = this;
		
		setTitle(Lang.get("title_activity_reset_password"));
        setContentView(R.layout.activity_reset_password);
        
        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        /* submit button handler */
        Button submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				/* validate form */
				Boolean error = false;
				
				// email
				final EditText email = (EditText) findViewById(R.id.email);
				Matcher matcher = Pattern.compile("(.+@.+\\.[a-z]+)").matcher(email.getText().toString());
				
				if ( !matcher.matches() ) {
					error = true;
					email.requestFocus();
					email.setError(Lang.get("bad_email"));
				}
				
				/* do request */
				if ( !error ) {
					/* show progressbar */
					final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("loading"));
					
		    		AsyncHttpClient client = new AsyncHttpClient();
		    		client.setTimeout(30000); // set 30 seconds for this task
		    		
		    		final String url = Utils.buildRequestUrl("resetPassword");
		    		
		    		HashMap<String, String> params = new HashMap<String, String>();
		    		params.put("email", email.getText().toString());
		    		
		        	client.post(url, Utils.toParams(params), new AsyncHttpResponseHandler() {
		        		
						@Override
						public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
							// called when response HTTP status is "200 OK"
							try {
								String response = String.valueOf(new String(server_response, "UTF-8"));
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
										Element element = (Element) errorsNode.item(0);
										NodeList errors = element.getChildNodes();

										if ( errors.getLength() > 0 ) {
											email.setError(Lang.get("email_doesnt_exist"));
											email.requestFocus();
										}
									}
									/* complete the request */
									else {
										instance.finish();
										Toast.makeText(Config.context, Lang.get("reset_password_link_sent"), Toast.LENGTH_LONG).show();
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