package com.neusou.vivacious;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public abstract class RestfulCallback extends BroadcastReceiver {

	public static final String LOG_TAG = "RestfulCallback";

	String callbackAction;
	
	/**
	 * Creates a RestfulCallback with the intent action to listen
	 * @param action
	 */
	public RestfulCallback(String action) {
		callbackAction = action;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(LOG_TAG,"onReceive "+intent.getAction());
	}

	public void register(Context ctx){
		IntentFilter filter = new IntentFilter();
		filter.addAction(callbackAction);
		ctx.registerReceiver(this, filter);
	}
	
	public void unregister(Context ctx){
		ctx.unregisterReceiver(this);
	}

}
