package com.acentria.benslist;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.acentria.benslist.adapters.CheckboxDialogAdapter;
import com.acentria.benslist.adapters.PlaceAutocompleteAdapter;
import com.acentria.benslist.adapters.SpinnerAdapter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Forms {
	
	/**
	 * number and years field width
	 */
	public static int short_field_width = 160;
    private static HashMap<String, View> fieldViewsForm;
    private static String use_on_map = "0";
    public static String searchField = "0";
	public static HashMap<String, HashMap<String, View>> fieldViewsSearch = new HashMap<String, HashMap<String, View>>();
	
	/**
	 * build search form
	 * 
	 * @param layout - view to build form in
	 * @param fields - fields array
     * @param typeKey - listing/account type key
	 * @param formData - form data to apply changes in form to
	 * @param fieldItems - field items array
	 * @param instance - parent instance

	 */
	public static void buildSearchFields(LinearLayout layout, String typeKey, ArrayList<HashMap<String, String>> fields,
			final HashMap<String,String> formData, final HashMap<String, ArrayList<HashMap<String, String>>> fieldItems, 
			final Context instance) {
		searchField = "1";
		HashMap<String, View> fieldViews = new HashMap<String, View>();
		/* define margin top params */
		LayoutParams marginTop = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		marginTop.setMargins(0, Utils.dp2px(15), 0, 0);
		
		for (final HashMap<String, String> entry : fields) {
    		/* select */
    		if ( entry.get("type").equals("select") ) {
				if(entry.get("data").equals("years")) {

					LinearLayout build_layout = (LinearLayout) ((Activity) instance).getLayoutInflater()
							.inflate(R.layout.field_years_search, null);

					build_layout.setLayoutParams(marginTop);
					String field_key = entry.get("key");
					entry.put("data", "years");

					final Spinner from = (Spinner) build_layout.findViewById(R.id.from);
					final Spinner to = (Spinner) build_layout.findViewById(R.id.to);

					SpinnerAdapter adapter = new SpinnerAdapter(Config.context, fieldItems.get(field_key), entry, formData, "search");
					from.setAdapter(adapter);
					from.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
							HashMap<String, String> currency_val = (HashMap<String, String>) to.getSelectedItem();
							String val = fieldItems.get(entry.get("key")).get(position).get("key");
							formData.put(entry.get("key"), val + "-" + currency_val.get("key"));
						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {
						}
					});
					from.setVisibility(View.VISIBLE);

					SpinnerAdapter adapterTo = new SpinnerAdapter(Config.context, fieldItems.get(field_key), entry, formData, "search");
					to.setAdapter(adapterTo);
					to.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
							HashMap<String, String> currency_val = (HashMap<String, String>) from.getSelectedItem();
							String val = fieldItems.get(entry.get("key")).get(position).get("key");
							formData.put(entry.get("key"), currency_val.get("key")+"-"+val);
						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {
						}
					});
					to.setVisibility(View.VISIBLE);
					layout.addView(build_layout);
					if(searchField.equals("1")){
						fieldViews.put(entry.get("key"), build_layout);
					}
				}
				else {
					layout.addView(selectField(entry, fieldItems.get(entry.get("key")), formData, typeKey, 0, instance));

					// multi category field
					if ( entry.get("key").contains(Config.categoryFieldKey) && !entry.get("data").isEmpty() ) {
						int form_number = Integer.parseInt(entry.get("data"));
						if ( form_number > 1 ) {
							for (int i = 1; i < form_number; i++) {
								ArrayList<HashMap<String, String>> sc_data = new ArrayList<HashMap<String, String>>();
								HashMap<String, String> itemDefault = new HashMap<String, String>();
								itemDefault.put("name", Lang.get("android_any_field").replace("{field}", Lang.get("subcategory")));
								itemDefault.put("key", "");
								sc_data.add(itemDefault);

								HashMap<String, String> sc_entry = (HashMap<String, String>) entry.clone();
								sc_entry.put("key", "sub_categories_level_"+i);
								if ( i+1 == form_number ) {
									sc_entry.put("data", "");
								}

								layout.addView(selectField(sc_entry, sc_data, formData, typeKey, 0, instance));
							}
						}
					}
				}
    		}
    		/* yes-no */
    		else if ( entry.get("type").equals("bool") ) {
    			LinearLayout bool_field = (LinearLayout) booleanField(entry, formData, 0, instance);
    			layout.addView(bool_field);
    		}
    		/* text */
    		else if ( entry.get("type").equals("text") ) {
    			LinearLayout text_field_cont = (LinearLayout) ((Activity) instance).getLayoutInflater()
            	    	.inflate(R.layout.field_text, null);
    			final EditText textField = (EditText) text_field_cont.getChildAt(0);
    			
    			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, Utils.dp2px(44));
				params.setMargins(0, Utils.dp2px(15), 0, 0);
				textField.setLayoutParams(params);
				
				textField.setHint(entry.get("name"));
    			
    			layout.addView(text_field_cont);
    			
    			/* listener */
    			textField.addTextChangedListener(new TextWatcher() {
					@Override
					public void afterTextChanged(Editable text) {
						if ( text.toString().isEmpty() ) {
							formData.remove(entry.get("key"));
						}
						else {
							formData.put(entry.get("key"), text.toString());
						}
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {}
    	        });
    		}
    		/* price */
			else if ( entry.get("type").matches("price|mixed") ) {

				final String field_key = entry.get("key");

				LinearLayout priceBar = (LinearLayout) ((Activity) instance).getLayoutInflater()
						.inflate(R.layout.field_price_search, null);

				priceBar.setLayoutParams(marginTop);

				final EditText price_from = (EditText) priceBar.findViewById(R.id.price);
				final EditText price_to = (EditText) priceBar.findViewById(R.id.price_to);

				// add currency
				final Spinner currency = (Spinner) priceBar.findViewById(R.id.currency);
				final TextView currency_one = (TextView) priceBar.findViewById(R.id.currency_one);

				price_from.setHint(entry.get("name") + " " + Lang.get("android_from"));
				price_to.setHint(entry.get("name") + " " + Lang.get("android_to"));

				/* listener */
				price_from.addTextChangedListener(new TextWatcher() {
					@Override
					public void afterTextChanged(Editable text) {
						HashMap<String, String> currency_val;
						if ( fieldItems.containsKey(field_key) && fieldItems.get(field_key).size() > 1 ) {
							currency_val = (HashMap<String, String>) currency.getSelectedItem();
						}
						else {
							currency_val = fieldItems.get(field_key).get(0);
						}

						String from = text.toString();
						String to =  price_to.getText().toString();
						if(from.isEmpty() && to.isEmpty() ) {
							formData.remove(entry.get("key"));
						}
						else {
							formData.put(entry.get("key"), from + "-" + to + "|" + currency_val.get("key"));
						}
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}
				});
				/* listener */
				price_to.addTextChangedListener(new TextWatcher() {
					@Override
					public void afterTextChanged(Editable text) {
						HashMap<String, String> currency_val;
						if ( fieldItems.containsKey(field_key) && fieldItems.get(field_key).size() > 1 ) {
							currency_val = (HashMap<String, String>) currency.getSelectedItem();
						}
						else {
							currency_val = fieldItems.get(field_key).get(0);
						}
						String from = price_from.getText().toString();
						String to = text.toString();
						if(from.isEmpty() && to.isEmpty() ) {
							formData.remove(entry.get("key"));
						}
						else {
							formData.put(entry.get("key"), from + "-" + to + "|" + currency_val.get("key"));
						}
					}

					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {
					}

					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {
					}
				});

				layout.addView(priceBar);

				if ( fieldItems.containsKey(field_key) && fieldItems.get(field_key).size() > 1 ) {
					SpinnerAdapter adapter = new SpinnerAdapter(Config.context, fieldItems.get(field_key), entry, formData, "search");
					// currency listener
					currency.setAdapter(adapter);
					currency.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {

							String tmp_unit = "";
							String from = price_from.getText().toString();
							String to = price_to.getText().toString();

							String price_val = from + "-" + to;

							if (fieldItems.get(field_key).get(position).get("key") != null) {
								tmp_unit = fieldItems.get(field_key).get(position).get("key");
							}
							if(!from.isEmpty() || !to.isEmpty()) {
								formData.put(field_key, price_val + "|" + tmp_unit);
							}
						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {
						}
					});
					currency.setVisibility(View.VISIBLE);
				}
				else {
					currency.setVisibility(View.GONE);
					currency_one.setVisibility(View.VISIBLE);
					currency_one.setText(fieldItems.get(field_key).get(0).get("name"));
					formData.put(field_key + "_currency", fieldItems.get(field_key).get(0).get("key"));
				}
    		}
    		/* number */
    		else if ( entry.get("type").equals("number") ) {
    			if ( entry.get("key").contains("zip") ) {
    				LinearLayout zipField = (LinearLayout) ((Activity) instance).getLayoutInflater()
            	    	.inflate(R.layout.field_zipcode, null);
    				
    				SpinnerAdapter adapter = new SpinnerAdapter(Config.context,	fieldItems.get(entry.get("key")), entry, formData, null);
					
    				/* distance spinner */
    				final Spinner distanceDropdown = (Spinner) zipField.findViewById(R.id.dropdown);
    				distanceDropdown.setAdapter(adapter);
    				
    				/* zipcode edit text */
    				final EditText zipEditText = (EditText) zipField.findViewById(R.id.zipcode);
    				
    				if ( Utils.getCacheConfig("zip_numeric_input").equals("1") ) {
    					zipEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
    				}
    				
					zipField.setLayoutParams(marginTop);
					layout.addView(zipField);
					
					/* listener */
					distanceDropdown.setOnItemSelectedListener(new OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
							/* save value */
							if ( position == 0 ) {
								formData.remove(entry.get("key"));
							}
							else {
								String distance = fieldItems.get(entry.get("key")).get(position).get("key");
								Editable zipcode = zipEditText.getText();
								String setData = distance +'-'+ zipcode; 
								formData.put(entry.get("key"), setData);
							}
						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {}
	    			});
					
					zipEditText.addTextChangedListener(new TextWatcher() {
						@Override
						public void afterTextChanged(Editable text) {
							if ( text.toString().isEmpty() ) {
								formData.remove(entry.get("key"));
							}
							else {
								int position = distanceDropdown.getSelectedItemPosition();
								String distance = fieldItems.get(entry.get("key")).get(position).get("key");
								String zipcode = text.toString();
								String setData = distance +'-'+ zipcode; 
								formData.put(entry.get("key"), setData);
							}
						}

						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {}
	    	        });
    			}
    			else {


					LinearLayout priceBar = (LinearLayout) ((Activity) instance).getLayoutInflater()
							.inflate(R.layout.field_price_search, null);

					priceBar.setLayoutParams(marginTop);

					final EditText price_from = (EditText) priceBar.findViewById(R.id.price);
					final EditText price_to = (EditText) priceBar.findViewById(R.id.price_to);

					// add currency
					Spinner currency = (Spinner) priceBar.findViewById(R.id.currency);
					TextView currency_one = (TextView) priceBar.findViewById(R.id.currency_one);
					currency.setVisibility(View.GONE);
					currency_one.setVisibility(View.GONE);

					price_from.setHint(entry.get("name") + " " + Lang.get("android_from"));
					price_to.setHint(entry.get("name") + " " + Lang.get("android_to"));

					/* listener */
					price_from.addTextChangedListener(new TextWatcher() {
						@Override
						public void afterTextChanged(Editable text) {

							String from = text.toString();
							String to =  price_to.getText().toString();
							if(from.isEmpty() && to.isEmpty() ) {
								formData.remove(entry.get("key"));
							}
							else {
								formData.put(entry.get("key"), from + "-" + to);
							}
						}

						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
						}
					});
					/* listener */
					price_to.addTextChangedListener(new TextWatcher() {
						@Override
						public void afterTextChanged(Editable text) {
							String from = price_from.getText().toString();
							String to = text.toString();
							if(from.isEmpty() && to.isEmpty() ) {
								formData.remove(entry.get("key"));
							}
							else {
								formData.put(entry.get("key"), from + "-" + to);
							}
						}

						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {
						}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
						}
					});

					layout.addView(priceBar);
    			}
    		}
    		/* checkbox */
    		else if ( entry.get("type").equals("checkbox") ) {
    			checkboxField(layout, entry, fieldItems, formData, instance, null, 0);
    		}
    		/* radio */
    		else if ( entry.get("type").equals("radio") ) {
    			radioField(layout, entry, fieldItems, formData, fieldViews, 0, instance);
    		}
    	}
		fieldViewsSearch.put(typeKey, fieldViews);
	}

	/**
	 * build submit form
	 * 
	 * @param layout - view to build form in
	 * @param fields - fields array
	 * @param formData - form data to apply changes in form to
	 * @param fieldItems - field items array
	 * @param instance - parent instance
	 * @param multilingualSpinner - the alternative layout for apply multilingual spinner
	 * @param fieldViews - map of the views related to each field in form, map key is key of the field 
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	public static void buildSubmitFields(LinearLayout layout, HashMap<String, HashMap<String, String>> fields,
										 final HashMap<String,String> formData, final HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
										 final Context instance, final MenuItem multilingualSpinner, HashMap<String, View> fieldViews, boolean hasGroups) {
		
		/* define margin top params */
		LayoutParams marginTop = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		int content_padding = hasGroups ? Utils.dp2px(15) : 0;
		marginTop.setMargins(0, Utils.dp2px(15), 0, 0);
		
		boolean has_multilingual = false;
        boolean onMap = false;
        if ( fields.equals("account_address_on_map") ) {
            HashMap<String, String> on_maps = fields.get("account_address_on_map");
            if ( on_maps.get("current").equals("1") ) {
                onMap = true;
            }
        }
		
		/* build fields */
		for (final Entry<String, HashMap<String, String>> entrySet : fields.entrySet()) {
			final HashMap<String, String> entry = entrySet.getValue();
			final String field_key = entry.get("key");
			
			if ( entry.get("multilingual").equals("1") && Config.cacheLanguagesWebs.size() > 1 ) {
				has_multilingual = true;
			}

			/* divider */
			if ( entry.get("type").equals("divider") ) {
				LinearLayout divider = (LinearLayout) Config.context.getLayoutInflater()
						.inflate(R.layout.fieldset, null);
				
				divider.setLayoutParams(marginTop);
				
				((TextView) divider.findViewById(R.id.divider_text)).setText(entry.get("name"));
				layout.addView(divider);
			}
    		/* select */
			else if ( entry.get("type").equals("select") ) {
    			LinearLayout spinner = (LinearLayout) selectField(entry, fieldItems.get(entry.get("key")), formData, null, content_padding, instance);
    			
    			layout.addView(spinner);
    			fieldViews.put(entrySet.getKey(), spinner);
    		}
    		/* yes-no */
    		else if ( entry.get("type").equals("bool") ) {
    			LinearLayout bool_field = (LinearLayout) booleanField(entry, formData, content_padding, instance);
    			layout.addView(bool_field);
    			fieldViews.put(entrySet.getKey(), bool_field);
    		}
    		/* text | textarea | number */
    		else if ( entry.get("type").matches("text|textarea|number") ) {
				if(field_key.equals("availability")) {
					availability(layout, entry, formData, fieldItems, fieldViews, content_padding, instance);
				}
				else if(field_key.equals("escort_tours")) {
					escortTours(layout, entry, formData, fieldItems, fieldViews, content_padding, instance);
				}
				else {
					// multilingual mode
					if ( !entry.get("type").equals("number") && entry.get("multilingual").equals("1") && Config.cacheLanguagesWebs.size() > 1 ) {
						for ( HashMap<String, String> m_lang : Config.cacheLanguagesWebs ) {
							LinearLayout text_field = (LinearLayout) textField(entry, formData, m_lang.get("key"), content_padding, instance);
							layout.addView(text_field);
							text_field.setTag("multilingual_"+m_lang.get("key"));

							fieldViews.put(entrySet.getKey()+"_"+m_lang.get("key"), text_field);

							if ( !m_lang.get("key").equals(Lang.getSystemLang()) ) {
								text_field.setVisibility(View.GONE);
							}
						}
					}
					// default mode
					else {
						LinearLayout text_field = (LinearLayout) textField(entry, formData, null, content_padding, instance);
						layout.addView(text_field);
						fieldViews.put(entrySet.getKey(), text_field);
					}
				}
    		}
    		/* price/mixed */
    		else if ( entry.get("type").matches("price|mixed") ) {
				if(field_key.equals("escort_rates")) {
					escortRates(layout, entry, formData, fieldItems, fieldViews, content_padding, instance);
				}
				else {
					final LinearLayout price = (LinearLayout) ((Activity) instance).getLayoutInflater()
							.inflate(R.layout.field_price, null);

					LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
					params.setMargins(content_padding, Utils.dp2px(15), content_padding, 0);
					price.setLayoutParams(params);

					final EditText number = (EditText) price.findViewById(R.id.price);
					final Spinner currency = (Spinner) price.findViewById(R.id.currency);

					number.setHint(entry.get("name"));

					layout.addView(price);
					fieldViews.put(field_key, price);

					// number listener
					number.addTextChangedListener(new TextWatcher() {
						@Override
						public void afterTextChanged(Editable text) {
							collectCombo(formData, field_key, fieldItems, number, currency);
						}

						@Override
						public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

						@Override
						public void onTextChanged(CharSequence s, int start, int before, int count) {
							number.setBackgroundResource(R.drawable.edit_text);
							unsetError(price);

							String[] splited = s.toString().split("\\.");
							if ( splited.length > 1 && splited[1].length() > 2 ) {
								String formated = String.format("%.2f", Double.parseDouble(s.toString()));
								number.setText(formated);
								number.setSelection(formated.length());
							}
						}
					});

					/* set current number */
					if (entry.containsKey("current") && !entry.get("current").isEmpty()) {
						String[] exploded = entry.get("current").split("\\|");
						number.setText(exploded[0]);
					}

					// currency/units spinner
					if ( fieldItems.containsKey(field_key) && fieldItems.get(field_key).size() > 1 ) {
						SpinnerAdapter adapter = new SpinnerAdapter(Config.context, fieldItems.get(field_key), entry, formData, null);

						// currency listener
						currency.setAdapter(adapter);
						currency.setOnItemSelectedListener(new OnItemSelectedListener() {
							@Override
							public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
								collectCombo(formData, field_key, fieldItems, number, currency);
							}

							@Override
							public void onNothingSelected(AdapterView<?> arg0) {
							}
						});

						/* set current spinner item */
						if (entry.containsKey("current") && !entry.get("current").isEmpty()) {
							String[] exploded = entry.get("current").split("\\|");
							int selected = adapter.getPosition(exploded[1], "key");
							currency.setSelection(selected);
						}
						else if (entry.get("current").isEmpty() && entry.containsKey("default") && !entry.get("default").isEmpty()) {
							int selected = adapter.getPosition(entry.get("default"), "key");
							currency.setSelection(selected);
						}
					}
					else if ( fieldItems.containsKey(field_key) && fieldItems.get(field_key).size() == 1 ) {
						currency.setVisibility(View.GONE);
						ViewGroup currency_parent = (ViewGroup) currency.getParent();

						LayoutParams clp = new LayoutParams(Utils.dp2px(100), LayoutParams.WRAP_CONTENT);
						clp.setMargins(Utils.dp2px(15), 0, 0, 0);

						TextView single_item = new TextView(instance);
						single_item.setText(fieldItems.get(field_key).get(0).get("name"));

						currency_parent.addView(single_item, clp);
					}
					else {
						currency.setVisibility(View.INVISIBLE);
					}
                }
    		}
    		/* checkbox */
    		else if ( entry.get("type").equals("checkbox") ) {
    			checkboxField(layout, entry, fieldItems, formData, instance, fieldViews, content_padding);
    		}
    		/* radio */
    		else if ( entry.get("type").equals("radio") ) {
    			radioField(layout, entry, fieldItems, formData, fieldViews, content_padding, instance);
    		}
    		/* date */
    		else if ( entry.get("type").equals("date") ) {
    			dateField(layout, entry, fieldItems, formData, fieldViews, content_padding, instance);
    		}
    		/* accept */
    		else if ( entry.get("type").equals("accept") ) {
				acceptField(layout, entry, fieldItems, formData, fieldViews, content_padding, instance);
    		}
    		/* phone */
    		else if ( entry.get("type").equals("phone") ) {
    			final LinearLayout phone = (LinearLayout) Config.context.getLayoutInflater()
            	    	.inflate(R.layout.field_phone, null); // phone field container

    			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				params.setMargins(content_padding, Utils.dp2px(15), content_padding, 0);
				phone.setLayoutParams(params);

				/* get params */
				String[] options = entry.get("data").split("\\|");
				InputFilter[] filter = new InputFilter[1];
				
				/* code field */
				final EditText phone_code = (EditText) phone.findViewById(R.id.code);
				if ( options[0].equals("0") ) {
					phone_code.setVisibility(View.GONE);
					phone.findViewById(R.id.plus_sign).setVisibility(View.GONE);
					phone.findViewById(R.id.area_divider).setVisibility(View.GONE);
				}
				
				/* area field */
				final EditText phone_area = (EditText) phone.findViewById(R.id.area);
				filter[0] = new InputFilter.LengthFilter(Integer.parseInt(options[1]));			
				phone_area.setFilters(filter);
				
				/* number field */
				final EditText phone_number = (EditText) phone.findViewById(R.id.number);
				phone_number.setHint(entry.get("name"));
				filter[0] = new InputFilter.LengthFilter(Integer.parseInt(options[2]));			
				phone_number.setFilters(filter);
				
				/* set current values */
				if ( entry.containsKey("current") ) {
					formData.put(entry.get("key"), entry.get("current"));
					
					Pattern pattern = Pattern.compile("(c:([0-9]+))?\\|?(a:([0-9]+))?\\|(n:([0-9]+))?\\|?(e:([0-9]+))?");
					Matcher matcher = pattern.matcher(entry.get("current"));
					if ( matcher.matches() ) {
						phone_code.setText(matcher.group(2));
						phone_area.setText(matcher.group(4));
						phone_number.setText(matcher.group(6));
					}
				}
				
    			layout.addView(phone);
    			fieldViews.put(entrySet.getKey(), phone);
    			
    			/* set listeners */
    			TextWatcher phone_listener = new TextWatcher(){
					@Override
					public void afterTextChanged(Editable s) {
						unsetError(phone);
						collectPhone(phone_code, phone_area, phone_number, formData, entry.get("key"));
					}
					@Override
					public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
					@Override
					public void onTextChanged(CharSequence s, int start, int before, int count) {}
				};
    			
    			phone_code.addTextChangedListener(phone_listener);
    			phone_area.addTextChangedListener(phone_listener);
    			phone_number.addTextChangedListener(phone_listener);
    		}
    	}

        fieldViewsForm = fieldViews;
        if ( onMap == true ) {
            disabledFields("1");
        }

		/* multilingual field hanler */
		if ( multilingualSpinner != null && has_multilingual && Config.cacheLanguagesWebs.size() > 1 ) {
			/* set timeout 0.25 second */
	    	CountDownTimer timer = new CountDownTimer(250, 250) {
				public void onTick(long millisUntilFinished) {}
				
				public void onFinish() {
					multilingualSpinner.setVisible(true);
				}
			};
			timer.start();
		}
	}
	
	/**
	 * validate form data based on "is field required" information
	 * 
	 * @param formData - the data entered by the user in form
	 * @param fields - fields information array
	 *
	 * @return bool - true is form valid
	 */
	public static boolean validate(HashMap<String,String> formData, LinkedHashMap<String, HashMap<String,String>> fields,
			HashMap<String, View> fieldViews) {

		boolean valid = true;
		
		for (Entry<String, HashMap<String, String>> entry : fields.entrySet()) {
			String field_key = entry.getKey();
			String field_value = formData.containsKey(field_key) ? formData.get(field_key) : "";
			HashMap<String, String> field = entry.getValue();
			
			if ( fields.containsKey(field_key) ) {
                boolean required = true;
                if ( use_on_map.equals("1") && Account.onMapFields.contains(field_key)) {
                    required = false;
                }

				/* text fields */
				if ( field.get("type").matches("text|textarea|number") ) {
					if(field.get("key").equals("availability")) {
						String day_key = "availability_";
						for(int i=0; i<7; i++) {
							if(formData.containsKey(day_key+i) && formData.get(day_key+i).length()<4 && formData.get(day_key+i).length()>1) {
								LinearLayout field_view_cont = (LinearLayout) fieldViews.get(field_key);
								setError(field_view_cont, "dialog_selector_is_empty");
								valid = false;
							}
						}
					}
					else if(field_key.equals("escort_tours")) {
						boolean escortR = false;

						for (String key : formData.keySet()) {
							int index = key.indexOf(field_key);
							if (index >= 0 && !key.equals(field_key)) {
								HashMap<String, String> curr =  JSONParser.parseJson(formData.get(key));
								if(!curr.containsKey("location") || !curr.containsKey("to") || !curr.containsKey("from")) {
									escortR = true;
								}
							}
						}
						if(escortR) {
							LinearLayout view_cont = (LinearLayout) fieldViews.get(field_key);
							setError(view_cont, "android_dialog_field_is_empty");
							valid = false;
						}

					}
					/* multilingual */
					else if ( field.get("multilingual").equals("1") && Config.cacheLanguagesWebs.size() > 1 ) {
						boolean multi_error = true;
						EditText field_view_multi = null;
						LinearLayout field_view_multi_cont = null;
						for ( HashMap<String, String> m_lang : Config.cacheLanguagesWebs ) {

							String multilingual_field_key = field_key+"_"+m_lang.get("key");
							LinearLayout field_view_cont = (LinearLayout) fieldViews.get(multilingual_field_key);
							EditText field_view = (EditText) field_view_cont.getChildAt(0);
							field_value = formData.containsKey(multilingual_field_key) ? formData.get(multilingual_field_key) : "";

							if (!field_value.isEmpty()) {
								multi_error = false;
							}
							if ( m_lang.get("key").equals(Lang.getSystemLang()) ) {
								field_view_multi = field_view;
								field_view_multi_cont = field_view_cont;
							}
	    				}
						if ( multi_error == true && field.get("required").equals("1") && required == true) {
							field_view_multi.setBackgroundResource(R.drawable.edit_text_error);
							setError(field_view_multi_cont, "dialog_multilingual_field_is_empty");
							valid = false;
						}
					}
					/* standard */
					else {
						LinearLayout field_view_cont = (LinearLayout) fieldViews.get(field_key);
						EditText field_view = (EditText) field_view_cont.getChildAt(0);

						if (field.get("required").equals("1") && field_value.isEmpty() && required == true) {
							field_view.setBackgroundResource(R.drawable.edit_text_error);
							setError(field_view_cont, "android_dialog_field_is_empty");
							valid = false;
						} else if (field.get("data").equals("isEmail")) {
							Matcher matcher = Pattern.compile("(.+@.+\\.[a-z]+)").matcher(field_value);

							if (!matcher.matches()) {
								field_view.setBackgroundResource(R.drawable.edit_text_error);
								setError(field_view_cont, "bad_email");
								valid = false;
							}
						}
					}
				}
				/* select field */
				else if ( field.get("type").equals("select") ) {

                    if ( field.get("required").equals("1") && field_value.isEmpty() && required == true ) {

						LinearLayout spinner_view_cont = (LinearLayout) fieldViews.get(field_key);
						Spinner spinner_view = (Spinner) spinner_view_cont.getChildAt(0);
						setError(spinner_view_cont, "dialog_selector_is_empty");
						
						spinner_view.setBackgroundResource(R.drawable.spinner_error);
						valid = false;
					}
				}
				/* select field */
				else if ( field.get("type").equals("phone") ) {
					LinearLayout phone_view_cont = (LinearLayout) fieldViews.get(field_key);
					EditText number_view = (EditText) phone_view_cont.findViewById(R.id.number);
					EditText number_area_view = (EditText) phone_view_cont.findViewById(R.id.area);
                    if ( field.get("required").equals("1")  && number_view.getText().toString().isEmpty() && required == true ) {
						setError(phone_view_cont, "android_dialog_field_is_empty");
						valid = false;
					}
					else if (!number_area_view.getText().toString().isEmpty()
                            && number_area_view.getText().toString().substring(0,1).equals("0")
                            && number_area_view.getText().length()==1) {
						setError(phone_view_cont, "android_phone_null");
						valid = false;
					}
				}
                /* accept field */
                else if ( field.get("type").equals("accept") ) {

                    if ( field.get("required").equals("1") && field_value.isEmpty() && required == true ) {
                        LinearLayout view_cont = (LinearLayout) fieldViews.get(field_key);
                        setError(view_cont, "android_dialog_field_is_empty");

                        TextView error = (TextView) view_cont.findViewWithTag("error_message");
                        error.setText(Lang.get("android_dialog_field_is_empty_accept"));
                        error.setVisibility(View.VISIBLE);
                        valid = false;
                    }
                }/* checkbox field */
                else if ( field.get("type").equals("checkbox") ) {

                    if ( field.get("required").equals("1") && field_value.isEmpty() && required == true ) {
                        LinearLayout view_cont = (LinearLayout) fieldViews.get(field_key);
                        setError(view_cont, "android_dialog_field_is_empty");

                        TextView error = (TextView) view_cont.findViewWithTag("error_message");
                        error.setText(Lang.get("android_dialog_field_is_empty"));
                        error.setVisibility(View.VISIBLE);
                        valid = false;
                    }
                }
				/* date field */
				else if ( field.get("type").equals("date")) {

                    LinearLayout view_cont = (LinearLayout) fieldViews.get(field_key);
                    EditText view = (EditText) view_cont.findViewWithTag("start");

                    if ( field.get("required").equals("1") && field_value.isEmpty() && required == true ) {
                        view.setBackgroundResource(R.drawable.edit_text_error);
                        setError(view_cont, "android_dialog_field_is_empty");
                        valid = false;
                    }
                    if (field.get("default").equals("multi")) {
                        String multi_key = field_key + "_multi";
                        String multi_value = formData.containsKey(multi_key) ? formData.get(multi_key) : "";
                        EditText view_end = (EditText) view_cont.findViewWithTag("end");
                        if ( field.get("required").equals("1") && multi_value.isEmpty() && required == true ) {
                            view_end.setBackgroundResource(R.drawable.edit_text_error);
                            setError(view_cont, "android_dialog_field_is_empty");
                            valid = false;
                        }
                    }
				}
				/* price and combo fields */
				else if ( field.get("type").matches("price|mixed") ) {

                    if(field_key.equals("escort_rates")) {
						boolean escortR = false;
                        for (String key : formData.keySet()) {
							int index = key.indexOf("escort_rates");
							if (index >= 0 && !key.equals(field_key)) {
								String[] exp = formData.get(key).split("\\|");
								if(exp[0].isEmpty() || exp[2].isEmpty()) {
									escortR = true;
								}
							}
                        }
                        if(escortR) {
							LinearLayout view_cont = (LinearLayout) fieldViews.get(field_key);
							EditText view = (EditText) view_cont.findViewWithTag("number");
							setError(view_cont, "android_dialog_field_is_empty");

							view.setBackgroundResource(R.drawable.edit_text_error);
							valid = false;
						}
                    }
                    else {

                        String[] exp = field_value.split("\\|");

                        if (field.get("required").equals("1") && exp[0].isEmpty() && required == true) {
                            LinearLayout view_cont = (LinearLayout) fieldViews.get(field_key);
                            EditText view = (EditText) view_cont.findViewWithTag("number");
                            setError(view_cont, "android_dialog_field_is_empty");

                            view.setBackgroundResource(R.drawable.edit_text_error);
                            valid = false;
                        }
                    }
				}
			}
		}
		return valid;
	}	
	
    /**
     * disabled filed if use account address on map
     *
     * @param checker - account address on map
     */

    private static void disabledFields( String checker ) {

        for (int i = 0; i < Account.onMapFields.size(); i++) {
            if (fieldViewsForm.containsKey(Account.onMapFields.get(i))) {
            LinearLayout views = (LinearLayout) fieldViewsForm.get(Account.onMapFields.get(i));

            if( checker.equals("1")) {
                if (views.getChildAt(0) instanceof Spinner) {
                    Spinner view = (Spinner) views.getChildAt(0);
                    view.setEnabled(false);
                }
                else if (views.getChildAt(0) instanceof TextView) {
                    TextView view = (TextView) views.getChildAt(0);
                    view.setEnabled(false);
                }
                else if (views.getChildAt(0) instanceof EditText) {
                    EditText view = (EditText) views.getChildAt(0);
                    view.setEnabled(false);
                }
            }
            else {
                if (views.getChildAt(0) instanceof Spinner) {
                    Spinner view = (Spinner) views.getChildAt(0);
                    view.setEnabled(true);
                }
                else if (views.getChildAt(0) instanceof TextView) {
                    TextView view = (TextView) views.getChildAt(0);
                    view.setEnabled(true);
                }
                else if (views.getChildAt(0) instanceof EditText) {
                    EditText view = (EditText) views.getChildAt(0);
                    view.setEnabled(true);
                }
            }
            }
        }
    }

	private static void availability(LinearLayout layout,
									final HashMap<String, String> field,
									final HashMap<String, String> formData,
									final HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
									final HashMap<String, View> fieldViews,
									int padding,
									Context instance) {

        formData.put("availability", "1");
		String day_key = "availability_days";
		final String time_range_key = "availability_time_range";
		if(fieldItems.containsKey(day_key) && fieldItems.get(day_key).size() > 1) {

			for(int i=0; i<fieldItems.get(day_key).size(); i++) {
				final HashMap<String, String> tmpDays = fieldItems.get(day_key).get(i);

				final String field_key = field.get("key")+"_"+tmpDays.get("key");
				final LinearLayout availability = (LinearLayout) ((Activity) instance).getLayoutInflater()
						.inflate(R.layout.field_years_search, null);

				LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				params.setMargins(padding, Utils.dp2px(15), padding, 0);
				availability.setLayoutParams(params);


				final Spinner from = (Spinner) availability.findViewById(R.id.from);
				final Spinner to = (Spinner) availability.findViewById(R.id.to);

				final ArrayList<HashMap<String, String>> tmpItems = fieldItems.get(time_range_key);
				ArrayList<HashMap<String, String>> tmpRange = new ArrayList<HashMap<String, String>>(tmpItems);
				tmpRange.remove(0);
                HashMap<String, String> tmp = new HashMap<String, String>();
                tmp.put("key", "");
                tmp.put("name", tmpDays.get("name"));
				tmpRange.add(0, tmp);

				SpinnerAdapter adapter = new SpinnerAdapter(Config.context, tmpRange, field, formData, "search");
				from.setAdapter(adapter);
				from.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
						HashMap<String, String> currency_val = (HashMap<String, String>) to.getSelectedItem();
						String val = fieldItems.get(time_range_key).get(position).get("key");
						formData.put(field_key, val + "-" + currency_val.get("key"));
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});
				from.setVisibility(View.VISIBLE);

				SpinnerAdapter adapterTo = new SpinnerAdapter(Config.context, fieldItems.get(time_range_key), field, formData, "search");
				to.setAdapter(adapterTo);
				to.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
						HashMap<String, String> currency_val = (HashMap<String, String>) from.getSelectedItem();
						String val = fieldItems.get(time_range_key).get(position).get("key");
						formData.put(field_key, currency_val.get("key")+"-"+val);
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});
				from.setVisibility(View.VISIBLE);


				if ( AddListing.edit_listing_data!=null && AddListing.edit_listing_data.containsKey(field_key) ) {
					String current_value = AddListing.edit_listing_data.get(field_key);
					if(!current_value.equals("0")) {
						if(current_value.substring(0,2)!=null) {
							int selected = adapter.getPosition(current_value.substring(0, 2), "key");
							from.setSelection(selected);
							adapter.currentValue = current_value.substring(0, 2);
						}

						if(current_value.substring(2,4)!=null) {
							int selectedTo = adapterTo.getPosition(current_value.substring(2, 4), "key");
							to.setSelection(selectedTo);
							adapterTo.currentValue = current_value.substring(2, 4);
						}
					}
				}


				layout.addView(availability);
				if ( fieldViews != null ) {
					fieldViews.put(field.get("key"), availability);
				}
			}
		}

	}

	private static void escortTours(final LinearLayout layout,
									final HashMap<String, String> field,
									final HashMap<String, String> formData,
									final HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
									final HashMap<String, View> fieldViews,
									final int padding,
									final Context instance) {

		formData.put(field.get("key"), "1");

		final LinearLayout escort_tours = (LinearLayout) ((Activity) instance).getLayoutInflater()
				.inflate(R.layout.field_escort_tours, null);

		final int[] escortIndex = {0};
		final LinearLayout escort_box = escort_tours.findViewById(R.id.escort_tours);
		TextView add_tour = escort_tours.findViewById(R.id.add_tour);

        final GeoDataClient mGeoDataClient = Places.getGeoDataClient(Config.context, null);


        if (!field.get("current").isEmpty()) {
			ArrayList<HashMap<String, String>> currList =  JSONParser.parseJsontoArrayList(field.get("current"));

			for (int i = 0; i < currList.size(); i++) {
				escortIndex[0] = addEscortTour(escortIndex[0], escort_box, currList.get(i), field, formData, instance, mGeoDataClient);
			}
		}

		add_tour.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				escortIndex[0] = addEscortTour(escortIndex[0], escort_box, null,  field, formData, instance, mGeoDataClient);
			}
		});

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(padding, Utils.dp2px(15), padding, 0);
		escort_tours.setLayoutParams(params);

		layout.addView(escort_tours);
		if ( fieldViews != null ) {
			fieldViews.put(field.get("key"), escort_tours);
		}
	}

	private static int addEscortTour(final int escortIndex,
									 final LinearLayout layout,
									 final HashMap<String, String> current,
									 final HashMap<String, String> field,
									 final HashMap<String, String> formData,
									 Context instance,
									 final GeoDataClient mGeoDataClient) {

		final LinearLayout escort_tour = (LinearLayout) ((Activity) instance).getLayoutInflater()
				.inflate(R.layout.field_escort_tour_item, null);

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, Utils.dp2px(15));
		escort_tour.setLayoutParams(params);

		final HashMap<String, String> locationItem = new HashMap<String, String>();
		final String field_key = field.get("key");
		final AutoCompleteTextView autocomplete = (AutoCompleteTextView) escort_tour.findViewById(R.id.autocomplete);
		final EditText startDate = (EditText) escort_tour.findViewById(R.id.startDate);
		final EditText endDate = (EditText) escort_tour.findViewById(R.id.endDate);
		final ImageView remove_item = (ImageView) escort_tour.findViewById(R.id.remove_item);
		final LatLngBounds BOUNDS = null;
		final PlaceAutocompleteAdapter mAdapter = new PlaceAutocompleteAdapter(Config.context, mGeoDataClient, BOUNDS,
				null);

		if ( current!=null ) {
			locationItem.put("place_id", current.get("Place_ID"));
			locationItem.put("location", current.get("Location"));
			locationItem.put("latitude", current.get("Latitude"));
			locationItem.put("longitude", current.get("Longitude"));
			autocomplete.setText(current.get("Location"));
		}

		autocomplete.setAdapter(mAdapter);
		autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				AutocompletePrediction item = mAdapter.getItem(position);
				String placeId = item.getPlaceId();
				locationItem.put("place_id", item.getPlaceId());
				locationItem.put("location", item.getFullText(null).toString());

				mGeoDataClient.getPlaceById(placeId).addOnCompleteListener(new OnCompleteListener<PlaceBufferResponse>() {
					@Override
					public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
						if (task.isSuccessful()) {
							PlaceBufferResponse places = task.getResult();
							Place myPlace = places.get(0);
							locationItem.put("latitude", myPlace.getLatLng().latitude + "");
							locationItem.put("longitude", myPlace.getLatLng().longitude + "");

							collectTours(escortIndex, field_key, formData, locationItem);
							places.release();
						} else {
							Log.d("FD", "Place not found.");
						}
					}
				});
			}
		});


		final Calendar c = Calendar.getInstance();
		int sYear = c.get(Calendar.YEAR);
		int sMonth = c.get(Calendar.MONTH);
		int sDay = c.get(Calendar.DAY_OF_MONTH);

		if ( current != null && !current.get("From").isEmpty()) {
			String[] separated = current.get("From").split("-");
			sYear = Integer.parseInt(separated[0]);
			sMonth = Integer.parseInt(separated[1]) - 1;
			sDay = Integer.parseInt(separated[2]);
			locationItem.put("from", current.get("From"));
			startDate.setText(current.get("From"));
		}
		final DatePickerDialog dpsingle = new DatePickerDialog(instance,
				new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year,
										  int monthOfYear, int dayOfMonth) {
						c.set(year, monthOfYear, dayOfMonth);
						SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
						startDate.setText(dateFormatter.format(c.getTime()));
						locationItem.put("from", dateFormatter.format(c.getTime()) + "");

						collectTours(escortIndex, field_key, formData, locationItem);
						startDate.setBackgroundResource(R.drawable.edit_text);
					}
				}, sYear, sMonth, sDay);

		startDate.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				dpsingle.show();
				return true;
			}
		});


		int mYear = c.get(Calendar.YEAR);
		int mMonth = c.get(Calendar.MONTH);
		int mDay = c.get(Calendar.DAY_OF_MONTH);

		if ( current != null && !current.get("To").isEmpty() ) {
			String[] separated = current.get("To").split("-");
			mYear = Integer.parseInt(separated[0]);
			mMonth = Integer.parseInt(separated[1]) - 1;
			mDay = Integer.parseInt(separated[2]);
			locationItem.put("to", current.get("To"));
			endDate.setText(current.get("To"));
		}

		final DatePickerDialog dpmulti = new DatePickerDialog(instance,
				new DatePickerDialog.OnDateSetListener() {
					@Override
					public void onDateSet(DatePicker view, int year,
										  int monthOfYear, int dayOfMonth) {
						c.set(year, monthOfYear, dayOfMonth);
						SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
						endDate.setText(dateFormatter.format(c.getTime()));
						locationItem.put("to", dateFormatter.format(c.getTime()) + "");
						collectTours(escortIndex, field_key, formData, locationItem);
						endDate.setBackgroundResource(R.drawable.edit_text);
					}
				}, mYear, mMonth, mDay);

		endDate.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				dpmulti.show();
				return true;
			}
		});

		//sumilate post
		if ( current!=null ) {
			collectTours(escortIndex, field_key, formData, locationItem);
		}

		remove_item.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				layout.removeView(escort_tour);
				formData.remove(field_key+"_"+escortIndex);
			}
		});

		layout.addView(escort_tour);
		return escortIndex + 1;
	}

	private static void escortRates(final LinearLayout layout,
                                    final HashMap<String, String> field,
                                    final HashMap<String, String> formData,
                                    final HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
                                    final HashMap<String, View> fieldViews,
                                    final int padding,
                                    final Context instance) {

		formData.put(field.get("key"), "1");

        final LinearLayout escort_rates = (LinearLayout) ((Activity) instance).getLayoutInflater()
				.inflate(R.layout.field_escort_rates, null);

		final int[] escortIndex = {0};
        final LinearLayout escort_box = escort_rates.findViewById(R.id.escort_rates);
		TextView add_rate = escort_rates.findViewById(R.id.add_rate);

		if (!field.get("current").isEmpty()) {
			ArrayList<HashMap<String, String>> currList =  JSONParser.parseJsontoArrayList(field.get("current"));

			for (int i = 0; i < currList.size(); i++) {
				escortIndex[0] = addEscortRate(escortIndex[0], escort_box, currList.get(i), field, formData, fieldItems, instance);
			}
		}

		add_rate.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				escortIndex[0] = addEscortRate(escortIndex[0], escort_box, null,  field, formData, fieldItems, instance);
			}
		});

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(padding, Utils.dp2px(15), padding, 0);
		escort_rates.setLayoutParams(params);

		layout.addView(escort_rates);
		if ( fieldViews != null ) {
			fieldViews.put(field.get("key"), escort_rates);
		}
	}

	private static int addEscortRate(final int escortIndex,
									 final LinearLayout layout,
									 final HashMap<String, String> current,
									 final HashMap<String, String> field,
									 final HashMap<String, String> formData,
									 final HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
									 Context instance) {

        final LinearLayout escort_rates = (LinearLayout) ((Activity) instance).getLayoutInflater()
                .inflate(R.layout.field_escort_rate_item, null);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, Utils.dp2px(15));
        escort_rates.setLayoutParams(params);

		final String field_key = field.get("key");
		final EditText price = (EditText) escort_rates.findViewById(R.id.price);
		final EditText custom_text = (EditText) escort_rates.findViewById(R.id.custom);
		final Spinner rates = (Spinner) escort_rates.findViewById(R.id.rates);
		final Spinner currency = (Spinner) escort_rates.findViewById(R.id.currency);
		ImageView remove_item = (ImageView) escort_rates.findViewById(R.id.remove_item);
		ImageView remove_custom = (ImageView) escort_rates.findViewById(R.id.remove_custom);
		final LinearLayout custom_box = (LinearLayout) escort_rates.findViewById(R.id.custom_box);

		final String currency_key = "rates_value";

		price.setHint(field.get("name"));
		// set current
		if(current!=null && !current.get("price").isEmpty()) {
			price.setText(current.get("price"));
		}
		// number listener
		price.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable text) {
				collectEscortRates(escortIndex, field_key, formData, fieldItems, rates, custom_text, price, currency);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				price.setBackgroundResource(R.drawable.edit_text);

				String[] splited = s.toString().split("\\.");
				if ( splited.length > 1 && splited[1].length() > 2 ) {
					String formated = String.format("%.2f", Double.parseDouble(s.toString()));
					price.setText(formated);
					price.setSelection(formated.length());
				}
			}
		});


		// set current
		if(current!=null && current.get("rate").equals("*cust0m*") && !current.get("custom_rate").isEmpty()) {
			custom_text.setText(current.get("custom_rate"));
		}
		custom_text.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable text) {
				collectEscortRates(escortIndex, field_key, formData, fieldItems, rates, custom_text, price, currency);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});

		// currency/units spinner
		if ( fieldItems.containsKey(currency_key) && fieldItems.get(currency_key).size() > 1 ) {
			SpinnerAdapter adapter = new SpinnerAdapter(Config.context, fieldItems.get(currency_key), field, formData, null);

			// currency listener
			currency.setAdapter(adapter);
			currency.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
					collectEscortRates(escortIndex, field_key, formData, fieldItems, rates, custom_text, price, currency);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});

			/* set current spinner item */
			if(current!=null && !current.get("currency").isEmpty() ) {
				int selected = adapter.getPosition(current.get("currency"), "key");
				currency.setSelection(selected);
			}
		}
		else if ( fieldItems.containsKey(currency_key) && fieldItems.get(currency_key).size() == 1 ) {
			currency.setVisibility(View.GONE);
			ViewGroup currency_parent = (ViewGroup) currency.getParent();

			LayoutParams clp = new LayoutParams(Utils.dp2px(100), LayoutParams.WRAP_CONTENT);
			clp.setMargins(Utils.dp2px(15), 0, 0, 0);

			TextView single_item = new TextView(instance);
			single_item.setText(fieldItems.get(currency_key).get(0).get("name"));

			currency_parent.addView(single_item, 2, clp);
		}
		else {
			currency.setVisibility(View.GONE);
		}


		//  rates
		if ( fieldItems.containsKey(field_key) && fieldItems.get(field_key).size() >= 1 ) {
			final SpinnerAdapter adapterRates = new SpinnerAdapter(Config.context, fieldItems.get(field_key), field, formData, null);

			// currency listener
			rates.setAdapter(adapterRates);
			rates.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View view, int position, long arg3) {
					collectEscortRates(escortIndex, field_key, formData, fieldItems, rates, custom_text, price, currency);

					HashMap<String, String> item = (HashMap<String, String>) arg0.getItemAtPosition(position);
					if(item.get("key").equals("*cust0m*")) {
						rates.setVisibility(View.GONE);
						custom_box.setVisibility(View.VISIBLE);
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				}
			});

			/* set current spinner item */
			if(current!=null && !current.get("rate").isEmpty() ) {
				int selected = adapterRates.getPosition(current.get("rate"), "key");
				rates.setSelection(selected);
			}
		}
		else {
			rates.setVisibility(View.INVISIBLE);
		}

		remove_custom.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				custom_text.setText("");
				rates.setSelection(0);
				rates.setVisibility(View.VISIBLE);
				custom_box.setVisibility(View.GONE);
			}
		});
		remove_item.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				layout.removeView(escort_rates);
				formData.remove(field_key+"_"+escortIndex);
			}
		});

		layout.addView(escort_rates);
		return escortIndex + 1;
	}

	private static View textField( final HashMap<String, String> field,	final HashMap<String,String> formData, final String langCode, int padding, Context instance) {
		final LinearLayout text_field_cont = (LinearLayout) ((Activity) instance).getLayoutInflater()
    	    	.inflate(field.get("type").equals("textarea") ? R.layout.field_textarea : R.layout.field_text, null);
		
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(padding, Utils.dp2px(15), padding, 0);
		text_field_cont.setLayoutParams(params);
		
		final EditText textField = (EditText) text_field_cont.getChildAt(0);  
		
		int height = field.get("type").equals("textarea") ?  LayoutParams.WRAP_CONTENT : Utils.dp2px(44);
		int width = LayoutParams.MATCH_PARENT;
		
		if ( field.get("type").equals("number") || field.get("key").matches(".*(zip|postal).*") ) {
			width = Utils.dp2px(short_field_width);
			
			int max_lenght = 14;
			if ( !field.get("data").isEmpty() ) {
				max_lenght = Integer.parseInt(field.get("data")); 
			}
			
			InputFilter[] input_filter = new InputFilter[1];
			input_filter[0] = new InputFilter.LengthFilter(max_lenght);
			textField.setFilters(input_filter);
		}
		
		params = new LayoutParams(width, height);
		textField.setLayoutParams(params);
		textField.setHint(field.get("name"));

		/* set current value */
		if ( field.containsKey("current") && !field.get("current").isEmpty() && !field.get("current").equals("0") ) {

			if ( langCode != null && field.get("multilingual").equals("1") ) {
				Pattern pattern = Pattern.compile(".*(\\{\\|"+langCode+"\\|\\})(.*)(\\{\\|/"+langCode+"\\|\\}).*", Pattern.DOTALL);
				Matcher matcher = pattern.matcher(field.get("current"));
				if ( matcher.matches() ) {
					textField.setText(field.get("data").equals("html") ? Html.fromHtml(matcher.group(2)) : matcher.group(2));
					formData.put(field.get("key")+"_"+langCode, matcher.group(2));
				}
				else {
					Pattern pattern1 = Pattern.compile(".*(\\{\\|([a-zA-Z]{2})\\|\\}).*", Pattern.DOTALL);
					Matcher matcher1 = pattern1.matcher(field.get("current"));
					if (!matcher1.matches() && Config.cacheLanguagesWebsDefault.equals(langCode)) {
						textField.setText(field.get("data").equals("html") ? Html.fromHtml(field.get("current")) : field.get("current"));
						formData.put(field.get("key") + "_" + langCode, field.get("current"));
					}
				}
			}
			else {
				textField.setText(field.get("data").equals("html") ? Html.fromHtml(field.get("current")) : field.get("current"));
				formData.put(field.get("key"), field.get("current"));
			}
		}
        /* set default value */
		else if ( field.containsKey("default") ) {
		    if ( !field.get("default").isEmpty() ) {
                if ( langCode != null && field.get("multilingual").equals("1") ) {
                    Pattern pattern = Pattern.compile(".*(\\{\\|"+langCode+"\\|\\})(.*)(\\{\\|/"+langCode+"\\|\\}).*", Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(field.get("default"));

                    if ( matcher.matches() ) {
                        textField.setText(field.get("data").equals("html") ? Html.fromHtml(matcher.group(2)) : matcher.group(2));
                        formData.put(field.get("key")+"_"+langCode, matcher.group(2));
                    }
                }
                else {
                    textField.setText(field.get("data").equals("html") ? Html.fromHtml(field.get("default")) : field.get("default"));
                    formData.put(field.get("key"), field.get("default"));
                }
            }
		}
		
		/* set flag */
		if ( langCode != null && field.get("multilingual").equals("1") ) {
			try {
				textField.setCompoundDrawablesWithIntrinsicBounds(null, null, Utils.getFlag(langCode), null);
			}
			catch (Exception e) {}
		}
		
		/* change input type for numeric fields */
		if ( field.get("type").equals("number") ) {
			if ( field.get("key").contains("zip") ) {
				if ( Utils.getCacheConfig("zip_numeric_input").equals("1") ) {
					textField.setInputType(InputType.TYPE_CLASS_NUMBER);
				}
			}
			else {
				textField.setInputType(InputType.TYPE_CLASS_NUMBER);
			}
		}

        /* region field */
        if ( field.get("key").equals("region") ) {
            text_field_cont.setTag("region_field");
        }
		
		/* listener */
		textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable text) {
                String field_key = field.get("key");
                if (langCode != null && field.get("multilingual").equals("1")) {
                    field_key = field_key + "_" + langCode;
                }
                formData.remove(field_key);
                formData.put(field_key, text.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                unsetError(text_field_cont);
                textField.setBackgroundResource(R.drawable.edit_text);

                if (langCode != null && field.get("multilingual").equals("1")) {
					/* reset flag because the previous background set removed it */
                    try {
                        textField.setCompoundDrawablesWithIntrinsicBounds(null, null, Utils.getFlag(langCode), null);
                    } catch (Exception e) {
                    }
                }
            }
        });
		
		return text_field_cont;
	}
	
	private static View booleanField(final HashMap<String, String> field, final HashMap<String,String> formData, int padding, Context instance) {
		LinearLayout radioField = (LinearLayout) ((Activity) instance).getLayoutInflater()
	    	.inflate(R.layout.field_bool, null);
		
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(padding, Utils.dp2px(15), padding, 0);
		radioField.setLayoutParams(params);

		TextView radioName = (TextView) radioField.findViewById(R.id.name);
		radioName.setText(field.get("name") + ":");

		/* set checked */
		if ( field.containsKey("current") ) {
            String curr_val;
            if(!field.get("current").isEmpty()) {
                curr_val = field.get("current");
            }
            else if(field.get("current").isEmpty() && !field.get("default").isEmpty()) {
                curr_val = field.get("default");
            }
            else {
                curr_val = "0";
            }
			RadioButton target = (RadioButton) radioField.findViewWithTag(curr_val);
			if ( target != null ) {
				target.setChecked(true);
				formData.put(field.get("key"), curr_val);
			}
            if( field.get("key").equals("account_address_on_map") ) {
                use_on_map = curr_val;
            }

		}

		/* listener */
		RadioGroup radioGroup = (RadioGroup) radioField.findViewById(R.id.radio_group);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				RadioButton checked = (RadioButton) group.findViewById(checkedId);
				formData.put(field.get("key"), checked.getTag().toString());

				if (field.get("key").equals("account_address_on_map")) {
					disabledFields(checked.getTag().toString());
					use_on_map = checked.getTag().toString();
				}
			}
		});
		
		return radioField;
	}
	
	/**
	 * build spinner/select field
	 * 
	 * @param field - field details, key, name and data items are required 
	 * @param fieldItems - field items array
 	 * @param formData - form data to apply changes to
	 */
	private static View selectField( final HashMap<String, String> field, ArrayList<HashMap<String, String>> fieldItems,
			final HashMap<String,String> formData, String typeKey, int padding, Context instance ) {
		
		ArrayList<HashMap<String, String>> customItems = new ArrayList<HashMap<String, String>>();
		
		if ( fieldItems == null ) {
        	if ( field.get("data").equalsIgnoreCase("multiField") ) {
        		/* create fake first item */
        		HashMap<String, String> infoItem = new HashMap<String, String>();  
        		infoItem.put("name", Lang.get("android_any_field").replace("{field}", field.get("name")));
        		infoItem.put("key", "");
        		customItems.add(infoItem);
        	}
        	else {
                HashMap<String, String> infoItem = new HashMap<String, String>();
                infoItem.put("name", Lang.get("select"));
                infoItem.put("key", "");
                customItems.add(infoItem);
        	}
		}
		
		LinearLayout spinner_cont = (LinearLayout) ((Activity) instance).getLayoutInflater()
    		.inflate(R.layout.field_spinner, null);
		
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(padding, Utils.dp2px(15), padding, 0);
		spinner_cont.setLayoutParams(params);
		
		int spinner_width = LayoutParams.MATCH_PARENT;
		if ( field.get("data").equals("years") ) {
			spinner_width = Utils.dp2px(short_field_width);
		}
        else if ( field.get("key").equals("b_states") ) {
            spinner_cont.setTag("us_states_field");
        }

		params = new LayoutParams(spinner_width, Utils.dp2px(44));
		Spinner spinner = (Spinner) spinner_cont.getChildAt(0);
		spinner.setLayoutParams(params);

		spinner.setTag(field.get("key")); // add listing type

		SpinnerAdapter adapter = new SpinnerAdapter(instance, customItems.size() > 0 ? customItems : fieldItems, field,	formData, typeKey);
		   			
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(adapter);
		
		if ( field.containsKey("current") && !field.get("current").isEmpty() ) {
			int selected = adapter.getPosition(field.get("current"), "key");
			spinner.setSelection(selected);
        	adapter.currentValue = field.get("current");
		}
		else if ( field.containsKey("default") ) {
            if ( !field.get("default").isEmpty() ) {
                int selected = adapter.getPosition(field.get("default"), "key");
                spinner.setSelection(selected);
                adapter.currentValue = field.get("default");
                formData.put(field.get("key"), field.get("default"));
		    }
		}

		if ( field.get("data").equals("multiField") && field.get("key").contains("_level")
          || field.get("key").contains("sub_categories_level") ) {
			spinner.setEnabled(false);
		}
		
		return spinner_cont;
	}
	
	private static void radioField( LinearLayout layout, final HashMap<String, String> field, 
			HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
			final HashMap<String,String> formData,
			final HashMap<String, View> fieldViews,
			int padding,
            Context instance) {
		
		if ( !fieldItems.containsKey(field.get("key")) ) {
			Utils.bugRequest("buildFields, add radio field fail", "No items found for field: "+field.get("key"));
			return;
		}

		LinearLayout radioField = (LinearLayout) ((Activity) instance).getLayoutInflater()
                    .inflate(R.layout.field_radio, null);
		
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(padding, Utils.dp2px(15), padding, 0);
		radioField.setLayoutParams(params);

		TextView radioName = (TextView) radioField.findViewById(R.id.name);
		radioName.setText(field.get("name") + ":");
		
		LayoutParams top_margin = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		top_margin.setMargins(0, Utils.dp2px(3), 0, 0);
		
		RadioGroup group = (RadioGroup) radioField.findViewById(R.id.group);
		
		int i = 0;
		for ( HashMap<String,String> radioItem : fieldItems.get(field.get("key"))) {
			RadioButton radioButton = new RadioButton(Config.context);
			radioButton.setText(radioItem.get("name"));
			radioButton.setTag(radioItem.get("key"));
			group.addView(radioButton);
			
			if ( i != 0 ) {
				radioButton.setLayoutParams(top_margin);
			}

			if ( field.containsKey("current") && !field.get("current").isEmpty() ) {
				if ( radioItem.get("key").equals(field.get("current")) ) {
					radioButton.toggle();
					formData.put(field.get("key"), field.get("current"));
				}
			}
            else if ( field.containsKey("default") ) {
				if ( radioItem.get("key").equals(field.get("default"))  && !field.get("default").isEmpty() ) {
					radioButton.toggle();
					formData.put(field.get("key"), field.get("default"));
				}
                else if ( i == 0 ) {
                    radioButton.toggle();
                    formData.put(field.get("key"), radioItem.get("key"));
                }
			}
			else {
				if ( i == 0 && searchField.equals("0") ) {
					radioButton.toggle();
					formData.put(field.get("key"), radioItem.get("key"));
				}
			}
			
			i++;
		}
		

		if(field.get("key").equals("time_frame") && formData.containsKey("sale_rent")) {

			if (formData.get("sale_rent").equals("2")) {
				radioField.setVisibility(View.VISIBLE);
			}
			else {
				radioField.setVisibility(View.GONE);
			}
		}

		layout.addView(radioField);
		
		if ( fieldViews != null ) {
			fieldViews.put(field.get("key"), radioField);
		}
		
		/* listener */
		group.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				RadioButton checked = (RadioButton) group.findViewById(checkedId);
				formData.put(field.get("key"), checked.getTag().toString());

				if(field.get("key").equals("sale_rent") && fieldViews!=null && fieldViews.containsKey("time_frame")) {
					View view_time_frame = (View) fieldViews.get("time_frame");
					if(checked.getTag().equals("2")) {
						view_time_frame.setVisibility(View.VISIBLE);
					}
					else {
						view_time_frame.setVisibility(View.GONE);
					}
				}
	        }
	    });
	}
	
    private static void dateField( LinearLayout layout, final HashMap<String, String> field,
			HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
			final HashMap<String,String> formData,
			final HashMap<String, View> fieldViews,
			int padding,
			final Context instance) {

		final LinearLayout dateField = (LinearLayout) ((Activity) instance).getLayoutInflater()
                .inflate(R.layout.field_date, null);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(padding, Utils.dp2px(15), padding, 0);
        dateField.setLayoutParams(params);

        final EditText single = (EditText) dateField.findViewById(R.id.single);
        final EditText multi = (EditText) dateField.findViewById(R.id.multi);

        if(!field.get("current").isEmpty()) {
			single.setText(field.get("current"));
            formData.put(field.get("key"), field.get("current"));
        }
		else {
			if (field.get("default").equals("multi")) {
				TextView date_name = (TextView) dateField.findViewById(R.id.date_name);
				date_name.setText(field.get("name"));
				date_name.setVisibility(View.VISIBLE);
				single.setHint(Lang.get("android_from"));
			}
			else {
				single.setHint(field.get("name"));
			}
		}

        final Calendar c = Calendar.getInstance();
        int sYear = c.get(Calendar.YEAR);
        int sMonth = c.get(Calendar.MONTH);
        int sDay = c.get(Calendar.DAY_OF_MONTH);

        if ( !field.get("current").equals("0000-00-00") && !field.get("current").isEmpty()) {
            String[] separated = field.get("current").split("-");
            sYear = Integer.parseInt(separated[0]);
            sMonth = Integer.parseInt(separated[1]) - 1;
            sDay = Integer.parseInt(separated[2]);
        }

        final DatePickerDialog dpsingle = new DatePickerDialog(instance,
			new DatePickerDialog.OnDateSetListener() {
				@Override
				public void onDateSet(DatePicker view, int year,
									  int monthOfYear, int dayOfMonth) {
					c.set(year, monthOfYear, dayOfMonth);
					SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
					single.setText(dateFormatter.format(c.getTime()));
					formData.put(field.get("key"), dateFormatter.format(c.getTime()));

					unsetError(dateField);
					single.setBackgroundResource(R.drawable.edit_text);
				}
			}, sYear, sMonth, sDay);

		single.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                dpsingle.show();
                return true;
            }
        });

        if( field.get("default").equals("multi") ) {

            if(!field.get("data").isEmpty()) {
				multi.setText(field.get("data"));
				formData.put(field.get("key")+"_multi", field.get("data"));
            }
			else {
				multi.setHint(Lang.get("android_to"));
			}

            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);

            if ( !field.get("data").equals("0000-00-00") && !field.get("data").isEmpty() ) {
                String[] separated = field.get("data").split("-");
                mYear = Integer.parseInt(separated[0]);
                mMonth = Integer.parseInt(separated[1]) - 1;
                mDay = Integer.parseInt(separated[2]);
            }

            final DatePickerDialog dpmulti = new DatePickerDialog(instance,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                    c.set(year, monthOfYear, dayOfMonth);
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    multi.setText(dateFormatter.format(c.getTime()));
                    formData.put(field.get("key")+"_multi", dateFormatter.format(c.getTime()));

                    unsetError(dateField);
                    multi.setBackgroundResource(R.drawable.edit_text);
                }
            }, mYear, mMonth, mDay);

            multi.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    dpmulti.show();
                    return true;
                }
            });
        }
        else {
            multi.setVisibility(View.INVISIBLE);
        }

        layout.addView(dateField);
        if ( fieldViews != null ) {
            fieldViews.put(field.get("key"), dateField);
        }
    }

 	private static void acceptField( LinearLayout layout, final HashMap<String, String> field,
									 HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
									 final HashMap<String,String> formData,
									 final HashMap<String, View> fieldViews,
									 int padding,
									 final Context instance) {

		final LinearLayout acceptField = (LinearLayout) Config.context.getLayoutInflater()
				.inflate(R.layout.field_accept, null);

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(padding, Utils.dp2px(15), padding, 0);
		acceptField.setLayoutParams(params);

		final CheckBox checkbox = (CheckBox) acceptField.findViewById(R.id.accept_field);
		checkbox.setText(Lang.get("android_i_accept"));
		checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				/* save value */
				formData.put(field.get("key"), isChecked ? "1" : "");
				unsetError(acceptField);
			}
		});
		final TextView terms = (TextView) acceptField.findViewById(R.id.terms_field);
		terms.setText(field.get("name"));
		terms.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(Config.context, AcceptActivity.class);

				intent.putExtra("key", field.get("key"));
				intent.putExtra("name", field.get("name"));
				intent.putExtra("data", field.get("data"));

				int version = Utils.getCacheConfig("rl_version") != null ? Config.compireVersion(Utils.getCacheConfig("rl_version"), "4.7.0") : -1 ;
				if(version >= 0) {
					intent.putExtra("mode", field.get("values").equals("static") ? "view" : "webview");
				}
				((Activity) instance).startActivityForResult(intent, AddListingActivity.RESULT_ACCEPT);
			}
		});

		layout.addView(acceptField);
		if ( fieldViews != null ) {
			fieldViews.put(field.get("key"), acceptField);
		}
	}

	public static void confirmAccept(String key) {
		LinearLayout accept = (LinearLayout) fieldViewsForm.get(key);
		CheckBox acceptField = (CheckBox) accept.findViewById(R.id.accept_field);
		acceptField.setChecked(true);
	}


	private static void checkboxField( LinearLayout layout, final HashMap<String, String> field,
			final HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
			final HashMap<String,String> formData, final Context instance,
			final HashMap<String, View> fieldViews,
			int padding) {
		
		if ( !fieldItems.containsKey(field.get("key")) ) {
			Utils.bugRequest("buildFields, add checkbox field fail", "No items found for field: "+field.get("key"));
			return;
		}
		
		final LinearLayout checkboxField = (LinearLayout) Config.context.getLayoutInflater()
	    	.inflate(R.layout.field_checkbox, null);
		
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(padding, Utils.dp2px(15), padding, 0);
		checkboxField.setLayoutParams(params);

		final ArrayList<HashMap<String, String>> items = fieldItems.get(field.get("key"));
		final TextView checkboxName = (TextView) checkboxField.findViewById(R.id.name);
		final String field_name = Lang.get("android_select_field").replace("{field}", field.get("name"));
		checkboxName.setText(field_name);
		
		/* set selected checkbox items as field name */
		if ( field.containsKey("current") || field.containsKey("default") ) {
			String setItems = "";
			String selectedItems = "";
			String currVal = !field.get("current").isEmpty() ? field.get("current") : field.get("default");
			List<String> current = Utils.string2list(currVal.split(","));

			for ( HashMap<String, String> current_item : items ) {
				if ( current.indexOf(current_item.get("key")) >= 0 ) {
					setItems += current_item.get("name")+", ";
					selectedItems += current_item.get("key")+";";
				}
			}

			if ( setItems.isEmpty() ) {
				checkboxName.setText(field_name);
			}
			else {
				setItems = setItems.substring(0, setItems.length()-2);// cut comma and space at the end
				selectedItems = selectedItems.substring(0, selectedItems.length()-1);// cut last comma
				checkboxName.setText(setItems);
				formData.put(field.get("key"), selectedItems);
			}
		}

		checkboxField.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final LinearLayout view = new LinearLayout(Config.context);
				view.setOrientation(LinearLayout.VERTICAL);

				List<String> current = new ArrayList<String>();

				if (formData.containsKey(field.get("key"))) {
					current = Utils.string2list(formData.get(field.get("key")).split(";"));
				}
                else if (field.containsKey("current")) {
					current = Utils.string2list(field.get("current").split(","));
				}
                else if (field.containsKey("default")) {
					current = Utils.string2list(field.get("default").split(","));
				}

				CheckboxDialogAdapter adapter = new CheckboxDialogAdapter(items, current);

				LayoutParams params = new LayoutParams(
						LayoutParams.MATCH_PARENT,
						LayoutParams.MATCH_PARENT
				);

				final ListView listView = (ListView) Config.context.getLayoutInflater()
						.inflate(R.layout.list_view, null);

				listView.setLayoutParams(params);
				listView.setAdapter(adapter);
				listView.setOnItemClickListener(adapter);

				AlertDialog.Builder checkboxesDialog = new AlertDialog.Builder(instance);

				checkboxesDialog.setTitle(field_name);
				checkboxesDialog.setView(listView);
        		
            	/* set listener */
				final List<String> finalCurrent = current;
				checkboxesDialog.setPositiveButton(Lang.get("android_dialog_set"), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						String selectedItems = "";
						String setItems = "";

						if (!finalCurrent.isEmpty()) {
							for (int j = 0; j < items.size(); j++) {
								HashMap<String, String> item = items.get(j);
								if (finalCurrent.contains(item.get("key"))) {
									setItems += item.get("name") + ", ";
									selectedItems += item.get("key") + ";";
								}
							}
						}

						if (setItems.isEmpty()) {
							checkboxName.setText(field_name);
						} else {
							setItems = setItems.substring(0, setItems.length() - 2);// cut comma and space at the end
							selectedItems = selectedItems.substring(0, selectedItems.length() - 1);// cut last comma
							checkboxName.setText(setItems);
						}
        				
        				/* save value */
						formData.put(field.get("key"), selectedItems);
						unsetError(checkboxField);
						return;
					}
				});
				checkboxesDialog.setNegativeButton(Lang.get("android_dialog_cancel"), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				AlertDialog alert = checkboxesDialog.create();
        		alert.show();
			}
        });
		
		layout.addView(checkboxField);

		if ( fieldViews != null ) {
			fieldViews.put(field.get("key"), checkboxField);
		}
	}
	
	private static void setError(View view, String phrase_key) {
		TextView error = (TextView) view.findViewWithTag("error_message");
		error.setText(Lang.get(phrase_key));
		error.setVisibility(View.VISIBLE);
	}

	public static void unsetError(View view) {
		TextView error =  (TextView) view.findViewWithTag("error_message");
		error.setVisibility(View.GONE);
	}
	
	private static int defineStep(int from, int to) {
		int step = 1;
		int diff = to - from;
		
		if ( diff <= 50 ) {
			step = 1;
		}
		else if ( diff > 50 && diff <= 100 ) {
			step = 2;
		}
		else if ( diff > 100 && diff <= 200 ) {
			step = 5;
		}
		else if ( diff > 200 && diff <= 500 ) {
			step = 10;
		}
		else if ( diff > 500 && diff <= 1000 ) {
			step = 25;
		}
		else if ( diff > 1000 && diff <= 5000 ) {
			step = 100;
		}
		else if ( diff > 5000 && diff <= 20000 ) {
			step = 250;
		}
		else if ( diff > 20000 && diff <= 50000 ) {
			step = 500;
		}
		else if ( diff > 50000 && diff <= 500000 ) {
			step = 1000;
		}
		else if ( diff > 500000 && diff <= 1000000 ) {
			step = 5000;
		}
		else if ( diff > 1000000 ) {
			step = 50000;
		}
		
		return step;
	}
	
	private static void collectPhone(EditText code, EditText area, EditText number, HashMap<String, String> formData, String fieldKey) {
		String out = "";
		
		/* add code */
		if ( !code.getText().toString().isEmpty() ) {
			out += "c:"+code.getText().toString()+"|";
		}

		/* add area */
		if ( !area.getText().toString().isEmpty() ) {
			out += "a:"+area.getText().toString()+"|";
		}
		
		/* add number */
		if ( !number.getText().toString().isEmpty() ) {
			out += "n:"+number.getText().toString();
		}
		
		formData.remove(fieldKey);
		formData.put(fieldKey, out);
	}
	
	private static void collectCombo(HashMap<String,String> formData, String fieldKey, HashMap<String, ArrayList<HashMap<String, String>>> fieldItems, EditText number, Spinner unit){
        String unit_key = "";
        if ( unit.getAdapter() != null ) {
            HashMap<String, String> unit_item = (HashMap<String, String>) unit.getSelectedItem();
            unit_key = unit_item.get("key");
        }
        else if ( unit.getAdapter() == null && fieldItems.containsKey(fieldKey) ) {
            unit_key = fieldItems.get(fieldKey).get(0).get("key");
        }

		String val = number.getText().toString()+"|"+unit_key;
		formData.put(fieldKey, val);
	}

	private static void collectEscortRates(int escortIndex,
										   String field_key,
										   HashMap<String, String> formData,
										   HashMap<String, ArrayList<HashMap<String, String>>> fieldItems,
										   Spinner rates,
										   EditText customText,
										   EditText price,
										   Spinner currency) {
		String unit_key = "";
		String rates_key = "";
		String currency_key = "rates_value";
		if ( currency.getAdapter() != null ) {
			HashMap<String, String> unit_item = (HashMap<String, String>) currency.getSelectedItem();
			unit_key = unit_item.get("key");
		}
		else if ( currency.getAdapter() == null && fieldItems.containsKey(currency_key) ) {
			unit_key = fieldItems.get(currency_key).get(0).get("key");
		}


		if ( rates.getAdapter() != null ) {
			HashMap<String, String> unit_item = (HashMap<String, String>) rates.getSelectedItem();
			rates_key = unit_item.get("key");
		}
		else if ( rates.getAdapter() == null && fieldItems.containsKey(field_key) ) {
			rates_key = fieldItems.get(field_key).get(0).get("key");
		}
		String custom = rates_key.equals("*cust0m*") ? customText.getText().toString() : "";

		String new_val = rates_key + "|" + custom + "|" + price.getText().toString() + "|" + unit_key;
		formData.put(field_key+"_"+escortIndex, new_val);
	}

	private static void collectTours(int escortIndex,
									 String field_key,
									 HashMap<String, String> formData,
									 HashMap<String, String> locationItem) {

		JSONObject obj=new JSONObject(locationItem);
		formData.put(field_key+"_"+escortIndex, obj.toString());
	}
}