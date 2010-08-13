package com.neusou.vivacious;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class FlickrIntentService extends IntentService {

	public static final String LOG_TAG = "FlickrIntentService";

	public FlickrIntentService() {
		super("FlickrIntentService");
	}
		
	@Override
	protected void onHandleIntent(Intent intent) {		
		String name = Thread.currentThread().getName();
		String action = intent.getAction();
		Bundle data = intent.getExtras();		
		Log.d(LOG_TAG, name + ", " + action);
			
		if(action == null){
			Flickr f = Flickr.getInstance();    	
        	Bundle b = intent.getExtras();
        	f.restfulClient.execute(b);
		}
		
	}

}
