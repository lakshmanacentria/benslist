package com.acentria.benslist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.InputFilter;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class Listing {

	public static int lastRequestTotalListings;
	public static ArrayList<HashMap<String, String>> sortingFields = new ArrayList<HashMap<String, String>>();
	
    private static AlertDialog comment_dialog;
    public static RatingBar rating;
    public static LinearLayout add_comment_view;
    public static Context customContext;
    public static Boolean checkComment;

	/**
	 * prepare listing in grid, parse xml and populate listing fields
	 * 
	 * @param listings - listings data
	 * @param type - listing type key
	 * @param dateDiff - is date diff field presents (recently added mode)
	 * @param aFields - additional fields to parse
	 * @return
	 */
    public static ArrayList<HashMap<String, String>> prepareGridListing(NodeList listings, String type, boolean dateDiff, String[] aFields){
		int tmp_date = 0;

		ArrayList<HashMap<String, String>> listingsOut = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> tmpFields, tmpSortingField;
		
		for( int i=0; i<listings.getLength(); i++ )
		{
			Element listing = (Element) listings.item(i);
			
			if ( listing.getTagName().equals("statistic") ) {
				NodeList stats = listing.getChildNodes();
				for ( int j = 0; j < stats.getLength(); j++ )
				{
					Element tag = (Element) stats.item(j);
					if ( tag.getTagName().equals("total") ) {
						lastRequestTotalListings = tag.getTextContent().isEmpty() ? 0 : Integer.parseInt(tag.getTextContent());
					}
				}
			}
			else if ( listing.getTagName().equals("sorting") ) {
				sortingFields.clear();
				
				NodeList sortingNodes = listing.getChildNodes();
				for ( int j = 0; j < sortingNodes.getLength(); j++ ) {
					tmpSortingField = new HashMap<String, String>();//clear steck
					Element sortingField = (Element) sortingNodes.item(j);
					
					tmpSortingField.put("name", sortingField.getTextContent());
					tmpSortingField.put("key", sortingField.getAttribute("key"));
					tmpSortingField.put("type", sortingField.getAttribute("type"));
					
					sortingFields.add(tmpSortingField);
				}
			}
			else {
				//NodeList fields = listing.getChildNodes();
				tmpFields = new HashMap<String, String>();//clear steck 
				
				/* convert fields from nodes to array */
				tmpFields.put("id", Utils.getNodeByName(listing, "id"));
				tmpFields.put("photo", Utils.getNodeByName(listing, "main_photo"));
				tmpFields.put("photos_count", Utils.getNodeByName(listing, "photos_count"));
				tmpFields.put("listing_type", Utils.getNodeByName(listing, "listing_type"));
				tmpFields.put("featured", Utils.getNodeByName(listing, "featured"));
				tmpFields.put("price", Utils.getNodeByName(listing, Utils.getCacheConfig("price_field_key")));
				tmpFields.put("title", Utils.getNodeByName(listing, "title"));
				tmpFields.put("middle_field", Utils.getNodeByName(listing, "middle_field"));
				
				/* additional fields handler */
				if ( aFields != null && aFields.length > 0 ) {
					for (int j = 0; j < aFields.length; j++) {
						tmpFields.put(aFields[j], Utils.getNodeByName(listing, aFields[j]));
					}
				}
				
				type =  Utils.getNodeByName(listing, "listing_type") != null ? Utils.getNodeByName(listing, "listing_type") : type;
				
				tmpFields.put("photo_allowed", Config.cacheListingTypes.get(type).get("photo"));//photo allowed by listing type
//				tmpFields.put("page_allowed", Config.cacheListingTypes.get(type).get("page"));//own page allowed by listing type
				
				/* date_diff */
				if ( dateDiff )
				{
					DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
	
					int date = Integer.parseInt(Utils.getNodeByName(listing, "date_diff"));
					if ( tmp_date != date )
					{
						String caption = "";
						if ( date == 1 )
						{
							caption = Lang.get("android_today");
						}
						else if ( date == 2 )
						{
							caption = Lang.get("android_yesterday");
						}
						else if ( date > 2 && date < 8 )
						{
							caption = Lang.get("android_number_days_ago").replace("{number}", (date-1)+"");
						}
						else
						{
							Calendar currentDate = Calendar.getInstance();
							currentDate.add(Calendar.DAY_OF_MONTH, -(date-1));
							String setDate = df.format(currentDate.getTime());
							caption = setDate;
						}
						
						tmpFields.put("date_diff", caption);
						tmp_date = date;
					}
					else
					{
						tmpFields.put("date_diff", "");
					}
				}
				else
				{
					tmpFields.put("date_diff", "");
				}
	
				listingsOut.add(i, tmpFields);
			}
		}
		
		return listingsOut;
	}    

    /**
     * populate comments container
     * 
     * @param view - comments view
     * @param comments - comments data
     */
    public static void populateComments(LinearLayout view, ArrayList<HashMap<String, String>> comments){

        view.removeAllViews();
    	if ( comments.size() > 0 ) {

			for (final HashMap<String, String> entry : comments) {
				
				LinearLayout container = (LinearLayout) Config.context.getLayoutInflater()
		    			.inflate(R.layout.comment_item, null);
				
				LayoutParams params = new LayoutParams(
				        LayoutParams.MATCH_PARENT,
				        LayoutParams.WRAP_CONTENT
				);
				container.setLayoutParams(params);

				/* fill in data */
				TextView title = (TextView) container.findViewById(R.id.title);
				title.setText(entry.get("Title"));

				TextView author = (TextView) container.findViewById(R.id.author);
				author.setText(entry.get("Author"));
				
				TextView date = (TextView) container.findViewById(R.id.date);
                if ( entry.get("Status").equals("pending") ) {
                    date.setText( Html.fromHtml("<font color='#EE0000'>"+Lang.get("status_pending")+"</font>"));
                }
                else {
					date.setText(entry.get("Date"));
                }
				
				TextView description = (TextView) container.findViewById(R.id.description);
				if ( entry.containsKey("use_html")) {
					description.setText(entry.get("Description"));
				}
				else {
					description.setText(Html.fromHtml(entry.get("Description")));
				}								
				description.setMovementMethod(LinkMovementMethod.getInstance());
				
                RatingBar rating = (RatingBar) container.findViewById(R.id.comments_rating);
				if ( Utils.getCacheConfig("comments_rating_module").equals("1") ) {
                    int star = Integer.parseInt(entry.get("Rating"));
                    if ( star > 0 ) {
                        rating.setRating(star);
                        rating.setVisibility(View.VISIBLE);
                    }
                    else {
                        rating.setVisibility(View.GONE);
                    }
                }
				view.addView(container);
			}
		}
    	else {
    		TextView message = (TextView) ListingDetailsActivity.instance.getLayoutInflater()
	    			.inflate(R.layout.info_message, null);
    		
    		message.setText(Lang.get("android_no_comments_exist"));
    		view.addView(message);
    	}
    }

    /*
    * show add comments dialog
    */
    public static void showCommentDialog( Context cont, Boolean check ) {

        checkComment = check;
        customContext = cont;

        AlertDialog.Builder builder = new AlertDialog.Builder(customContext);
        builder.setTitle(Lang.get("add_comment"));
        LayoutInflater content = LayoutInflater.from(customContext);

        add_comment_view = (LinearLayout) content.inflate(R.layout.add_comment, null);
        builder.setView(add_comment_view);

        rating = (RatingBar)add_comment_view.findViewById(R.id.add_rating);

        if ( Utils.getCacheConfig("comments_rating_module").equals("1") ) {
            rating.setVisibility(View.VISIBLE);
        }
        else {
            rating.setVisibility(View.GONE);
        }

        if ( Account.loggedIn ) {
            TextView author = (TextView) add_comment_view.findViewById(R.id.add_author);
            author.setText(Account.accountData.get("full_name"));
        }

        TextView add_description = (TextView) add_comment_view.findViewById(R.id.add_description);
        String maxLength =  Utils.getCacheConfig("comment_message_symbols_number");
        add_description.setFilters(new InputFilter[] {new InputFilter.LengthFilter(Integer.parseInt(maxLength))});

        builder.setPositiveButton(Lang.get("add_comment"), onClickAddComments);
        builder.setNeutralButton(Lang.get("android_dialog_cancel"), onClickAddComments);

        comment_dialog = builder.create();
        comment_dialog.show();

        Button btnOK = comment_dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnOK.setOnClickListener(addCommentsOK);
    }

    static DialogInterface.OnClickListener onClickAddComments = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {}
    };

    static View.OnClickListener addCommentsOK = new View.OnClickListener() {

        public void onClick(View v) {

            //  set variables
            Boolean error = false;
            HashMap<String, String> comment = new HashMap<String, String>();
            Float ratings = rating.getRating();
            int rating = Math.round(ratings);
            comment.put("Rating", Integer.toString(rating));

            TextView author = (TextView) add_comment_view.findViewById(R.id.add_author);
            if ( author.getText().toString().isEmpty() ) {
                error = true;
                author.setError(Lang.get("no_field_value"));
            }
            else {
                author.setError(null);
                comment.remove("Author");
                comment.put("Author", author.getText().toString());
            }
				
            TextView title = (TextView) add_comment_view.findViewById(R.id.add_title);
            if ( title.getText().toString().isEmpty() ) {
                error = true;
                title.setError(Lang.get("no_field_value"));
            }
            else {
                title.setError(null);
                comment.remove("Title");
                comment.put("Title", title.getText().toString());
			}

            TextView description = (TextView) add_comment_view.findViewById(R.id.add_description);
            if ( description.getText().toString().isEmpty() ) {
                error = true;
                description.setError(Lang.get("no_field_value"));
		    }
    	    else {
                description.setError(null);
                comment.remove("Description");
                comment.put("Description", description.getText().toString());
            }
    		
            if ( Utils.getCacheConfig("comment_auto_approval").equals("0") ) {
                comment.put("Status", "pending");
            }
            else {
                comment.put("Status", "active");
            }
            comment.put("use_html", "1");

    		
            if ( error == false ) {
                if (checkComment) {
                    CommentsActivity.addComment(comment);
                } else {
                    ListingDetailsActivity.addComment(comment);
                }
                comment_dialog.dismiss();
    	    }
        }
    };

    /**
     * get available zoom level
     * 
     * @param map - google map object
     * @param zoom - zoom requested
     * 
     * @return available zoom level for current map position
     */
    public static int getAvailableZoomLevel(GoogleMap map, String zoom) {
    	int set_zoom = Integer.parseInt(zoom);
    	if ( set_zoom < map.getMinZoomLevel() ) {
    		set_zoom = (int) map.getMinZoomLevel();
    	}
    	if ( set_zoom > map.getMaxZoomLevel() ) {
    		set_zoom = (int) map.getMaxZoomLevel();
    	}
		return set_zoom;
    }
    
    public static void countFavorites(String mode) {
    	String current = SwipeMenu.menuData.get(SwipeMenu.favoriteIndex).get("count");
    	int count = current == null ? 0 : Integer.parseInt(current);
    	count = mode == "add" ? count + 1 : count - 1;
    	String setCount = count == 0 ? null : count + "";
    	
    	SwipeMenu.menuData.get(SwipeMenu.favoriteIndex).put("count", setCount);
    	SwipeMenu.adapter.notifyDataSetChanged();
    }
}