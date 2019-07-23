package com.acentria.benslist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import com.acentria.benslist.controllers.MyMessages;
import com.google.analytics.tracking.android.EasyTracker;

import java.util.HashMap;

public class MessagesActivity extends AppCompatActivity {
	public static MessagesActivity instance;
	private static Intent intent;
	public static Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Lang.setDirection(this);
		instance = this;
		context = instance;

		/* get account data from instance */
		intent = getIntent();
		MyMessages.contactID = intent.getStringExtra("id");
		MyMessages.contactInfo = (HashMap<String, String>) intent.getSerializableExtra("data");
		MyMessages.sendMail = intent.getStringExtra("sendMail");
		MyMessages.listing_id = intent.getStringExtra("listing_id");

		/* enable back action */
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		setTitle(Html.fromHtml(MyMessages.contactInfo.get("full_name")));
		setContentView(R.layout.activity_messages);

		RelativeLayout content = (RelativeLayout) findViewById(R.id.activity_messages);
		MyMessages.getMessages(content);

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
