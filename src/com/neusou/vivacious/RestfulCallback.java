package com.neusou.vivacious;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.neusou.vivacious.RestfulClient.RestfulMethod;

public abstract class RestfulCallback<S extends Parcelable> extends BroadcastReceiver {

	public static final String LOG_TAG = "RestfulCallback";

	String callbackAction;
	String xtraMethod;
	String xtraResponse;
	String xtraError;
	
	/**
	 * Creates a RestfulCallback with the intent action to listen
	 * @param action
	 */
	public RestfulCallback(RestfulClient<?> client) {
		callbackAction = client.CALLBACK_INTENT;
		xtraMethod = client.XTRA_METHOD;
		xtraResponse = client.XTRA_RESPONSE;
		xtraError = client.XTRA_ERROR;
	}
	
	@Override
	final public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG,"onReceive "+intent.getAction());
		
		Log.d(LOG_TAG,"onReceive method:"+xtraMethod);
		Log.d(LOG_TAG,"onReceive response:"+xtraResponse);
		Log.d(LOG_TAG,"onReceive error:"+xtraError);
		
		Bundle b = intent.getExtras();
    	RestfulClient<?> restfulClient = Flickr.getInstance().restfulClient;
    	RestfulMethod restMethod = (RestfulMethod) b.getParcelable(xtraMethod);
    	S response = (S) b.getParcelable(xtraResponse);
    	String error = b.getString(xtraError);
    	onCallback(restMethod, response, error);
	}
	
	public abstract <T extends RestfulMethod> void onCallback(T restMethod, S response, String error);

	public void register(Context ctx){
		IntentFilter filter = new IntentFilter();
		filter.addAction(callbackAction);
		ctx.registerReceiver(this, filter);
	}
	
	public void unregister(Context ctx){
		ctx.unregisterReceiver(this);
	}

}
