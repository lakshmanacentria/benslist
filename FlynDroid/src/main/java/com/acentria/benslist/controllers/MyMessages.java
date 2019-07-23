package com.acentria.benslist.controllers;

import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.SwipeMenu;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.acentria.benslist.adapters.ContactsItemAdapter;
import com.acentria.benslist.adapters.MessageItemAdapter;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;


public class MyMessages extends AbstractController {

	private static MyMessages instance;

	public static ContactsItemAdapter ContactsAdapter;
	public static ListView list_view;
	public static int[] menuItems = {R.id.menu_settings};

	public static String contactID;
	public static HashMap<String, String> contactInfo;
	public static String sendMail;
	public static String listing_id;
	public static LinearLayout main_content;

	public static MessageItemAdapter MessageAdapter;
	public static Integer requestSteckMessages;
	public static boolean loadingInProgressMessages = true;
	public static boolean switcherMs = false;
	public static Integer lastTotalMessages;
	public static Integer lastAdded = 0;
	public static ListView listViewMessages;
	public static ArrayList<HashMap<String, String>> contacts;
	public static ArrayList<HashMap<String, String>> contactMessages;
	public static HashMap<String, String> lastMessage = new HashMap<String, String>();

	public static MyMessages getInstance() {
		if ( instance == null ) {
			try {
				instance = new MyMessages();
			}
			catch(Exception e) {
				Utils.bugRequest("getInstance()", e.getStackTrace(), e.getMessage());
			}
			Config.activeInstances.add(instance.getClass().getSimpleName());
		}
		else {
			Utils.restroreInstanceView(instance.getClass().getSimpleName(), Lang.get("title_activity_my_messages"));
			if (Config.tabletMode && MessageAdapter != null) {
				restoreTableView();
			}
		}
		
		handleMenuItems(menuItems);
		return instance;
	}
	
	public static void removeInstance(){
		instance = null;
	}
	
	public MyMessages(){
		/* set content title */
		Config.context.setTitle(Lang.get("title_activity_my_messages"));

		/* add content view */
		Utils.addContentView(R.layout.view_my_messages);
		
		/* get related view */
		main_content = (LinearLayout)  Config.context.findViewById(R.id.MyMessages);

		getContacts();
		/* hide menu */
		Utils.showContent();
	}


