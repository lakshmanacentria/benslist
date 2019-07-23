package com.acentria.benslist;

import android.content.Intent;
import com.acentria.benslist.preferences.Preferences;

public class FlynMenu {
	
	public static void menuItemSetting(){
		Config.context.startActivity(new Intent(Config.context, Preferences.class));
	}
	
}
