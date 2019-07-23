package com.acentria.benslist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.acentria.benslist.adapters.SortingAdapter;
import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Dialog {
	
	public static void simpleWarning(int message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(Config.context);
		
        builder.setMessage(message)
           .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) { }
           });
        
        builder.create();
        builder.show();
	}
	
	public static void simpleWarning(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(Config.context);
		
        builder.setMessage(message)
           .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) { }
           });
        
        builder.create();
        builder.show();
	}
	

	public static void simpleWarning(String message, Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
        builder.setMessage(message)
           .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) { }
           });
        
        builder.create();
        builder.show();
	}
	
	public static void simpleWarning(int message, Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
        builder.setMessage(message)
           .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) { }
           });
        
        builder.create();
        builder.show();
	}
	
	/**
	 * show dialog once the network unavaialble on welcome page with domain 
	 **/
	public static void welcomeNetworkUnavailable(int message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(Config.context);
		
        builder.setMessage(message)
           .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
            	   FlynaxWelcome.animateRefresh();
               }
           });
        
        builder.create();
        builder.show();
	}
	
	/**
	 * show dialog once the requested host unavaialble on welcome page
	 **/
	public static void welcomeHostUnavailable(int message) {
		if ( !Utils.getConfig("customer_domain").isEmpty() ) {
            welcomeNetworkUnavailable(message);
            return;
        }

		AlertDialog.Builder builder = new AlertDialog.Builder(Config.context);
		
        builder.setMessage(message)
           .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
            	   FlynaxWelcome.animateForm();
            	   EditText domainField = (EditText) Config.context.findViewById(R.id.domain_name);
            	   domainField.setText(Utils.getSPConfig("domain", null));
            	   domainField.setSelection(Utils.getSPConfig("domain", null).length());
               }
           });
        
        builder.create();
        builder.show();
	}
	
	/**
	 * show confirm action dialog with ok and cancel buttons
	 * 
	 * 
	 **/
	public static void confirmAction(String message, Context context, String positiveText, String negativeText, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
		context = context == null ? Config.context : context;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
        builder.setMessage(message)
        	.setPositiveButton(positiveText, positiveListener)
        	.setNegativeButton(negativeText, negativeListener);
        
        builder.create();
        builder.show();
	}
	/**
	 * show confirm action dialog with ok and cancel buttons
	 *
	 *
	 **/
	public static void confirmActionApp(int message, Context context, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
		context = context == null ? Config.context : context;

		AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(message)
        	.setPositiveButton(R.string.dialog_ok, positiveListener)
        	.setNegativeButton(R.string.dialog_cancel, negativeListener);

        builder.create();
        builder.show();
	}
	
	/**
	 * show confirm action dialog with ok and cancel buttons
	 * 
	 * 
	 **/
	public static void confirmAction(String message, Context context, DialogInterface.OnClickListener positiveListener, DialogInterface.OnClickListener negativeListener) {
        confirmAction(message, context, 
        		Lang.get("android_dialog_ok"),
        		Lang.get("android_dialog_cancel"),
        		positiveListener,
        		negativeListener
        );
	}

	/**
	 * show confirm action dialog with ok, cancel buttons and custom view
	 * 
	 * 
	 **/
	public static AlertDialog confirmActionView(String message, Context context, View view, 
			DialogInterface.OnClickListener positiveListener, 
			DialogInterface.OnClickListener negativeListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
		builder.setView(view);
        builder.setMessage(message)
        	.setPositiveButton(R.string.dialog_ok, positiveListener)
        	.setNegativeButton(Lang.get("android_dialog_cancel"), negativeListener);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        return dialog;
	}
	
	/**
	 * show information popup with custom view
	 * 
	 * 
	 **/
	public static AlertDialog infoView(String message, Context context, View view) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
		builder.setView(view);
        builder.setTitle(message)
        	.setPositiveButton(R.string.dialog_ok, null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        return dialog;
	}
	
	/**
	  * Display custom message dialog
	  *  
	  * 
	  * */
	public static void CustomDialog(String title, String message){
		
		AlertDialog.Builder builder = new AlertDialog.Builder(Config.context);
		
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int which) {
               return;
             }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	/**
	  * Display custom message dialog
	  *  
	  * 
	  * */
	public static void CustomDialog(String title, String message, Context context, DialogInterface.OnClickListener listener){
		
		context = context == null ? Config.context : context;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
		if ( !title.isEmpty() ) {
			builder.setTitle(title);
		}
		builder.setMessage(message);
		builder.setNegativeButton(Lang.get("android_dialog_cancel"), null);
		builder.setPositiveButton("OK", listener); 
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	 /**
	  * Display dialog to choose classifieds type 
	  * Set to input value
	  * 
	  * */
	public static void ListDialog(){
		final Resources resource = Config.context.getResources();
		final CharSequence[] typeName = resource.getStringArray(R.array.list_demo_selector);
		final CharSequence[] urlRelations = resource.getStringArray(R.array.list_demo_relations);

		AlertDialog.Builder builder = new AlertDialog.Builder(Config.context);
		builder.setTitle(resource.getString(R.string.list_dialog_title));
		builder.setItems(typeName, new DialogInterface.OnClickListener() {
			
		    public void onClick(DialogInterface dialog, int item) {
		    	EditText editText = (EditText) Config.context.findViewById(R.id.domain_name);
		    	editText.setText(urlRelations[item]);
		    	
		    	final Button go_button = (Button) Config.context.findViewById(R.id.welcome_go);
		    	go_button.setSoundEffectsEnabled(false);
		    	go_button.performClick();
		    }
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	public static void mapTypeDialog(Context context, final GoogleMap map) {
		final LinkedHashMap<String, Integer> types = new LinkedHashMap<String, Integer>();
		types.put("normal", GoogleMap.MAP_TYPE_NORMAL);
		types.put("satellite", GoogleMap.MAP_TYPE_SATELLITE);
		types.put("hybrid", GoogleMap.MAP_TYPE_HYBRID);
		types.put("terrain", GoogleMap.MAP_TYPE_TERRAIN);

		LayoutParams params = new LayoutParams(
	        LayoutParams.MATCH_PARENT,
	        LayoutParams.MATCH_PARENT
		);

		final RadioGroup group = new RadioGroup(context);
		group.setPadding(Utils.dp2px(5), Utils.dp2px(5), Utils.dp2px(5), Utils.dp2px(5));

		int i = 1;
		for (Map.Entry<String, Integer> entry : types.entrySet()) {
			RadioButton item = new RadioButton(context);
			item.setText(Lang.get("android_map_type_"+entry.getKey()));
			item.setLayoutParams(params);
			//item.setPadding(Utils.dp2px(5), Utils.dp2px(8), Utils.dp2px(5), Utils.dp2px(8));
			item.setTag(entry.getKey());
			item.setId(i);

			if ( Utils.getSPConfig("mapType", "").equals(entry.getKey())
					|| Utils.getSPConfig("mapType", null) == null && entry.getKey().equals("normal") ) {
				item.setChecked(true);
			}

			group.addView(item);
			i++;
		}

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(Lang.get("android_menu_map_type"));
    	dialog.setView(group);

    	/* set listener */
    	dialog.setNegativeButton(Lang.get("android_dialog_cancel"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
    	dialog.setPositiveButton(Lang.get("android_dialog_set"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String selectedKey = (String) group.findViewById(group.getCheckedRadioButtonId()).getTag();
				Utils.setSPConfig("mapType", selectedKey);
				map.setMapType(types.get(selectedKey));
			}
		});

		AlertDialog alert = dialog.create();
		alert.show();
	}
	
	public static void distanceUnitDialog(Context context) {
		String[][] units = {
			{"mi", Lang.get("android_unit_miles")},
			{"km", Lang.get("android_unit_kilometers")}
		};
		
		LayoutParams params = new LayoutParams(
	        LayoutParams.MATCH_PARENT,
	        LayoutParams.MATCH_PARENT
		);
		
		final RadioGroup group = new RadioGroup(context);
		group.setPadding(Utils.dp2px(5), Utils.dp2px(5), Utils.dp2px(5), Utils.dp2px(5));
		
		for (int i=0; i<units.length; i++) {
			RadioButton item = new RadioButton(context);
			item.setText(units[i][1]);
			item.setLayoutParams(params);
			//item.setPadding(Utils.dp2px(5), Utils.dp2px(8), Utils.dp2px(5), Utils.dp2px(8));
			item.setTag(units[i][0]);
			item.setId(i);
			
			if ( Utils.getSPConfig("distanceUnit", "").equals(units[i][0])
					|| Utils.getSPConfig("distanceUnit", null) == null && units[i][0].equals("mi") ) {
				item.setChecked(true);
			}
			
			group.addView(item);
		}
		
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);		
		dialog.setTitle(Lang.get("android_menu_distance_unit"));
    	dialog.setView(group);
		
    	/* set listener */
    	dialog.setNegativeButton(Lang.get("android_dialog_cancel"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
    	dialog.setPositiveButton(Lang.get("android_dialog_set"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String selectedKey = (String) group.findViewById(group.getCheckedRadioButtonId()).getTag();
				Utils.setSPConfig("distanceUnit", selectedKey);
			}
		});
    	
		AlertDialog alert = dialog.create();
		alert.show();
	}
	
	public static void sortingDialog(Context context, OnClickListener listener, 
			String currentKey, ArrayList<HashMap<String, String>> fields, String sortingField) {
		
		if ( fields.size() == 0 ) {
			Dialog.simpleWarning(Lang.get("android_no_sorting_fields_available"), context);
			return;
		}

		String[] f = {"price", "mixed", "number"};
		List<String> listMulti = Utils.string2list(f);

		String[][] sortTypes = {
			{"asc", Lang.get("android_sort_asc")},
			{"desc", Lang.get("android_sort_desc")}
		};

		/* prepare fields */
		ArrayList<HashMap<String, String>> fieldsOut = new ArrayList<HashMap<String, String>>();
		for ( HashMap<String, String> enter : fields ) {
			if ( listMulti.indexOf(enter.get("type")) >= 0 ) {
				for ( int j = 0; j < sortTypes.length; j++ ) {
					HashMap<String, String> item = new HashMap<String, String>();

					String tag = enter.get("key")+"|/|"+sortTypes[j][0];
					item.put("key", tag);
					item.put("name", enter.get("name")+" "+sortTypes[j][1]);
					fieldsOut.add(item);
				}
			}
			else {
				fieldsOut.add(enter);
			}
		}

		LayoutParams params = new LayoutParams(
	        LayoutParams.MATCH_PARENT,
	        LayoutParams.MATCH_PARENT
		);

		SortingAdapter adapter = new SortingAdapter(fieldsOut, context, sortingField);

		ListView listView = (ListView) ((Activity) context).getLayoutInflater()
				.inflate(R.layout.list_view, null);

		listView.setLayoutParams(params);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(adapter);

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(Lang.get("android_sort_listings_by"));
    	dialog.setView(listView);

    	/* set negative listener */
    	dialog.setNegativeButton(Lang.get("android_dialog_cancel"), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});

    	dialog.setPositiveButton(Lang.get("android_dialog_sort"), listener);
		dialog.create().show();
	}
	
	public static void toast(String key) {
		toast(key, Config.context);
	}
	
	public static void toast(Integer key) {
		toast(key, Config.context);
	}
	
	public static void toast(String key, Context instance) {
		Toast.makeText(instance, Lang.get(key), Toast.LENGTH_LONG).show();
	}
	
	public static void toast(Integer key, Context instance) {
        Toast.makeText(instance, Config.context.getResources().getString(key), Toast.LENGTH_LONG).show();
	}
}
