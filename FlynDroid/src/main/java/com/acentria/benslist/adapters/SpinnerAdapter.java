package com.acentria.benslist.adapters;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.AddListingActivity;
import com.acentria.benslist.Config;
import com.acentria.benslist.Forms;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;
import com.acentria.benslist.XMLParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.Header;


public class SpinnerAdapter extends BaseAdapter implements OnItemSelectedListener {

	private ArrayList<HashMap<String, String>> items;
	private HashMap<String,String> formData;
	private HashMap<String, String> fieldInfo;
	private Context instance;
    private String form_type_key;
	public String currentValue;
	
	public SpinnerAdapter(Context context, ArrayList<HashMap<String, String>> itemsArray, 
			HashMap<String, String> field, HashMap<String,String> data, String typeKey) {
		
		items = itemsArray;
		fieldInfo = field;
		formData = data;
		instance = context;
        form_type_key = typeKey;
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		
		if ( convertView == null ) {
			view = ((Activity) instance).getLayoutInflater().inflate(R.layout.spinner_item, parent, false);
		}
		else {
			view = convertView;
		}
		
		String name_index = "name";
		
		if ( fieldInfo.containsKey("data") && fieldInfo.get("data").equals("years") && position != 0 ) {
			name_index = "key";
		}
		
		((TextView) view).setText(items.get(position).get(name_index));
		if(Lang.isRtl()) {
			((TextView) view).setTextDirection(View.TEXT_DIRECTION_RTL);
		}
		
		if ( items.get(position).containsKey("margin") ) {
			int setLeft = 10 + Integer.parseInt(items.get(position).get("margin")); 
			view.setPadding(Utils.dp2px(setLeft), Utils.dp2px(10), Utils.dp2px(10), Utils.dp2px(11));
		}
		else {
			view.setPadding(Utils.dp2px(10), Utils.dp2px(10), Utils.dp2px(10), Utils.dp2px(11));
		}

		if ( position == 0 && items.get(position).get("key").isEmpty() ) {
			((TextView) view).setTextColor(Color.parseColor("#7a7a7a"));
		}
		else {
			((TextView) view).setTextColor(Color.parseColor("#2b2b2b"));
		}
		
		return view;
	}

