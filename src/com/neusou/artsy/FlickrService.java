package com.neusou.artsy;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.neusou.Logger;
import com.neusou.bioroid.restful.RestfulResponseProcessor;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.restful.RestfulClient.RestfulResponse;

public class FlickrService extends Service {

	public static final String LOG_TAG = Logger.registerLog(FlickrService.class);
	RestfulResponseProcessor<RestfulResponse> mProcessor; 
	
	public FlickrService() {
		super();		
		mProcessor = new RestfulResponseProcessor<RestfulResponse>(this, FlickrService.class) {		
			@Override
			protected void handleResponse(RestfulResponse response, RestfulMethod method, Bundle requestdata, String error) {				
			}
		};
	}
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
		Logger.l(Logger.DEBUG, LOG_TAG, "onDestroy");
	}
	
	@Override
	public void onCreate() {	
		super.onCreate();
		Logger.l(Logger.DEBUG, LOG_TAG, "onCreate");			
	}
	
	/*
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Logger.l(Logger.DEBUG, LOG_TAG, "onStartCommand. action:"+intent.getAction());		
		return super.onStartCommand(intent, flags, startId);
	}
	*/
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if(intent == null){
			return;
		}
		String action = intent.getAction();
		Logger.l(Logger.DEBUG, LOG_TAG, "onStart. action:"+action);
		Flickr f = Flickr.getInstance();
		
		if(action == null){
			
		}
		else if(action.equals(f.restfulClient.INTENT_EXECUTE_REQUEST)){
	    	Bundle b = intent.getExtras();
        	f.restfulClient.execute(b);
		}else if(action.equals(f.restfulClient.INTENT_PROCESS_RESPONSE)){
			mProcessor.onHandleIntent(intent);	
		}
	}
	
	public void execute(Bundle data){
		Logger.l(Logger.DEBUG, LOG_TAG, "execute");			
		Flickr f = Flickr.getInstance();    	
      	f.restfulClient.execute(data);
	}
	
	class FlickrBinder extends Binder{
		public FlickrService getService(){
			return FlickrService.this;
		}
	}
	
	private final IBinder mBinder = new FlickrBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		String action = intent.getAction();
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onBind()] action: "+action);        
		return mBinder;
	}

}
