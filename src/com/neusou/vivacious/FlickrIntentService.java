package com.neusou.vivacious;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.neusou.bioroid.restful.RestfulResponseProcessor;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.restful.RestfulClient.RestfulResponse;

public class FlickrIntentService extends IntentService {

	public static final String LOG_TAG = Logger.registerLog(FlickrIntentService.class);
	
	private RestfulResponseProcessor<RestfulResponse> mProcessor; 

	public FlickrIntentService() {
		super(FlickrIntentService.class.getName());
		mProcessor = new RestfulResponseProcessor<RestfulResponse>(this, FlickrIntentService.class) {			
			@Override
			protected void handleResponse(RestfulResponse response,
					RestfulMethod method, Bundle requestdata, String error) {
			}
		};
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Logger.l(Logger.DEBUG, LOG_TAG,"onBind.");
		return super.onBind(intent);
	}
	
	@Override
	public void onCreate() {	
		super.onCreate();
		Logger.l(Logger.DEBUG, LOG_TAG,"onCreate.");	
	}
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
		Logger.l(Logger.DEBUG, LOG_TAG,"onDestroy");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {				
		
		String action = intent.getAction();
		Logger.l(Logger.DEBUG, LOG_TAG,"onHandleIntent. action:"+action);
		Flickr f = Flickr.getInstance();	
		
		if(action == null){
			
		}else if(action.equals(f.restfulClient.INTENT_EXECUTE_REQUEST)){
        	Bundle b = intent.getExtras();
        	f.restfulClient.execute(b);
		}else if(action.equals(f.restfulClient.INTENT_PROCESS_RESPONSE)){
			mProcessor.onHandleIntent(intent);	
		}
		
	}

}
