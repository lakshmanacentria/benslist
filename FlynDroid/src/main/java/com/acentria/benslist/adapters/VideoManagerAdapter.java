package com.acentria.benslist.adapters;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.acentria.benslist.AddListingActivity;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class VideoManagerAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
	private Context instance;
	private int videosCount = 20;
	private boolean videosUnlim = false;
	private TextView dividerLegend;
	private LinearLayout upload;
	private ListView list;
	
	public VideoManagerAdapter(ArrayList<HashMap<String, String>> insertElements, Context context, TextView legend, ListView listView, LinearLayout uploadButton) {
		items = insertElements;
		instance = context;
		dividerLegend = legend;
		upload = uploadButton;
		list = listView;
	}

	@Override
	public void onItemClick(AdapterView<?> gridView, View itemView, final int position, long longItemId) {
		PopupMenu popup = new PopupMenu(instance, itemView);
        popup.getMenuInflater().inflate(R.menu.edit_video, popup.getMenu());
        for (int i = 0; i < popup.getMenu().size(); i++ ) {
        	popup.getMenu().getItem(i).setTitle(Lang.get(popup.getMenu().getItem(i).getTitle().toString()));
        }
//		popup.b
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch (item.getItemId()) {
					case R.id.delete:
						Dialog.toast("dialog_video_removed", instance);
						remove(position);
						break;
				}
				
				return false;
			}}
         );
         popup.show();
	}

	public void update(int count, boolean unlim) {
		videosCount = count;
		videosUnlim = unlim;
		
		recount();
	}
	
	private void recount() {
		boolean allow = true;

		if ( !videosUnlim && videosCount <= items.size() ) {
			allow = false;
		}
		
		allowUpload(allow);
		
		String set = Lang.get("videos_divider");
		if ( !videosUnlim && videosCount > 0 ) {
			int left = videosCount - (items.size());

			set = Lang.get("videos_divider_left")
					.replace("{number}", ""+left)
					.replace("{total}", ""+videosCount);
		}

		dividerLegend.setText(set);
	}
	
	public void add(HashMap<String, String> nextPicture) {
		items.add(nextPicture);
		recount();
	    notifyDataSetChanged();
	}
	
	public void remove(int position) {
		items.remove(position);
		recount();
	    notifyDataSetChanged();
	}
	
	public void cut(int limit) {
		if ( limit <= 0 || items.size() < 1 )
				return;
		
		Iterator<HashMap<String, String>> iterator = items.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			HashMap<String, String> next = iterator.next();
			if ( i >= limit ) {
				iterator.remove();
			}
			i++;
		}
		
		recount();
		notifyDataSetChanged();
	}
	
	public void allowUpload(boolean state) {
		upload.setVisibility(state ? View.VISIBLE : View.GONE);
	}
	
	@Override
	public int getCount() {
		list.setVisibility(items.size() == 0 ? View.GONE : View.VISIBLE);
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		if ( view == null ) {
			view = AddListingActivity.instance.getLayoutInflater()
					.inflate(R.layout.upload_video, null);
		}
						
		HashMap<String, String> video = items.get(position);
		
		/* set image */
		ImageView image = (ImageView) view.findViewById(R.id.image);
		
		ImageAware imageAware = new ImageViewAware(image, false);
		Utils.imageLoaderFeatured.displayImage(video.get("uri"), imageAware, Utils.imageLoaderOptionsFeatured);
		
		/* set title */
		TextView title = (TextView) view.findViewById(R.id.title);
		title.setText(video.get("title"));
		
		return view;
	}
	
	public String getVideoIDs() {
		String ids = "";
		
		for ( HashMap<String, String> video : items ) {
			ids += video.get("preview") + "||";
		}
		
		return ids;
	}
}