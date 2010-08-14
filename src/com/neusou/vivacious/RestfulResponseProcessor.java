package com.neusou.vivacious;

import java.util.Set;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import com.neusou.vivacious.RestfulClient.RestfulMethod;

public abstract class RestfulResponseProcessor<S extends Parcelable> extends
		IntentService {

	public static final String LOG_TAG = RestfulResponseProcessor.class
			.getSimpleName();

	public RestfulResponseProcessor(String name) {
		// super(RestfulResponseProcessor.class.getCanonicalName());
		super(name);
	}

	private void broadcastCallback(Bundle data, String action) {
		RestfulClient.broadcastCallback(this, data, action);
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		Logger.l(Logger.DEBUG, LOG_TAG, "onRebind()");
	}

	@Override
	public IBinder onBind(Intent intent) {
		Logger.l(Logger.DEBUG, LOG_TAG, "onBind()");
		return super.onBind(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Logger.l(Logger.DEBUG, LOG_TAG, "onCreate()");

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.l(Logger.DEBUG, LOG_TAG, "onDestroy()");
	}

	@Override
	final protected void onHandleIntent(Intent intent) {

		String action = intent.getAction();
		Bundle data = intent.getExtras();
		Logger.l(Logger.DEBUG, LOG_TAG, "onHandleIntent() action: " + action);

		String xtra_method = null;
		String xtra_response = null;
		String xtra_error = null;
		String xtra_request = null;
		String callback_intent = null;

		Bundle metadata;
		try {
			metadata = getPackageManager().getServiceInfo(
					new ComponentName(this,
							FlickrRestfulResponseProcessor.class),
					PackageManager.GET_META_DATA).metaData;

			/*
			 * Set<String> metakeys = metadata.keySet(); for(String key :
			 * metakeys){
			 * Log.d(LOG_TAG,"meta: "+key+":"+metadata.getString(key)); }
			 */
			String identifier = metadata.getString("identifier");

			xtra_method = RestfulClient.generateKey(identifier,
					RestfulClient.KEY_CALL_METHOD);
			xtra_response = RestfulClient.generateKey(identifier,
					RestfulClient.KEY_RESPONSE);
			xtra_error = RestfulClient.generateKey(identifier,
					RestfulClient.KEY_ERROR);
			xtra_request = RestfulClient.generateKey(identifier,
					RestfulClient.KEY_REQUEST);
			callback_intent = RestfulClient.generateKey(identifier,
					RestfulClient.KEY_CALLBACK_INTENT);

		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		String error = data.getString(xtra_error);
		Bundle request = data.getBundle(xtra_request);

		if (request.containsKey(xtra_method)) {
			Parcelable restMethod = request.getParcelable(xtra_method);
			S response = data.getParcelable(xtra_response);
			handleResponse(response, (RestfulMethod) restMethod, data
					.getBundle(xtra_request), error);
		} else {
			Log.d(LOG_TAG, "no method data.");
		}
		broadcastCallback(data, callback_intent);

	}

	protected abstract void handleResponse(S response, RestfulMethod method,
			Bundle requestdata, String error);

}
