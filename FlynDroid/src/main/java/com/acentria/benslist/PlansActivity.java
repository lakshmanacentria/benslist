package com.acentria.benslist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.acentria.benslist.controllers.MyListings;
import com.google.analytics.tracking.android.EasyTracker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class PlansActivity extends AppCompatActivity {
	
	private Activity activity;
	private Context instance;
	private HashMap<String, String> listing_hash;
	private ArrayList<HashMap<String, String>> plans;
	
	private String selected_id = null;
	private String selected_type = "standard";
	private Integer selected_position = null;
	private String featured_only = "0";
	private boolean open_for_result = false;
	
	private LinearLayout plan_container;
	private LinearLayout view; 
	
	public Payment payment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Lang.setDirection(this);

		setTitle(Lang.get("title_activity_select_plan"));
        setContentView(R.layout.activity_plans);
        
        activity = this;
        instance = this;
        
        // enable back action
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        final Intent intent = getIntent();
        
        view = (LinearLayout) getLayoutInflater().inflate(R.layout.list_view_plans, null);
        
        String category_id = intent.getStringExtra("category_id");
        
        if ( intent.hasExtra("featured_only") && intent.getStringExtra("featured_only").equals("1") ) {
        	featured_only = "1";
        }
        
        if ( category_id == null || category_id.isEmpty() ) {
        	Dialog.simpleWarning(R.string.plans_no_category);
        	finish();
        }
        
        if ( intent.hasExtra("mode") && intent.getStringExtra("mode").equals("select") ) {
        	open_for_result = true;
        	selected_id = intent.getStringExtra("plan_id");
        	
        	plans = (ArrayList<HashMap<String, String>>) intent.getSerializableExtra("plans");
        	
        	init();
        }
        else {
        	listing_hash = (HashMap<String, String>) intent.getSerializableExtra("hash");
        
	        // set up billing
	        payment = new Payment(activity, false, null);
	        
	        // get plans data
	        HashMap<String, String> params = new HashMap<String, String>();
	        params.put("account_id", Account.accountData.get("id"));
			params.put("password_hash", Utils.getSPConfig("accountPassword", null));
	        
			params.put("category_id", category_id);
			params.put("account_type", Account.accountData.get("type"));
			params.put("featured_only", featured_only);
			
			final String url = Utils.buildRequestUrl("getPlans", params, null);
	
			/* do async request */
	    	AsyncHttpClient client = new AsyncHttpClient();
	    	client.get(url, new AsyncHttpResponseHandler() {
	
				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String str = String.valueOf(new String(server_response, "UTF-8"));
//						plans = Utils.parseXML(xml, url, instance);

						if(JSONParser.isJson(str)) {
							plans = JSONParser.parseJsontoArrayList(str);
						}

						// synchronize plans
				 		payment.synchronizePlans(plans);

				 		if ( featured_only.equals("0") ) {
							selected_id = listing_hash.get("plan");
							selected_type = listing_hash.containsKey("Featured") && listing_hash.get("Featured").equals("1") ? "Featured" : selected_type;
						}

				 		init();

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
	
	private void init() {
		LinearLayout parent = (LinearLayout) findViewById(R.id.container);
		parent.removeAllViews();

		// prepare plans list view
		if ( plans.size() == 0 ) {
			TextView message = (TextView) getLayoutInflater()
	    			.inflate(R.layout.info_message, null);
    		
    		message.setText(Lang.get("listing_no_upgrade_plans"));
    		parent.addView(message);
		}
//		else if ( plans.size() == 1 ) {
//			// single plan mode, I guess we have to show it anyway
//		}
		else {
			LayoutParams params = new LayoutParams(
			        LayoutParams.MATCH_PARENT,
			        LayoutParams.MATCH_PARENT
			);
			view.setLayoutParams(params);
			
			parent.setGravity(Gravity.TOP);
			parent.addView(view);
			
			plan_container = (LinearLayout) findViewById(R.id.plans_container);
			
			drowPlans();
			drowButton();
		}
	}
	
	private void drowButton() {
		// button
		Button select_button = (Button) view.findViewById(R.id.select_plan);
		select_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectPlan();
			}
		});
	}
	
	private void drowPlans() {
		LayoutParams margin_bottom = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		margin_bottom.setMargins(0, 0, 0, Utils.dp2px(10));
		View scroll_to_view = null;
		
		for ( int i=0; i<plans.size(); i++ ) {
			View view = getLayoutInflater().inflate(R.layout.plan, null);
			if ( i != plans.size() - 1 ) {
				view.setLayoutParams(margin_bottom);
			}
			
			final int position = i;
			
			LinearLayout color = (LinearLayout) view.findViewById(R.id.color);
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView type = (TextView) view.findViewById(R.id.type);
			TextView price = (TextView) view.findViewById(R.id.price);
			TextView description = (TextView) view.findViewById(R.id.description);
			TextView notice_text = (TextView) view.findViewById(R.id.notice_text);
			LinearLayout photo = (LinearLayout) view.findViewById(R.id.photo);
			LinearLayout video = (LinearLayout) view.findViewById(R.id.video);
			LinearLayout listing_live = (LinearLayout) view.findViewById(R.id.listing_live);
			RadioGroup radio_group = (RadioGroup) view.findViewById(R.id.radio_group);
			View spliter = (View) radio_group.findViewById(R.id.spliter);
			LinearLayout standard_box = (LinearLayout) radio_group.findViewById(R.id.standard_box);
			final RadioButton radio_standard = (RadioButton) radio_group.findViewById(R.id.standard_button);
			TextView standard_button_name = (TextView) radio_group.findViewById(R.id.standard_button_name);
			TextView standard_button_count = (TextView) radio_group.findViewById(R.id.standard_button_count);
			final RadioButton radio_featured = (RadioButton) radio_group.findViewById(R.id.featured_button);
			LinearLayout featured_box = (LinearLayout) radio_group.findViewById(R.id.featured_box);
			TextView featured_button_name = (TextView) radio_group.findViewById(R.id.featured_button_name);
			TextView featured_button_count = (TextView) radio_group.findViewById(R.id.featured_button_count);
			
			final HashMap<String, String> item = plans.get(i);
			
			// texts
			title.setText(item.get("name"));
			type.setText(Lang.get("plan_"+item.get("type")));
			price.setText(Integer.parseInt(item.get("Listings_remains")) > 0 ? Lang.get("available_package") : Utils.buildPrice(item.get("Price")));
			
			// color
			if ( item.containsKey("Color") && !item.get("Color").isEmpty() ) {
				try {
					color.setBackgroundColor(Color.parseColor("#"+item.get("Color")));
				} catch (Exception e) {
					e.printStackTrace();
					color.setBackgroundColor(getResources().getColor(R.color.plan_default_color));
				}
			}
			
			// photos
			if ( !item.get("Image").equals("0") || !item.get("Image_unlim").equals("0") ) {
				String photo_count = item.get("Image_unlim").equals("0") ? item.get("Image") : "\u221E";
				
				photo.setVisibility(View.VISIBLE);
				((TextView) photo.findViewWithTag("text")).setText(photo_count);
			}
			
			// video
			if ( !item.get("Video").equals("0") || !item.get("Video_unlim").equals("0") ) {
				String video_count = item.get("Video_unlim").equals("0") ? item.get("Video") : "\u221E";
				
				video.setVisibility(View.VISIBLE);
				((TextView) video.findViewWithTag("text")).setText(video_count);
			}
			
			// live till
			String live_till = item.get("Listing_period").equals("0") ? "\u221E" : Lang.get("listing_period_days").replace("{days}", item.get("Listing_period"));
			((TextView) listing_live.findViewWithTag("text")).setText(Html.fromHtml(live_till));
			
			// radio
			int standard_visible = View.VISIBLE;
			String standard_tail = "";
			Boolean standard_enabled = true;
			Boolean standard_checked = false;
			
			int featured_visible = View.VISIBLE;
			String featured_tail = "";
			Boolean featured_enabled = true;
			Boolean featured_checked = false;
			
			final boolean is_enabled = Integer.parseInt(item.get("Limit")) > 0 && item.get("Using").equals("0") ? false : true;

			// advanced mode
			if ( item.get("Advanced_mode").equals("1") ) {
				/*** STANDARD ***/
				int standard_remains = item.get("Standard_remains").isEmpty() ? 0 : Integer.parseInt(item.get("Standard_remains"));
				
				// phrase
				if ( Integer.parseInt(item.get("Standard_listings")) > 0 ) {
					standard_tail = item.get("Standard_listings");
					if ( Integer.parseInt(item.get("Listings_remains")) > 0 ) {
						standard_tail = standard_remains <= 0 ? Lang.get("used_up") : item.get("Standard_remains");
					}
				}
				standard_tail = standard_tail.isEmpty() ? "" :  standard_tail +" "+ Lang.get("android_listings");
				
				// status
				standard_enabled = !item.get("Package_ID").isEmpty() && standard_remains <= 0 && !item.get("Standard_listings").equals("0") ? false : true;
				
				// checked
				standard_checked = item.get("ID").equals(selected_id) && selected_type.equals("standard") ? true : false;
				
				/*** FEATURED ***/
				int featured_remains = item.get("Featured_remains").isEmpty() ? 0 : Integer.parseInt(item.get("Featured_remains"));
				
				// phrase
				if ( Integer.parseInt(item.get("Featured_listings")) > 0 ) {
					featured_tail = item.get("Featured_listings");
					if ( Integer.parseInt(item.get("Listings_remains")) > 0 ) {
						featured_tail = featured_remains <= 0 ? Lang.get("used_up") : item.get("Featured_remains");
					}
				}
				featured_tail = featured_tail.isEmpty() ? "" : featured_tail +" "+ Lang.get("android_listings");
				
				// status
				featured_enabled = !item.get("Package_ID").isEmpty() && featured_remains <= 0 && !item.get("Featured_listings").equals("0") ? false : true;
				
				// checked
				featured_checked = item.get("ID").equals(selected_id) && selected_type.equals("Featured") ? true : false;
				
				// general check logic
				if ( standard_checked && !standard_enabled && featured_enabled ) {
					standard_checked = false;
					featured_checked = true;
					selected_type = "featured";
				}
				else if ( featured_checked && !featured_enabled && standard_enabled ) {
					standard_checked = true;
					featured_checked = false;
					selected_type = "standard";
				}
				else if ( !featured_enabled && !standard_enabled ) {
					standard_checked = false;
					featured_checked = false;
				}
				spliter.setVisibility(View.VISIBLE);
				radio_group.setPadding(Utils.dp2px(8),Utils.dp2px(15),Utils.dp2px(8),Utils.dp2px(15));
			}
			// default mode
			else {
				// standard
				if ( item.get("Featured").equals("1") || item.get("Type").equals("Featured") ) {
					standard_visible = View.GONE;
					featured_checked = item.get("ID").equals(selected_id) && is_enabled ? true : false;
				}
				// featured
				else {
					featured_visible = View.GONE;
					standard_checked = item.get("ID").equals(selected_id) && is_enabled ? true : false;
				}
				
				if ( !is_enabled ) {
					radio_group.setVisibility(View.GONE);
					notice_text.setVisibility(View.VISIBLE);
				}
				spliter.setVisibility(View.GONE);
				standard_enabled = featured_enabled = is_enabled;
			}
			standard_box.setVisibility(standard_visible);
			radio_standard.setVisibility(standard_visible);
			standard_button_name.setText(Lang.get("standard_listing"));
			if(standard_tail.isEmpty()) {
				standard_button_count.setVisibility(View.GONE);
			}
			else {
				standard_button_count.setText(standard_tail);
				standard_button_count.setVisibility(View.VISIBLE);
			}
//			radio_standard.setText(Lang.get("standard_listing")+standard_tail);
			if ( !standard_enabled ) {
				radio_standard.setEnabled(standard_enabled);
			}
			if ( standard_checked ) {
				radio_standard.setChecked(standard_checked);
			}
			
			featured_box.setVisibility(featured_visible);
			radio_featured.setVisibility(featured_visible);
//			radio_featured.setText(Lang.get("featured_listing")+featured_tail);
			featured_button_name.setText(Lang.get("featured_listing"));
			if(featured_tail.isEmpty()) {
				featured_button_count.setVisibility(View.GONE);
			}
			else {
				featured_button_count.setText(featured_tail);
				featured_button_count.setVisibility(View.VISIBLE);
			}

			if ( !featured_enabled ) {
				radio_featured.setEnabled(featured_enabled);
			}
			if ( featured_checked ) {
				radio_featured.setChecked(featured_checked);
			}
			
			OnClickListener listener = new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onChange(position, v);
				}
			};
			
			// radio click listener
			radio_standard.setOnClickListener(listener);
			radio_featured.setOnClickListener(listener);
			
			standard_box.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onChange(position, radio_standard);
					radio_standard.setChecked(true);
				}
			});
			featured_box.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onChange(position, radio_featured);
					radio_featured.setChecked(true);
				}
			});
			
			// add view
			plan_container.addView(view);
			
			if ( item.get("ID").equals(selected_id) ) {
				scroll_to_view = view;
				selected_position = i;
			}
		}
		
		focusOnView(scroll_to_view);
	}
	
	private void onChange(int position, View view) {

		if ( plans.get(position).get("ID").equals(selected_id) && view.getTag().equals(selected_type) )
			return;
		
		// reset all radio buttons in list view
		for (int i = 0; i < plans.size(); i++) {
			LinearLayout ll = (LinearLayout) plan_container.getChildAt(i);

			if  ( ll != null && i != position ) {
				RadioGroup rg = (RadioGroup) ll.findViewById(R.id.radio_group);
				rg.clearCheck();
			}
		}

		selected_type = (String) view.getTag();
		selected_id = plans.get(position).get("ID");
		selected_position = position;
	}
	
	private void selectPlan() {
		
		if ( selected_id == null || selected_id.equals("0") || selected_position == null ) {
			Dialog.simpleWarning(Lang.get("dialog_no_plan_selected"), instance);
		}
		else {
			HashMap<String, String> plan = plans.get(selected_position);
			
			if ( open_for_result ) {
				Intent resultIntent = new Intent(); 
				resultIntent.putExtra("selected_id", selected_id);
				resultIntent.putExtra("selected_type", selected_type);
				resultIntent.putExtra("selected_position", selected_position);
				setResult(RESULT_OK, resultIntent);
				((Activity) instance).finish();
			}
			else {
				Log.d("FD", listing_hash.toString());
				
				int listing_reminds = plan.get("Listings_remains").isEmpty() ? 0 : Integer.parseInt(plan.get("Listings_remains"));
				
				// free plan or available package mode
				if ( plan.get("Price").equals("0") || listing_reminds > 0 ) {
					final ProgressDialog progress = ProgressDialog.show(instance, null, Lang.get("android_sync_with_server"));
					
					// get plans data
			        HashMap<String, String> params = new HashMap<String, String>();
			        params.put("account_id", Account.accountData.get("id"));
					params.put("password_hash", Utils.getSPConfig("accountPassword", null));
					
					params.put("listing_id", listing_hash.get("id"));
					params.put("plan_id", plan.get("ID"));
					params.put("appearance", selected_type);
					
					final String url = Utils.buildRequestUrl("upgradePlan", params, null);

					/* do async request */
			    	AsyncHttpClient client = new AsyncHttpClient();
			    	client.get(url, new AsyncHttpResponseHandler() {
	
						@Override
						public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
							// called when response HTTP status is "200 OK"
							try {
								String response = String.valueOf(new String(server_response, "UTF-8"));
								progress.dismiss();
								Log.d("FD - free mode", response);

								/* parse response */
								XMLParser parser = new XMLParser();
								Document doc = parser.getDomElement(response, url);

								if ( doc == null ) {
									Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), instance);
								}
								else {
									NodeList successNode = doc.getElementsByTagName("success");
									if ( successNode.getLength() > 0 ) {
										Element success = (Element) successNode.item(0);

										HashMap<String, String> last_listing_hash = Utils.parseHash(success.getChildNodes());
										last_listing_hash.put("photo", Utils.getNodeByName(success, "photo"));
										last_listing_hash.put("photos_count", Utils.getNodeByName(success, "photos_count"));

										MyListings.updateItem(Integer.parseInt(listing_hash.get("id")), last_listing_hash);

										Dialog.simpleWarning(Lang.get("listing_plan_upgraded"));
										activity.finish();
									}
									else {
										Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), instance);
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
				// paid plan mode
				else {
					// apply selected plan data
					listing_hash.put("item", plan.get("Type"));//plan type
					listing_hash.put("service", "listings");//services
					listing_hash.put("amount", plan.get("Price"));//plan price
					listing_hash.put("title", Utils.buildTitleForOrder(plan.get("Type"), listing_hash.get("id")));// listing title
					listing_hash.put("plan", plan.get("ID"));//plan id
					listing_hash.put("featured", selected_type.equals("Featured") ? "1" : "0");//appearance type
					listing_hash.put("plan_key", plan.get("Key"));
					listing_hash.put("success_phrase", Lang.get(Utils.getCacheConfig("listing_auto_approval").equals("1") ? "listing_paid_auto_approved" : "listing_paid_pending"));
					
					// start activity
					Intent intent = new Intent(activity, PurchaseActivity.class);
					intent.putExtra("hash", listing_hash);
					
					startActivityForResult(intent, Config.RESULT_PAYMENT);
				}
			}
		}
	}
	
	private final void focusOnView(final View view){
		if ( view == null )
			return;
		
        new Handler().post(new Runnable() {
            @Override
            public void run() {
            	ScrollView sw = (ScrollView) findViewById(R.id.plans_scroll);
        		sw.smoothScrollTo(0, view.getTop());
            }
        });
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		switch (requestCode) {
			case Config.RESULT_PAYMENT:
				if ( resultCode == RESULT_OK ) {
					if ( data.hasExtra("success") ) {
						HashMap<String, String> hash = (HashMap<String, String>) data.getSerializableExtra("success");
						MyListings.updateItem(Integer.parseInt(listing_hash.get("id")), hash);
						
						Dialog.simpleWarning(listing_hash.get("success_phrase"));
					}
					else {
						Log.d("FD", "Plans Activity - no success data received, listview update failed");
						Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
					}
				}
				else if (resultCode == Config.RESULT_TRANSACTION_FAILED ) {
					Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
					Utils.bugRequest("Payment result error ("+Utils.getSPConfig("domain", "")+")", data.toString());
				}
				
				activity.finish();
				
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
		super.onDestroy();
		
		if ( payment != null && payment.mService != null ) {
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