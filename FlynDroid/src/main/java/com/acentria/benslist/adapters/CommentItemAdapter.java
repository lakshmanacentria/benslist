package com.acentria.benslist.adapters;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class CommentItemAdapter extends BaseAdapter{

	public ArrayList<HashMap<String, String>> comments = new ArrayList<HashMap<String, String>>();
	
	public CommentItemAdapter(ArrayList<HashMap<String, String>> insertElements) {
        comments = insertElements;
	}

	private class ViewHolder {
		public TextView title;
		public TextView author;
		public TextView date;
		public TextView description;
		public RatingBar rating;
	}
	
	/**
	 * 
	 * @param comment - next comments stack
	*/
	public void add(ArrayList<HashMap<String, String>> nextComments, boolean first) {
		for (int i = 0; i < nextComments.size(); i++ ) {
            if ( first == true ) {
                comments.add(0, nextComments.get(i));
            }
            else {
                comments.add(nextComments.get(i));
            }
		}
	    notifyDataSetChanged();
	}
	
	/**
	 * add one entry
	 * 
	 * @param comments - next comments stack
	 */
	public void addEntry(HashMap<String, String> entry) {
        comments.add(entry);
	    notifyDataSetChanged();
	}
	
	public void removeEntry(HashMap<String, String> entry) {
		for (HashMap<String, String> comment : comments) {
			if ( comment.get("id").equals(entry.get("id")) ) {
                comments.remove(comment);
				notifyDataSetChanged();
				break;
			}
		}
	}
	
	public void remove(int position) {
        comments.remove(position);
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return comments.size();
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
		
		if ( view == null ){
			
			view = Config.context.getLayoutInflater().inflate(R.layout.comment_item, null);
			
			holder = new ViewHolder();
			
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.author = (TextView) view.findViewById(R.id.author);
			holder.date = (TextView) view.findViewById(R.id.date);
			holder.description = (TextView) view.findViewById(R.id.description);
			holder.rating = (RatingBar) view.findViewById(R.id.comments_rating);

			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}

		HashMap<String, String> comment = comments.get(position);

        holder.title.setText(comment.get("Title"));
        holder.author.setText(comment.get("Author"));
		holder.description.setText(comment.get("Description"));

        if ( comment.get("Status").equals("pending") ) {
            holder.date.setText( Html.fromHtml("<font color='#EE0000'>"+Lang.get("status_pending")+"</font>"));
        }
        else {
            holder.date.setText(comment.get("Date"));
        }

        if ( comment.containsKey("use_html") ) {
            holder.description.setText(comment.get("Description"));
        }
        else {
            holder.description.setText(Html.fromHtml(comment.get("Description")));
        }
        holder.description.setMovementMethod(LinkMovementMethod.getInstance());

        if ( Utils.getCacheConfig("comments_rating_module").equals("1") ) {
            int star = Integer.parseInt(comment.get("Rating"));
            if ( star > 0 ) {
                holder.rating.setRating(star);
                holder.rating.setVisibility(View.VISIBLE);
            }
            else {
                holder.rating.setVisibility(View.GONE);
            }
        }
		return view;
	}
}