package com.neusou.vivacious;

import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.vivacious.RestfulClient.RestfulResponse;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

public class RestfulResponseProcessor extends IntentService {
	
	public static final String LOG_TAG = RestfulResponseProcessor.class.getSimpleName();
	
	public RestfulResponseProcessor() {
		super(RestfulResponseProcessor.class.getCanonicalName());
	}
	
	private void broadcastCallback(Bundle data, String action){		
		RestfulClient.broadcastCallback(this,data,action);
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		String name = Thread.currentThread().getName();
		String action = intent.getAction();
		Bundle data = intent.getExtras();		
		Log.d(LOG_TAG, name + ", " + action);
			
		Flickr f = Flickr.getInstance();
		
		if(action == null){			    	
        	Bundle b = intent.getExtras();
        	f.restfulClient.execute(b);
		}
		else if(action.equals(f.restfulClient.INTENT_PROCESS_RESPONSE)){
			if(data.containsKey(f.restfulClient.XTRA_METHOD)){
				Parcelable restMethod = data.getParcelable(f.restfulClient.XTRA_METHOD);
				RestfulResponse response = data.getParcelable(f.restfulClient.XTRA_RESPONSE);
				try {
					JSONObject jsonResponse = new JSONObject(response.data);
					Log.d(LOG_TAG,jsonResponse.toString(3));
				} catch (JSONException e) {
					e.printStackTrace();
				}    		
				
			}else{
				Log.d(LOG_TAG,"no data");
			}    		
    	}    
		
		broadcastCallback(data,f.restfulClient.CALLBACK_INTENT);
	}
	
	
}
