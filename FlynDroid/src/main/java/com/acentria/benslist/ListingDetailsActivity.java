package com.acentria.benslist;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.acentria.benslist.adapters.VideoAdapter;
import com.acentria.benslist.controllers.Favorites;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.viewpagerindicator.TabPageIndicator;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class ListingDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
	public static ListingDetailsActivity instance;
	private static Intent intent;
	
	private static List<String> TAB_NAMES = new ArrayList<String>();
	public static List<String> TAB_KEYS = new ArrayList<String>();
    public static final int RETURN_CODE = 1;
	
	private static SupportMapFragment mapFragment;
	private static GoogleMap map;
	private static Menu pMenu;
	
	public static LinearLayout gallery;
	
	private static String ldListingID;
	private static HashMap<String, String> listingData;
	private static ArrayList<HashMap<String, String>> fields;
	private static ArrayList<HashMap<String, String>> comments;
	
	public static ArrayList<HashMap<String, String>> photos;
	public static ArrayList<HashMap<String, String>> videos;
		
	private static HashMap<String, String> sellerData;
//	private static HashMap<String, String> locationData;
	private static ArrayList<HashMap<String, String>> sellerFields;
	private LinearLayout content;
	
	private AsyncHttpClient client;

    public static ViewPager tabPager;
	public static FragmentAdapter adapter;
	public static TabPageIndicator indicator;

	public static LinearLayout commentsContent;
	public static LinearLayout see_more_view;
	public static Button add_comment_button;
	public static TextView see_more_comments;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;

		Lang.setDirection(this);

		/* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        setTitle(Lang.get("android_title_activity_listing_details"));
        setContentView(R.layout.activity_listing_details);
        
        /* get listing data from instance */
        intent = getIntent();
        ldListingID = intent.getStringExtra("id");
        
        /* clear data */
        TAB_NAMES = new ArrayList<String>();
        TAB_KEYS = new ArrayList<String>();
        listingData = new HashMap<String, String>();
        photos = new ArrayList<HashMap<String, String>>();
        videos = new ArrayList<HashMap<String, String>>();
        fields = new ArrayList<HashMap<String, String>>();
        comments = new ArrayList<HashMap<String, String>>();
        
        sellerData = new HashMap<String, String>();
