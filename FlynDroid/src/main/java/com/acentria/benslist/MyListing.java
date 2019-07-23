package com.acentria.benslist;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;

public class MyListing {
	
	public int lastRequestTotalListings;
	public ArrayList<HashMap<String, String>> sortingFields = new ArrayList<HashMap<String, String>>();
	
	/**
	 * prepare listing in grid, parse xml and populate listing fields
	 * 
	 * @param listXml - listings data
	 * @param type - listing type key
	 * 
	 * @return
	 */
    public ArrayList<HashMap<String, String>> prepareGridListing(NodeList listings, String type){
		
		ArrayList<HashMap<String, String>> listingsOut = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> tmpFields, tmpSortingField;
		
		for( int i=0; i<listings.getLength(); i++ ) {
			Element listing = (Element) listings.item(i);
			
			if ( listing.getTagName().equals("statistic") ) {
				NodeList stats = listing.getChildNodes();
				for ( int j = 0; j < stats.getLength(); j++ ) {
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
				tmpFields = Utils.parseHash(listing.getChildNodes());
				
				type = type == null ? tmpFields.get("listing_type") : type;
				
				tmpFields.put("photo_allowed", Config.cacheListingTypes.get(type).get("photo"));//photo allowed by listing type
//				tmpFields.put("page_allowed", Config.cacheListingTypes.get(type).get("page"));//own page allowed by listing type
	
				listingsOut.add(i, tmpFields);
			}
		}

		return listingsOut;
	}
}