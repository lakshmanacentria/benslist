package com.acentria.benslist.controllers;

import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;

import java.util.ArrayList;

public abstract class AbstractController extends AppCompatActivity {
	
	protected static void handleMenuItems(final int[] menuItems) {
		if ( Config.menu == null ) {
			CountDownTimer timer = new CountDownTimer(450, 450) {
				public void onTick(long millisUntilFinished) {}
			
				public void onFinish() {
					handleMenuItems(menuItems);
				}
			};
			timer.start();
			return;
		}
		
		ArrayList<Integer> items = new ArrayList<Integer>();
		for ( int i = 0; i < menuItems.length; i++) {
			items.add(menuItems[i]);
		}
		
		for ( int i = 0; i < Config.menu.size(); i++) {
			MenuItem mi = (MenuItem) Config.menu.getItem(i);
			
			if ( items.indexOf(mi.getItemId()) >= 0 ) {
				switch(mi.getItemId()) {
					case R.id.menu_logout:
						mi.setVisible(Account.loggedIn ? true : false);
						break;

					case R.id.menu_remove_account:
						int version = Utils.getCacheConfig("rl_version") != null ? Config.compireVersion(Utils.getCacheConfig("rl_version"), "4.7.0") : -1 ;
						mi.setVisible(version >= 0  && Account.loggedIn ? true : false);
						break;
					
					default:
						mi.setVisible(true);
						break;
				}
			}
			else {
				mi.setVisible(false);
			}
		}
	}
}