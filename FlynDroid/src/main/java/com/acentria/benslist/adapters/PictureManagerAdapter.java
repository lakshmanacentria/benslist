package com.acentria.benslist.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.acentria.benslist.AddListingActivity;
import com.acentria.benslist.Config;
import com.acentria.benslist.Dialog;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class PictureManagerAdapter extends BaseAdapter implements OnItemClickListener {

	private ArrayList<HashMap<String, String>> photos = new ArrayList<HashMap<String, String>>();
	private Context instance;
	private GridView picturesGrid;
	private boolean allowUpload = true;
	private int photosCount = 20;
	private boolean photosUnlim = false;
	private TextView dividerLegend;
	private ArrayList<String> removed_ids = new ArrayList<String>(); 
	
	public PictureManagerAdapter(GridView grid, ArrayList<HashMap<String, String>> insertElements, Context context, TextView legend) {
		picturesGrid = grid;
		photos = insertElements;
		instance = context;
		dividerLegend = legend;
	}

	@Override
	public void onItemClick(AdapterView<?> gridView, View itemView, final int position, long longItemId) {
		HashMap<String, String> photo = photos.get(position);
		
		if ( photo.containsKey("upload") ) {
			if ( AddListingActivity.addListing.selected_plan_id > 0 ) {
				AddListingActivity.requestRead();
			}
			else {
				Dialog.simpleWarning(Lang.get("dialog_no_plan_selected"), AddListingActivity.instance);
			}
		}
		else {
			 PopupMenu popup = new PopupMenu(instance, itemView);
             popup.getMenuInflater().inflate(R.menu.edit_picture, popup.getMenu());
             for (int i = 0; i < popup.getMenu().size(); i++ ) {
            	 popup.getMenu().getItem(i).setTitle(Lang.get(popup.getMenu().getItem(i).getTitle().toString()));
             }
             
             popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){

				@Override
				public boolean onMenuItemClick(MenuItem item) {
					String phrase = "";
					
					switch (item.getItemId()) {
						case R.id.delete:
							phrase = "dialog_photo_removed";
							remove(position);
							break;
							
						case R.id.edit_description:
							editDescription(position);
							break;
							
					}
					
					if ( !phrase.isEmpty() ) {
						Dialog.toast(phrase, instance);
					}
					
					return false;
				}}
             );
             popup.show();
		}
	}

	public void update(int count, boolean unlim) {
		photosCount = count;
		photosUnlim = unlim;
		
		recount();
	}
	
	private void recount() {
		boolean allow = true;

		if ( !photosUnlim && photosCount <= photos.size() - 1 ) {
			allow = false;
		}
		
		allowUpload(allow);
		
		String set = Lang.get("pictures_divider");
		if ( !photosUnlim && photosCount > 0 ) {
			int left = photosCount - (photos.size() - 1);

			set = Lang.get("pictures_divider_left")
					.replace("{number}", ""+left)
					.replace("{total}", ""+photosCount);
		}

		reRangePhotoBox();
		dividerLegend.setText(set);
	}

	public void reRangePhotoBox() {
		View lastChild = picturesGrid.getChildAt(0);
		if(lastChild!=null) {
			int itemHeight = lastChild.getMeasuredHeight();
			int rows = (int) Math.ceil((double) picturesGrid.getCount() / (double) picturesGrid.getNumColumns());
			int spaceSize = Config.context.getResources().getDimensionPixelSize(R.dimen.pictures_grid_column_space);
			int height = rows * itemHeight + (spaceSize * rows);
			picturesGrid.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, height));
		}
	}
	
	public void add(HashMap<String, String> nextPicture) {
		photos.add(photos.size() - 1, nextPicture);
		recount();
	    notifyDataSetChanged();
	}
	
	public void remove(int position) {
		if ( photos.get(position).containsKey("id") ) {
			removed_ids.add(photos.get(position).get("id"));
		}
		
		photos.remove(position);
		recount();
	    notifyDataSetChanged();
	    
	    // remove tmp thumbnail
	    if ( photos.get(position).containsKey("tmp") ) {
	    	removeTmpFile(photos.get(position));
	    }
	}
	
	public void cut(int limit) {
		if ( limit <= 0 || photos.size() <= 1 )
				return;
		
		Iterator<HashMap<String, String>> iterator = photos.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			HashMap<String, String> next = iterator.next();
			if ( !next.containsKey("upload") && i >= limit ) {
				iterator.remove();
			}
			i++;
		}
		
		recount();
		notifyDataSetChanged();
	}
	
	public String getRemovedIDs() {
		String ids = "";
		
		if ( removed_ids.size() > 0 ) {
			ids = TextUtils.join(",", removed_ids);
			Log.d("FD", ids+"-removed ods");
		}
		
		return ids;
	}
	
	public void allowUpload(boolean state) {
		allowUpload = state;
		notifyDataSetChanged();
	}
	
	private void editDescription(final int position) {
		LinearLayout cont = (LinearLayout) ((Activity) instance).getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
		final EditText text_field = (EditText) cont.findViewById(R.id.text);
		
		if ( photos.get(position).containsKey("description") ) {
			text_field.setText(photos.get(position).get("description"));
		}
		
		Dialog.confirmActionView(Lang.get("menu_edit_description"), instance, cont, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				photos.get(position).put("description", text_field.getText().toString());
				Utils.hideKeyboard(text_field, instance);
				Dialog.toast("dialog_description_updated", instance);
			}
         }, null);
	}
	
	@Override
	public int getCount() {
		int count = photos.size();
		if ( !allowUpload ) {
			count--;
		} 
		return count;
	}
	
	public int getRealCount() {
		return photos.size();
	}

	@Override
	public Object getItem(int position) {
		return photos.get(position);
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
					.inflate(R.layout.upload_picture, null);
		}
						
		HashMap<String, String> photo = photos.get(position);
		
		/* set text values */
		ImageView image = (ImageView) view.findViewById(R.id.image);
		ImageAware imageAware = new ImageViewAware(image, false);
		
		if ( photo.containsKey("upload") ) {
			image.setBackgroundResource(R.drawable.add_picture);
			Utils.imageLoaderFeatured.displayImage("drawable://"+R.mipmap.blank, imageAware, Utils.imageLoaderOptionsFeatured);
		}
		else {
			Utils.imageLoaderFeatured.displayImage(photo.get("uri"), imageAware, Utils.imageLoaderOptionsFeatured);
		}
		
		/* set primary legend */
		RelativeLayout primary = (RelativeLayout) view.findViewById(R.id.primary);
		primary.setVisibility(photo.containsKey("main") ? View.VISIBLE : View.GONE);
		
		return view;
	}
	
	public void removeTmpFiles() {
		// remove tmp file
		File file = new File(Environment.getExternalStorageDirectory() + "/tmpfiletosend.jpg");
		file.delete();
		
		for ( HashMap<String, String> photo : photos ) {
			if ( photo.containsKey("id") || photo.containsKey("upload") )
				continue;
			
			removeTmpFile(photo);
		}
	}
	
	private void removeTmpFile(HashMap<String, String> photo) {
		try {
			Uri thumb_uri = Uri.parse(photo.get("uri"));
			File thumb = new File(thumb_uri.getPath());
			thumb.delete();
			
			Uri large_uri = Uri.parse(photo.get("original"));
			File large = new File(large_uri.getPath());
			large.delete();
		}
		catch (Exception e) {
			// we tried...
			e.printStackTrace();
		}
	}
}