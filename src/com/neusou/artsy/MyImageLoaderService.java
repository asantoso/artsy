package com.neusou.artsy;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.neusou.Logger;
import com.neusou.bioroid.image.ImageLoaderService;

public class MyImageLoaderService extends ImageLoaderService{

	public static final String LOG_TAG = Logger.registerLog(ImageLoaderService.class);
	
	@Override
	public void onDestroy() {	
		super.onDestroy();
		Logger.l(Logger.DEBUG, LOG_TAG, "onDestroy");
	}
	
	@Override
	public void onCreate() {
		mContext = this;
		super.onCreate();
		Logger.l(Logger.DEBUG, LOG_TAG, "onCreate");	
		mImageLoader.initCacheDatabase(this,mCacheDbName,R.raw.bioroid_imageloader_cache);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if(intent == null){
			return;
		}
		String action = intent.getAction();
		Logger.l(Logger.DEBUG, LOG_TAG, "onStart. action:"+action);
	
	}
	
	class ServiceBinder extends Binder{
		public MyImageLoaderService getService(){
			return MyImageLoaderService.this;
		}
	}
	
	private final IBinder mBinder = new ServiceBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		String action = intent.getAction();
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onBind()] action: "+action);        
		return mBinder;
	}

}