	@Override
	public void onItemSelected(AdapterView<?> spinnerView, View itemView, int position, long arg3) {						
		String spinner_key = fieldInfo.get("key");
		
		/* save value */
        String send_key = spinner_key.contains("sub_categories_level") || spinner_key.contains(Config.categoryFieldKey) ? Config.categoryFieldKey : spinner_key;

		formData.remove(send_key);
		formData.put(send_key, items.get(position).get("key"));

        /* region field action */
        if ( spinner_key.equals("b_country") && instance instanceof AddListingActivity) {
            LinearLayout region_field = (LinearLayout) ((Activity) instance).getWindow().getDecorView().findViewWithTag("region_field");
            LinearLayout us_states_field = (LinearLayout) ((Activity) instance).getWindow().getDecorView().findViewWithTag("us_states_field");

            if ( region_field != null && us_states_field != null ) {
                region_field.setVisibility(items.get(position).get("key").equals("united_states") ? View.GONE : View.VISIBLE);
                us_states_field.setVisibility(items.get(position).get("key").equals("united_states") ? View.VISIBLE : View.GONE);
            }
        }

		// hide error
		if ( position > 0 ) {
			spinnerView.setBackgroundResource(R.drawable.spinner);
			
			View parent = (View) spinnerView.getParent();
			if ( parent != null && parent.getTag() != null && parent.getTag().equals("spinner_field_container") ) {
				Forms.unsetError(parent);
			}
		}

        /* multifield mode */
		if ( fieldInfo.containsKey("data") && fieldInfo.get("data").equals("multiField") ) {
			String next_spinner = "";
			String base_key = null;
			int reset_iteration = 0;
			
			if ( spinner_key.contains("_level") ) {
				Pattern p = Pattern.compile("(.*?)_level([0-9])");				
				Matcher m = p.matcher(spinner_key);
				if ( m.find() ) {
					next_spinner = m.group(1)+"_level"+(Integer.parseInt(m.group(2)) + 1);
					base_key = m.group(1);
					reset_iteration = Integer.parseInt(m.group(2)) + 2;
				}
			}
			else {
				next_spinner = spinner_key + "_level1";
				base_key = spinner_key;
				reset_iteration = 2;
			}
			
			View parentForm = (View) spinnerView.getParent().getParent();

			if ( next_spinner != null ) {
				if ( position != 0 ) {
					Spinner next_spinner_element = (Spinner) parentForm.findViewWithTag(next_spinner);

					if ( next_spinner_element != null ) {
						SpinnerAdapter nextSpinnerAdapter = (SpinnerAdapter) next_spinner_element.getAdapter();						
						
						loadData(items.get(position).get("key"), nextSpinnerAdapter, nextSpinnerAdapter.items.get(0), next_spinner_element);
						nextSpinnerAdapter.showLoading();
						next_spinner_element.setEnabled(true);
											
						Spinner spinner_element_toreset = (Spinner) parentForm.findViewWithTag( base_key + "_level" + reset_iteration );
						while( spinner_element_toreset != null ) {
							SpinnerAdapter toresetSpinnerAdapter = (SpinnerAdapter) spinner_element_toreset.getAdapter();
							toresetSpinnerAdapter.clearSpinner( toresetSpinnerAdapter.items.get(0) );
							spinner_element_toreset.setEnabled(false);
							
							reset_iteration++;							
							spinner_element_toreset = (Spinner) parentForm.findViewWithTag( base_key + "_level" + reset_iteration );							
						}
					}
				}
				else {
					//reset all childs if empty clicked
					int reset_iteration2 = reset_iteration;
					reset_iteration2--;
					Spinner spinner_element_toreset = (Spinner) parentForm.findViewWithTag( base_key + "_level" + reset_iteration2 );
					while( spinner_element_toreset != null ) {
						SpinnerAdapter toresetSpinnerAdapter = (SpinnerAdapter) spinner_element_toreset.getAdapter();
						toresetSpinnerAdapter.clearSpinner( toresetSpinnerAdapter.items.get(0) );
						spinner_element_toreset.setEnabled(false);
						
						reset_iteration2++;
						spinner_element_toreset = (Spinner) parentForm.findViewWithTag( base_key + "_level" + reset_iteration2 );							
					}				
				}
			}
		}
        /* multi category spinner mode */
        else if ( fieldInfo.get("key").contains(Config.categoryFieldKey) || fieldInfo.get("key").contains("sub_categories_level") ) {
            String next_spinner_key = "sub_categories_level_1";
            String prev_spinner_key = null;

            if ( fieldInfo.get("key").contains("sub_categories_level") ) {
                Matcher m = Pattern.compile("(.*?)_level_([0-9])").matcher(spinner_key);

                if ( m.find() ) {
                    int index = Integer.parseInt(m.group(2));
                    next_spinner_key = "sub_categories_level_"+(index + 1);
                    prev_spinner_key = index == 1 ? Config.categoryFieldKey+"|"+form_type_key : "sub_categories_level_"+(index - 1);
                }
            }

            View parentForm = (View) spinnerView.getParent().getParent();
            Spinner next_spinner = (Spinner) parentForm.findViewWithTag(next_spinner_key);

            if ( next_spinner != null ) {
                SpinnerAdapter nextSpinnerAdapter = (SpinnerAdapter) next_spinner.getAdapter();

                if ( position == 0 ) {
                    nextSpinnerAdapter.clearSpinner(null);
                    getPreviousCategorySelection(parentForm, prev_spinner_key);
                }
                /* get next spinner categories */
                else {
                    nextSpinnerAdapter.showLoading();
                    loadCategories(items.get(position).get("key"), nextSpinnerAdapter, next_spinner);
                }
            }
            else {
                if ( position == 0 ) {
                    getPreviousCategorySelection(parentForm, prev_spinner_key);
                }
            }
        }
	}

	public void updateItems(ArrayList<HashMap<String, String>> data) {
		items.clear();
		this.items = data;
		notifyDataSetChanged();
	} 
	
	private void updateSpinner( NodeList loadItems, HashMap<String, String> firstItem ) {
		ArrayList<HashMap<String, String>> itemsOut = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> tmpItem;

		itemsOut.add(firstItem);
		
		for ( int i=0; i<loadItems.getLength(); i++ ) {			
			Element item = (Element) loadItems.item(i);			

            String set_key = Utils.getNodeByName(item, "key");
            String set_name = Utils.getNodeByName(item, "name");

            // category mode
            if ( Utils.getNodeByName(item, "key").isEmpty() ) {
                set_key = Utils.getNodeByName(item, "id");
                String count = Utils.getNodeByName(item, "count");
                if ( !count.isEmpty() && Integer.parseInt(count) > 0 ) {
                    set_name += " ("+Utils.getNodeByName(item, "count")+")";
                }
            }

			tmpItem = new HashMap <String, String>();
			tmpItem.put("key", set_key);
			tmpItem.put("name", set_name);
			
			itemsOut.add(tmpItem);
		}		
		
		items.clear();
		items = itemsOut;
		notifyDataSetChanged();
	}
	
