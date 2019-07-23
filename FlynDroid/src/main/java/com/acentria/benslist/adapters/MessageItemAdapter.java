package com.acentria.benslist.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.Account;
import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.ListingDetailsActivity;
import com.acentria.benslist.MessagesActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.controllers.MyMessages;

import java.util.ArrayList;
import java.util.HashMap;

public class MessageItemAdapter extends BaseAdapter{

	public ArrayList<HashMap<String, String>> messages = new ArrayList<HashMap<String, String>>();
	private Context context;

	public MessageItemAdapter() {
		messages = MyMessages.contactMessages;
		if (Config.tabletMode) {
			context = Config.context;
		}
		else {
			context = MessagesActivity.context;
		}
	}

	private class ViewHolder {
		public LinearLayout content_left;
		public LinearLayout content_right;
		public LinearLayout divider;
		public TextView divider_text;
		public TextView date;
		public TextView date_right;
		public TextView message;
		public TextView message_right;
		public ImageView image;
		public LinearLayout thumbnail;
		public LinearLayout messeage_arrow;
		public LinearLayout messeage_arrow_right;
		public LinearLayout visitor;
		public LinearLayout visitor_mail;
		public TextView visitor_mail_name;
		public TextView visitor_mail_value;
		public LinearLayout visitor_phone;
		public TextView visitor_phone_name;
		public TextView visitor_phone_value;
		public LinearLayout visitor_link;
		public TextView visitor_link_name;
		public TextView visitor_link_value;
		public LinearLayout visitor_link_right;
		public TextView visitor_link_name_right;
		public TextView visitor_link_value_right;
	}
	
	/**
	 * 
	 * @param messages - next comments stack
	*/
	public void add(ArrayList<HashMap<String, String>> nextMessages, boolean first) {
		for (int i = 0; i < nextMessages.size(); i++ ) {
            if ( first == true ) {
				messages.add(0, nextMessages.get(i));
            }
            else {
				messages.add(nextMessages.get(i));
            }
		}
	    notifyDataSetChanged();
		
	}

	/**
	 * add one entry
	 *
	 * @param messages - next comments stack
	 */
	public void addEntry(HashMap<String, String> entry) {
		messages.add(entry);
	    notifyDataSetChanged();
	}

	public void addSimple(HashMap<String, String> entry) {
		messages.add(entry);
	}

	public void removeEntry(HashMap<String, String> entry) {
		for (HashMap<String, String> comment : messages) {
			if ( comment.get("id").equals(entry.get("id")) ) {
				messages.remove(comment);
				notifyDataSetChanged();
				break;
			}
		}
	}
	
