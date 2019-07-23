package com.acentria.benslist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.adapters.PictureManagerAdapter;
import com.acentria.benslist.adapters.SpinnerAdapter;
import com.acentria.benslist.adapters.VideoManagerAdapter;
import com.acentria.benslist.controllers.MyListings;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import cz.msebera.android.httpclient.Header;


public class AddListing {

	public String selected_type_key = "";
	public int selected_category_id = 0;
	public int selected_plan_id = 0;
	public HashMap<String, String> selected_plan;

	public int last_listing_id = 0;
	private HashMap<String, String> last_listing_hash = new HashMap<String, String>();
	
	public PictureManagerAdapter picturesAdapter;
	public VideoManagerAdapter videoAdapter;
	
	final private String YOUTUBE_PREVIEW = "http://i.ytimg.com/vi/{id}/0.jpg"; 
	
	// views
	public Spinner listing_type_spinner_view;
	public SpinnerAdapter listing_type_spinner_adapter;

    public LinearLayout main_container;
	public LinearLayout category_spinners_view;
	public LinearLayout category_spinners_container_view;
	public LinearLayout category_options;
	public LinearLayout category_selected;
	public LinearLayout plan_container;
	public LinearLayout fields_area;
	public LinearLayout photos_container;
	public LinearLayout videos_container;
	public LinearLayout listing_plan_type;
	
	public TextView listing_type_through_hint_view;
	public Button select_category_button;
	public Button cancel_category_button;
	public Button submit;
	public TextView category_locked_warning;
	public ImageView edit_category;

	// data stores
	public HashMap<String, LinkedHashMap<Integer, ArrayList<HashMap<String, String>>>> categories_data = 
			   new HashMap<String, LinkedHashMap<Integer, ArrayList<HashMap<String, String>>>>();
	
	public LinkedHashMap<String, HashMap<String,String>> data = new LinkedHashMap<String, HashMap<String,String>>();
	private HashMap<String,String> formData = new HashMap<String,String>();
	private HashMap<String, View> fieldViews = new HashMap<String, View>();
	private ArrayList<HashMap<String, String>> plans;
	
	// other
	private ProgressDialog progress;
	public Boolean requestPurchase = true;
	
	public Context instance;
	
	// edit mode vars
	private boolean edit_mode = false;
	private boolean edit_load = false;
	private String edit_listing_id;
	private HashMap<String, String> edit_listing_hash;
	public static HashMap<String, String> edit_listing_data = new HashMap<String, String>();
 	private ArrayList<Integer> edit_category_parent_ids = new ArrayList<Integer>();
	
	AddListing (Context activity, Intent intent) {
		// clear data
		data.clear();
		formData.clear();
		fieldViews.clear();
		
		instance = activity;
		
		// get layouts
        main_container = (LinearLayout) ((Activity) instance).findViewById(R.id.main_container);
        listing_type_spinner_view = (Spinner) main_container.findViewById(R.id.listing_type);
        category_spinners_view = (LinearLayout) main_container.findViewById(R.id.category_spinners);
        category_spinners_container_view = (LinearLayout) main_container.findViewById(R.id.category_spinners_cont);
        category_options = (LinearLayout) main_container.findViewById(R.id.category_options);
        category_selected = (LinearLayout) main_container.findViewById(R.id.category_selected);
        plan_container = (LinearLayout) main_container.findViewById(R.id.plan_container);
        fields_area = (LinearLayout) main_container.findViewById(R.id.fields_area);
        photos_container = (LinearLayout) main_container.findViewById(R.id.photos_container);
        videos_container = (LinearLayout) main_container.findViewById(R.id.videos_container);
        listing_plan_type = (LinearLayout) main_container.findViewById(R.id.listing_plan_type);
        
        listing_type_through_hint_view = (TextView) main_container.findViewById(R.id.listing_type_through_hint);
        select_category_button = (Button) main_container.findViewById(R.id.select_category_button);
		cancel_category_button = (Button) main_container.findViewById(R.id.cancel_category_button);
        submit = (Button) main_container.findViewById(R.id.submit);
        category_locked_warning = (TextView) main_container.findViewById(R.id.category_locked_warning);
        edit_category = (ImageView) main_container.findViewById(R.id.edit_category);

        // edit listing mode
        if ( intent.getStringExtra("id") != null ) {
			edit_mode = true;
			edit_listing_id = intent.getStringExtra("id");
			edit_listing_hash = (HashMap<String, String>) intent.getSerializableExtra("listingHash");
			
			this.loadEditListingData(); // do async request to retrieve listing data first, then load editListingInit() method
		}
        // add listing mode
        else {
			// initialize events
	        this.events();
	        
			// initialize listing type spinner
	        this.initListingTypeSpinner(null);
			
			// initialize listing plan spinner
	        this.initPlanSpinner();
	        
	        // prepare photos container
	        this.preparePhotos(null);
	        
	        // prepare videos container
	        this.prepareVideos(null);
        }
	}
	
