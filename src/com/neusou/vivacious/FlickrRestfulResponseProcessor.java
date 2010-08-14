package com.neusou.vivacious;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;

import com.neusou.vivacious.RestfulClient.RestfulMethod;
import com.neusou.vivacious.RestfulClient.RestfulResponse;

public class FlickrRestfulResponseProcessor extends RestfulResponseProcessor<RestfulResponse> {
	
	public static final String LOG_TAG = FlickrRestfulResponseProcessor.class.getSimpleName();
	
	public FlickrRestfulResponseProcessor() {
		super(LOG_TAG);
	}
	
	protected void handleResponse(RestfulResponse response, RestfulMethod method, Bundle data, String error){
		
		Logger.l(Logger.DEBUG, LOG_TAG,"error: "+ (error==null?"null":error));
		
		if(response != null){
		try {
			JSONObject jsonResponse = new JSONObject(response.data);
			//Logger.l(Logger.DEBUG, LOG_TAG, jsonResponse.toString(3));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		}
		
	}	
	
}
