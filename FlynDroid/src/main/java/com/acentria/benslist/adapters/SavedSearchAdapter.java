package com.acentria.benslist.adapters;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.Html;
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
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.SearchResultsActivity;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.controllers.SavedSearch;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;

public class SavedSearchAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> savedFields;
		
	public SavedSearchAdapter() {
		savedFields = SavedSearch.fields;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {
		final HashMap<String, String> item = savedFields.get(position);

		Intent search_intent = new Intent(Config.context, SearchResultsActivity.class);
		search_intent.putExtra("id", item.get("id"));
		search_intent.putExtra("type", item.get("listing_type"));
		if(SavedSearch.getSSPreference(2, item.get("id")) != null && !SavedSearch.getSSPreference(2, item.get("id")).isEmpty()) {
			search_intent.putExtra("find_ids", SavedSearch.getSSPreference(1, item.get("id")));
		}
		Config.context.startActivity(search_intent);
		SavedSearch.clearSSItem(item.get("id"));
	}
	
	private class ViewHolder {
		public LinearLayout search_fields;
		public TextView title;
		public TextView search_values;
		public TextView date;
		public TextView status;
		public TextView search_count;
		public ImageView icon_action;
	}

	public void remove(int position) {
		savedFields.remove(position);
		if(savedFields.isEmpty()) {
			SavedSearch.setEmpty();
		}

		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return this.savedFields.size();
	}

	@Override
	public Object getItem(int position) {
		return savedFields.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		
		View view = convertView;
		final ViewHolder holder;

		view = Config.context.getLayoutInflater().inflate(R.layout.saved_search_items, null);

		holder = new ViewHolder();

		holder.search_fields = (LinearLayout) view.findViewById(R.id.search_fields);
		holder.title = (TextView) view.findViewById(R.id.search_title);
		holder.search_values = (TextView) view.findViewById(R.id.search_values);
		holder.date = (TextView) view.findViewById(R.id.search_date);
		holder.status = (TextView) view.findViewById(R.id.search_status);
		holder.search_count = (TextView) view.findViewById(R.id.search_count);
		holder.icon_action = (ImageView) view.findViewById(R.id.icon_action);

		view.setTag(holder);
						
		final HashMap<String, String> item = savedFields.get(position);

		/* set text values */
		holder.title.setText(Html.fromHtml(item.get("title")));
		holder.date.setText(item.get("date"));
		holder.search_values.setText(item.get("fields_value"));
		holder.status.setText(Lang.get(item.get("status").equals("active") ? "status_active" : "status_approval"));

		if(SavedSearch.getSSPreference(2, item.get("id")) != null && !SavedSearch.getSSPreference(2, item.get("id")).isEmpty()) {
			holder.search_count.setText(SavedSearch.getSSPreference(2, item.get("id")));
			holder.search_count.setVisibility(View.VISIBLE);
		}
		else {
			holder.search_count.setVisibility(View.GONE);
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
        popup.getMenuInflater().inflate(R.menu.save_search, popup.getMenu());
        for (int i = 0; i < popup.getMenu().size(); i++ ) {
			if(popup.getMenu().getItem(i).getOrder() == 1) {
				String phrase = savedFields.get(position).get("status").equals("active") ? "android_deactivate" : "android_activate";
				popup.getMenu().getItem(i).setTitle(Lang.get(phrase));
			}
			else {
				popup.getMenu().getItem(i).setTitle(Lang.get(popup.getMenu().getItem(i).getTitle().toString()));
			}
        }


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

			@Override
			public boolean onMenuItemClick(MenuItem item) {

				switch (item.getItemId()) {
					case R.id.search_activate:
						String mode = savedFields.get(position).get("status").equals("active") ? "approval" : "active";
						actionSavedSearch(position, mode);
						break;

					case R.id.search_delete:
						Dialog.confirmAction(Lang.get("android_remove_item"), null, Lang.get("dialog_delete"), Lang.get("dialog_cancel"), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								actionSavedSearch(position, "delete");
							}
						}, null);

						break;
				}
				
				return false;
			}}
        );
        popup.show();
	}

	private void actionSavedSearch(final int position,final String mode) {
		final HashMap<String, String> savedItem = savedFields.get(position);
		String loading_phrase = "";
		if(mode.equals("active")) {
			loading_phrase = Lang.get("dialog_activating");
		}
		else if(mode.equals("approval")) {
			loading_phrase = Lang.get("dialog_deactivating");
		}
		else if(mode.equals("delete")) {
			loading_phrase = Lang.get("dialog_deleting");
		}

		final ProgressDialog progress = ProgressDialog.show(Config.context, null, loading_phrase);

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		params.put("id", savedItem.get("id"));
		params.put("mode", mode);

		final String url = Utils.buildRequestUrl("actionSavedSearch", params, null);

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

						// handle errors
						if ( errorNode.getLength() > 0 ) {
							Element error = (Element) errorNode.item(0);
							Dialog.simpleWarning(Lang.get(error.getTextContent()));
						}
						// finish this activity and show toast
						else {
							NodeList successNode = doc.getElementsByTagName("success");
							if (successNode.getLength() > 0) {
								if(mode.equals("active") || mode.equals("approval")) {
									savedItem.put("status", mode);
									Dialog.simpleWarning(Lang.get(mode.equals("active") ? "android_item_updated" : "android_item_disabled"));
								}
								else if(mode.equals("delete")) {
									remove(position);
									Dialog.simpleWarning(Lang.get("android_item_removed"));
								}

								notifyDataSetChanged();
							}
							else {
								Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), Config.context);
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

}