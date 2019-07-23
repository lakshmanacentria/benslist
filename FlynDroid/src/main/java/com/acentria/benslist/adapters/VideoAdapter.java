package com.acentria.benslist.adapters;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.acentria.benslist.Config;
import com.acentria.benslist.ListingDetailsActivity;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.YoutubeActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class VideoAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
	
	public VideoAdapter(ArrayList<HashMap<String, String>> insertElements) {
		items = insertElements;
	}

	@Override
	public void onItemClick(AdapterView<?> gridView, View itemView, int position, long longItemId) {
		Intent intent = new Intent(ListingDetailsActivity.instance, YoutubeActivity.class);
		intent.putExtra("id", items.get(position).get("Video"));
		intent.putExtra("type", items.get(position).get("Type"));
		intent.putExtra("video", items.get(position).get("Video"));
		ListingDetailsActivity.instance.startActivity(intent);
	}
	
	private class ViewHolder {
		public TextView titleText;
		public ImageView preview;
	}
	
	public void add(ArrayList<HashMap<String, String>> newStack) {
		for (int i = 0; i < newStack.size(); i++ ) {
			items.add(newStack.get(i));
		}
	    notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return items.size();
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
		ViewHolder holder;
		
		if ( view == null ) {			
			view = Config.context.getLayoutInflater().inflate(R.layout.video_item, null);
			holder = new ViewHolder();

			//holder.titleText = (TextView) view.findViewById(R.id.featured_title);
			holder.preview = (ImageView) view.findViewById(R.id.preview);
			
			view.setTag(holder);
		}
		else {
			holder = (ViewHolder) view.getTag();
		}
						
		HashMap<String, String> item = items.get(position);
		
		/* set text values */
		//holder.titleText.setText(listing.get("title"));
		
		/* load image to list (AsyncTask) */
		if ( !item.get("Preview").isEmpty() ){
			Utils.imageLoaderMixed.displayImage(item.get("Preview"), holder.preview, Utils.imageLoaderOptionsMixed);
		}
		else {
			holder.preview.setImageResource(R.mipmap.no_image);
		}
		
		return view;
	}
}