	public void getContacts(){
		lastMessage = new HashMap<String, String>();
		contacts = new ArrayList<HashMap<String, String>>();
		contactID = "0";
		/* build request url */
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		final String url = Utils.buildRequestUrl("getConversations", params, null);
		/* do async request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					/* parse response */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if (doc == null) {
						main_content.removeViewAt(0);
						TextView message = (TextView) Config.context.getLayoutInflater()
								.inflate(R.layout.info_message, null);

						message.setText(Lang.get("returned_xml_failed"));
						main_content.addView(message);
					} else {
						NodeList listingNode = doc.getElementsByTagName("contacts");
						Element nlE = (Element) listingNode.item(0);
						NodeList contact = nlE.getChildNodes();
						if (contact.getLength() > 0) {
							MyMessages.parseContacts(contact);
						}
						initContacts();
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

	public static void initContacts() {
		LinearLayout progress = (LinearLayout) main_content.findViewById(R.id.progress_bar_custom);
		LinearLayout layoutList = (LinearLayout) main_content.findViewById(R.id.list_view_custom);
		progress.setVisibility(View.GONE);

		/* populate list */
		list_view = (ListView) Config.context.getLayoutInflater()
				.inflate(R.layout.list_view, null);

		if (MyMessages.contacts.size() > 0) {
			layoutList.removeAllViews();
			ContactsAdapter = new ContactsItemAdapter();
			list_view.setAdapter(ContactsAdapter);
			list_view.setOnItemClickListener(ContactsAdapter);
			layoutList.setVisibility(View.VISIBLE);
			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT
			);
			list_view.setLayoutParams(params);
			layoutList.setGravity(Gravity.TOP);
		}
		/* display no contacts */
		else {
			setEmpty(progress);
		}
		layoutList.addView(list_view);
	}
	/*
    * set if empty contacts
    */
	public static void setEmpty(LinearLayout layout) {
		layout.setVisibility(View.VISIBLE);
		layout.removeAllViews();
		TextView message = (TextView) Config.context.getLayoutInflater()
				.inflate(R.layout.info_message, null);
		message.setText(Lang.get("android_message_area"));
		layout.setGravity(Gravity.CENTER);
		layout.addView(message);
	}

	public static void updateContacts(boolean me) {

		if (SwipeMenu.menuListView!=null && lastMessage.get("new_messages")!=null) {
			String messagesCount = Integer.parseInt(lastMessage.get("new_messages")) > 0 ? lastMessage.get("new_messages") : null;
			SwipeMenu.menuData.get(SwipeMenu.adapter.getPositionByController("MyMessages")).put("count", messagesCount);
			SwipeMenu.menuListView.post(new Runnable() {
				@Override
				public void run() {
					if (SwipeMenu.adapter != null) {
						SwipeMenu.adapter.notifyDataSetChanged();
					}
				}
			});
		}

		if (list_view!=null && Config.activeInstances.contains("MyMessages") && lastMessage.containsKey("from") && ContactsAdapter != null) {

			ContactsAdapter.updateContacts(lastMessage, me, false);
			list_view.post(new Runnable() {
				@Override
				public void run() {
					if (ContactsAdapter != null) {
						initContacts();
						ContactsAdapter.notifyDataSetChanged();
					}
				}
			});
		}
		else if (Config.activeInstances.contains("MyMessages")) {
			list_view.post(new Runnable() {
				@Override
				public void run() {
					MyMessages.contacts.add(lastMessage);
					initContacts();
				}
			});

		}
	}
	/**
	 * switch controller
	 */
	public static void switchToMyMessages() {

		if (!Config.currentView.equals("MyMessages")) {
			Config.prevView = Config.currentView;
			Config.currentView = "MyMessages";

			SwipeMenu.adapter.previousPosition = SwipeMenu.adapter.currentPosition;
			SwipeMenu.adapter.currentPosition = SwipeMenu.adapter.getPositionByController("MyMessages");

			SwipeMenu.adapter.notifyDataSetChanged();

			MyMessages.getInstance();
			Config.pushView = "";
		}
	}

	public static void parseContacts(NodeList contactNodes){

		HashMap<String, String> contactHash;

		for( int i=0; i<contactNodes.getLength(); i++ )
		{
			contactHash = new HashMap<String, String>();//clear steck
			Element contact = (Element) contactNodes.item(i);

			/* convert data from nodes to array */
			contactHash.put("full_name", Utils.getNodeByName(contact, "full_name"));
			contactHash.put("from", Utils.getNodeByName(contact, "from"));
			contactHash.put("to", Utils.getNodeByName(contact, "to"));
			contactHash.put("visitor_mail", Utils.getNodeByName(contact, "visitor_mail"));
			contactHash.put("message", Config.convertChars(Utils.getNodeByName(contact, "message")));
			contactHash.put("date", Utils.getNodeByName(contact, "date"));
			contactHash.put("divider", Utils.getNodeByName(contact, "divider"));
			contactHash.put("count", Utils.getNodeByName(contact, "count"));
			contactHash.put("photo", Utils.getNodeByName(contact, "photo"));
			contactHash.put("admin", Utils.getNodeByName(contact, "admin"));

			contacts.add(contactHash);
		}
	}


	public static void restoreTableView() {
		LinearLayout content = (LinearLayout) main_content.findViewById(R.id.content_right);
		LinearLayout message_area_empty = (LinearLayout) content.findViewById(R.id.message_area_empty);
		RelativeLayout content_message = (RelativeLayout) Config.context.findViewById(R.id.activity_messages);
		message_area_empty.setVisibility(View.VISIBLE);
		content_message.setVisibility(View.GONE);
		contactID = "0";
	}
	// code for messages activity
	public static void messagesTablet(String id, HashMap<String, String> data) {
		if (!contactID.equals(id)) {
			contactID = id;
			contactInfo = data;
			sendMail = "0";
			listing_id = "0";
			LinearLayout content = (LinearLayout) main_content.findViewById(R.id.content_right);
			LinearLayout message_area_empty = (LinearLayout) content.findViewById(R.id.message_area_empty);
			RelativeLayout content_message = (RelativeLayout) main_content.findViewById(R.id.activity_messages);
			LinearLayout progress = (LinearLayout) content.findViewById(R.id.progress_bar_custom);
			message_area_empty.setVisibility(View.GONE);
			content_message.setVisibility(View.VISIBLE);
			progress.setVisibility(View.VISIBLE);
			getMessages(content_message);
		}
	}

	public static void getMessages(final RelativeLayout content) {
		contactMessages = new ArrayList<HashMap<String, String>>();
		lastTotalMessages = 0;
		requestSteckMessages = 1;
		lastAdded = 0;
		lastMessage.put("from", contactID);
		lastMessage.put("admin", contactInfo.get("admin"));

		final LinearLayout messagesArea = (LinearLayout) content.findViewById(R.id.message_area);
		/* build request url */
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("user_id", contactID);
		params.put("start", "0");
		params.put("admin", contactInfo.get("admin"));
		final String url = Utils.buildRequestUrl("fetchMessages", params, null);

		/* do async request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					/* parse response */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);
					if (doc == null) {
						messagesArea.removeViewAt(0);
						TextView message = (TextView) Config.context.getLayoutInflater()
								.inflate(R.layout.info_message, null);

						message.setText(Lang.get("returned_xml_failed"));
						messagesArea.addView(message);
					} else {
						LinearLayout progress = (LinearLayout) messagesArea.findViewById(R.id.progress_bar_custom);
						progress.setVisibility(View.GONE);
						NodeList messageNode = doc.getElementsByTagName("messages");

						Element nlE = (Element) messageNode.item(0);
						NodeList messages = nlE.getChildNodes();
						/* populate list */

						LinearLayout layoutList = (LinearLayout) messagesArea.findViewById(R.id.list_view_custom);
						layoutList.removeAllViews();
						parseMessages(messages);
						MessageAdapter = new MessageItemAdapter();

						layoutList.removeAllViews();
						layoutList.setVisibility(View.VISIBLE);

						listViewMessages = (ListView) Config.context.getLayoutInflater()
								.inflate(R.layout.list_view, null);

						listViewMessages.setVisibility(View.VISIBLE);
						listViewMessages.setDivider(null);
						listViewMessages.setFocusable(false);
						listViewMessages.setSelector(new StateListDrawable());

						AbsListView.OnScrollListener onScrollListener = null;
						int message_number = 15;
						int rest_messages = lastTotalMessages - message_number;

						if (rest_messages > 0) {
							int preloadView = R.layout.list_view_footer_button;
							String buttonPhraseKey = "android_load_previous_number_messages";

							if (Utils.getSPConfig("preload_method", null).equals("scroll")) {
								preloadView = R.layout.list_view_footer_loading;
								buttonPhraseKey = "android_loading_previous_number_messages";
							}

							final View headerView = (View) Config.context.getLayoutInflater()
									.inflate(preloadView, null);

							final Button preloadButton = (Button) headerView.findViewById(R.id.preload_button);
							int set_rest_messages = rest_messages >= message_number ? message_number : rest_messages;
							String buttonPhrase = Lang.get(buttonPhraseKey).replace("{number}", "" + set_rest_messages);
							preloadButton.setText(buttonPhrase);

							/* on scroll listener */
							onScrollListener = new AbsListView.OnScrollListener() {
								@Override
								public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
									if (!loadingInProgressMessages && firstVisibleItem == 0 && totalItemCount < lastTotalMessages) {

										loadingInProgressMessages = true;
										requestSteckMessages++;
										loadNextMessages(preloadButton, headerView);
									}
								}

								@Override
								public void onScrollStateChanged(AbsListView view, int scrollState) {
								}
							};

							listViewMessages.addHeaderView(headerView);
						}
						listViewMessages.setAdapter(MessageAdapter);
						/* set listeners */
						listViewMessages.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true, onScrollListener));
						listViewMessages.setSelection(MessageAdapter.getCount());
						listViewMessages.post(new Runnable() {
							@Override
							public void run() {
								if (MessageAdapter != null) {
									MessageAdapter.notifyDataSetChanged();
								}
							}
						});
						layoutList.setGravity(Gravity.TOP);
						layoutList.addView(listViewMessages);
						loadingInProgressMessages = false;

					}

				} catch (UnsupportedEncodingException e1) {

				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
				// called when response HTTP status is "4XX" (eg. 401, 403, 404)
			}
		});
		LinearLayout send_content = (LinearLayout) content.findViewById(R.id.send_content);

		if (contactInfo.containsKey("visitor_mail")) {
			if (!contactInfo.get("visitor_mail").isEmpty()) {
				send_content.setVisibility(View.GONE);
			} else {
				send_content.setVisibility(View.VISIBLE);
			}
		}
		//send message
		sendMessage(content);
	}

	public static void loadNextMessages(final Button preloadButton, final View headerView) {

		/* build request url */
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("user_id", contactID);
		params.put("start", "" + requestSteckMessages);
		final String url = Utils.buildRequestUrl("fetchMessages", params, null);

		/* do async request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					/* parse response */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if (doc == null) {
						Dialog.simpleWarning(Lang.get("returned_xml_failed"));
					} else {
						NodeList messageNode = doc.getElementsByTagName("messages");

						Element nlE = (Element) messageNode.item(0);
						NodeList messages = nlE.getChildNodes();

						parseMessages(messages);
						int index = listViewMessages.getFirstVisiblePosition() + lastAdded;
						View v = listViewMessages.getChildAt(listViewMessages.getHeaderViewsCount());

						/* update button text */
						int message_number = 15;
						int rest_messages = lastTotalMessages - (message_number * requestSteckMessages);
						if (rest_messages > 0) {
							String buttonPhraseKey = "android_load_previous_number_messages";
							if (Utils.getSPConfig("preload_method", null).equals("scroll")) {
								buttonPhraseKey = "android_loading_previous_number_messages";
							}

							int set_rest_messages = rest_messages >= message_number ? message_number : rest_messages;
							String buttonPhrase = Lang.get(buttonPhraseKey)
									.replace("{number}", "" + set_rest_messages);
							preloadButton.setText(buttonPhrase);
						} else {
							listViewMessages.removeHeaderView(headerView);
						}

						/* populate list */
						if (messages.getLength() > 1) {
							int top = (v == null) ? 0 : v.getTop();

	//						MessageAdapter.add(messagesNew, true);
							MessageAdapter.notifyDataSetChanged();
							if (lastTotalMessages == MessageAdapter.getCount()) {
								listViewMessages.setSelectionFromTop(index, top);
							} else {
								listViewMessages.setSelectionFromTop(index + 1, top);
							}
							loadingInProgressMessages = false;
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

	/*
	* when user click on send message on messages activity
	*/
	public static void sendMessage(final RelativeLayout layout) {
		Button send = (Button) layout.findViewById(R.id.send);

		final EditText new_message = (EditText) layout.findViewById(R.id.add_message);
		new_message.setHint(Lang.get("comments_message"));

		send.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (!new_message.getText().toString().isEmpty()) {

					SimpleDateFormat fdiv = new SimpleDateFormat("MMM dd, yyyy");
					SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
					String time = sdf.format(new Date());
					String divider = fdiv.format(new Date());
					lastMessage = new HashMap<String, String>();
					lastMessage.put("from", Account.accountData.get("id"));
					lastMessage.put("to", contactID);
					lastMessage.put("date", time);
					lastMessage.put("time", time);
					lastMessage.put("divider", divider);
					lastMessage.put("visitor_mail", "");
					lastMessage.put("count", "0");
					lastMessage.put("full_name", contactInfo.get("full_name"));
					lastMessage.put("photo", contactInfo.get("photo"));
					lastMessage.put("admin", contactInfo.get("admin"));
					lastMessage.put("message", new_message.getText().toString());
					new_message.setText("");

					lastTotalMessages++;
					MyMessages.contactMessages.add(lastMessage);
					MyMessages.MessageAdapter.notifyDataSetChanged();
					listViewMessages.smoothScrollToPosition(MessageAdapter.getCount());
					MyMessages.updateContacts(true);

					/* build request url */
					HashMap<String, String> params = new HashMap<String, String>();
					params.put("from", Account.accountData.get("id"));
					params.put("to", contactID);
					params.put("listing_id", listing_id);
					params.put("notification", sendMail);
					final String url = Utils.buildRequestUrl("sendMessage", params, null);

					/* do async request */
					AsyncHttpClient client = new AsyncHttpClient();
					client.setTimeout(50000);
					client.post(url, Utils.toParams(lastMessage), new AsyncHttpResponseHandler() {
						@Override
						public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
							// called when response HTTP status is "200 OK"
							try {
								String response = String.valueOf(new String(server_response, "UTF-8"));
								/* parse response */
								XMLParser parser = new XMLParser();
								Document doc = parser.getDomElement(response, url);

								if (doc == null) {
									Dialog.simpleWarning(Lang.get("returned_xml_failed"), Config.context);
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
		});
	}

	/*
	*  send new message to contact
	*/
	public static void sendNewMessage(final boolean updateNew) {
		lastTotalMessages++;
		MyMessages.contactMessages.add(lastMessage);
		listViewMessages.post(new Runnable() {
			@Override
			public void run() {
				if (MessageAdapter != null) {
					MessageAdapter.notifyDataSetChanged();
					listViewMessages.smoothScrollToPosition(MessageAdapter.getCount());
					if (updateNew) {
						setStatusReeded();
					} else {
						updateContacts(false);
					}
				}
			}
		});
	}

	/*
	* set status reeded
	*/
	public static void setStatusReeded(){
		// update account messages
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("user_id", contactID);
		final String url = Utils.buildRequestUrl("getCountMessages", params, null);
		/* do async request */
		AsyncHttpClient client = new AsyncHttpClient();
		client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					/* parse response */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if (doc != null) {
						NodeList contNod = doc.getElementsByTagName("new_messages");
						String newMessCount = contNod.item(0).getTextContent();
						String messagesCount = Integer.parseInt(newMessCount) > 0 ? newMessCount : "0";

						lastMessage.put("new_messages", messagesCount);
						lastMessage.put("count", "0");
						MyMessages.updateContacts(false);
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

	public static void parseMessages(NodeList messageNodes){
		lastAdded = 0;
		HashMap<String, String> messHash;

		for( int i=0; i<messageNodes.getLength(); i++ )
		{
			messHash = new HashMap<String, String>();
			//clear steck
			Element message = (Element) messageNodes.item(i);

			if (message.getTagName().equals("total")) {
				lastTotalMessages = message.getTextContent().isEmpty() ? 0 : Integer.parseInt(message.getTextContent());
			}
			else if (message.getTagName().equals("new_messages")) {
				String messagesCount = Integer.parseInt(message.getTextContent()) > 0 ? message.getTextContent() : null;
				SwipeMenu.menuData.get(SwipeMenu.adapter.getPositionByController("MyMessages")).put("count", messagesCount);
				SwipeMenu.adapter.notifyDataSetChanged();

				if (lastMessage.containsKey("from") && MyMessages.ContactsAdapter!=null) {
					MyMessages.ContactsAdapter.updateContactsStatus(false);
				}
			}
			else {
				lastAdded++;
				/* convert data from nodes to array */
				messHash.put("from", Utils.getNodeByName(message, "from"));
				messHash.put("to", Utils.getNodeByName(message, "to"));
				messHash.put("message", Config.convertChars(Utils.getNodeByName(message, "message")));
				messHash.put("photo", Utils.getNodeByName(message, "photo"));
				messHash.put("full_name", Utils.getNodeByName(message, "full_name"));
				messHash.put("visitor_mail", Utils.getNodeByName(message, "visitor_mail"));
				messHash.put("listing_id", Utils.getNodeByName(message, "listing_id"));
				messHash.put("listing_title", Utils.getNodeByName(message, "listing_title"));
				messHash.put("visitor_phone", Utils.getNodeByName(message, "visitor_phone"));
				messHash.put("date", Utils.getNodeByName(message, "date"));
				messHash.put("divider", convertTimeZone(Utils.getNodeByName(message, "date"), "MMM dd, yyyy"));
				messHash.put("time", convertTimeZone(Utils.getNodeByName(message, "date"), "h:mm a"));

				contactMessages.add(0, messHash);
			}
		}
	}
	public static HashMap<String, String> parseNewMessage(Bundle data) {

		HashMap<String, String> messHash = new HashMap<String, String>();
		messHash.put("push", "true");
		messHash.put("from", data.getString("from_id"));
		messHash.put("to", data.getString("to_id"));
		messHash.put("message", Config.convertChars(data.getString("message")));
		messHash.put("photo", data.getString("photo"));
		messHash.put("count", data.getString("count"));
		messHash.put("full_name", data.getString("full_name"));
		messHash.put("visitor_mail", data.getString("visitor_mail"));
		messHash.put("listing_id", data.getString("listing_id"));
		messHash.put("listing_title", data.getString("listing_title"));
		messHash.put("visitor_phone", data.getString("visitor_phone"));
		messHash.put("new_messages", data.getString("new_messages"));
		messHash.put("date", data.getString("date"));
		messHash.put("admin", data.getString("admin"));
		messHash.put("divider", convertTimeZone(data.getString("date"), "MMM dd, yyyy"));
		messHash.put("time", convertTimeZone(data.getString("date"), "h:mm a"));

		lastMessage = messHash;
		return messHash;
	}

	public static String convertTimeZone(String timeStamp, String format) {
		long time = Integer.parseInt(timeStamp) * (long) 1000;
		Date date = new Date(time);
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		String current_time = dateFormat.format(date);
//		Log.d("FD", current_time.toString());
		return current_time;
	}
}