	public void remove(int position) {
		messages.remove(position);
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return messages.size();
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
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		final ViewHolder holder;

		final HashMap<String, String> message = messages.get(position);
		if ( view == null ){

			view = Config.context.getLayoutInflater().inflate(R.layout.messages, null);
			holder = new ViewHolder();

			holder.content_left = (LinearLayout) view.findViewById(R.id.content_left);
			holder.content_right = (LinearLayout) view.findViewById(R.id.content_right);
			holder.divider = (LinearLayout) view.findViewById(R.id.divider);
			holder.divider_text = (TextView) view.findViewById(R.id.divider_text);
			holder.message = (TextView) view.findViewById(R.id.message);
			holder.message_right = (TextView) view.findViewById(R.id.message_right);
			holder.date = (TextView) view.findViewById(R.id.date);
			holder.date_right = (TextView) view.findViewById(R.id.date_right);
			holder.messeage_arrow = (LinearLayout) view.findViewById(R.id.messeage_arrow);
			holder.messeage_arrow_right = (LinearLayout) view.findViewById(R.id.messeage_arrow_right);
			holder.image = (ImageView) view.findViewById(R.id.thumbnail_image);
			holder.thumbnail = (LinearLayout) view.findViewById(R.id.thumbnail);

			holder.visitor = (LinearLayout) view.findViewById(R.id.visitor);
			holder.visitor_mail = (LinearLayout) view.findViewById(R.id.visitor_mail);
			holder.visitor_mail_name = (TextView) view.findViewById(R.id.visitor_mail_txt);
			holder.visitor_mail_value = (TextView) view.findViewById(R.id.visitor_mail_value);
			holder.visitor_phone = (LinearLayout) view.findViewById(R.id.visitor_phone);
			holder.visitor_phone_name = (TextView) view.findViewById(R.id.visitor_phone_txt);
			holder.visitor_phone_value = (TextView) view.findViewById(R.id.visitor_phone_value);
			holder.visitor_link = (LinearLayout) view.findViewById(R.id.visitor_link);
			holder.visitor_link_name = (TextView) view.findViewById(R.id.visitor_link_txt);
			holder.visitor_link_value = (TextView) view.findViewById(R.id.visitor_link_value);

			holder.visitor_link_right = (LinearLayout) view.findViewById(R.id.visitor_link_right);
			holder.visitor_link_name_right = (TextView) view.findViewById(R.id.visitor_link_txt_right);
			holder.visitor_link_value_right = (TextView) view.findViewById(R.id.visitor_link_value_right);

			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}
		if (message.get("to").equals(Account.accountData.get("id"))) {
			holder.content_left.setVisibility(View.VISIBLE);
			holder.content_right.setVisibility(View.GONE);
			holder.message.setText(Html.fromHtml(message.get("message")));
			/* thumbnail manager */
			if (holder.image != null && message.get("photo") != null) {
				if (!message.get("photo").isEmpty()) {
					Utils.imageLoaderMixed.displayImage(message.get("photo"), holder.image, Utils.imageLoaderOptionsMixed);
				} else {
					holder.image.setImageResource(R.mipmap.seller_no_photo);
				}
			}
			else {
				holder.image.setImageResource(R.mipmap.seller_no_photo);
			}

			if (!message.get("time").isEmpty()) {
				holder.date.setText(message.get("time"));
				holder.date.setVisibility(View.VISIBLE);
				holder.thumbnail.setVisibility(View.VISIBLE);
				holder.messeage_arrow.setVisibility(View.VISIBLE);
			}
			else {
				holder.date.setVisibility(View.GONE);
				holder.messeage_arrow.setVisibility(View.INVISIBLE);
				holder.thumbnail.setVisibility(View.INVISIBLE);
			}
		}
		else {
			holder.content_left.setVisibility(View.GONE);
			holder.content_right.setVisibility(View.VISIBLE);
			holder.message_right.setText(Html.fromHtml(message.get("message")));
			if (!message.get("time").isEmpty()) {
				holder.date_right.setText(message.get("time"));
				holder.date_right.setVisibility(View.VISIBLE);
				holder.messeage_arrow_right.setVisibility(View.VISIBLE);
			}
			else {
				holder.date_right.setVisibility(View.GONE);
				holder.messeage_arrow_right.setVisibility(View.INVISIBLE);
			}

			if (message.get("listing_id")!=null && !message.get("listing_id").isEmpty() && !message.get("listing_id").equals("0")) {
				holder.visitor_link_right.setVisibility(View.VISIBLE);
				holder.visitor_link_name_right.setText(Lang.get("android_related_to") + ":");
				if (message.get("listing_title")!=null) {
					holder.visitor_link_value_right.setText(message.get("listing_title"));
				}
				else {
					holder.visitor_link_value_right.setText(Lang.get("android_title_activity_listing_details"));
				}
				holder.visitor_link_right.setVisibility(View.VISIBLE);
				/* action icon listener */
				holder.visitor_link_value_right.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent intent = new Intent(Config.context, ListingDetailsActivity.class);
						intent.putExtra("id", message.get("listing_id"));
						context.startActivity(intent);
					}
				});
			}
			else {
				holder.visitor_link_right.setVisibility(View.GONE);
			}
		}

		int prev_post = position > 0 ? position  - 1 : position;
		HashMap<String, String> messageprev = messages.get(prev_post);
		String prev_dvd = messageprev.get("divider");

		if (!message.get("divider").equals(prev_dvd) || MyMessages.contactMessages.size() >= MyMessages.lastTotalMessages && position == 0) {
			holder.divider_text.setText(message.get("divider"));
			holder.divider.setVisibility(View.VISIBLE);
			if (Config.tabletMode && Config.currentView.equals("MyMessages")) {
				holder.divider_text.setBackgroundColor(Config.context.getResources().getColor(R.color.landscape_side_bg));
			}
		}
		else {
			holder.divider.setVisibility(View.GONE);
		}

		if (!message.get("visitor_mail").isEmpty()) {
			holder.visitor.setVisibility(View.VISIBLE);

			holder.visitor_mail_name.setText(Lang.get("android_hint_email") + ":");
			holder.visitor_mail_value.setText(message.get("visitor_mail"));
			holder.visitor_mail.setVisibility(View.VISIBLE);
			/* action icon listener */
			holder.visitor_mail_value.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent contactIntent = new Intent(Intent.ACTION_SEND);
					contactIntent.setType("plain/text");
					contactIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{message.get("visitor_mail")});
					try {
						context.startActivity(contactIntent);
					} catch (android.content.ActivityNotFoundException ex) {
						Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
					}
				}
			});

			if (!message.get("visitor_phone").isEmpty()) {
				holder.visitor_phone_name.setText(Lang.get("android_phone") + ":");
				holder.visitor_phone_value.setText(message.get("visitor_phone"));
				holder.visitor_phone.setVisibility(View.VISIBLE);
			}
			else {
				holder.visitor_phone.setVisibility(View.GONE);
			}


			if (!message.get("listing_id").isEmpty() && !message.get("listing_id").equals("0")) {
				holder.visitor_link_name.setText(Lang.get("android_related_to") + ":");
				if (message.get("listing_title")!=null) {
					holder.visitor_link_value.setText(message.get("listing_title"));
				}
				else {
					holder.visitor_link_value.setText(Lang.get("android_title_activity_listing_details"));
				}
				holder.visitor_link.setVisibility(View.VISIBLE);
				/* action icon listener */
				holder.visitor_link_value.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent intent = new Intent(Config.context, ListingDetailsActivity.class);
						intent.putExtra("id", message.get("listing_id"));
						context.startActivity(intent);
					}
				});
			}
			else {
				holder.visitor_link.setVisibility(View.GONE);
			}

		}
		else {
			if (message.get("listing_id")!=null && !message.get("listing_id").isEmpty() && !message.get("listing_id").equals("0")) {
				holder.visitor.setVisibility(View.VISIBLE);
				holder.visitor_link_name.setText(Lang.get("android_related_to") + ":");
				if (message.get("listing_title")!=null) {
					holder.visitor_link_value.setText(message.get("listing_title"));
				}
				else {
					holder.visitor_link_value.setText(Lang.get("android_title_activity_listing_details"));
				}
				holder.visitor_link.setVisibility(View.VISIBLE);
				/* action icon listener */
				holder.visitor_link_value.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent intent = new Intent(Config.context, ListingDetailsActivity.class);
						intent.putExtra("id", message.get("listing_id"));
						context.startActivity(intent);
					}
				});
			}
			else {
				holder.visitor.setVisibility(View.GONE);
				holder.visitor_link.setVisibility(View.GONE);
			}
		}


		return view;
	}
}