//        locationData = new HashMap<String, String>();
        sellerFields = new ArrayList<HashMap<String, String>>();

        content = (LinearLayout) findViewById(R.id.activity_details);
        // set ad sense
        Utils.setAdsense(content, "listing_details");

        /* build tabs */
        if ( !ldListingID.isEmpty() )
        {
        	TAB_NAMES.add(Lang.get("android_tab_caption_details"));
        	TAB_KEYS.add("details");

        	if ( !Config.tabletMode ) {
	        	TAB_NAMES.add(Lang.get("android_tab_caption_seller"));
	        	TAB_KEYS.add("seller_info");
        	}

        	TAB_NAMES.add(Lang.get("android_tab_caption_map"));
        	TAB_KEYS.add("map");
        	
        	/* init adapter */
        	adapter = new FragmentAdapter(getSupportFragmentManager());
    		
        	tabPager = (ViewPager)findViewById(R.id.pager);
			tabPager.setPageMargin(10);
			tabPager.setAdapter(adapter);
            tabPager.setOffscreenPageLimit(3);
			if (Lang.isRtl()) {
//				tabPager.setRotationY(180);
			}

			indicator = (TabPageIndicator)findViewById(R.id.indicator);
            indicator.setViewPager(tabPager);
            indicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
				@Override
				public void onPageSelected(final int position) {
					updateMenu();
				}

				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				}

				@Override
				public void onPageScrollStateChanged(int state) {}
            });

        	/* build request url */
            loadData();
        }
	}

    private void loadData() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("id", ldListingID);
        if ( Account.loggedIn ) {
            params.put("account_id", Account.accountData.get("id"));
        }
        final String url = Utils.buildRequestUrl("getListingDetails", params, null);

    		/* do request */
        client = new AsyncHttpClient();
        client.get(url, new AsyncHttpResponseHandler() {

			@Override
			public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
				// called when response HTTP status is "200 OK"
				try {
					String response = String.valueOf(new String(server_response, "UTF-8"));
					prepareDetails(response);
//					prepareDetails(response, url);
					if (listingData.containsKey("error")) {
						indicator.setVisibility(View.INVISIBLE);
						content.removeAllViews();

						TextView message = (TextView) Config.context.getLayoutInflater()
								.inflate(R.layout.info_message, null);

						message.setText(Lang.get("listing_no_available"));
						content.setGravity(Gravity.CENTER);
						content.addView(message);
					}
					else {
						drawListing(true, null, null);
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
	 * Fragment adapter
	 * 
	 * @author Freeman
	 */
	class FragmentAdapter extends FragmentStatePagerAdapter {
        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	return ListingDetailsFragment.newInstance(TAB_KEYS.get(position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
        	return TAB_NAMES.get(position % TAB_NAMES.size()).replace(" ", "\u00A0").toUpperCase(); // converting to non-breaking space
        }

        @Override
        public int getCount() {
        	return TAB_NAMES.size();
        }
    }
	
	/**
	 * Fragment intance for adapted above
	 * 
	 * @author Freeman
	 */
	public final static class ListingDetailsFragment extends Fragment {
	    
	    private String tabKey;

	    public static ListingDetailsFragment newInstance(String key) {
	    	ListingDetailsFragment fragment = new ListingDetailsFragment();
	    	fragment.tabKey = key.toString();
	        return fragment;
	    }

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	    }

	    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
		@Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
	    	LinearLayout layout;
	    	
	    	if ( tabKey.equals("map") ) {
	    		layout = (LinearLayout) instance.getLayoutInflater()
	        			.inflate(R.layout.listing_map, null);
				((SupportMapFragment) instance.getSupportFragmentManager().findFragmentById(R.id.map)).getMapAsync(instance);
	    	}
	    	else {
		        layout = new LinearLayout(getActivity());
		        layout.setTag(tabKey);
		        layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		        layout.setGravity(Gravity.CENTER);
		        
		        if ( tabKey.equals("details") )
		        {
		        	ProgressBar progressBar = (ProgressBar) Config.context.getLayoutInflater()
			    			.inflate(R.layout.loading, null);
			        layout.addView(progressBar);
		        }
	    	}

			if (Lang.isRtl()) {
//				layout.setRotationY(180);
			}

			return layout;
	    }

	    @Override
	    public void onSaveInstanceState(Bundle outState) {
	    	super.onSaveInstanceState(outState);
	    }
	}

//	public static void prepareDetails(String response, String url){
//        /* clear data */
//        listingData.clear();
//        photos.clear();
//        videos.clear();
//        fields.clear();
//        sellerData.clear();
//        sellerFields.clear();
//        comments.clear();
//        listingData.put("id", ldListingID);
//    	/* parse xml response */
//    	XMLParser parser = new XMLParser();
//		Document doc = parser.getDomElement(response, url);
//
//		if ( doc == null ) {
//			Dialog.simpleWarning(Lang.get("returned_xml_failed"));
//		}
//		else {
//			NodeList errorNode = doc.getElementsByTagName("error");
//			/* handle errors */
//			if ( errorNode.getLength() > 0 ) {
//				listingData.put("error", "true");
//			}
//			else {
//				NodeList listingNode = doc.getElementsByTagName("listing");
//
//				Element nlE = (Element) listingNode.item(0);
//				NodeList listing = nlE.getChildNodes();
//
//				for( int i=0; i<listing.getLength(); i++ )
//				{
//					Element node = (Element) listing.item(i);
//
//					/* get title */
//					if ( node.getTagName().equals("title") ) {
//						listingData.put("title", Config.convertChars(node.getTextContent()));
//					}
//
//					/* get type */
//					if ( node.getTagName().equals("listing_type") ) {
//						listingData.put("listing_type", node.getTextContent());
//					}
//
//					/* get price */
//					if ( node.getTagName().equals("price") ) {
//						listingData.put("price", node.getTextContent());
//					}
//
//					/* get featured*/
//					if ( node.getTagName().equals("featured") ) {
//						listingData.put("featured", node.getTextContent());
//					}
//
//					/* get photos_count*/
//					if ( node.getTagName().equals("photos_count") ) {
//						listingData.put("photos_count", node.getTextContent());
//					}
//
//					/* get main_photo*/
//					if ( node.getTagName().equals("photo") ) {
//						listingData.put("photo", node.getTextContent());
//					}
//
////					/* get location */
////					if ( node.getTagName().equals("location") ) {
////						locationData.put("direct", node.getAttribute("direct"));
////						locationData.put("search", node.getTextContent());
////					}
//
//					/* get listing url */
//					if ( node.getTagName().equals("url") ) {
//						listingData.put("url", node.getTextContent());
//					}
//
//					/* get photos */
//					if ( node.getTagName().equals("photos") ) {
//						NodeList photoNodes = node.getChildNodes();
//
//						for (int j = 0; j < photoNodes.getLength(); j++)
//						{
//							Element photoNode = (Element) photoNodes.item(j);
//							HashMap<String, String> photo = new HashMap<String, String>();
//							photo.put("large", photoNode.getAttribute("large"));
//							photo.put("thumbnail", photoNode.getAttribute("thumbnail"));
//							photo.put("description", photoNode.getTextContent());
//							photos.add(photo);
//						}
//					}
//
//					/* get videos */
//					if ( node.getTagName().equals("videos") ) {
//						NodeList videoNodes = node.getChildNodes();
//
//						for (int j = 0; j < videoNodes.getLength(); j++)
//						{
//							Element videoNode = (Element) videoNodes.item(j);
//							HashMap<String, String> video = new HashMap<String, String>();
//							video.put("type", videoNode.getAttribute("type"));
//							video.put("video", videoNode.getAttribute("video"));
//							video.put("preview", videoNode.getAttribute("preview"));
//							videos.add(video);
//						}
//					}
//
//					/* get listing fields */
//					if ( node.getTagName().equals("details") ) {
//						NodeList groups = node.getChildNodes();
//						boolean middle_f = false;
//						for (int j = 0; j < groups.getLength(); j++)
//						{
//							Element groupNode = (Element) groups.item(j);
//							if ( groupNode.getTagName().equals("group") )
//							{
//								HashMap<String, String> groupHash = new HashMap<String, String>();
//								groupHash.put("item", "group");
//								groupHash.put("name", groupNode.getAttribute("name"));
//								fields.add(groupHash);
//
//								NodeList fieldNodes = groupNode.getChildNodes();
//								for (int e = 0; e < fieldNodes.getLength(); e++)
//								{
//									Element fieldNode = (Element) fieldNodes.item(e);
//									HashMap<String, String> fieldHash = new HashMap<String, String>();
//									fieldHash.put("item", "field");
//									fieldHash.put("key", fieldNode.getAttribute("key"));
//									fieldHash.put("name", fieldNode.getAttribute("name"));
//									fieldHash.put("type", fieldNode.getAttribute("type"));
//									fieldHash.put("value", fieldNode.getTextContent());
////									fieldHash.put("value", Config.convertChars(fieldNode.getTextContent()));
//
//									if (!middle_f){
//										listingData.put("middle_field", fieldNode.getTextContent());
//										middle_f = true;
//									}
//									fields.add(fieldHash);
//								}
//							}
//							else if ( groupNode.getTagName().equals("field") ) {
//								//TODO if the field our of group
//							}
//						}
//					}
//
//					/* get comments */
//					if ( Utils.getCacheConfig("comment_plugin").equals("1") ) {
//						if ( node.getTagName().equals("comments") ) {
//							NodeList commentNodes = node.getChildNodes();
////							comments = CommentsActivity.prepareComments(commentNodes);
//						}
//					}
//
//					/* get seller data */
//					if ( node.getTagName().equals("seller") ) {
//						NodeList sellerNodes = node.getChildNodes();
//
//						for (int j = 0; j < sellerNodes.getLength(); j++)
//						{
//							Element sellerNode = (Element) sellerNodes.item(j);
//
//							/* get ID */
//							if ( sellerNode.getTagName().equals("id") ) {
//								sellerData.put("id", sellerNode.getTextContent());
//							}
//
//							/* get name */
//							if ( sellerNode.getTagName().equals("name") ) {
//								sellerData.put("name", Config.convertChars(sellerNode.getTextContent()));
//							}
//
//							/* get listings count */
//							if ( sellerNode.getTagName().equals("listings_count") ) {
//								sellerData.put("listings_count", sellerNode.getTextContent());
//							}
//
//							/* get email */
//							if ( sellerNode.getTagName().equals("email") ) {
//								sellerData.put("email", sellerNode.getTextContent());
//							}
//
//							/* get thumbnail */
//							if ( sellerNode.getTagName().equals("thumbnail") ) {
//								sellerData.put("thumbnail", sellerNode.getTextContent());
//							}
//
//							/* get fields */
//							if ( sellerNode.getTagName().equals("fields") ) {
//								NodeList fieldNodes = sellerNode.getChildNodes();
//
//								for (int e = 0; e < fieldNodes.getLength(); e++)
//								{
//									Element fieldNode = (Element) fieldNodes.item(e);
//									HashMap<String, String> field = new HashMap<String, String>();
//									field.put("key", fieldNode.getAttribute("key"));
//									field.put("name", fieldNode.getAttribute("name"));
//									field.put("type", fieldNode.getAttribute("type"));
//									field.put("value", fieldNode.getTextContent());
////									field.put("value", Config.convertChars(fieldNode.getTextContent()));
//									sellerFields.add(field);
//								}
//							}
//						}
//					}
//				}
//				listingData.put("photo_allowed", Config.cacheListingTypes.get(listingData.get("listing_type")).get("photo"));//photo allowed by listing type
////				listingData.put("page_allowed", Config.cacheListingTypes.get(listingData.get("listing_type")).get("page"));//own page allowed by listing type
//			}
//		}
//    }

	public static void prepareDetails(String response){
		/* clear data */
		listingData.clear();
		photos.clear();
		videos.clear();
		fields.clear();
		sellerData.clear();
		sellerFields.clear();
		comments.clear();
		listingData.put("id", ldListingID);

		JSONObject json = null;
		try {
			json = new JSONObject( response );

			if(!json.isNull("error")) {
				listingData.put("error", "true");
			}
			else {
				if (!json.isNull("data")) {
					listingData = JSONParser.parseJson(json.getString("data"));
				}

				if (!json.isNull("photos")) {
					photos = JSONParser.parseJsontoArrayList(json.getString("photos"));
				}

				if (!json.isNull("videos")) {
					videos = JSONParser.parseJsontoArrayList(json.getString("videos"));
				}

				if (!json.isNull("details")) {
					fields = JSONParser.parseJsontoArrayList(json.getString("details"));
				}

				/* get seller */
				if (!json.isNull("seller")) {
					sellerData = JSONParser.parseJson(json.getString("seller"));
				}

				/* get seller fields */
				if (!json.isNull("seller_fields")) {
					sellerFields = JSONParser.parseJsontoArrayList(json.getString("seller_fields"));
				}

				/* get comments */
				if ( Utils.getCacheConfig("comment_plugin").equals("1") && !json.isNull("comments") ) {
					comments = CommentsActivity.prepareComments(json.getString("comments"));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
    
    /**
     * draw listing details by tab key or on load details
     * 
     * @param onLoad - if true (A): draw two first tabs on listing details load, if false (B): then load by tabKey request
     * @param tabKey - tabKey *B
     * @param layout - tab container layout
     */
    public void drawListing(boolean onLoad, String tabKey, LinearLayout layout){
    	if ( onLoad ) {
			drawListingDetails(tabPager);
			drawListingSeller(tabPager);
			drawListingMap(tabPager);
			if ( videos.size() > 0 ) {
				TAB_NAMES.add(Lang.get("android_tab_caption_video"));
				TAB_KEYS.add("video");
				indicator.notifyDataSetChanged();
				adapter.notifyDataSetChanged();
				drawListingVideo(tabPager);
			}
    	}
    	else {
    		//deprecated for a while :)
    		if ( listingData.size() > 0 )
    		{
    			if ( tabKey.equals("details") ) {
    				//drawListingDetails(null, layout);
    			}
    			else if ( tabKey.equals("seller_info") ) {
    				//drawListingSeller(null, layout);
    			}
    			else if ( tabKey.equals("map") ) {
    				//drawListingMap(layout);
    			}
    		}
    	}
    }

    /**
     * drow listing details - details tab container
     * 
     * @param pager - pager to find layout
     */
    public static void drawListingDetails(ViewPager pager)
    {
    	LinearLayout detailsContainer = (LinearLayout) pager.findViewWithTag("details");
	
		if ( detailsContainer == null )
    		return;
		
    	detailsContainer.removeAllViews();
		detailsContainer.setGravity(Gravity.TOP);
    	
		/* inflate details tab */
    	LinearLayout details = (LinearLayout) Config.context.getLayoutInflater()
    			.inflate(R.layout.listing_details, null);
    	details.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    	
    	/* set title */
    	TextView title = (TextView) details.findViewById(R.id.title);
    	title.setText(listingData.get("title"));
    	
    	/* set price */
    	TextView price = (TextView) details.findViewById(R.id.price);
    	price.setText(listingData.get("price"));
    	
    	/* gallery */
    	LinearLayout gallery = (LinearLayout) details.findViewById(R.id.gallery);
        populateGallary(gallery, photos);

        /* listing details */
        if ( fields.size() > 0 ) {
        	LinearLayout fieldsTable = (LinearLayout) details.findViewById(R.id.fields_table);
        	int index = 0;
        	
	        for (HashMap<String, String> entry : fields) {

	        	if ( entry.get("item").equals("group") && !entry.get("name").isEmpty() ) {
	        		/* create row view */
	        		LinearLayout groupRow = (LinearLayout) Config.context.getLayoutInflater()
	    	    			.inflate(R.layout.fieldset, null);
	        		
	        		/* set margin */
	        		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	        		int topmargin = index > 0 ? Utils.dp2px(5) : 0;
	    			params.setMargins(0, topmargin, 0, Utils.dp2px(5));
	    			groupRow.setLayoutParams(params);
	        		
	    			/* set divider name */
	        		TextView groupName = (TextView) groupRow.findViewById(R.id.divider_text);
	        		groupName.setText(entry.get("name"));
	        		
	        		fieldsTable.addView(groupRow, index);
	        		index++;
	        	}
	        	else if (entry.get("item").equals("field")) {
					if (entry.get("key").matches("escort_rates|availability")) {
						fieldsTable.addView(escortFieldsDisplay(entry), index);
						index++;
					} else if (entry.get("key").equals("escort_tours")) {
						fieldsTable.addView(escortToursDisplay(entry), index);
						index++;
					} else {
						/* create row view */
						LinearLayout fieldRow = (LinearLayout) Config.context.getLayoutInflater()
								.inflate(R.layout.listing_details_field, null);

						/* set field name */
						TextView fieldName = (TextView) fieldRow.findViewById(R.id.field_name);
						fieldName.setText(entry.get("name") + ":");

						/* set field value */
						TextView fieldValue = (TextView) fieldRow.findViewById(R.id.field_value);

						if (entry.get("type").equals("image")) {
							fieldValue.setVisibility(View.GONE);
							ImageView fieldImage = (ImageView) fieldRow.findViewById(R.id.field_image);
							fieldImage.setVisibility(View.VISIBLE);
							Utils.imageLoaderDisc.displayImage(entry.get("value"), fieldImage, Utils.imageLoaderOptionsDisc);
						} else {
							/* set field value */
							fieldValue.setText(Html.fromHtml(entry.get("value")));
							fieldValue.setMovementMethod(LinkMovementMethod.getInstance());
						}
						fieldsTable.addView(fieldRow, index);
						index++;
					}
				}
	        }
        }
        
        /* add tab view to container */
        detailsContainer.addView(details);
        
        /* share icon handler */
        ImageView iconShare = (ImageView) details.findViewById(R.id.icon_share);
        iconShare.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
				shareIntent.setType("text/plain");
				shareIntent.putExtra(Intent.EXTRA_TEXT, listingData.get("url"));
				instance.startActivity(Intent.createChooser(shareIntent, "Share..."));
            }
        });
        
        /* comment */
        final ImageView iconComments = (ImageView) details.findViewById(R.id.icon_comments);
        Boolean comments_login = false;
        if ( Utils.getCacheConfig("comments_login_access") != null ) {
            if ( Utils.getCacheConfig("comments_login_access").equals("0") ) {
                comments_login = true;
            }
        }
        if ( Utils.getCacheConfig("comment_plugin").equals("1") && comments_login == true ) {
        	final LinearLayout commentsView = (LinearLayout) details.findViewById(R.id.comments_view); 
        	commentsContent = (LinearLayout) details.findViewById(R.id.comments);
        	LinearLayout commentDivider = (LinearLayout) details.findViewById(R.id.comments_divider);
        	TextView dividerText = (TextView) commentDivider.findViewById(R.id.divider_text);
        	dividerText.setText(Lang.get("android_comments_divider"));
        	
        	/* add comments */
	        Listing.populateComments(commentsContent, comments);
        	
        	/* icon handler */
            final ScrollView scroll_content = (ScrollView) instance.findViewById(R.id.listing_details);
	        iconComments.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
					scroll_content.post(new Runnable() {
						@Override
						public void run() {
							scroll_content.smoothScrollBy( 0, commentsView.getTop());
						}
                    });
	            }
	        });
	        

            see_more_comments = (TextView) details.findViewById(R.id.see_more_comments);
            see_more_comments.setText(Lang.get("see_more"));

            see_more_view = (LinearLayout) details.findViewById(R.id.see_comments_view);
	       
            if ( comments.size() >= 5 ) {
                see_more_view.setVisibility(View.VISIBLE);
    		}
            else {
                see_more_view.setVisibility(View.GONE);
            }

            /* add comment activity */
            add_comment_button = (Button) details.findViewById(R.id.add_comment);
            if ( Utils.getCacheConfig("comments_login_post").equals("1") && Account.loggedIn == false ) {
                add_comment_button.setVisibility(View.GONE);
            }
            else {
				add_comment_button.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						Context context = instance;
						Listing.showCommentDialog(context, false);
					}
				});
            }

            see_more_comments.setOnClickListener(new View.OnClickListener() {
                @Override
	        	public void onClick(View v) {
                    Intent intent = new Intent(instance, CommentsActivity.class);
                    intent.putExtra("listing_id", ldListingID);
                    if ( Account.loggedIn ) {
                        String account_id = Account.accountData.get("id");
                        intent.putExtra("account_id", account_id.toString());
                    }
                    Config.context.startActivity(intent);
	            }
	        });
        }
        else {
            LinearLayout commentsView = (LinearLayout) details.findViewById(R.id.comments_view);
            commentsView.setVisibility(View.GONE);
        	iconComments.setVisibility(View.GONE);
        }
        
        /* favorite icon handler */
        final ImageView iconFavorite = (ImageView) details.findViewById(R.id.icon_like);
        String favoriteIDs = Utils.getSPConfig("favoriteIDs", "");
    	if ( favoriteIDs.length() > 0 && favoriteIDs.indexOf(ldListingID) >= 0 ) {
    		iconFavorite.setImageResource(R.mipmap.details_icon_like_active);
    	}
        
        iconFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	int prevId = Config.context.getResources()
            			.getIdentifier(Config.currentView, "id", Config.context.getPackageName());
            	ImageView paferenceIcon = (ImageView) Config.context.getWindow()
            			.findViewById(prevId).findViewWithTag("favorite_"+ldListingID); 
            	
            	String favoriteIDs = Utils.getSPConfig("favoriteIDs", "");
            	
            	if ( favoriteIDs.length() > 0 && favoriteIDs.indexOf(ldListingID) >= 0 ) {
            		favoriteIDs = Utils.removeFromSet(favoriteIDs, ldListingID);
            		iconFavorite.setImageResource(R.mipmap.details_icon_like);

            		Listing.countFavorites("remove");

            		if ( paferenceIcon != null ) {
            			paferenceIcon.setBackgroundResource(R.mipmap.icon_like);
            		}

            		/* favorites manager mode */
                    if ( Config.activeInstances.contains("Favorites") ) {
                        if (intent.getSerializableExtra("listingHash") != null) {
							Favorites.ListingsAdapter.removeEntry((HashMap<String, String>) intent.getSerializableExtra("listingHash"));
						}
                        else {
                            Favorites.ListingsAdapter.removeEntry(listingData);
                        }
            		}
                }
				else {
            		favoriteIDs = Utils.addToSet(favoriteIDs, ldListingID);
            		iconFavorite.setImageResource(R.mipmap.details_icon_like_active);

            		Listing.countFavorites("add");

            		if ( paferenceIcon != null ) {
            			paferenceIcon.setBackgroundResource(R.mipmap.details_icon_like_active);
            		}

            		/* add favorite to favorites if controller available */
            		if ( Config.activeInstances.contains("Favorites") ) {
                        if (intent.getSerializableExtra("listingHash") != null) {
							Favorites.ListingsAdapter.addEntry((HashMap<String, String>) intent.getSerializableExtra("listingHash"));
						}
                        else {
                            Favorites.ListingsAdapter.addEntry(listingData);
                        }
            		}
            	}
            	Utils.setSPConfig("favoriteIDs", favoriteIDs);
            }
        });
    }

	/**
	 * draw listing details - show escort rates/availibility
	 *
	 * @param hashmap field - field
	 */
	public static LinearLayout escortFieldsDisplay( HashMap<String, String> field ) {
		LinearLayout escortFieldsContainer = new LinearLayout(Config.context);

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		escortFieldsContainer.setLayoutParams(params);
		escortFieldsContainer.setOrientation(LinearLayout.VERTICAL);

		ArrayList<HashMap<String, String>> escortRates = JSONParser.parseJsontoArrayList(field.get("value"));

		if(escortRates.size() > 0) {
			for (HashMap<String, String> entry : escortRates) {
				/* create row view */
				LinearLayout fieldRow = (LinearLayout) Config.context.getLayoutInflater()
						.inflate(R.layout.listing_details_field, null);

				/* set field name */
				TextView fieldName = (TextView) fieldRow.findViewById(R.id.field_name);
				String name = "";
				if(field.get("key").equals("escort_rates")) {
					name = entry.get("title");
				}
				else if(field.get("key").equals("availability")) {
					name = entry.get("title");
				}
				fieldName.setText(name + ":");

				/* set field value */
				TextView fieldValue = (TextView) fieldRow.findViewById(R.id.field_value);
				String value = "";
				if(field.get("key").equals("escort_rates")) {
					value = entry.get("subtitle");
				}
				else if(field.get("key").equals("availability")) {
					value = entry.get("from") +" - "+ entry.get("to");
				}
				fieldValue.setText(value);
				escortFieldsContainer.addView(fieldRow);
			}
		}
		else {}

		return escortFieldsContainer;
	}

	/**
	 * draw listing details - show escort rates/availibility
	 *
	 * @param hashmap field - field
	 */
	public static LinearLayout escortToursDisplay( HashMap<String, String> field ) {
		LinearLayout escortTourContainer = new LinearLayout(Config.context);

		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		escortTourContainer.setLayoutParams(params);
		escortTourContainer.setOrientation(LinearLayout.VERTICAL);

		final ArrayList<HashMap<String, String>> escortRates = JSONParser.parseJsontoArrayList(field.get("value"));

		if(escortRates.size() > 0) {
			for (HashMap<String, String> entry : escortRates) {
				/* create row view */
				LinearLayout fieldRow = (LinearLayout) Config.context.getLayoutInflater()
						.inflate(R.layout.listing_details_field, null);
				fieldRow.setOrientation(LinearLayout.VERTICAL);
				/* set field name */
				TextView fieldName = (TextView) fieldRow.findViewById(R.id.field_name);
				String name = entry.get("from") +" - "+ entry.get("to");
				fieldName.setText(name);
				fieldName.setLayoutParams(params);

				/* set field value */
				TextView fieldValue = (TextView) fieldRow.findViewById(R.id.field_value);
				fieldValue.setText(entry.get("location"));
				escortTourContainer.addView(fieldRow);
			}
		}
		else {}

		if(escortRates.size()>1) {
			TextView show_on_map = new TextView(Config.context);

			LayoutParams paramsText = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			paramsText.setMargins(Utils.dp2px(15), Utils.dp2px(5), Utils.dp2px(15), 0);
			show_on_map.setLayoutParams(paramsText);

			show_on_map.setText(Lang.get("android_show_on_map"));
			show_on_map.setTextColor(Color.parseColor("#006ec2"));
			show_on_map.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					Intent intent = new Intent(Config.context, ToursOnMapActivity.class);
					intent.putExtra("data", escortRates);
					Config.context.startActivity(intent);
				}
			});

			escortTourContainer.addView(show_on_map);
		}


		return escortTourContainer;
	}
    
    /**
     * draw listing details - comment
     *
     * @param comment - comment data
     */
    public static void addComment( HashMap<String, String> comment ) {
        	
        	comment.put("listing_id", ldListingID.toString());
        	
        	if ( Account.loggedIn ) { 
        		String account_id = Account.accountData.get("id");
        		comment.put("account_id", account_id.toString());
 		    }
        	
        	String now = DateFormat.getDateInstance().format(new Date());
        	comment.put("Date", now);
        	
            if ( comments.size() == 0 ) {
                see_more_view.setVisibility(View.GONE);
            }
            comments.add(comment);
            Listing.populateComments(commentsContent, comments);

            final ScrollView scroll_content = (ScrollView) instance.findViewById(R.id.listing_details);
            scroll_content.post(new Runnable() {
                @Override
                public void run() {
                    scroll_content.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });

            //  do async request
            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(30000); // set 30 seconds for this task
            final String url = Utils.buildRequestUrl("addComment", comment, null);
            client.post(url, Utils.toParams(comment), new AsyncHttpResponseHandler() {

				@Override
				public void onSuccess(int statusCode, Header[] headers, byte[] server_response) {
					// called when response HTTP status is "200 OK"
					try {
						String response = String.valueOf(new String(server_response, "UTF-8"));
						// parse response
						XMLParser parser = new XMLParser();
						Document doc = parser.getDomElement(response, url);

						if ( doc == null ) {
							commentsContent.removeViewAt(comments.size());
							comments.remove(comments.size()-1);
							Dialog.simpleWarning(Lang.get("returned_xml_failed"), instance);
						}
						else {
							NodeList errorNode = doc.getElementsByTagName("error");
							// handle errors
							if ( errorNode.getLength() > 0 ) {
								Element error = (Element) errorNode.item(0);
								Dialog.simpleWarning(Lang.get(error.getTextContent()), instance);
							}
							else {
								if ( Utils.getCacheConfig("comment_auto_approval").equals("1") ) {
									Dialog.toast("comment_added", instance);
								}
								else {
									Dialog.toast("comment_added_approval", instance);
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
    
    /**
     * draw listing details - seller tab container
     * 
     * @param pager - related view
     */
    public static void drawListingSeller(ViewPager pager){
    	LinearLayout sellerContainer;
    	
    	if ( Config.tabletMode ) {
    		LinearLayout mainContainer = (LinearLayout) pager.findViewWithTag("details");
    		sellerContainer = (LinearLayout) mainContainer.findViewById(R.id.seller_info);
    	}
    	else {
    		sellerContainer = (LinearLayout) pager.findViewWithTag("seller_info");
    	}
        sellerContainer.removeAllViews();
		sellerContainer.setGravity(Gravity.TOP);
		
		/* inflate seller tab */
    	LinearLayout sellerInfo = (LinearLayout) instance.getLayoutInflater()
    			.inflate(R.layout.listing_seller, null);
    	sellerInfo.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    	
    	/* set seller thumbnail */
    	ImageView sellerThumbnail = (ImageView) sellerInfo.findViewById(R.id.seller_thumbnail);
    	if ( sellerData.containsKey("thumbnail") ) {
	    	Utils.imageLoaderDisc.displayImage(sellerData.get("thumbnail"), sellerThumbnail, Utils.imageLoaderOptionsDisc);
    	}
    	
    	/* thumbnail click listener */
    	sellerThumbnail.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	HashMap<String, String> accountHash = new HashMap<String, String>();
            	accountHash.put("photo", sellerData.get("thumbnail"));
            	accountHash.put("full_name", sellerData.get("name"));
            	Intent intent = new Intent(Config.context, AccountDetailsActivity.class);
        		intent.putExtra("id", sellerData.get("ID"));
        		intent.putExtra("accountHash", accountHash);				
        		Config.context.startActivity(intent);
            }
    	});
    	
    	/* set seller name */
    	TextView sellerName = (TextView) sellerInfo.findViewById(R.id.name);
    	sellerName.setText(sellerData.get("name"));
    	
    	/* seller fields */
        if ( sellerFields.size() > 0 ) {
        	LinearLayout fieldsTable = (LinearLayout) sellerInfo.findViewById(R.id.fields_table);
        	int index = 0;
        	
	        for (HashMap<String, String> entry : sellerFields) {
        		/* create row view */
        		LinearLayout fieldRow = (LinearLayout) Config.context.getLayoutInflater()
    	    			.inflate(R.layout.seller_info_field, null);
        		
        		/* set field name */
        		TextView fieldName = (TextView) fieldRow.findViewById(R.id.field_name);
        		fieldName.setText(entry.get("name")+":");
        		
        		TextView fieldValue = (TextView) fieldRow.findViewById(R.id.field_value);
        		
        		if ( entry.get("type").equals("image" ) ) {
        			fieldValue.setVisibility(View.GONE);
        			ImageView fieldImage = (ImageView) fieldRow.findViewById(R.id.field_image);
        			fieldImage.setVisibility(View.VISIBLE);
        			Utils.imageLoaderDisc.displayImage(entry.get("value"), fieldImage, Utils.imageLoaderOptionsDisc);
        		}
        		else {
	        		/* set field value */
	        		fieldValue.setText(Html.fromHtml(entry.get("value")));
	        		fieldValue.setMovementMethod(LinkMovementMethod.getInstance());
        		}
        		fieldsTable.addView(fieldRow, index);
        		index++;
	        }
        }
    	
    	/* add tab view to container */
    	sellerContainer.addView(sellerInfo);
    	
    	/* contact seller handler */
        ImageView iconContact = (ImageView) sellerInfo.findViewById(R.id.icon_contact);
        iconContact.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                contactOwner();
            }
        });
		if (Account.loggedIn) {
			if (Account.accountData.get("id").equals(sellerData.get("ID"))) {
				iconContact.setVisibility(View.GONE);
			}
		}

        /* other listings button listener */
        Button otherListings = (Button) sellerContainer.findViewById(R.id.other_listings);
        if ( Integer.parseInt(sellerData.get("listings_count")) > 0 ) {
        	String buttonText = (String) otherListings.getText() + " ("+sellerData.get("listings_count")+")";
        	otherListings.setText(buttonText);
        	
	        otherListings.setOnClickListener(new View.OnClickListener() {
	            public void onClick(View v) {
	            	HashMap<String, String> accountHash = new HashMap<String, String>();
	            	accountHash.put("photo", sellerData.get("thumbnail"));
	            	accountHash.put("full_name", sellerData.get("name"));
	            	Intent intent = new Intent(Config.context, AccountDetailsActivity.class);
	        		intent.putExtra("id", sellerData.get("ID"));
	        		intent.putExtra("focusOnListings", "1");
	        		intent.putExtra("accountHash", accountHash);				
	        		Config.context.startActivity(intent);
	            }
	        });
        }
        else {
        	otherListings.setVisibility(View.GONE);
        }
    }
    
    public static void contactOwner() {
		if (Account.loggedIn) {
			HashMap<String, String> accountHash = new HashMap<String, String>();

			accountHash.put("photo", sellerData.get("thumbnail")==null ? "":sellerData.get("thumbnail"));
			accountHash.put("full_name", sellerData.get("name"));
			Intent intent = new Intent(Config.context, MessagesActivity.class);
			intent.putExtra("id", sellerData.get("ID"));
			intent.putExtra("data", accountHash);
			intent.putExtra("sendMail", "1");
			intent.putExtra("listing_id", ldListingID);
			Config.context.startActivity(intent);
		}
		else {
			Intent intent = new Intent(Config.context, ContactOwnerActivity.class);
			intent.putExtra("id", sellerData.get("ID"));
			intent.putExtra("listing_id", ldListingID);
			instance.startActivityForResult(intent, 1104);
        }

    }

    public static void drawListingVideo(ViewPager pager) {
    	LinearLayout videoContainer = (LinearLayout) pager.findViewWithTag("video");
        videoContainer.removeAllViews();
		
		if ( videos.size() > 0 && Config.cacheListingTypes.get(listingData.get("listing_type")).get("video").equals("1") ) {
			videoContainer.setGravity(Gravity.TOP);
			
			/* inflate video tab view */
	    	LinearLayout videoTab = (LinearLayout) instance.getLayoutInflater()
	    			.inflate(R.layout.listing_video, null);
	    	videoTab.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	    	
	    	VideoAdapter adapter = new VideoAdapter(videos);

	    	/* get grid view */
	    	GridView gridView = (GridView) videoTab.findViewById(R.id.video_grid);
	    	gridView.setAdapter(adapter);
	    	gridView.setOnScrollListener(new PauseOnScrollListener(Utils.imageLoaderMixed, true, true));
	    	gridView.setOnItemClickListener(adapter);

	    	videoContainer.addView(videoTab);
		}
		else {
			TextView message = (TextView) Config.context.getLayoutInflater()
	    			.inflate(R.layout.info_message, null);
    		
    		message.setText(Lang.get("android_no_listing_video"));
    		videoContainer.addView(message);
		}
    }



	@Override
	public void onMapReady(GoogleMap googleMap) {
		map = googleMap;
	}


	/**
     * draw listing details - map tab container
     * 
     * @param pager - related view
     */
    public static void drawListingMap(ViewPager pager){
    	/* create map */
    	if ( map == null ) {
    		return;
    	}

        if ( !listingData.get("direct").isEmpty() && listingData.get("direct").contains(",") ) {
        	String[] coordinates = listingData.get("direct").split(",");
        	LatLng position = new LatLng(Double.valueOf(coordinates[0]), Double.valueOf(coordinates[1]));
        	MarkerOptions marker = new MarkerOptions().position(position);
        	if ( !listingData.get("search").isEmpty() )
        	{
        		marker.title(listingData.get("search"));
        	}
            map.addMarker(marker);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position,
            		Listing.getAvailableZoomLevel(map, Utils.getCacheConfig("android_listing_details_map_zoom"))));
        }
        else {
        	Toast.makeText(instance, Lang.get("android_no_listing_location_found"), Toast.LENGTH_SHORT).show();
        }
    }
	
    /**
     * populate gallery container
     * 
     * @param gallery - gallery view
     * @param photos - photos array
     */
    public static void populateGallary(LinearLayout gallery, List<HashMap<String, String>> photos){
    	if ( photos.size() > 0 ) {
    		/* set gallery container visible */
    		gallery.setVisibility(View.VISIBLE);
	    	
	    	/* add pictures to the gallery view */
	    	int index = 0;
			for (final HashMap<String, String> entry : photos) {
				final int final_index = index;
				LinearLayout container = (LinearLayout) Config.context.getLayoutInflater()
		    			.inflate(R.layout.gallery_image, null);
				
				LayoutParams params = new LayoutParams(
				        LayoutParams.WRAP_CONTENT,
				        LayoutParams.WRAP_CONTENT
				);

				if (Lang.isRtl()) {
					params.setMargins(Utils.dp2px(15), 0, 0, 0);
				} else {
					params.setMargins(0, 0, Utils.dp2px(15), 0);
				}

				container.setLayoutParams(params);
				
				gallery.addView(container);
				
				ImageView image = (ImageView) container.findViewById(R.id.image);
				Utils.imageLoaderDisc.displayImage(entry.get("Thumbnail"), image, Utils.imageLoaderOptionsDisc);
				
				/* add listener */
				image.setOnClickListener(new View.OnClickListener() {
		            public void onClick(View v) {
		            	Intent intent = new Intent(ListingDetailsActivity.instance, GalleryActivity.class);
		        		intent.putExtra("index", final_index+"");
		        		ListingDetailsActivity.instance.startActivity(intent);
		            }
		        });
				index++;
			}
    	}
    }

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this);
	}

	@Override
    public void onStop() {
    	client.cancelRequests(instance, true);
		EasyTracker.getInstance(this).activityStop(this);
    	super.onStop();
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RETURN_CODE:
                if ( resultCode == RESULT_OK ) {
                    loadData();

                    String phrase = Utils.getCacheConfig("edit_listing_auto_approval").equals("1") ? "listing_edit_auto_approved" : "listing_edit_pending";
                    Dialog.simpleWarning(Lang.get(phrase), instance);
                }
                break;
			case 1104:
				if ( resultCode == RESULT_OK ) {
				Toast.makeText(instance, Lang.get("android_message_sent"), Toast.LENGTH_SHORT).show();
				}
                break;
        }
    }

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_listing_details, menu);
		
		/* set phrases */
		for (int i=0; i<menu.size(); i++) {
			String title = (String) menu.getItem(i).getTitle();
			menu.getItem(i).setTitle(Lang.get(title));
		}
        pMenu = menu;
        return true;
    }
	

    private void updateMenu() {
        if (pMenu != null) {
            MenuItem item = pMenu.findItem(R.id.map_type);
            if ( TAB_KEYS.get(tabPager.getCurrentItem()).equals("map") ) {
                item.setVisible(true);
            }
            else {
                item.setVisible(false);
            }

            MenuItem itemC = pMenu.findItem(R.id.contact_owner);
            MenuItem itemEdit= pMenu.findItem(R.id.edit_listing_button);
            if ( Account.loggedIn && Account.accountData.get("id").equals(sellerData.get("ID")) ) {
                itemEdit.setVisible(true);
				itemC.setVisible(false);
            }
            else {
				itemC.setVisible(true);
                itemEdit.setVisible(false);
            }
			MenuItem reportItem = pMenu.findItem(R.id.report_listing);
			if ( Utils.getCacheConfig("reportBroken_plugin").equals("1") ) {
				reportItem.setVisible(true);
			}
			else {
				reportItem.setVisible(false);
			}

            //ActivityCompat.invalidateOptionsMenu(this.instance);
        }
    }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenu();
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        	case R.id.map_type:
	            Dialog.mapTypeDialog(instance, map);
	            return true;
            
            case R.id.contact_owner:
                contactOwner();
                tabPager.setCurrentItem(TAB_KEYS.indexOf("seller_info"));
	            return true;

            case R.id.report_listing:
				Intent report_intent = new Intent(instance, ReportListingActivity.class);
				report_intent.putExtra("id", ldListingID);
				instance.startActivity(report_intent);
	            return true;

            case R.id.edit_listing_button:
                Intent edit_intent = new Intent(instance, AddListingActivity.class);
                edit_intent.putExtra("id", ldListingID);
                edit_intent.putExtra("listingHash", listingData);
                instance.startActivityForResult(edit_intent, RETURN_CODE);
            return true;
            
	        case android.R.id.home:
	        	super.onBackPressed();
				return true;
				
	        default:
	            return super.onOptionsItemSelected(item);
        }
    }
}