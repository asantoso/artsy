package com.neusou.artsy;

import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.neusou.Logger;
import com.neusou.bioroid.restful.RestfulClient;
import com.neusou.bioroid.restful.RestfulService;

public class FlickrService extends RestfulService {

	public static final String LOG_TAG = Logger.registerLog(FlickrService.class);
		
	@Override
	public void onDestroy() {	
		super.onDestroy();
		Logger.l(Logger.DEBUG, LOG_TAG, "onDestroy");
	}
	
	RestfulClient<?> restfulClient;
	@Override
	public void onCreate() {	
		super.onCreate();
		Logger.l(Logger.DEBUG, LOG_TAG, "onCreate");
		restfulClient = Flickr.getInstance(this).restfulClient;
		applyMetadata(this);
	}
	
	public void execute(Bundle data){
		Logger.l(Logger.DEBUG, LOG_TAG, "execute");			
		restfulClient.execute(data);
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

	@Override
	public void onExecuteRequest(Intent intent, int startId) {
				
	}

	@Override
	public void onProcessResponse(Intent intent, int startId) {
		
	}

	@Override
	public RestfulClient<?> getClient() {
		return restfulClient;
	}

}
