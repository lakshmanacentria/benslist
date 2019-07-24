package com.acentria.benslist.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.R;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

public class SwipeMenuAdapter extends BaseAdapter implements OnItemClickListener {

	private static final String TAG = "SwipeMenuAdapter=>";
	private ArrayList<HashMap<String, String>> data;
    private LayoutInflater inflater = null;
    public int currentPosition = 0;
    public int previousPosition = 0;

    public SwipeMenuAdapter(ArrayList<HashMap<String, String>> menuData) {
        data = menuData;
        inflater = Config.context.getLayoutInflater();
    }

    public int getCount() {
    	int size = data.size();
    	if ( !Account.loggedIn ) {
    		size -= SwipeMenu.accountItems;
    	}
        return size;
    }

    public Object getItem(int position) {
        return position;
    }

	private class ViewHolder {
		public LinearLayout menuItem;
		public TextView menuItemName;
		public TextView menuItemCount;
		public ImageView menuItemIcon;
	}
    
    public long getItemId(int position) {
        return position;
    }

	public boolean isEnabled(int position) {
		if ( position > SwipeMenu.loginIndex+1 && !Account.loggedIn ) {
    		position += SwipeMenu.accountItems;
    	}
		
    	if ( data.get(position).get("icon") == "divider" ) {
    		return false;
    	}
        return true;
    }
    
	public View getView(int position, View convertView, ViewGroup parent) {
		if ( position > SwipeMenu.loginIndex+1 && !Account.loggedIn ) {
    		position += SwipeMenu.accountItems;
    	}
		
        HashMap<String, String> item = new HashMap<String, String>();
        item = data.get(position);
        ViewHolder holder;

        View view = convertView;
        
        if ( view == null ){
        	view = inflater.inflate(R.layout.menu_item, null);
        	
        	holder = new ViewHolder();
        	
        	holder.menuItem = (LinearLayout)view.findViewById(R.id.menu_item);
            holder.menuItemName = (TextView)view.findViewById(R.id.menu_item_name);
            holder.menuItemCount = (TextView)view.findViewById(R.id.menu_item_count);
            holder.menuItemIcon = (ImageView)view.findViewById(R.id.menu_item_icon);
            
            view.setTag(holder);
        }
        else {
        	holder = (ViewHolder) view.getTag();
        }
        
        if ( item.get("icon") == "divider" )
        {
        	holder.menuItem.setPadding(Utils.dp2px(12), Utils.dp2px(15), Utils.dp2px(12), Utils.dp2px(8));
    		holder.menuItemName.setText(item.get("name").toUpperCase());
    		holder.menuItemName.setTextSize(13);
    		holder.menuItemName.setTextColor(Color.parseColor("#898989"));
    		holder.menuItemCount.setVisibility(View.GONE);
    		holder.menuItemIcon.setVisibility(View.GONE);
    		view.setBackgroundResource(R.mipmap.blank);
    		Log.e(TAG," dividerside=>"+item.get("name"));

        }
        else
        {
        	holder.menuItem.setPadding(Utils.dp2px(12), Utils.dp2px(15), Utils.dp2px(12), Utils.dp2px(15));
        	holder.menuItemName.setText(item.get("name"));
			Log.e(TAG,"without divider side=>"+item.get("name"));
        	holder.menuItemName.setTextColor(Color.parseColor("#ffffff"));
    		
            String count = item.get("count");
            if ( count != null )
            {
            	holder.menuItemCount.setText(item.get("count"));
            	holder.menuItemCount.setVisibility(View.VISIBLE);
            }
            else
            {
            	holder.menuItemCount.setVisibility(View.GONE);        	
            }
            holder.menuItemName.setTextSize(17);
            
            holder.menuItemIcon.setImageResource(Config.context.getResources().getIdentifier(item.get("icon"), "mipmap", Config.context.getPackageName()));
            holder.menuItemIcon.setVisibility(View.VISIBLE);

            if ( position == currentPosition ) {
            	view.setBackgroundResource(R.drawable.sm_list_view);
            }
            else {
            	view.setBackgroundResource(R.mipmap.blank);
            }
        }

        return view;
	}

	public int getPositionByController(String controller)
	{
		for (int position=0; position<SwipeMenu.menuData.size(); position++)
			if (SwipeMenu.menuData.get(position).get("controller") == controller)
				return position;
		return -1;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {
		if ( position > SwipeMenu.loginIndex+1 && !Account.loggedIn ) {
    		position += SwipeMenu.accountItems;
    	}
		
		String className = data.get(position).get("controller");
		Log.e(TAG, "ClassName onItememName=> "+className+"\ntype=> "+data.get(position).get("type"));
		if ( data.get(position).get("type").equals(SwipeMenu.CON) )	{
			if ( className == Config.currentView 
					&& !SwipeMenu.menuData.get(position).get("controller").equals("ListingType")
					&& !SwipeMenu.menuData.get(position).get("controller").equals("AccountType")) {
				SwipeMenu.menu.showContent();
			}
			else {
				try {
					/* save current view */
					Config.prevView = Config.currentView;
					Config.currentView = className;
					
					/* set current menu position as current */
					previousPosition = currentPosition;
					currentPosition = position;

					notifyDataSetChanged();
					
					/* invoke getInstance method of the requested class */
					Class.forName("com.acentria.benslist.controllers."+className).getMethod("getInstance").invoke(className);
				}
				catch (ClassNotFoundException exception) {
					Context context = Config.context.getApplicationContext();
		    		CharSequence text = "No related class found for: "+data.get(position).get("name");
		    		Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else {
			try {
				Log.e(TAG, "goto Class=> "+className);
				Class<?> activity = Class.forName("com.acentria.benslist."+className);
				Intent intent = new Intent(Config.context, activity);				
				Config.context.startActivity(intent);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}