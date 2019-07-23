package com.acentria.benslist;

import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;

public class Categories {
	
	public static ArrayList<HashMap<String, String>> parse(NodeList categoryNodes){
		ArrayList<HashMap<String, String>> categories = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> categoryHash;

		for( int i=0; i<categoryNodes.getLength(); i++ )
		{
			NodeList category = categoryNodes.item(i).getChildNodes();
			categoryHash = new HashMap<String, String>();//clear steck 

			/* convert data from nodes to array */
			categoryHash.put("id", category.item(0).getTextContent());
			categoryHash.put("name", Config.convertChars(category.item(1).getTextContent()));
			categoryHash.put("count", category.item(2).getTextContent());
			categoryHash.put("sub_categories", category.item(3).getTextContent());
			
			categories.add(categoryHash);
		}

		return categories;
	}
}