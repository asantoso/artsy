package com.neusou.vivacious;

import java.util.Set;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.neusou.vivacious.RestfulClient.RestfulMethod;

public abstract class RestfulResponseProcessor<S extends Parcelable> extends IntentService {
	
	public static final String LOG_TAG = RestfulResponseProcessor.class.getSimpleName();
	
	public RestfulResponseProcessor() {
		super(RestfulResponseProcessor.class.getCanonicalName());
	}
	
	private void broadcastCallback(Bundle data, String action){		
		RestfulClient.broadcastCallback(this, data, action);
	}
	
	@Override
	final protected void onHandleIntent(Intent intent) {
		String name = Thread.currentThread().getName();
		String action = intent.getAction();
		Bundle data = intent.getExtras();		
		Log.d(LOG_TAG, name + ", " + action);
			
		String xtra_method = null;
		String xtra_response = null;
		String xtra_error = null;
		String callback_intent = null;
		
		Bundle metadata;
		try {
			metadata = getBaseContext().getPackageManager().getServiceInfo(new ComponentName(this,FlickrRestfulResponseProcessor.class),PackageManager.GET_META_DATA).metaData;
			//Info(App.class.getPackage().getName(), PackageManager.GET_META_DATA).metaData;
			Set<String> metakeys = metadata.keySet();
			for(String key : metakeys){
				Log.d(LOG_TAG,"meta: "+key+":"+metadata.getString(key));
			}
			xtra_method = metadata.getString("xtra_method");
			xtra_response = metadata.getString("xtra_response");
			xtra_error = metadata.getString("xtra_error");
			callback_intent = metadata.getString("callback_intent");
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		String error = data.getString(xtra_error);
		
		if(data.containsKey(xtra_method)){
			Parcelable restMethod = data.getParcelable(xtra_method);
			S response = data.getParcelable(xtra_response);
			handleResponse(response, (RestfulMethod) restMethod, error);
		}else{
			Log.d(LOG_TAG,"no data");
		}    		
		 
		broadcastCallback(data,callback_intent);
	}
	
	protected abstract void handleResponse(S response, RestfulMethod method, String error);
	
}
