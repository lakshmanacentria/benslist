package com.acentria.benslist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.adapters.SpinnerAdapter;
import com.acentria.benslist.controllers.AccountArea;
import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;


public class CreateAccountActivity extends AppCompatActivity {

	public final static int RESULT_ACCEPT = 215;
	private static HashMap<String,String> formData = new HashMap<String,String>();
	private static CreateAccountActivity instance;
	private static Context context;
	private static Intent intent;
	private static HashMap<String, View> fieldViews = new HashMap<String, View>();
	public static HashMap<String, LinkedHashMap<String, HashMap<String,String>>> agreementsFields = new HashMap<String, LinkedHashMap<String, HashMap<String,String>>>();
	public static HashMap<String, LinkedHashMap<String, HashMap<String,String>>> aFields = new HashMap<String, LinkedHashMap<String, HashMap<String,String>>>();
	public static HashMap<String,  HashMap<String, ArrayList<HashMap<String, String>>>> items_data = new  HashMap<String, HashMap<String, ArrayList<HashMap<String, String>>>>();

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		instance = this;
		context = this;
		intent = getIntent();

		Lang.setDirection(this);
		
		setTitle(Lang.get("title_activity_create_account"));
        setContentView(R.layout.activity_create_account);
        
        final View parent = findViewById(R.id.ca_parent);
        
        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        /* prepare account type spinner */
        final Spinner spinner = (Spinner) findViewById(R.id.type);
		
        final ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
        HashMap<String, String> caption = new HashMap<String, String>();
        caption.put("name", Lang.get("account_type"));
        caption.put("key", "");
        items.add(caption);

		if(aFields.isEmpty()) {
			loadData();
		}
        
        for (Entry<String, HashMap<String, String>> entry : Config.cacheAccountTypes.entrySet()) {
        	items.add(entry.getValue());
        }
        
        HashMap<String, String> field_info = new HashMap<String, String>();
        field_info.put("key", "account_type");
        field_info.put("data", "");
		SpinnerAdapter adapter = new SpinnerAdapter(this, items, field_info, formData, null);
		   			
		spinner.setAdapter(adapter);

		final LinearLayout agreements = (LinearLayout) parent.findViewById(R.id.agreements);
		final LinearLayout second_step = (LinearLayout) parent.findViewById(R.id.second_step);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
				fieldViews.clear();
                formData.put("account_type", items.get(position).get("key"));
				if(Utils.getCacheConfig("android_second_step")!=null && Utils.getCacheConfig("android_second_step").equals("1")) {
					second_step.setVisibility(View.VISIBLE);
					second_step.removeAllViews();
					if(position>0 && aFields.get(formData.get("account_type"))!=null) {
						Forms.buildSubmitFields(second_step, aFields.get(formData.get("account_type")), formData, items_data.get(formData.get("account_type")), context, EditProfileActivity.actionbarSpinner, fieldViews, false);

					}
				}

