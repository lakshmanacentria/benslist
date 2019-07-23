package com.acentria.benslist;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;


public class JSONParser {
 
    static InputStream is = null;
    static JSONObject jObj = null;
    static String json = "";
 
    // constructor
    public JSONParser() {
 
    }

    public JSONObject getJSONFromUrl(String url) {
 
        // Making HTTP request
        try {
            // defaultHttpClient
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
 
            HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();
 
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
 
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "n");
            }
            is.close();
            json = sb.toString();
        } catch (Exception e) {
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }
 
        // try parse the string to a JSON object
        try {
            jObj = new JSONObject(json);
        } catch (JSONException e) {
            Log.e("JSON Parser", "Error parsing data " + e.toString());
        }
 
        // return JSON String
        return jObj;
 
    }
    // Checker is Json or not
    public static boolean isJson(String Json) {
        try {
            new JSONObject(Json);
        } catch (JSONException ex) {
            try {
                new JSONArray(Json);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    // string
    // convert json to hash map
    public static HashMap<String, String> parseJson(String data) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        try {
            Object json = new JSONTokener(data).nextValue();

            if (json instanceof JSONObject) {
                hashMap = JSONObject2hash(data);
            }
            else if (json instanceof JSONArray) {
                hashMap = JSONArray2hash(data);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hashMap;
    }

    // string
    // convert json to array map
    public static ArrayList<HashMap<String, String>> parseJsontoArrayList(String data) {
        ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
        try {
            Object json = new JSONTokener(data).nextValue();

            if (json instanceof JSONObject) {
                JSONObject jsonObject = new JSONObject(data);
                Iterator<String> keysItr = jsonObject.keys();
                while(keysItr.hasNext()) {
                    String key = keysItr.next();
                    Object value = jsonObject.get(key);

                    arrayList.add(parseJson(value.toString()));
                }
            }
            else if (json instanceof JSONArray) {
                JSONArray jsonArray = new JSONArray(data);
                for (int i = 0; i < jsonArray.length(); i++) {
                    arrayList.add(JSONObject2hash(jsonArray.get(i).toString()));
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return arrayList;
    }



    public static HashMap<String, String> JSONObject2hash(String array) {
        HashMap<String, String> hashMap = new HashMap<String, String>();

        JSONObject json = null;

        try {
            json = new JSONObject( array );

            Iterator<String> keysItr = json.keys();
            while(keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = json.get(key);
                String val = value.toString().equals("null") ? "" : value.toString();
                hashMap.put(key, val);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hashMap;
    }

    public static HashMap<String, String> JSONArray2hash(String array) {
        HashMap<String, String> hashMap = new HashMap<String, String>();

        // Object value = data.get("data");
        JSONArray json = null;

        try {
            json = new JSONArray( array );
            for (int i = 0; i < json.length(); i++) {
                hashMap = JSONObject2hash(json.get(i).toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return hashMap;
    }
}