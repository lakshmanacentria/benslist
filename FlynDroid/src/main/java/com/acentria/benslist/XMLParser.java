package com.acentria.benslist;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLParser {

	// constructor
	public XMLParser() {
		
	}
	
	/**
	 * Getting XML DOM element
	 * @param XML string
	 * */
	public Document getDomElement(String xml, String url){
		Document doc = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
            xml = xml.replaceAll("&amp;quot;", "”").replaceAll("&amp;rsquo;", "'");

			dbf.setCoalescing(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
		        is.setCharacterStream(new StringReader(xml));
		        doc = db.parse(is);

			} catch (ParserConfigurationException e) {
				bugReport(e.getStackTrace(), url);
				
				Log.d("Error: ", e.getMessage());
				return null;
			} catch (SAXException e) {
				bugReport(e.getStackTrace(), url);
				
				Log.d("Error: ", e.getMessage());
	            return null;
			} catch (IOException e) {
				bugReport(e.getStackTrace(), url);
				
				Log.d("Error: ", e.getMessage());
				return null;
			} catch (Exception e) {
				bugReport(e.getStackTrace(), url);
				
				Log.d("Parser Error: ", e.getMessage());
				return null;
			}

			/* checking for the first table node, means if table received then mysql error occurred */
			if ( doc != null ) {
				if ( doc.getFirstChild().getNodeName().equals("table") ) {
		        	return null;
		        }
			}

	        return doc;
	}
	
	private void bugReport(StackTraceElement[] stackTrace, String url) {
		url = url == null ? "URL is not specified" : url;
		Utils.bugRequest("Received XML response is corrupt ("+ Utils.getSPConfig("domain", "") +")", stackTrace, "URL:"+url);
	}
	
	/** Getting node value
	  * @param elem element
	  */
	 public final String getElementValue( Node elem ) {
	     Node child;
	     if( elem != null){
	         if (elem.hasChildNodes()){
	             for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
	            	 return elem.getTextContent();
	             }
	         }
	     }
	     return "";
	 }
	 
	 /**
	  * Getting node value
	  * @param Element node
	  * @param key string
	  * */
	 public String getValue(Element item, String str) {		
			NodeList n = item.getElementsByTagName(str);
			return this.getElementValue(n.item(0));
	 }
	 
	 /**
	  * Get counts node
	  * @param XML
	  * @return ArrayList lang codes
	  * */
	 public static ArrayList XgetLangCodes(String xml){
		 
		 XMLParser parser = new XMLParser();
		 Document doc = parser.getDomElement(xml, "");
		 NodeList nl = doc.getElementsByTagName("item");
		 
		 ArrayList langCodes = new ArrayList(); 
		 for (int i=0; i<nl.getLength(); i++) {
			 Element e = (Element) nl.item(i);
		     String name = parser.getValue(e, "code");
		     langCodes.add(i,name);
		 }
		 
		 return langCodes;
	 }
	 
}