	private void clearSpinner(HashMap<String, String>firstItem) {		
		ArrayList<HashMap<String, String>> loadingItem = new ArrayList<HashMap<String, String>>();

        if ( firstItem == null ) {
            firstItem = items.get(0);
        }

		loadingItem.add(firstItem);
		items.clear();
		items = loadingItem;
		notifyDataSetChanged();
	}
	
	private void showLoading() {
		ArrayList<HashMap<String, String>> loadingItem = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> tmpItem = new HashMap <String, String>();
		
		tmpItem.put("key", "");
		tmpItem.put("name", Lang.get("android_loading"));
		loadingItem.add(tmpItem);
		items.clear();
		items = loadingItem;
		notifyDataSetChanged();
	}
	
	public void select(String key, Spinner spinner, String field) {
		int position = 0;

		field = field == null ? "key" : field;
		
		for (HashMap<String, String> entry: items ) {
			if ( entry.containsKey(field) ) {
				if ( entry.get(field).equals(key) ) {
					spinner.setSelection(position);
					break;
				}
			}
			else {
				Log.d("FD ERROR", "SpinnerAdapter.select() no '"+key+"' key in HashMap");
			}
			position++;
		}
	}
	
	private void loadData(String parent, final SpinnerAdapter nextAdapter, final HashMap<String, String> firstItem, final Spinner spinnerView) {
		/* build request url */
    	HashMap<String, String> params = new HashMap<String, String>();	
		params.put("parent", parent);
		
		final String url = Utils.buildRequestUrl("getMultiFieldNext", params, null);
		
		/* do async request */
    	AsyncHttpClient client = new AsyncHttpClient();
    	client.get(url, new AsyncHttpResponseHandler() {
    		
			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if ( doc == null ) {
						CharSequence text = Lang.get("returned_xml_failed");
						Toast.makeText(instance, text, Toast.LENGTH_SHORT).show();
					}
					else {
						NodeList itemNode = doc.getElementsByTagName("items");

						Element nlE = (Element) itemNode.item(0);
						NodeList next_items = nlE.getChildNodes();

						nextAdapter.updateSpinner( next_items, firstItem );

						if ( nextAdapter.currentValue != null ) {
							int selected = nextAdapter.getPosition(nextAdapter.currentValue, "key");
							spinnerView.setSelection(selected);
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

    private void loadCategories(String parent, final SpinnerAdapter nextAdapter, final Spinner spinnerView) {
        /* build request url */
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("parent", parent);
        params.put("type", form_type_key);

        final String url = Utils.buildRequestUrl("getCategories", params, null);

		/* do async request */
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {
        	
			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					XMLParser parser = new XMLParser();
					Document doc = parser.getDomElement(response, url);

					if ( doc == null ) {
						CharSequence text = Lang.get("returned_xml_failed");
						Toast.makeText(instance, text, Toast.LENGTH_SHORT).show();
					}
					else {
						NodeList itemNode = doc.getElementsByTagName("items");

						Element nlE = (Element) itemNode.item(0);
						NodeList next_items = nlE.getChildNodes();

						HashMap<String, String> first_item = new HashMap<String, String>();
						first_item.put("name", Lang.get("android_any_field").replace("{field}", Lang.get("subcategory")));
						first_item.put("key", "");

						nextAdapter.updateSpinner(next_items, first_item);
						spinnerView.setEnabled(true);
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

    private void getPreviousCategorySelection(View parent, String spinner_key) {
        Spinner spinner = (Spinner) parent.findViewWithTag(spinner_key);
        if ( spinner == null )
            return;

        HashMap<String, String> item = (HashMap<String, String>) spinner.getAdapter().getItem(spinner.getSelectedItemPosition());
        formData.put(Config.categoryFieldKey, item.get("key"));
    }
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {}
	
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public HashMap<String, String> getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public int getPosition(String string, String field) {
		if ( items.size() <= 0 )
			return 0;

		int index = 0;
		for (int i = 0; i < this.getCount(); i++) {
			if ( items.get(i).get(field).equals(string) ) {
				index = i;
				break;
			}
		}
		
		return index;
	}
}