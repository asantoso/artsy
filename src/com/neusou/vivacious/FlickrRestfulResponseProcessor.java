package com.neusou.vivacious;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.neusou.bioroid.restful.RestfulResponseProcessor;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.restful.RestfulClient.RestfulResponse;

public class FlickrRestfulResponseProcessor extends IntentService {
	
	public static final String LOG_TAG = FlickrRestfulResponseProcessor.class.getSimpleName();
	RestfulResponseProcessor<RestfulResponse> mProcessor;
	
	public FlickrRestfulResponseProcessor(String name) {
		super(name);
		mProcessor = new RestfulResponseProcessor<RestfulResponse>(this, FlickrRestfulResponseProcessor.class) {			
			@Override
			protected void handleResponse(RestfulResponse response,
					RestfulMethod method, Bundle requestdata, String error) {
				Logger.l(Logger.DEBUG, LOG_TAG,"handleResponse. has error message?: "+ (error==null?"null":error));
				
				if(response != null){
				try {
					JSONObject jsonResponse = new JSONObject(response.getData());
					//Logger.l(Logger.DEBUG, LOG_TAG, jsonResponse.toString(3));
				} catch (JSONException e) {
					e.printStackTrace();
				}
				}
				
			}
		};
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		mProcessor.onHandleIntent(intent);		
	}	
	
}
