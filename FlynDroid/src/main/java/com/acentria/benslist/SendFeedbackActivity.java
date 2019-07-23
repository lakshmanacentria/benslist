package com.acentria.benslist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;

import com.google.analytics.tracking.android.EasyTracker;


public class SendFeedbackActivity extends AppCompatActivity {
	
	private int currentSubject = 0;
	private SendFeedbackActivity instance;
	private static Intent intent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Lang.setDirection(this);

		instance = this;
		intent = getIntent();
		
		setTitle(Lang.get("android_title_activity_send_feedback"));
        setContentView(R.layout.form);
        
        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        final LinearLayout layout = (LinearLayout) findViewById(R.id.fields_area);
        addFields(layout);
        
        /* reset form listener */
        Button resetButton = (Button) findViewById(R.id.form_reset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	clearForm();
            }
        });
	}
	
	public void clearForm() {
		LinearLayout layout = (LinearLayout) findViewById(R.id.fields_area);
		layout.removeAllViews();
    	addFields(layout);
    	currentSubject = 0;
	}
	
	public void addFields(LinearLayout fieldArea) {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, Utils.dp2px(44));
        params.setMargins(0, Utils.dp2px(15), 0, 0);
        
        /* name field */
        LinearLayout name_cont = (LinearLayout) getLayoutInflater().inflate(R.layout.field_text, null);
        final EditText textName = (EditText) name_cont.getChildAt(0);

		textName.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable text) {
				textName.setError(null);
			}
		});

		textName.setHint(Lang.get("android_your_name"));

        name_cont.setLayoutParams(params);
        fieldArea.addView(name_cont);
        
        /* e-mail field */
        LinearLayout email_cont = (LinearLayout) getLayoutInflater().inflate(R.layout.field_text, null);
        final EditText textEmail = (EditText) email_cont.getChildAt(0);

		textEmail.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable text) {
				textEmail.setError(null);
			}
		});

        textEmail.setHint(Lang.get("android_your_email"));
        textEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        email_cont.setLayoutParams(params);
        fieldArea.addView(email_cont);
        
        /* subject field */
        final Spinner spinnerSubject = (Spinner) getLayoutInflater().inflate(R.layout.spinner, null);
        spinnerSubject.setLayoutParams(params);
		
        String[] values = new String[] {
        		Lang.get("android_send_feedback_feedback"),
        		Lang.get("android_send_feedback_bug_report"),
        		Lang.get("android_send_feedback_feature_request"),
        		Lang.get("android_send_feedback_contact_us")};
        
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) new ArrayAdapter<String>(this,
		        R.layout.spinner_item, values);
		
		spinnerSubject.setAdapter(adapter);
		spinnerSubject.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
				currentSubject = position;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		if ( intent.hasExtra("selection") ) {
			if ( intent.getStringExtra("selection").equals("contact_us") ) {
				spinnerSubject.setSelection(3);
			}
		}
		
		fieldArea.addView(spinnerSubject);
		
		 /* message field */
        LinearLayout message_cont = (LinearLayout) getLayoutInflater().inflate(R.layout.field_textarea, null);
        final EditText textareaMessage = (EditText) message_cont.getChildAt(0);
		textareaMessage.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void afterTextChanged(Editable text) {
				textareaMessage.setError(null);
			}
		});

        textareaMessage.setHint(Lang.get("android_send_feedback_message"));

        params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, Utils.dp2px(15), 0, 0);

        message_cont.setLayoutParams(params);
        fieldArea.addView(message_cont);

        /* submit form listener */
        Button searchButton = (Button) findViewById(R.id.form_submit);
        searchButton.setText(Lang.get("android_send"));

        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	boolean error = false;

            	String name = textName.getText().toString();
            	if ( name.isEmpty() ) {
            		textName.setError(Lang.get("android_dialog_field_is_empty"));
            		error = true;
            	}

            	String message = textareaMessage.getText().toString();
            	if ( message.isEmpty() ) {
            		textareaMessage.setError(Lang.get("android_dialog_field_is_empty"));
            		error = true;
            	}

				String emailFrom = textEmail.getText().toString();
				if ( !isValidEmail(emailFrom) ) {
					textEmail.setError(Lang.get("bad_email"));
					error = true;
				}

            	if ( !error ) {

            		emailFrom = emailFrom.isEmpty() ? Utils.getCacheConfig("feedback_email") : emailFrom;
            		String emailTo = currentSubject == 1 ? "android@acentria.com" : Utils.getCacheConfig("feedback_email");

    		    	EditText currentFocus = (EditText) instance.getCurrentFocus();
    		    	Utils.hideKeyboard(currentFocus);

    		    	Dialog.simpleWarning(Lang.get("android_send_feedback_completed"), instance);

					Utils.sendEmail(spinnerSubject.getSelectedItem().toString()+" (from " + name + ")",
							textareaMessage.getText().toString(), emailFrom, emailTo);

					clearForm();
            	}
            }
        });
	}

	public final static boolean isValidEmail(CharSequence target) {
		if (TextUtils.isEmpty(target)) {
			return false;
		} else {
			return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
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