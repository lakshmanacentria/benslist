package com.acentria.benslist.adapters;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.AddListingActivity;
import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.ListingDetailsActivity;
import com.acentria.benslist.PlansActivity;
import com.acentria.benslist.PurchaseActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class MyListingAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> listings = new ArrayList<HashMap<String, String>>();
	private Fragment parentFragment;
		
	public MyListingAdapter(ArrayList<HashMap<String, String>> insertElements, Fragment fragment) {
		this.listings = insertElements;
		parentFragment = fragment;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {
//		if ( listings.get(position).get("page_allowed").equals("1") ) {
			ViewHolder holder = (ViewHolder) itemView.getTag();
			
			Intent intent = new Intent(Config.context, ListingDetailsActivity.class);
			intent.putExtra("id", listings.get(position).get("id"));
			intent.putExtra("listingHash", listings.get(position));
			
			Config.context.startActivity(intent);
//		}
	}
	
	private class ViewHolder {
		public RelativeLayout listingItem;
		public TextView titleText;
		public TextView priceText;
		public TextView photoCount;
		public ImageView image;
		public ImageView icon_action;
		public LinearLayout thumbnail;
		public TextView status;
	}
	
	/**
	 * add more items to listview
	 * 
	 * @param nextListings - next listings stack
	 */
	public void add(ArrayList<HashMap<String, String>> nextListings) {
		for (int i = 0; i < nextListings.size(); i++ ) {
			listings.add(nextListings.get(i));
		}
	    notifyDataSetChanged();
	}
	
	public void addFirst(HashMap<String, String> listing) {
		listings.add(0, listing);
		notifyDataSetChanged();
	}
	
	public void updateOne(String listing_id, HashMap<String, String> last_listing_hash) {
		for (int i = 0; i < listings.size(); i++) {
			if ( listings.get(i).get("id").equals(listing_id) ) {
				listings.set(i, last_listing_hash);
				notifyDataSetChanged();
				break;
			}
		}
	}
	
	/**
	 * add one entry
	 * 
	 * @param nextListings - next listings stack
	 */
	public void addEntry(HashMap<String, String> entry) {
		listings.add(entry);
	    notifyDataSetChanged();
	}
	
	public void remove(int position) {
		listings.remove(position);
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return this.listings.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		
		View view = convertView;
		final ViewHolder holder;
		
		if ( view == null ) {
			view = Config.context.getLayoutInflater().inflate(R.layout.my_listing, null);
			
			holder = new ViewHolder();
			
			holder.listingItem = (RelativeLayout) view.findViewById(R.id.listing_item);
			holder.titleText = (TextView) view.findViewById(R.id.title);
			holder.priceText = (TextView) view.findViewById(R.id.price);
			holder.photoCount = (TextView) view.findViewById(R.id.pic_count);
			holder.image = (ImageView) view.findViewById(R.id.thumbnail_image);
			holder.thumbnail = (LinearLayout) view.findViewById(R.id.thumbnail);
			holder.status = (TextView) view.findViewById(R.id.status);
			holder.icon_action = (ImageView) view.findViewById(R.id.icon_action);
			
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}
						
		final HashMap<String, String> listing = listings.get(position);
		
		/* set text values */
		holder.titleText.setText(listing.get("title"));
		holder.priceText.setText(listing.get("price"));

		/* featured status handler */
		if ( listing.containsKey("featured_expire") ) {
			if ( listing.get("featured_expire").isEmpty() ) {
				holder.listingItem.setBackgroundResource(R.drawable.listing_border);
			}
			else {
				holder.listingItem.setBackgroundResource(R.drawable.listing_border_featured);
			}
		}
		else {
			Log.d("FD", "ERROR LISTING CONSISTENCY: "+listing.toString());
			holder.listingItem.setBackgroundResource(R.drawable.listing_border);
		}
		
		/* status and expiration date */
		holder.status.setText(listing.get("status"));
		String status = listing.get("status");
		
		if ( listing.get("status").equals("active") ) {
			String set_status = listing.get("plan_expire").equals(listing.get("pay_date")) ? Lang.get("unlimited") : listing.get("plan_expire");
			status = Lang.get("active_till").replace("{date}", set_status);
		}
		else {
			status = Lang.get("status_is").replace("{status}", Lang.get("status_"+listing.get("status")));
		}
		
		int style = Config.context.getResources().getIdentifier("status_"+listing.get("status"), "style", Config.context.getPackageName());
		style = style > 0 ? style : R.style.status_approval;
		holder.status.setTextAppearance(Config.context, style);
		
		holder.status.setText(status);

		/* thumbnail manager */
		if ( listing.get("photo_allowed").equals("1") ) {
			holder.thumbnail.setVisibility(View.VISIBLE);
			
			/* load image to list (AsyncTask) */
			if ( !listing.get("photos_count").isEmpty() ){
				Utils.imageLoaderMixed.displayImage(listing.get("photo"), holder.image, Utils.imageLoaderOptionsMixed);
				//holder.image.setImageResource(R.drawable.no_image);
			}
			else {
				holder.image.setImageResource(R.mipmap.no_image);
			}
			
			/* handle listing counter */
			if ( listing.get("photos_count").equals("0") ) {
				holder.photoCount.setVisibility(View.GONE);
			}
			else {
				holder.photoCount.setText(listing.get("photos_count"));
				holder.photoCount.setVisibility(View.VISIBLE);
			}
		}
		else {
			holder.thumbnail.setVisibility(View.GONE);
			holder.photoCount.setVisibility(View.GONE);
		}
		
		/* action icon listener */
		holder.icon_action.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				actionMenu(holder.icon_action, position);
			}
		});
		
		return view;
	}
	
	private void actionMenu(ImageView view, final int position) {
		PopupMenu popup = new PopupMenu(Config.context, view);
        popup.getMenuInflater().inflate(R.menu.edit_listing, popup.getMenu());
        for (int i = 0; i < popup.getMenu().size(); i++ ) {
        	popup.getMenu().getItem(i).setTitle(Lang.get(popup.getMenu().getItem(i).getTitle().toString()));
        }

        if ( !listings.get(position).get("featured_expire").isEmpty() || listings.get(position).get("status").equals("incomplete") ) {
        	popup.getMenu().findItem(R.id.upgrade_to_featured).setVisible(false);
    	}
        
        if ( listings.get(position).get("status").equals("incomplete") ) {
			if( listings.get(position).get("last_step").equals("checkout") ) {
				popup.getMenu().findItem(R.id.upgrade_listing).setTitle(Lang.get("menu_make_payment"));
			}
			else {
				popup.getMenu().findItem(R.id.upgrade_listing).setVisible(false);
			}
    	}
        
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				HashMap<String, String> details;
				
				switch (item.getItemId()) {
					case R.id.delete_listing:
						Dialog.confirmAction(Lang.get("dialog_confirm_listing_removal"), null, Lang.get("dialog_delete"), Lang.get("dialog_cancel"), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								removeListing(position);
							}
						}, null);
						
						break;
						
					case R.id.edit_listing:
						Intent edit_intent = new Intent(Config.context, AddListingActivity.class);
						edit_intent.putExtra("id", listings.get(position).get("id"));
						edit_intent.putExtra("listingHash", listings.get(position));				
						Config.context.startActivity(edit_intent);
						
						break;
						
					case R.id.upgrade_listing:
						details = new HashMap<String, String>();
						details.put("service", "listings");
						details.put("item", listings.get(position).get("plan_type"));
						details.put("title", Utils.buildTitleForOrder(listings.get(position).get("plan_type"), listings.get(position).get("id")));
						details.put("id", listings.get(position).get("id"));
						details.put("amount", listings.get(position).get("plan_price"));
						details.put("plan", listings.get(position).get("plan_id"));
						details.put("plan_key", listings.get(position).get("plan_real_key"));
						
						// make payment for incomplete listing
						if ( listings.get(position).get("status").equals("incomplete") && !listings.get(position).get("plan_id").isEmpty() ) {
							details.put("success_phrase", Lang.get(Utils.getCacheConfig("listing_auto_approval").equals("1") ? "listing_paid_auto_approved" : "listing_paid_pending"));
							
							Intent intent = new Intent(Config.context, PurchaseActivity.class);
							intent.putExtra("hash", details);
							
							parentFragment.startActivityForResult(intent, Config.RESULT_PAYMENT); // starts for results, see response in MyListings contoller (in fragment)
						}
						// upgrade/renew listing
						else {
							Intent intent = new Intent(Config.context, PlansActivity.class);
							intent.putExtra("category_id", listings.get(position).get("category_id"));
							
							details.put("success_phrase", Lang.get("listing_plan_upgraded"));
							details.put("featured", listings.get(position).get("featured_expire").isEmpty() ? "0" : "1");
							
							intent.putExtra("hash", details);
							parentFragment.startActivity(intent);
						}
						
						break;
						
					case R.id.upgrade_to_featured:
						details = new HashMap<String, String>();
						details.put("item", listings.get(position).get("plan_type")); 
						details.put("title", Utils.buildTitleForOrder(listings.get(position).get("plan_type"), listings.get(position).get("id")));
						details.put("id", listings.get(position).get("id"));
						details.put("amount", listings.get(position).get("plan_price"));
						details.put("plan", listings.get(position).get("plan_id"));
						
						details.put("success_phrase", Lang.get("listing_upgraded_to_featured"));
						
						Intent intent = new Intent(Config.context, PlansActivity.class);
						intent.putExtra("category_id", listings.get(position).get("category_id"));
						intent.putExtra("featured_only", "1");
						
						intent.putExtra("hash", details);
						parentFragment.startActivity(intent);
						
						break;
						
					case R.id.view_statistics:
						showStatistics(position);
						
						break;
				}
				
				return false;
			}}
        );
        popup.show();
	}
	
	private void removeListing(final int position) {
		final ProgressDialog progress = ProgressDialog.show(Config.context, null, Lang.get("dialog_deleting"));
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		params.put("listing_id", listings.get(position).get("id"));
		
		final String url = Utils.buildRequestUrl("removeListing", params, null);

		/* do async request */
    	AsyncHttpClient client = new AsyncHttpClient();
    	client.setTimeout(30000); // 30 seconds limit for this task
    	client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					Log.d("FD", "delete | "+response);
					progress.dismiss();

					/* parse xml */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if ( doc == null ) {
						Dialog.simpleWarning(Lang.get("returned_xml_failed"));
					}
					else {
						NodeList errorNode = doc.getElementsByTagName("error");

						/* handle errors */
						if ( errorNode.getLength() > 0 ) {
							Element error = (Element) errorNode.item(0);
							Dialog.simpleWarning(Lang.get(error.getTextContent()));
						}
						/* finish this activity and show toast */
						else {
							NodeList successNode = doc.getElementsByTagName("success");
							if ( successNode.getLength() > 0 ) {
								remove(position);
								Dialog.toast("dialog_listing_removed");

								// update account statistics section
								Account.updateStat();
							}
							else {
								Dialog.simpleWarning(Lang.get("remove_listing_fail"));
							}
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
	
	private void showStatistics(final int position) {
		HashMap<String, String> listing = listings.get(position);
		
		ScrollView view = (ScrollView) Config.context.getLayoutInflater()
				.inflate(R.layout.dialog_listing_statistics, null);
		
		// set title
		((TextView) view.findViewById(R.id.listing_title)).setText(listing.get("title"));
		
		// set statistics data
		LinearLayout container = (LinearLayout) view.findViewById(R.id.data);
		String[][] fields = new String[][] {
			new String[] {"stat_category", "category_name"},
			new String[] {"stat_plan", "plan_name"},
			new String[] {"stat_views", "shows"},
			new String[] {"stat_status", "status"},
			new String[] {"stat_label", "sub_status"},
			new String[] {"stat_added", "date"},
			new String[] {"stat_active_till", "plan_expire"},
			new String[] {"stat_featured_till", "featured_expire"},
		};
		
		for ( int i = 0; i < fields.length; i++ ) {
			if ( !listing.containsKey(fields[i][1]) )
				continue;
			
			if ( listing.get(fields[i][1]).isEmpty() )
				continue;

            // don't show the "active till" if the status is not equals active
            if ( fields[i][1].equals("plan_expire") && !listing.get("status").equals("active") )
                continue;
			
			/* create row view */
			LinearLayout field_row = (LinearLayout) Config.context.getLayoutInflater()
	    			.inflate(R.layout.listing_details_field, null);
			
			/* set field name */
			TextView field_name = (TextView) field_row.findViewById(R.id.field_name);
			field_name.setText(Lang.get(fields[i][0])+":");
			
			/* set field value */
			String val = listing.get(fields[i][1]);
			
			if ( fields[i][1].equals("status") ) {
				val = Lang.get("status_"+val);
			}
			else if ( fields[i][1].equals("plan_expire") ) {
				val = val.equals(listing.get("pay_date")) ? Lang.get("unlimited") : val;
			}
			else if ( fields[i][1].equals("featured_expire") ) {
				val = val.equals(listing.get("featured_date")) ? Lang.get("unlimited") : val;
			}
			
			TextView field_value = (TextView) field_row.findViewById(R.id.field_value);
			field_value.setText(val);
			
			container.addView(field_row);
		}
		
		Dialog.infoView(Lang.get("dialog_statistics"), Config.context, view);
	}
}