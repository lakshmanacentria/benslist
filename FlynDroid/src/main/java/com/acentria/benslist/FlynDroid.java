package com.acentria.benslist;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.acentria.benslist.controllers.AccountArea;
import com.acentria.benslist.controllers.Home;
import com.acentria.benslist.controllers.MyMessages;
import com.acentria.benslist.controllers.MyPackages;
import com.acentria.benslist.controllers.SavedSearch;
import com.acentria.benslist.controllers.Search;
import com.google.analytics.tracking.android.EasyTracker;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class FlynDroid extends SlidingActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.Theme_FlynaxDefaultHome);

        /* pass main activity context to the main config */
        Config.context = this;

		// set intent
		Config.configIntent = getIntent();
		Intent intent = getIntent();
		if(intent.getStringExtra("key")!=null) {
			Config.pushView = intent.getStringExtra("key");
		}

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
					this.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }

        /* set behind view and create sliding menu */
        //if ( savedInstanceState == null ) {
        setContentView(R.layout.responsive_content_frame);
        //}

		/* check if the content frame contains the menu frame */
        if ( findViewById(R.id.menu_frame) == null ) {

            setBehindContentView(R.layout.menu);
            SwipeMenu.menu = getSlidingMenu();
            SwipeMenu.menu.setSlidingEnabled(true);
            SwipeMenu.menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);
        } else {
            View v = new View(this);
            setBehindContentView(v);
            SwipeMenu.menu = getSlidingMenu();
            SwipeMenu.menu.setSlidingEnabled(false);
            SwipeMenu.menu.setTouchModeAbove(SlidingMenu.TOUCHMODE_NONE);

            Config.tabletMode = true;
        }

        if ( android.os.Build.VERSION.SDK_INT > 9 ) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}
		String customer_domain = getString(R.string.customer_domain);
        
        // DEMO MODE
        if ( Utils.getConfig("customer_domain").isEmpty() && customer_domain.isEmpty()) {
	        if ( Utils.getSPConfig("domain", null) == null ) {
	        	/* prepare welcome connect form  */
	            FlynaxWelcome.animateForm();
	        }
	        else {
	        	/* initialize client website cache */
	        	FlynaxWelcome.showLaunch();
	        	FlynaxWelcome.checkAvailability(false);
	        }
        }
        // CUSTOMER MODE
        else {
        	if ( Utils.getSPConfig("domain", null) == null && !Utils.getConfig("customer_domain").isEmpty()) {
          		Utils.setSPConfig("domain", Utils.getConfig("customer_domain"));
            }
			else if ( Utils.getSPConfig("domain", null) == null && !customer_domain.isEmpty()) {
          		Utils.setSPConfig("domain", customer_domain);
            }

        	FlynaxWelcome.showLaunch();
        	FlynaxWelcome.checkAvailability(false);
        }
    }

    @Override
	public void onNewIntent(Intent intent){
		Bundle extras = intent.getExtras();

		Config.configIntent = intent;
		if(extras != null){
			Config.pushView = intent.getStringExtra("key");

			if (intent.getStringExtra("key")!=null && Account.loggedIn) {
				if(intent.getStringExtra("key").equals("message") && MyMessages.switcherMs) {
					MyMessages.switchToMyMessages();
				}
				else if(intent.getStringExtra("key").equals("save_search") && Config.cacheListingTypes!=null && Config.cacheListingTypes.size() > 0) {
					SavedSearch.switchToSavedSearch(intent);
				}
			}
		}
		MyMessages.switcherMs = true;
	}

	@Override
	public void onBackPressed() {
		if ( Utils.getSPConfig("domain", null) == null ) {
			super.onBackPressed();
		}
		else {
			if ( SwipeMenu.menu.isMenuShowing() ) {
				SwipeMenu.menu.showContent();
			}
			else {
				if ( Config.currentView.equals("Home") ) {
					super.onBackPressed();
					MyMessages.switcherMs = false;
				}
				else {
					/* save current view */
					Config.prevView = Config.currentView;
					Config.currentView = "Home";
					
					/* set current menu position as current */
					SwipeMenu.adapter.previousPosition = SwipeMenu.adapter.currentPosition;
					SwipeMenu.adapter.currentPosition = 0;

					SwipeMenu.adapter.notifyDataSetChanged();
					
					Home.getInstance();
				}
			}
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Config.menu = menu;
        getMenuInflater().inflate(R.menu.activity_flyndroid, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
	            FlynMenu.menuItemSetting();
	            
	            return true;
	           
	        case R.id.menu_about_app:
	        	Intent intent1 = new Intent(Config.context, AboutAppActivity.class);
    			Config.context.startActivity(intent1);
    			
	        	return true;
	        
	        case R.id.menu_send_feedback:
	        	Intent intent2 = new Intent(Config.context, SendFeedbackActivity.class);
    			Config.context.startActivity(intent2);

	        	return true;

	        case R.id.menu_search:
	        	Config.prevView = Config.currentView;
				Config.currentView = "Search";

	            SwipeMenu.adapter.previousPosition = SwipeMenu.adapter.currentPosition;
	            SwipeMenu.adapter.currentPosition = -1;
	            SwipeMenu.adapter.notifyDataSetChanged();

	            Search.getInstance();

	            return true;

	        case R.id.menu_logout:
	        	AccountArea.logout();

                return true;

			case R.id.menu_remove_account:
	        	AccountArea.removeAccount();

                return true;

            case android.R.id.home:
                SwipeMenu.menu.showMenu();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
	
	@Override	
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		if ( Utils.getSPConfig("domain", null) != null && !Config.tabletMode ) {
			SwipeMenu.changeBehindOffset();
		}
	    
		Utils.onOrientationChangeHandler(newConfig.orientation);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Config.menu = menu;
		return super.onPrepareOptionsMenu(menu);
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

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

		if (requestCode == 2222) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Home.updateMapHost(true);
			} else {
				// Permission Denied
				Toast.makeText(Config.context, "Permission Denied", Toast.LENGTH_LONG).show();
				Home.updateMapHost(false);
			}
			return;
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);

    	switch (requestCode) {
    		case AccountArea.PROFILE_IMAGE:
    			Image.manageSelectedImage(resultCode, data, Config.context);
    			break;
    			
    		case Image.PIC_CROP:
    			if ( data == null || resultCode == RESULT_CANCELED ) {
    				Toast.makeText(this, Lang.get("file_selection_canceled"), Toast.LENGTH_LONG).show();
    			}
    			else {
    	            Bundle extras = data.getExtras();
    	            Bitmap selectedBitmap = extras.getParcelable("data");
    	            Image.upload(selectedBitmap);
    	        }
    			break;
    			
            case AccountArea.FB_SIGN_IN:
                AccountArea.callbackManager.onActivityResult(requestCode, resultCode, data);
    			
    			break;

			case Config.RESULT_PAYMENT:
				if ( resultCode == RESULT_OK ) {
					Dialog.simpleWarning(Lang.get("listing_plan_upgraded"));
					if ( data.hasExtra("success") ) {
						HashMap<String, String> plan = (HashMap<String, String>) data.getSerializableExtra("success");
						MyPackages.updatePackage(plan, MyPackages.availablePlans);
					}
					else {
						Log.d("FD", "Add Listing Activity - no success data received, listview update failed");
						Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
					}
				}
				else if (resultCode == Config.RESULT_TRANSACTION_FAILED ) {
					Dialog.simpleWarning(Lang.get("dialog_unable_approve_transaction"));
					Utils.bugRequest("Payment result error ("+Utils.getSPConfig("domain", "")+")", data.toString());
				}
				break;

//    		case Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE:
//    			Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
//
//    			break;
    	};
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }
}