				if(!agreementsFields.isEmpty()) {
					agreements.setVisibility(View.VISIBLE);
					agreements.removeAllViews();
					if(position>0 && agreementsFields.get(formData.get("account_type"))!=null) {
						buildAgreemntsFields(agreements, agreementsFields.get(formData.get("account_type")), formData);
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		
		final EditText username = (EditText) findViewById(R.id.username);
		if ( Utils.getCacheConfig("account_login_mode").equals("email") ) {
			username.setVisibility(View.GONE);
		}
		else {
			username.setVisibility(View.VISIBLE);
		}
		
		/* submit button handler */
		Button submit = (Button) findViewById(R.id.submit);
		submit.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View arg0) {
	            /* validate form */
				Boolean error = false;
				
				// username
				Matcher matcher = Pattern.compile("([a-zA-Z0-9\\-]+)").matcher(username.getText().toString());
				if ( !Utils.getCacheConfig("account_login_mode").equals("email") ) {
					if ( username.getText().toString().isEmpty() ) {
						error = true;
						username.requestFocus();
						username.setError(Lang.get("no_field_value"));
					}
					else if ( username.getText().toString().length() < 3 ) {
						error = true;
						username.requestFocus();
						username.setError(Lang.get("notice_reg_length").replace("{field}", Lang.get("android_hint_username")));
					}
					else if ( !matcher.matches() ) {
						error = true;
						username.requestFocus();
						username.setError(Lang.get("invalid_username"));
					}
					else {
						formData.remove("username");
						formData.put("username", username.getText().toString());
					}
				}
				
				// password
				EditText password = (EditText) findViewById(R.id.password);
				matcher = Pattern.compile("((?=.*\\d)(?=.*[a-z]).{5,20})").matcher(password.getText().toString());

				if ( !matcher.matches() ) {
					if ( !error ) {
						error = true;
						password.requestFocus();
						password.setError(Lang.get("password_weak"));
					}
				}
				else {	
					formData.remove("password");
					formData.put("password", password.getText().toString());
				}
				
				// email
				final EditText email = (EditText) findViewById(R.id.email);
				matcher = Pattern.compile("(.+@.+\\.[a-z]+)").matcher(email.getText().toString());
				
				if ( !matcher.matches() ) {
					if ( !error ) {
						error = true;
						email.requestFocus();
						email.setError(Lang.get("bad_email"));
					}
				}
				else {
					formData.remove("email");
					formData.put("email", email.getText().toString());
				}
				
				// account type
				if ( formData.get("account_type") != null && formData.get("account_type").isEmpty()) {
					error = true;
					Dialog.simpleWarning(Lang.get("account_type_empty"), instance);
				}
				else {
					if(Utils.getCacheConfig("android_second_step")!=null && Utils.getCacheConfig("android_second_step").equals("1") && aFields.get(formData.get("account_type"))!=null) {

						if (!Forms.validate(formData, aFields.get(formData.get("account_type")), fieldViews)) {
							error = true;
						}
					}
					if (agreementsFields !=null && !agreementsFields.isEmpty() && !Forms.validate(formData, agreementsFields.get(formData.get("account_type")), fieldViews)) {
						error = true;
					}
				}

				if ( !error ) {
					/* show progressbar */
					final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("loading"));
					
					/* hide keyboard */
					Utils.hideKeyboard(parent.findFocus(), instance);
					
		    		/* do request */
		    		AsyncHttpClient client = new AsyncHttpClient();
		    		client.setTimeout(30000); // set 30 seconds for this task
		    		
		    		final String url = Utils.buildRequestUrl("createAccount");
		        	client.post(url, Utils.toParams(formData), new AsyncHttpResponseHandler() {
		        		
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
				        				Element element = (Element) errorsNode.item(0);
				        				NodeList errors = element.getChildNodes();

				        				if ( errors.getLength() > 0 ) {
				        					Element error = (Element) errors.item(0);

				        					EditText field = username;
				        					if ( error.getAttribute("field").equals("email") ) {
				        						field = email;
				        					}

				        					field.setError(Lang.get(error.getTextContent()));
				        					field.requestFocus();
				        				}
			        				}
			        				/* process login */
			        				else {
										NodeList accountNode = doc.getElementsByTagName("account");
										AccountArea.confirmLogin(accountNode);

			        					/* finish this activity and show toast */
										instance.finish();

										String phrase = Account.accountData.get("status").equals("incomplete") ? Lang.get("registration_incompleted") : Lang.get("registration_completed");
										Toast.makeText(Config.context, phrase, Toast.LENGTH_LONG).show();
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

	private void loadData(){
		HashMap<String, String> params = new HashMap<String, String>();
		final String url = Utils.buildRequestUrl("getAccountForms", params, null);
		final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("android_sync_with_server"));
		/* do async request */

		AsyncHttpClient client = new AsyncHttpClient(true, 80, 443);
		client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					/* hide progressbar */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);
					progress.dismiss();
					if ( doc == null ) {
						Dialog.simpleWarning(Lang.get("returned_xml_failed"), Config.context);
					}
					else {

						NodeList listingNode = doc.getElementsByTagName("items");

						Element nlE = (Element) listingNode.item(0);
						NodeList listing = nlE.getChildNodes();

						for( int i=0; i<listing.getLength(); i++ ) {
							Element node = (Element) listing.item(i);
							if ( node.getTagName().equals("fields") ) {
								parseForm(node.getChildNodes());
							}
							else if( node.getTagName().equals("agreement") ) {
								parseAgreementFields(node.getChildNodes());
							}
						}
					}
				}
				catch (UnsupportedEncodingException e1) {

				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
				// called when response HTTP status is "4XX" (eg. 401, 403, 404)
			}
		});
	}

	public static void parseForm(NodeList fields) {
		for (int i = 0; i < fields.getLength(); i++) {
			Element tag = (Element) fields.item(i);
			NodeList nodes = tag.getChildNodes();
			if (nodes.getLength() > 0) {
				HashMap<String, ArrayList<HashMap<String, String>>> items = new HashMap<String, ArrayList<HashMap<String, String>>>();
				aFields.put(tag.getTagName(), Account.parseForm(nodes, items));
				items_data.put(tag.getTagName(), items);
			}
		}
	}

	public static void parseAgreementFields(NodeList fields) {
		for (int i = 0; i < fields.getLength(); i++) {
			Element tag = (Element) fields.item(i);
			NodeList nodes = tag.getChildNodes();
			if (nodes.getLength() > 0) {
				LinkedHashMap<String, HashMap<String, String>> out = new LinkedHashMap<String, HashMap<String, String>>();
				for ( int j = 0; j < nodes.getLength(); j++ ) {
					HashMap<String, String> tmpField = new HashMap<String, String>();

					Element field = (Element) nodes.item(j);
					tmpField.put("key", Utils.getNodeByName(field, "key"));
					tmpField.put("type", Utils.getNodeByName(field, "type"));
					tmpField.put("page_type", Utils.getNodeByName(field, "page_type"));
					tmpField.put("content", Utils.getNodeByName(field, "content"));
					tmpField.put("name", Utils.getNodeByName(field, "name"));
					tmpField.put("required", "1");
					tmpField.put("mode", tmpField.get("page_type").equals("static") ? "view" : "webview");
					out.put(Utils.getNodeByName(field, "key"), tmpField);

				}

				agreementsFields.put(tag.getTagName(), out);
			}
		}
	}

	private void buildAgreemntsFields(LinearLayout layout, LinkedHashMap<String, HashMap<String,String>> fields, final HashMap<String, String> formData) {

		for (Entry<String, HashMap<String, String>> entry : fields.entrySet()) {
			final HashMap<String, String> field = entry.getValue();
			final LinearLayout acceptField = (LinearLayout) Config.context.getLayoutInflater()
					.inflate(R.layout.field_accept, null);

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			params.setMargins(0, Utils.dp2px(15), 0, 0);
			acceptField.setLayoutParams(params);

			final CheckBox checkbox = (CheckBox) acceptField.findViewById(R.id.accept_field);
			checkbox.setText(Lang.get("android_i_accept"));
			checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					/* save value */
					formData.put(field.get("key"), isChecked ? "1" : "");
					Forms.unsetError(acceptField);
				}
			});
			final TextView terms = (TextView) acceptField.findViewById(R.id.terms_field);
			terms.setText(field.get("name"));
			terms.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(Config.context, AcceptActivity.class);
					intent.putExtra("key", field.get("key"));
					intent.putExtra("name", field.get("name"));
					intent.putExtra("mode", field.get("mode"));
					intent.putExtra("data", field.get("content"));
					((Activity) instance).startActivityForResult(intent, RESULT_ACCEPT);
				}
			});

			layout.addView(acceptField);
			if ( fieldViews != null ) {
				fieldViews.put(field.get("key"), acceptField);
			}
		}
	}

	public static void confirmAccept(String key) {
		LinearLayout accept = (LinearLayout) fieldViews.get(key);
		CheckBox acceptField = (CheckBox) accept.findViewById(R.id.accept_field);
		acceptField.setChecked(true);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case RESULT_ACCEPT:
				if (resultCode == RESULT_OK) {
					confirmAccept(data.getStringExtra("key"));
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