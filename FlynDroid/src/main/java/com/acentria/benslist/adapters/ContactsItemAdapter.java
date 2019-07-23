package com.acentria.benslist.adapters;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.MessagesActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.controllers.MyMessages;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;


public class ContactsItemAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> contacts = new ArrayList<HashMap<String, String>>();
		
	public ContactsItemAdapter() {
		contacts = MyMessages.contacts;
	}

	@Override
	public void onItemClick(AdapterView<?> listView, View itemView, int position, long longItemId) {

		if (Config.tabletMode) {
			MyMessages.messagesTablet(contacts.get(position).get("from"), contacts.get(position));
		}
		else {
			Intent intent = new Intent(Config.context, MessagesActivity.class);
			intent.putExtra("id", contacts.get(position).get("from"));
			intent.putExtra("data", contacts.get(position));
			intent.putExtra("sendMail", "0");
			intent.putExtra("listing_id", "0");

			Config.context.startActivity(intent);
		}
	}
	
	private class ViewHolder {
		public RelativeLayout contactItem;
		public TextView nameText;
		public TextView dateText;
		public TextView messageText;
		public TextView countText;
		public ImageView image;
		public LinearLayout thumbnail;
		public ImageView icon_action;
	}
	
	/**
	 * add more items to listview
	 * 
	 * @param nextListings - next listings stack
	 */
	public void add(ArrayList<HashMap<String, String>> nextListings) {
		for (int i = 0; i < nextListings.size(); i++ ) {
			contacts.add(nextListings.get(i));
		}
	    notifyDataSetChanged();
	}
	
	public void addFirst(HashMap<String, String> message) {

		HashMap<String, String> contactHash = new HashMap<String, String>();
		if (message.get("push") != null) {
			contactHash.put("from", message.get("from"));
			contactHash.put("to", message.get("to"));
			contactHash.put("count", message.get("count"));
			contactHash.put("new_messages", message.get("new_messages"));
		}
		else {
			contactHash.put("from", MyMessages.contactID);
			contactHash.put("to", Account.accountData.get("id"));
			contactHash.put("count", "0");
		}
		contactHash.put("full_name", message.get("full_name"));
		contactHash.put("visitor_mail", message.get("visitor_mail"));
		contactHash.put("message", message.get("message"));
		contactHash.put("date", message.get("date"));
		contactHash.put("divider", message.get("divider"));
		contactHash.put("admin", message.get("admin"));
		contactHash.put("photo", message.get("photo"));
		contacts.add(0, contactHash);
	}

	public int getPosition(boolean from) {
		int pos = -1;

		for (int position = 0; position < contacts.size(); position++) {
			HashMap<String, String> contact = contacts.get(position);
			if (contact.get("from").equals(from ? MyMessages.lastMessage.get("to") : MyMessages.lastMessage.get("from"))
					&& contact.get("admin").equals(MyMessages.lastMessage.get("admin")) ) {
				pos = position;
			}
		}
		return pos;
	}

	public void updateContactsStatus(boolean from) {
		if(this.getPosition(from)>0) {
		contacts.get(this.getPosition(from)).put("count", "");
		notifyDataSetChanged();
	}
	}

	public void updateContacts(HashMap<String, String> messHash, boolean from, boolean update) {
		int pos = this.getPosition(from);
		if (pos == -1) {
			this.addFirst(messHash);
		}
		else {
			HashMap<String, String> contact = contacts.get(pos);
			contact.put("message", messHash.get("message"));
			contact.put("count", messHash.get("count"));
			contact.put("divider", messHash.get("divider"));
			contacts.remove(pos);
			contacts.add(0, contact);
			if (update) {
				notifyDataSetChanged();
			}
		}
	}
	
	/**
	 * add one entry
	 * 
	 * @param nextAccounts - next accounts stack
	 */
	public void addEntry(HashMap<String, String> entry) {
		contacts.add(entry);
	    notifyDataSetChanged();
	}
	
	public void remove(int position) {
		contacts.remove(position);
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return this.contacts.size();
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

		if (view == null) {
			view = Config.context.getLayoutInflater().inflate(R.layout.contacts, null);
			holder = new ViewHolder();

			holder.contactItem = (RelativeLayout) view.findViewById(R.id.contact_item);
			holder.nameText = (TextView) view.findViewById(R.id.authorname);
			holder.dateText = (TextView) view.findViewById(R.id.date);
			holder.messageText = (TextView) view.findViewById(R.id.message);
			holder.countText = (TextView) view.findViewById(R.id.new_count);
			holder.image = (ImageView) view.findViewById(R.id.thumbnail_image);
			holder.thumbnail = (LinearLayout) view.findViewById(R.id.thumbnail);
			holder.icon_action = (ImageView) view.findViewById(R.id.icon_action);

			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		final HashMap<String, String> account = contacts.get(position);

		/* set text values */
		holder.nameText.setText(account.get("full_name"));
		holder.messageText.setText(account.get("message"));

		if (!account.get("count").isEmpty() && Integer.parseInt(account.get("count")) > 0) {
			holder.countText.setVisibility(View.VISIBLE);
			holder.countText.setText(account.get("count"));
		}
		else {
			holder.countText.setVisibility(View.GONE);
		}

		holder.dateText.setText(account.get("divider"));

		/* thumbnail manager */
		if (account.get("photo")!=null) {
			if (!account.get("photo").isEmpty()) {
				Utils.imageLoaderMixed.displayImage(account.get("photo"), holder.image, Utils.imageLoaderOptionsMixed);
			}
			else {
				holder.image.setImageResource(R.mipmap.seller_no_photo);
			}
		}
		else {
			holder.image.setImageResource(R.mipmap.seller_no_photo);
		}

		/* action icon listener */
		holder.icon_action.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				actionMenu(holder.icon_action, position);
			}
		});

		return view;
	}

	private void actionMenu(ImageView view, final int position) {
		PopupMenu popup = new PopupMenu(Config.context, view);
		popup.getMenuInflater().inflate(R.menu.my_messages, popup.getMenu());
		for (int i = 0; i < popup.getMenu().size(); i++ ) {
			popup.getMenu().getItem(i).setTitle(Lang.get(popup.getMenu().getItem(i).getTitle().toString()));
		}
		popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

			 @Override
			 public boolean onMenuItemClick(MenuItem item) {
				 HashMap<String, String> details;

				 switch (item.getItemId()) {
					 case R.id.delete_message:
						 Dialog.confirmAction(Lang.get("dialog_confirm_messages_removal"), null, Lang.get("dialog_delete"), Lang.get("dialog_cancel"), new DialogInterface.OnClickListener() {
							 public void onClick(DialogInterface dialog, int id) {
								 removeMessages(position);
							 }
						 }, null);

						 break;
				 }

				 return false;
			 }}
		);
		popup.show();
	}

	private void removeMessages(final int position) {
		final ProgressDialog progress = ProgressDialog.show(Config.context, null, Lang.get("dialog_deleting"));

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		params.put("user_id", contacts.get(position).get("from"));

		final String url = Utils.buildRequestUrl("removeMessages", params, null);

		/* do async request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.setTimeout(30000); // 30 seconds limit for this task
		client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					Log.d("FD", "delete | " + response);
					progress.dismiss();

					/* parse xml */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if (doc == null) {
						Dialog.simpleWarning(Lang.get("returned_xml_failed"));
					} else {
						NodeList errorNode = doc.getElementsByTagName("error");

						/* handle errors */
						if (errorNode.getLength() > 0) {
							Element error = (Element) errorNode.item(0);
							Dialog.simpleWarning(Lang.get(error.getTextContent()));
						}
						/* finish this activity and show toast */
						else {
							NodeList successNode = doc.getElementsByTagName("success");
							if (successNode.getLength() > 0) {
								String newMessCount = successNode.item(0).getTextContent();
								String messagesCount = Integer.parseInt(newMessCount) > 0 ? newMessCount : null;
								SwipeMenu.menuData.get(SwipeMenu.adapter.getPositionByController("MyMessages")).put("count", messagesCount);
								SwipeMenu.adapter.notifyDataSetChanged();

								remove(position);
								Dialog.toast("dialog_messages_removed");
								MyMessages.contactID = "";
								if (Config.tabletMode) {
									LinearLayout message_area = (LinearLayout) Config.context.findViewById(R.id.message_area_empty);
									message_area.setVisibility(View.VISIBLE);
									if (MyMessages.MessageAdapter!=null) {
										MyMessages.MessageAdapter.notifyDataSetChanged();
									}
								}
								if (MyMessages.ContactsAdapter!=null) {
									MyMessages.ContactsAdapter.notifyDataSetChanged();
								}
								if (getCount()==0) {
									LinearLayout layout = (LinearLayout) MyMessages.main_content.findViewById(R.id.progress_bar_custom);
									layout.setVisibility(View.VISIBLE);
									MyMessages.setEmpty(layout);
								}
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