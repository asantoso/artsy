package com.neusou.vivacious;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.neusou.vivacious.RestfulClient.RestfulMethod;
import com.neusou.vivacious.RestfulClient.RestfulResponse;

public class FlickrRestfulResponseProcessor extends RestfulResponseProcessor<RestfulResponse> {
	
	public static final String LOG_TAG = FlickrRestfulResponseProcessor.class.getSimpleName();
	
	public FlickrRestfulResponseProcessor() {
	
	}
	
	protected void handleResponse(RestfulResponse response, RestfulMethod method, String error){
		
		Log.d(LOG_TAG,"error: "+ (error==null?"null":error));
		
		if(response != null){
		try {
			JSONObject jsonResponse = new JSONObject(response.data);
			Log.d(LOG_TAG, jsonResponse.toString(3));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		}
	}	
	
}