	public void events() {
		// select category button listener
		select_category_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				((TextView) category_selected.findViewById(R.id.selected_category_name)).
						setText(getSelectedCategoryNames());

				changeStateCategoryView();

				if (edit_mode) {
					if (!edit_load) {
						edit_load = true;
					} else {

						if (Integer.parseInt(edit_listing_data.get("Category_ID")) == selected_category_id) {
							// show fields
							fields_area.setVisibility(View.VISIBLE);
							submit.setVisibility(View.VISIBLE);
						} else {
							// remove fields
							fields_area.removeAllViews();
							loadEditListingData();
						}
					}
				} else {
					// show plan spinner
					plan_container.setVisibility(View.VISIBLE);

					// load plans and fields form
					loadData();
				}
			}
		});

		// cancl category button listener
		cancel_category_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				changeStateCategoryView();
				fields_area.setVisibility(View.VISIBLE);
			}
		});
		
		// change category icon
		edit_category.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (fields_area.getChildCount() > 0) {
					Dialog.CustomDialog(Lang.get("dialog_confirm_action"), Lang.get("dialog_change_category_notice"), AddListingActivity.instance, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							AddListing.this.restoreCategorySelection();

							if (edit_mode) {
								// remove fields
								fields_area.setVisibility(View.INVISIBLE);
							} else {
								// remove fields
								fields_area.removeAllViews();
							}
						}
					});
				} else {
					AddListing.this.restoreCategorySelection();
				}
			}
		});
		
		// submit form
		submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// validate form data if not EDIT mode
				boolean plan_valid = edit_mode ? true : validatePlan();

				// hide keyboard
				Utils.hideKeyboard(fields_area.getFocusedChild(), AddListingActivity.instance);

				// validate form 
				if (!Forms.validate(formData, data, fieldViews) || !plan_valid) {
					Dialog.toast("dialog_fill_required_fields", AddListingActivity.instance);
				}
				// proceed to saving process
				else {
					saveListing();
				}
			}
		});
	}
	
	private void restoreCategorySelection(){
		// show 'select category' options
		category_selected.setVisibility(View.GONE);
			
		// hide 'selected category' bar
		category_options.setVisibility(View.VISIBLE);
		
		// hide submit button
		submit.setVisibility(View.GONE);
		
		// hide multilingual spinner
		AddListingActivity.actionbarSpinner.setVisible(false);
		((Spinner) AddListingActivity.actionbarSpinner.getActionView()).setSelection(0);
		
		// hide add picture
		photos_container.setVisibility(View.GONE);

		// hide add picture
		videos_container.setVisibility(View.GONE);

        // hide plans spinner
        plan_container.setVisibility(View.GONE);
	}
	
	/**
	 * creates listying type spinner
	 * 
	 * @param default_selection - select type by default
	 * 
	 * @return spinner
	 */
	public void initListingTypeSpinner(String default_selection) {
        final ArrayList<HashMap<String, String>> type_items = new ArrayList<HashMap<String, String>>();

        // fetch available type for current account
        for ( Entry<String, HashMap<String, String>> entry : Config.cacheListingTypes.entrySet() ) {
            if ( Account.abilities.indexOf(entry.getKey()) >= 0 && entry.getValue().get("admin").equals("0") ) {
                type_items.add(entry.getValue());
            }
        }

        // manage spinner
        if ( type_items.size() == 0 ) {
            TextView message = (TextView) ((Activity) instance).getLayoutInflater()
                    .inflate(R.layout.info_message, null);

            message.setText(Lang.get("no_available_listing_type_to_submit"));
            main_container.removeAllViews();
            main_container.addView(message);
        }
        else if ( type_items.size() == 1 ) {
            listing_type_through_hint_view.setVisibility(View.GONE);
            listing_type_spinner_view.setVisibility(View.GONE);
            ((TextView) ((Activity) instance).findViewById(R.id.listing_type_captoin)).setVisibility(View.GONE);

            HashMap<String, String> single_type = type_items.get(0);
            selected_type_key = single_type.get("key");
            getCategories(single_type.get("key"), 0, instance);// async to buildCategorySelector()
        }
        else {
            HashMap<String, String> type_field_info = new HashMap<String, String>();
            type_field_info.put("key", "account_type");

            Utils.addFirstItem(type_items, "select");
            listing_type_spinner_adapter = new SpinnerAdapter(instance, type_items, type_field_info, formData, null);

            listing_type_spinner_view.setAdapter(listing_type_spinner_adapter);
            listing_type_spinner_view.setOnItemSelectedListener(new OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    clearCategorySpinners(null);
                    changeSelectCategoryButtonState(View.GONE, false);

                    category_spinners_view.setVisibility(View.GONE);
                    listing_type_through_hint_view.setVisibility(View.VISIBLE);

                    selected_type_key = type_items.get(position).get("key");
                    getCategories(type_items.get(position).get("key"), 0, instance);// async to buildCategorySelector()
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // edit listing mode
            if (default_selection != null) {
                listing_type_spinner_adapter.select(default_selection, listing_type_spinner_view, "key");
            }
        }
	}
	
	/**
	 * build categories spinner
	 * 
	 * @param type - listing type key
	 * @param id - parent category id
	 *
	 */
	public void buildCategorySpinner(final String type, final int id, final Context instance) {
		// change items visibility
		category_spinners_view.setVisibility(View.VISIBLE);
		if ( instance instanceof AddListingActivity ) {
			listing_type_through_hint_view.setVisibility(View.GONE);
		}
		
		// generate spinner
		Spinner spinner = (Spinner) ((Activity) instance).getLayoutInflater()
 	    		.inflate(R.layout.spinner, null);
		
		LayoutParams margin = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		margin.setMargins(0, 0, 0, Utils.dp2px(15));
		spinner.setLayoutParams(margin);
		spinner.setTag("category_spinner_"+id);
		
		HashMap<String, String> type_field_info = new HashMap<String, String>();
		type_field_info.put("key", "category");
		
		final ArrayList<HashMap<String, String>> items = (ArrayList<HashMap<String, String>>) categories_data.get(type).get(id).clone();
		
		HashMap<String, String> first_item = new HashMap<String, String>();
		first_item.put("key", "");
		first_item.put("id", "0");
		first_item.put("lock", "0");
		first_item.put("name", Lang.get("select"));
		items.add(0, first_item);

		SpinnerAdapter adapter = new SpinnerAdapter(instance, items, type_field_info, formData, null);
		   			
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long long_id) {
				selectCategory(items.get(position), position, parent);
				clearCategorySpinners(parent);
				
				if ( position > 0 ) {
					int selected_id = Integer.parseInt(items.get(position).get("id"));

					// build next level categories
					if ( items.get(position).get("sub_categories").equals("1") ){
						getCategories(type, selected_id	, instance);// async to buildCategorySelector()
					}
				}	
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		category_spinners_container_view.addView(spinner);
		
		// edit listing mode
		if ( edit_mode ) {
			if ( edit_category_parent_ids.size() > 0 ) {
				adapter.select(edit_category_parent_ids.get(0)+"", spinner, "id");
				edit_category_parent_ids.remove(0);
			}
			else {
				// last level category loaded
				adapter.select(edit_listing_data.get("Category_ID"), spinner, "id");
				
				if(!edit_load) {
					// hide category spinner
					editListingTriggers();
				}
			}
		}
	}
	
	/**
	 * change the state of the select category button
	 * 
	 * @param state - predefined view state
	 * @param locked - show locked categort warning
	 *
	 */
	private void changeSelectCategoryButtonState(int state, boolean locked) {
		// change select category button state
		select_category_button.setVisibility(state);
		if(edit_mode) {
			cancel_category_button.setVisibility(state);
		}
		category_locked_warning.setVisibility(locked ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * select category action, sets current category id and controls selected category availabilty
	 * 
	 * @param item - selected item HashMap
	 * @param position - selected item porition
	 * @param view - actual spinner view
	 *
	 */
	private void selectCategory(HashMap<String, String> item, int position, View view) {
		int view_index = category_spinners_container_view.indexOfChild(view);
		
		if ( (position == 0 || item.get("lock").equals("1")) && view_index  > 0 ) {
			Spinner parent = (Spinner) category_spinners_container_view.getChildAt(view_index - 1);
			int parent_position = (int) parent.getSelectedItemId();
			HashMap<String, String> parent_item = (HashMap<String, String>) parent.getAdapter().getItem(parent_position);

			selectCategory(parent_item, parent_position, parent);
		}
		else {
			int id = Integer.parseInt(item.get("id"));

			// category locked or not selected
			if ( id == 0 || item.get("lock").equals("1") ) {
				changeSelectCategoryButtonState(View.GONE, item.get("lock").equals("1") ? true : false);
			}
			// successful category selected
			else {
				changeSelectCategoryButtonState(View.VISIBLE, false);
				selected_category_id = id;
			}
		}
	}
	
	/**
	 * clear categories spinner
	 * 
	 * @param view - current active view
	 *
	 */
	private void clearCategorySpinners(View view) {
		if ( view == null ) {
			category_spinners_container_view.removeAllViews();
		}
		else {
			int child_count = category_spinners_container_view.getChildCount();
			int index = category_spinners_container_view.indexOfChild(view);

			if ( child_count == 1 )
				return;

			for (int i = child_count - 1; i > index; i--) {
				category_spinners_container_view.removeViewAt(i);
			}
		}
	}
	
	/**
	 * get categories by type and parent id
	 * 
	 * @param type - listing type key
	 * @param id - parent category id
	 *
	 */
	private void getCategories(final String type, final int id, final Context instance) {
		if ( type.isEmpty() || id < 0 )
			return;

		if ( categories_data.containsKey(type) && categories_data.get(type).containsKey(id) ) {
			buildCategorySpinner(type, id, instance);
		}
		else {
	    	HashMap<String, String> params = new HashMap<String, String>();
	    	
			params.put("type", type);
			params.put("parent", ""+id);
			final String url = Utils.buildRequestUrl("getCatTree", params, null);

			/* do async request */
	    	AsyncHttpClient client = new AsyncHttpClient();
	    	client.get(url, new AsyncHttpResponseHandler() {

				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						LinkedHashMap<Integer, ArrayList<HashMap<String, String>>> type_categories;

						if ( categories_data.containsKey(type) ) {
							type_categories = categories_data.get(type);
						}
						else {
							type_categories = new LinkedHashMap<Integer, ArrayList<HashMap<String, String>>>();
						}

						type_categories.put(id, Utils.parseXML(response, url, AddListingActivity.instance));
						categories_data.put(type, type_categories);

						buildCategorySpinner(type, id, instance);

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
	
	/**
	 * initialize listing plan spinner
	 *
	 */
	public void initPlanSpinner() {
		// set plan listing type listener
		final RadioGroup type_group = (RadioGroup) plan_container.findViewById(R.id.radio_group);
		type_group.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				RadioButton checked = (RadioButton) group.findViewById(checkedId);
				formData.put("plan_listing_type", checked.getTag().toString());
			}
		});
		
		// init plan selector
		final LinearLayout plan_selector = (LinearLayout) plan_container.findViewById(R.id.listing_plan_selector);
		plan_selector.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (plans == null)
					return;

				Intent intent = new Intent(Config.context, PlansActivity.class);
				intent.putExtra("mode", "select");
				intent.putExtra("plans", plans);
				intent.putExtra("category_id", selected_category_id + "");
				intent.putExtra("plan_id", selected_plan_id + "");

				((Activity) instance).startActivityForResult(intent, Config.RESULT_SELECT_PLAN);
			}
		});
	}
	
	public void selectPlan(Integer id, String appearance, Integer position) {
		selected_plan_id = id;
		selected_plan = plans.get(position);

        if ( !appearance.isEmpty() ) {
            int button_id = appearance.equals("featured") ? R.id.listing_featured : R.id.listing_standard;
            ((RadioGroup) plan_container.findViewById(R.id.radio_group)).check(button_id);
        }
		if ( selected_plan.get("Video").equals("0") ) {
			// show add video
			videos_container.setVisibility(View.GONE);
		}
		else {
			// show add video
			videos_container.setVisibility(View.VISIBLE);
		}

		updatePictures();
		updateVideos();
		updatePlan();
		
		updatePlanView(selected_plan.get("name"));
		
		// reset error state
		TextView plan_name = (TextView) plan_container.findViewById(R.id.listing_plan_name);
		plan_name.setTextAppearance(Config.context, R.style.default_text);

		TextView plan_error = (TextView) plan_container.findViewById(R.id.listing_plan_error);
		plan_error.setVisibility(View.GONE);
	}
	
	private void updatePlanView(String name) {
		TextView plan_name = (TextView) plan_container.findViewById(R.id.listing_plan_name);
		plan_name.setText(name);
	}
	
	public void updatePlan() {
		HashMap<String, String> plan = selected_plan;
		
		if ( selected_plan_id == 0 ) {
			listing_plan_type.setVisibility(View.GONE);
		}
		else {
			// advanced type mode
			int set_visible = View.GONE;
			if ( plan.get("Type").equals("package") && plan.get("Advanced_mode").equals("1") ) {
				set_visible = View.VISIBLE;
				boolean check_featured = false;
				
				// standard
				String standard_left = Lang.get("unlimited_short");
				
				if ( Integer.parseInt(plan.get("Standard_listings")) > 0 ) {
					if ( Integer.parseInt(plan.get("Listings_remains")) <= 0 ) {
						standard_left = Lang.get("submits_left").replace("{count}", plan.get("Standard_listings"));
					}
					else {
						if ( Integer.parseInt(plan.get("Standard_remains")) <= 0 ) {
							standard_left = "("+Lang.get("used_up")+")";
						}
						else {
							standard_left = Lang.get("submits_left").replace("{count}", plan.get("Standard_remains"));
						}
					}
				}
				
				RadioButton standard_legend = (RadioButton) plan_container.findViewById(R.id.listing_standard);
				standard_legend.setText(Lang.get("listing_appearance_standard")+" "+standard_left);
				
				if ( !plan.get("Package_ID").isEmpty() && Integer.parseInt(plan.get("Standard_remains")) <= 0 && !plan.get("Standard_listings").equals("0") ) {
					standard_legend.setEnabled(false);
					check_featured = true;
				}
				else {
					standard_legend.setEnabled(true);
					//standard_legend.setChecked(true);
				}
				
				// featured
				String featured_left = Lang.get("unlimited_short");
				
				if ( Integer.parseInt(plan.get("Featured_listings")) > 0 ) {
					if ( Integer.parseInt(plan.get("Listings_remains")) <= 0 ) {
						featured_left = Lang.get("submits_left").replace("{count}", plan.get("Featured_listings"));
					}
					else {
						if ( Integer.parseInt(plan.get("Featured_remains")) <= 0 ) {
							featured_left = "("+Lang.get("used_up")+")";
						}
						else {
							featured_left = Lang.get("submits_left").replace("{count}", plan.get("Featured_remains"));
						}
					}
				}
				
				RadioButton featured_legend = (RadioButton) plan_container.findViewById(R.id.listing_featured);
				featured_legend.setText(Lang.get("listing_appearance_featured")+" "+featured_left);
				
				if ( !plan.get("Package_ID").isEmpty() && Integer.parseInt(plan.get("Featured_remains")) <= 0 && !plan.get("Featured_listings").equals("0") ) {
					featured_legend.setEnabled(false);
				}
				else {
					featured_legend.setEnabled(true);
//					if ( check_featured ) {
//						featured_legend.setChecked(true);
//					}
				}
			}
			
			listing_plan_type.setVisibility(set_visible);
		}
	}
	
	public void updatePictures() {
		if ( selected_plan_id == 0 )
			return;
		
		int count = Integer.parseInt(selected_plan.get("Image"));
		int in_use = picturesAdapter.getCount() - 1; // 1 is upload button
		Boolean unlim = selected_plan.get("Image_unlim").equals("1") ? true : false;
		int visibility_state = View.VISIBLE;

		if ( !unlim ) {
			if ( count <= 0 ) {
				visibility_state = View.GONE;
			}
			
			if ( in_use > count ) {
				picturesAdapter.cut(count);
			}
		}
		
		if(Config.cacheListingTypes.get(selected_type_key).get("photo").equals("0")) {
			visibility_state = View.GONE;
			count = 0;
		}
		
		picturesAdapter.update(count, unlim);

		// change photos container state
		photos_container.setVisibility(visibility_state);
	}
	
	public void updateVideos() {
		if ( selected_plan_id == 0 )
			return;
		
		int count = Integer.parseInt(selected_plan.get("Video"));
		int in_use = videoAdapter.getCount() - 1; // 1 is upload button
		Boolean unlim = selected_plan.get("Video_unlim").equals("1") ? true : false;
		int visibility_state = View.VISIBLE;
		
		if ( !unlim ) {
			if ( count <= 0 ) {
				visibility_state = View.GONE;
			}
			
			if ( in_use > count ) {
				videoAdapter.cut(count);
			}
		}
		
		if(Config.cacheListingTypes.get(selected_type_key).get("video").equals("0")) {
			visibility_state = View.GONE;
			count = 0;
		}

		videoAdapter.update(count, unlim);
		
		// change photos container state
		videos_container.setVisibility(visibility_state);
	}
	
	/**
	 * get selected categories name as bread crubms
	 * 
	 * @return categories name as bread crubms
	 */
	public String getSelectedCategoryNames() {
		String names = "";
		
		int count = category_spinners_container_view.getChildCount();
		for (int i=0; i<count; i++) {
			HashMap<String, String> item = (HashMap<String, String>) ((Spinner) category_spinners_container_view.getChildAt(i)).getSelectedItem();
			if ( !item.get("id").equals("0") ) {
				if ( i > 0 ) {
					names += Html.fromHtml("&nbsp;&nbsp;&#10095;&nbsp;&nbsp;");// arrow sign
				}
				names += item.get("name");
			}
		}
		
		return names;
	}
	/**
	 *  hide category spiner
	 */
	public void changeStateCategoryView() {
		// hide 'select category' options
		category_options.setVisibility(View.GONE);

		// show 'selected category' bar
		category_selected.setVisibility(View.VISIBLE);

		// show add picture
		if(!Config.cacheListingTypes.get(selected_type_key).get("photo").equals("0")) {
		photos_container.setVisibility(View.VISIBLE);
	}
		
	}
		
	/**
	 * load plans and fields form by selected cagefory
	 * 
	 */
	public void loadData() {
		updatePlanView(Lang.get("loading"));
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		
		params.put("account_type", Account.accountData.get("type"));
		params.put("listing_type", selected_type_key);
		params.put("id", selected_category_id+"");
		
		final String url = Utils.buildRequestUrl("getAddListingData", params, null);

		/* do async request */
    	AsyncHttpClient client = new AsyncHttpClient();
    	client.setTimeout(30000); // 30 seconds limit for this task
    	client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					Log.d("FD - load data response", response);
					JSONObject json = null;

					try {
						json = new JSONObject( response );

						if(!json.isNull("plans")) {
							plans = JSONParser.parseJsontoArrayList(json.getString("plans"));
								AddListingActivity.payment.synchronizePlans(plans);
								if (plans.size() == 0) {
									Dialog.simpleWarning(Lang.get("dialog_no_plans_category"), instance);
									updatePlanView(Lang.get("no_plans_available"));
								} else if (plans.size() == 1 && plans.get(0).get("Price").equals("0")) {
								selected_plan_id = Integer.parseInt(plans.get(0).get("ID"));
									selected_plan = plans.get(0);
									plan_container.setVisibility(View.GONE);
									updatePictures();
								} else {
									updatePlanView(Lang.get("select_plan"));
								}
							}
						if(!json.isNull("form")) {

							HashMap<String, ArrayList<HashMap<String, String>>> items = new HashMap<String, ArrayList<HashMap<String, String>>>();
							data = Account.parseJsonForm(json, items);

							fields_area.removeAllViews();

							if (!data.isEmpty()) {

									Forms.buildSubmitFields(fields_area, data, formData, items, instance, AddListingActivity.actionbarSpinner, fieldViews, true);

									// show submit button
									submit.setVisibility(View.VISIBLE);
								} else {
									TextView message = (TextView) ((Activity) instance).getLayoutInflater()
											.inflate(R.layout.info_message, null);

									message.setText(Lang.get("selected_category_no_fields"));
									fields_area.addView(message);
								}

								fields_area.setVisibility(View.VISIBLE);

							}
					} catch (JSONException e) {
						e.printStackTrace();
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
	
	/**
	 * prepare photos container
	 * 
	 */
	public void preparePhotos(ArrayList<HashMap<String, String>> pictures) {
		// set divider name
		TextView divider = (TextView) photos_container.findViewById(R.id.divider_text);
		divider.setText(Lang.get("pictures_divider"));
		
		if ( pictures == null ) {
			pictures = new ArrayList<HashMap<String, String>>();
		}
		
		HashMap<String, String> upload = new HashMap<String, String>();
		upload.put("upload", "1");
		pictures.add(upload);

		// initialize grid view
		GridView picturesGrid = (GridView) photos_container.findViewById(R.id.pictures);

		// create grid view adapter
		picturesAdapter = new PictureManagerAdapter(picturesGrid, pictures, AddListingActivity.instance, (TextView) photos_container.findViewById(R.id.divider_text));

		picturesGrid.setAdapter(picturesAdapter);
		picturesGrid.setOnItemClickListener(picturesAdapter);
		picturesGrid.post(new Runnable() {
			@Override
			public void run() {
				picturesAdapter.reRangePhotoBox();
				picturesAdapter.notifyDataSetChanged();
			}
		});
	}


	
	/**
	 * prepare videos container
	 * 
	 */
	public void prepareVideos(ArrayList<HashMap<String, String>> videos) {
		// set divider name
		TextView divider = (TextView) videos_container.findViewById(R.id.divider_text);
		divider.setText(Lang.get("videos_divider"));
		
		if ( videos == null ) {
			videos = new ArrayList<HashMap<String, String>>();
		}
		
		ImageView upload_button = (ImageView) videos_container.findViewById(R.id.upload_video);
		upload_button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if ( selected_plan_id > 0 ) { 
					LinearLayout cont = (LinearLayout) ((Activity) AddListingActivity.instance).getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
					final EditText text_field = (EditText) cont.findViewById(R.id.text);
									
					Dialog.confirmActionView(Lang.get("dialog_add_youtube_video"), AddListingActivity.instance, cont, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							String text = text_field.getText().toString();
							if ( !text.isEmpty() ) {
								String youtube_id = Utils.getYouTubeID(text);
								if ( youtube_id == null ) {
									youtube_id = text;
								}
								
								getVideoByID(youtube_id);
								
								Utils.hideKeyboard(text_field, AddListingActivity.instance);
							}
						}
			         }, null);
				}
				else {
					Dialog.simpleWarning(Lang.get("dialog_no_plan_selected"), AddListingActivity.instance);
				}
			}
		});
		
		LinearLayout video_button_contaoner = (LinearLayout) videos_container.findViewById(R.id.video_button_cont);
		
		// initialize list view
		ListView videoList = (ListView) videos_container.findViewById(R.id.videos);
		
		// create grid view adapter
		videoAdapter = new VideoManagerAdapter(videos, 
										AddListingActivity.instance,
										(TextView) videos_container.findViewById(R.id.divider_text),
										videoList,
										video_button_contaoner);

		videoList.setAdapter(videoAdapter);
		videoList.setOnItemClickListener(videoAdapter);
	}
	
	public void getVideoByID(final String id) {
		final String url = "https://www.youtube.com/oembed?format=json&url=http://www.youtube.com/watch?v="+id;

		// Creating new JSON Parser
		JSONParser jParser = new JSONParser();
		
		// Getting JSON from URL
		JSONObject json = jParser.getJSONFromUrl(url);


		if(json!=null) {
			try {
				HashMap<String, String> video = new HashMap<String, String>();
				video.put("title", json.getString("title"));
				video.put("uri", YOUTUBE_PREVIEW.replace("{id}", id));
				video.put("preview", id);

				videoAdapter.add(video);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		else {
			Dialog.simpleWarning(Lang.get("dialog_youtube_id_error"), AddListingActivity.instance);
		}
	}
	
	public void addPicture(String uri) {
		HashMap<String, String> tmp = new HashMap<String, String>();
		tmp.put("uri", createThumbnail(uri));
		tmp.put("original", uri);
		tmp.put("original", uri);
		tmp.put("orientation", ExifUtil.orientation + "");
		tmp.put("tmp", "1"); //picture uploaded from the device
		picturesAdapter.add(tmp);
	}
	
	public String createThumbnail(String imagePath){
		Bitmap bitmapImage = null;

		int thumbnailSide = (int) Math.ceil(Utils.getScreenWidth() / 2);

		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			AssetFileDescriptor fileDescriptor = null;
			fileDescriptor = AddListingActivity.instance.getContentResolver().openAssetFileDescriptor( Uri.parse(imagePath), "r");
			bitmapImage = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);

			bitmapImage = ThumbnailUtils.extractThumbnail(bitmapImage, thumbnailSide, thumbnailSide);
        }
        catch(Exception ex) {
        	Log.d("FD", "Unable to create thumbnail from file: "+imagePath);
        }

        bitmapImage = ExifUtil.rotateBitmap(imagePath, bitmapImage);

		String root = Environment.getExternalStorageDirectory().toString();
		File myDir = new File(root + "/thumbnails");
		myDir.mkdirs();
		
		String fname = Account.accountData.get("username") +"_"+ System.currentTimeMillis() +".jpg";
		String fpath = myDir +"/"+ fname;
		File file = new File(myDir, fname);

		if ( file.exists() ) {
			file.delete();
		}
		
		try {
			FileOutputStream out = new FileOutputStream(file);
			bitmapImage.compress(Bitmap.CompressFormat.JPEG, 90, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return "file://"+fpath;
	}

	/**
	 * validate plan spinner
	 * 
	 */
	public boolean validatePlan() {
		boolean allow = true;

		if ( selected_plan_id <= 0 ) {
			// set error message
			TextView plan_error = (TextView) plan_container.findViewById(R.id.listing_plan_error);
			plan_error.setText(Lang.get("dialog_plan_not_selected"));
			plan_error.setVisibility(View.VISIBLE);
			
			// set error state
			TextView plan_name = (TextView) plan_container.findViewById(R.id.listing_plan_name);
			plan_name.setTextAppearance(Config.context, R.style.error_text);
			
			allow = false;
		}
		
		return allow;
	}
	
	/**
	 * save listing on the server
	 * 
	 */
	public void saveListing() {
		progress = ProgressDialog.show(AddListingActivity.instance, null, Lang.get("dialog_saving_listing"));

		/* get form data - build request url */
    	HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		
		params.put("listing_type_key", selected_type_key);
    	params.put("listing_category_id", selected_category_id+"");
    	params.put("listing_plan_id", selected_plan_id+"");

    	// EDIT listing mode
    	if ( edit_mode ) {
    		params.put("listing_id", edit_listing_id);
    		params.put("removed_picture_ids", picturesAdapter.getRemovedIDs());
    	}
    	
    	if ( videoAdapter.getCount() > 0 ) {
    		params.put("youtube_video_ids", videoAdapter.getVideoIDs());
    	}
    	
    	String request_name = edit_mode ? "editListing" : "addListing";
		final String url = Utils.buildRequestUrl(request_name, params, null);

		/* do async request */
    	AsyncHttpClient client = new AsyncHttpClient();
    	client.setTimeout(50000);
    	client.post(url, Utils.toParams(formData), new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					Log.d("FD ... save listing...", response);
					/* parse response */
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if ( doc == null ) {
						progress.dismiss();
						Dialog.simpleWarning(Lang.get("returned_xml_failed"), AddListingActivity.instance);
					}
					else {
						NodeList errorNode = doc.getElementsByTagName("error");

						/* handle errors */
						if ( errorNode.getLength() > 0 ) {
							progress.dismiss();

							Element error = (Element) errorNode.item(0);
							Dialog.simpleWarning(Lang.get(error.getTextContent()), AddListingActivity.instance);
						}
						/* finish this activity and show toast */
						else {
							NodeList successNode = doc.getElementsByTagName("success");
							if ( successNode.getLength() > 0 ) {
								Element success = (Element) successNode.item(0);
								last_listing_hash = Utils.parseHash(success.getChildNodes());
								last_listing_id = Integer.parseInt(edit_mode ? edit_listing_id : last_listing_hash.get("id"));

								savePictures(0);
							}
							else {
								progress.dismiss();
								Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), AddListingActivity.instance);
							}
						}
					}

				} catch (UnsupportedEncodingException e1) {

				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
				progress.dismiss();
                Dialog.simpleWarning(Lang.get("dialog_unable_save_data_on_server"), AddListingActivity.instance);
			}
    	});
	}
	private boolean getDropboxIMGSize(Uri uri){
		boolean bigSize = false;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		ParcelFileDescriptor fd = null; // u is your Uri
		try {
			fd = Config.context.getContentResolver().openFileDescriptor(uri, "r");
			BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
			int width = options.outWidth;
			int height = options.outHeight;
			int img_size = width*height;
		if( img_size > 1440000) {
			bigSize = true;
		}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
	}
	
		return bigSize;
	}

	/**
	 * save pictures on the server
	 * 
	 */
	public void savePictures(final int position) {
		// saving is done
		if ( position > picturesAdapter.getRealCount() - 2 ) {
			if ( edit_mode ) {
				editListingDone();
			}
			else {
				addListingDone();
			}
			
			return;
		}
		
		HashMap<String, String> picture = (HashMap<String, String>) picturesAdapter.getItem(position);
		Log.d("FD", picture.toString());
		
		final Boolean last = picturesAdapter.getRealCount() - 2 == position ? true : false;
		
		// save picture
		final int display_position = position + 1;
		final int display_total = picturesAdapter.getRealCount() - 1;
		progress.setMessage(Lang.get("dialog_saving_picture").replace("{current}", display_position+"").replace("{total}", display_total+""));
		
		/* prepare GET params */
		HashMap<String, String> get_params = new HashMap<String, String>();
		get_params.put("account_id", Account.accountData.get("id"));
		get_params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		get_params.put("listing_id", last_listing_id + "");
		get_params.put("orientation", picture.get("orientation") + "");
		final String url = Utils.buildRequestUrl("savePicture", get_params, null);

		/* send selected image to the server */
		RequestParams params = new RequestParams();
		
		boolean allow_send = true;
		
		// send existing photo to update status and description
		if ( picture.containsKey("ID") ) {
			params.put("exist_id", picture.get("ID"));
		}
		// send new photo
		else {
			Uri file_uri = Uri.parse(picture.get("original"));
			File file = new File(file_uri.getPath());
			
			try {
				float file_size = file.length() / 2048;
				if(file_size < 2) {
				params.put("image", file);
				}
				else {
					Bitmap bitmapImage;
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inSampleSize = 4;
					AssetFileDescriptor fileDescriptor = null;
					fileDescriptor = AddListingActivity.instance.getContentResolver().openAssetFileDescriptor( Uri.parse(picture.get("original")), "r");
					bitmapImage = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
					File new_file = new File(Environment.getExternalStorageDirectory() + "/tmpfiletosend.jpg");
					FileOutputStream out;
					try {
						out = new FileOutputStream(new_file);
						bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
						params.put("image", new_file);
					} catch (FileNotFoundException eee) {
						allow_send = false;
						eee.printStackTrace();
					}
				}

			} catch (FileNotFoundException e) {
				try {
					allow_send = true;
					Bitmap bitmapImage;
					if ( getDropboxIMGSize(Uri.parse(picture.get("original"))) ) {
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inSampleSize = 4;
						AssetFileDescriptor fileDescriptor = null;
						fileDescriptor = AddListingActivity.instance.getContentResolver().openAssetFileDescriptor( Uri.parse(picture.get("original")), "r");
						bitmapImage = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options);
					}
					else {
						bitmapImage = MediaStore.Images.Media.getBitmap(AddListingActivity.instance.getContentResolver(), Uri.parse(picture.get("original")));
					}
					File new_file = new File(Environment.getExternalStorageDirectory() + "/tmpfiletosend.jpg");
					FileOutputStream out;
					try {
						out = new FileOutputStream(new_file);
						bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
						params.put("image", new_file);
					} catch (FileNotFoundException eee) {
						allow_send = false;
						eee.printStackTrace();
					}
				}
				catch (Exception ee) {
					allow_send = false;
					ee.printStackTrace();
				}
			}
		}
		if ( picture.containsKey("main") ) {
			params.put("main", "1");
		}
		if ( picture.containsKey("description") ) {
			params.put("description", picture.get("description"));
		}
		if ( last ) {
			params.put("last", "1");
		}
		
		if ( allow_send ) {
			/* do request */
			AsyncHttpClient client = new AsyncHttpClient();
			client.setTimeout(60000); // set 30 seconds for this task
	    	client.post(url, params, new AsyncHttpResponseHandler() {

				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						Log.d("FD add photo ... ", response);
						/* parse response */
						XMLParser parser = new XMLParser();
						Document doc = parser.getDomElement(response, url);

						if ( doc == null ) {
							progress.setMessage(Lang.get("dialog_failed_picture").replace("{current}", display_position+"").replace("{total}", display_total+""));
						}
						else {
							NodeList errorNode = doc.getElementsByTagName("error");

							/* handle errors */
							if ( errorNode.getLength() > 0 ) {
								progress.setMessage(Lang.get("dialog_failed_picture").replace("{current}", display_position+"").replace("{total}", display_total+""));
							}
							/* finish this activity and show toast */
							else {
								NodeList successNode = doc.getElementsByTagName("success");
								Element success = (Element) successNode.item(0);

								if ( successNode.getLength() > 0 ) {
									progress.setMessage(Lang.get("dialog_saved_picture").replace("{current}", display_position+"").replace("{total}", display_total+""));
								}
								else {
									progress.setMessage(Lang.get("dialog_failed_picture").replace("{current}", display_position+"").replace("{total}", display_total+""));
								}

								if ( last ) {
									// add updated photo url and count to the listing hash
									last_listing_hash.put("photo", Utils.getNodeByName(success, "photo"));
									last_listing_hash.put("photos_count", Utils.getNodeByName(success, "photos_count"));
								}
							}
						}

						CountDownTimer timer = new CountDownTimer(1000, 1000) {
							public void onTick(long millisUntilFinished) {}

							public void onFinish() {
								savePictures(position+1);
							}
						};
						timer.start();

					} catch (UnsupportedEncodingException e1) {

					}
				}

				@Override
				public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
					// called when response HTTP status is "4XX" (eg. 401, 403, 404)
					progress.setMessage(Lang.get("dialog_failed_picture").replace("{current}", display_position+"").replace("{total}", display_total+""));

					CountDownTimer timer = new CountDownTimer(1000, 1000) {
						public void onTick(long millisUntilFinished) {}

						public void onFinish() {
							savePictures(position+1);
						}
					};
					timer.start();
					Toast.makeText(AddListingActivity.instance, "Broken upload photos", Toast.LENGTH_LONG).show();
				}
	    	});
		}
		else {
			progress.setMessage(Lang.get("dialog_failed_picture").replace("{current}", display_position+"").replace("{total}", display_total+""));
			
			CountDownTimer timer = new CountDownTimer(1000, 1000) {
				public void onTick(long millisUntilFinished) {}
				
				public void onFinish() {
					savePictures(position+1);
				}
			};
			timer.start();
		}
	}
	
	private void addListingDone() {
		progress.dismiss();

		Double plan_price = Double.parseDouble(selected_plan.get("Price"));
		String show_price = Utils.getCacheConfig("currency_position").equals("before") 
				? Utils.getCacheConfig("system_currency") +" "+ plan_price 
				: plan_price + Utils.getCacheConfig("system_currency");
		
		String tmp_phrase = Lang.get("listing_added_payment_required");

		/* paid listing */
		if ( (selected_plan.get("Type").equals("listing") && plan_price > 0) ||
			 (selected_plan.get("Type").equals("package") && plan_price > 0 && selected_plan.get("Package_ID").isEmpty() )
		) {
			tmp_phrase = tmp_phrase.replace("{price}", show_price);
		}
		/* free listing */
		else {
			tmp_phrase = Lang.get(Utils.getCacheConfig("listing_auto_approval").equals("1") ? "listing_added_auto_approved" : "listing_added_pending");
			requestPurchase = false;
		}
		
		final String phrase = tmp_phrase;
		
		/* set timeout 0.5 second */
    	CountDownTimer timer = new CountDownTimer(500, 500) {
			public void onTick(long millisUntilFinished) {}
			
			public void onFinish() {
				Utils.requestTab("MyListings", selected_type_key);
				SwipeMenu.performClick(4);//open my listings

				// show message and start purchase activity in positive click case
				if ( requestPurchase ) {
					Dialog.confirmAction(phrase, null, Lang.get("dialog_pay_now"), Lang.get("dialog_pay_later"), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							HashMap<String, String> details = new HashMap<String, String>();
							details.put("service", "listings");
							details.put("item", selected_plan.get("Type"));
							details.put("title", Lang.get("order_item_"+selected_plan.get("Type")).replace("{id}", last_listing_id+""));
							details.put("id", ""+last_listing_id);
							details.put("amount", selected_plan.get("Price"));
							details.put("plan", ""+selected_plan_id);
							details.put("plan_key", selected_plan.get("Key"));
							details.put("success_phrase", Lang.get(Utils.getCacheConfig("listing_auto_approval").equals("1") ? "listing_paid_auto_approved" : "listing_paid_pending"));

							Intent intent = new Intent(Config.context, PurchaseActivity.class);
							intent.putExtra("hash", details);

							int index = MyListings.TAB_KEYS.indexOf(selected_type_key);

				        	if ( index >= 0 ) {
								Fragment fragment = Utils.findFragmentByPosition(index, null, MyListings.pager, MyListings.adapter);
								fragment.startActivityForResult(intent, Config.RESULT_PAYMENT);
				        	}
						}
					}, null);
				}
				// show message
				else {
					Dialog.simpleWarning(phrase);
				}
			}
		};
		timer.start();
		
		// remove temporary files
		picturesAdapter.removeTmpFiles();
		
		// add listing to my listings grid
		MyListings.addItem(last_listing_hash);
		
		// finish activity
		((Activity) instance).finish();
	}
	
	private void editListingDone() {
		progress.dismiss();

		// remove tmp files
		picturesAdapter.removeTmpFiles();
		
		// add listing to my listings grid
		MyListings.updateItem(last_listing_id, last_listing_hash);

		if (!selected_plan.get("Price").equals("0") && edit_listing_data.get("Status").equals("incomplete")) {
			Double plan_price = Double.parseDouble(selected_plan.get("Price"));
			String show_price = Utils.getCacheConfig("currency_position").equals("before")
					? Utils.getCacheConfig("system_currency") +" "+ plan_price
					: plan_price + Utils.getCacheConfig("system_currency");

			String tmp_phrase = Lang.get("listing_edit_payment_required");
			tmp_phrase = tmp_phrase.replace("{price}", show_price);
			final String phrase = tmp_phrase;

			Dialog.confirmAction(phrase, null, Lang.get("dialog_pay_now"), Lang.get("dialog_pay_later"), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					HashMap<String, String> details = new HashMap<String, String>();
					details.put("item", selected_plan.get("Type"));
					details.put("title", Lang.get("order_item_"+selected_plan.get("Type")).replace("{id}", last_listing_id+""));
					details.put("id", ""+last_listing_id);
					details.put("amount", selected_plan.get("Price"));
					details.put("plan", ""+selected_plan_id);
					details.put("plan_key", selected_plan.get("Key"));
					details.put("success_phrase", Lang.get(Utils.getCacheConfig("listing_auto_approval").equals("1") ? "listing_paid_auto_approved" : "listing_paid_pending"));

					Intent intent = new Intent(Config.context, PurchaseActivity.class);
					intent.putExtra("hash", details);

					int index = MyListings.TAB_KEYS.indexOf(selected_type_key);

					if ( index >= 0 ) {
						Fragment fragment = Utils.findFragmentByPosition(index, null, MyListings.pager, MyListings.adapter);
						fragment.startActivityForResult(intent, Config.RESULT_PAYMENT);
					}
				}
			}, null);
		}
		else {
			// called for results
			if (((Activity) instance).getCallingActivity() != null) {
				((Activity) instance).setResult(Activity.RESULT_OK);
			} else {
				String phrase = Utils.getCacheConfig("edit_listing_auto_approval").equals("1") ? "listing_edit_auto_approved" : "listing_edit_pending";
				Dialog.simpleWarning(Lang.get(phrase));
			}
		}

		// finish activity
		((Activity) instance).finish();
	}
	
	/**
	 * load listing data | EDIT LISTING MODE
	 * 
	 */
	public void loadEditListingData() {
		progress = ProgressDialog.show(instance, null, Lang.get("android_loading"));
		category_options.setVisibility(View.GONE);
		selected_type_key = edit_listing_hash.get("listing_type");
		
		final String type = edit_listing_hash.get("listing_type");
		
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("account_id", Account.accountData.get("id"));
		params.put("password_hash", Utils.getSPConfig("accountPassword", null));
		
		params.put("listing_category_id", selected_category_id + "");
		params.put("listing_type", edit_listing_hash.get("listing_type"));
		params.put("listing_id", edit_listing_id);
		final String url = Utils.buildRequestUrl("getEditListingInfo", params, null);

		/* do async request */
    	AsyncHttpClient client = new AsyncHttpClient();
    	client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					progress.dismiss();
					category_options.setVisibility(View.VISIBLE);

					ArrayList<HashMap<String, String>> pictures = null;
					ArrayList<HashMap<String, String>> videos = null;

					LinkedHashMap<Integer, ArrayList<HashMap<String, String>>> type_categories = new LinkedHashMap<Integer, ArrayList<HashMap<String, String>>>();


					JSONObject json = new JSONObject( response );

					// parse listing data
					if (!json.isNull("data")) {
						edit_listing_data = JSONParser.parseJson(json.getString("data"));
					}
					// parse category data
					if (!json.isNull("category")) {

						int id = Integer.parseInt(edit_listing_data.get("parent_cat")!=null? edit_listing_data.get("parent_cat"):"0");

						type_categories.put(id, JSONParser.parseJsontoArrayList(json.getString("category")));

						if (id != 0) {
							edit_category_parent_ids.add(id);
						}
					}
					// parse form data
					if (!json.isNull("form")) {
						HashMap<String, ArrayList<HashMap<String, String>>> items = new HashMap<String, ArrayList<HashMap<String, String>>>();
						data = Account.parseJsonForm(json, items);
						if (!data.isEmpty()) {
							Forms.buildSubmitFields(fields_area, data, formData, items, AddListingActivity.instance, AddListingActivity.actionbarSpinner, fieldViews, true);

							// show submit button
							submit.setVisibility(View.VISIBLE);
						} else {
							TextView message = (TextView) ((Activity) instance).getLayoutInflater()
									.inflate(R.layout.info_message, null);

							message.setText(Lang.get("selected_category_no_fields"));
							fields_area.addView(message);
						}

						fields_area.setVisibility(View.VISIBLE);
					}
					// parse pictures data
					if (!json.isNull("pictures")) {
						pictures = JSONParser.parseJsontoArrayList(json.getString("pictures"));
					}
					// parse videos data
					if (!json.isNull("videos")) {
						videos = JSONParser.parseJsontoArrayList(json.getString("videos"));
					}
					// parse plan data
					if (!json.isNull("plan")) {
						selected_plan = JSONParser.parseJson(json.getString("plan"));
					}

					// put all categories info
					categories_data.put(type, type_categories);
					Log.d("FD", type_categories.toString());
					// continue preparing edit listing process
					editListingInit(pictures, videos);

				}  catch (JSONException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
				// called when response HTTP status is "4XX" (eg. 401, 403, 404)
			}
		});
	}
	
	/**
	 * initialize edit listing process | EDIT LISTING MODE
	 * 
	 */
	public void editListingInit(ArrayList<HashMap<String, String>> pictures, ArrayList<HashMap<String, String>> videos) {
        
        // initialize events
		this.events();
        
		// initialize listing type spinner
        this.initListingTypeSpinner(edit_listing_hash.get("listing_type"));
		
		// hide listing plan spinner
        plan_container.setVisibility(View.GONE);
        selected_plan_id = Integer.parseInt(edit_listing_data.get("Plan_ID"));
        
        // prepare photos container
        this.preparePhotos(pictures);
        this.updatePictures();
        
        // prepare videos container
        this.prepareVideos(videos);
        this.updateVideos();

		if(edit_load) {
			changeStateCategoryView();
		}
		/* hide data*/
		listing_type_spinner_view.setEnabled(false);
	}
	
	/**
	 * trigger events | EDIT LISTING MODE
	 * 
	 */
	public void editListingTriggers() {
		// select category button
        select_category_button.performClick();
	}
}