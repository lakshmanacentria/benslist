package com.acentria.benslist.controllers;

import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;

public class SearchAround {

	private static SearchAround instance;
	private static final String Title = Lang.get("android_title_activity_search_around");
	
	public static SearchAround getInstance() {
		if ( instance == null ) {
			instance = new SearchAround();
			Config.activeInstances.add(instance.getClass().getSimpleName());
		}
		else {
			Utils.restroreInstanceView(instance.getClass().getSimpleName(), Title);
		}
		
		return instance;
	}
	
	public static void removeInstance(){
		instance = null;
	}
	
	public SearchAround(){
		
		/* set content title */
		Config.context.setTitle(Title);
		
		/* add content view */
		Utils.addContentView(R.layout.view_search_around);
		
		/* get related view */
		//final RelativeLayout layout = (RelativeLayout) Config.context.getWindow().findViewById(R.id.Favorites);
		
		/* hide menu */
		Utils.showContent();
	}
}