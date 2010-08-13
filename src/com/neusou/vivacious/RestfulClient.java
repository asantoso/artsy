package com.neusou.vivacious;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class RestfulClient<S extends Parcelable> {
	
	public static final String LOG_TAG = RestfulClient.class.getCanonicalName();	
	
	public String BASE_PACKAGE = "";	
	public String INTENT_PROCESS_RESPONSE = BASE_PACKAGE+".REST_PROCESS_INTENT";
	public String CALLBACK_INTENT = BASE_PACKAGE+".REST_CALLBACK_INTENT";
	public String CALLBACK_INTENT_ERROR = BASE_PACKAGE+".REST_CALLBACK_INTENT_ERROR";
	public String CALLBACK_INTENT_SUCCESS = BASE_PACKAGE+".REST_CALLBACK_INTENT_SUCCESS";
	public String XTRA_METHOD = "";
	public String XTRA_RESPONSE = BASE_PACKAGE+".XTRA_PESPONSE";	
	
	String mName = "";
	Context mContext;
	ThreadPoolExecutor executor = new ThreadPoolExecutor(1,5,1,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());
	
	LinkedHashMap<Integer, Class> mMethods = new LinkedHashMap<Integer, Class>(1);
	ResponseHandler<S> mResponseHandler;
	
	private void init(){
		BASE_PACKAGE = getClass().getPackage().getName()+"."+mName+".rest";
		XTRA_METHOD = BASE_PACKAGE+".CALL_METHOD";
		INTENT_PROCESS_RESPONSE = BASE_PACKAGE+".PROCESS_RESPONSE";
		XTRA_RESPONSE = BASE_PACKAGE+".RESPONSE";	
		CALLBACK_INTENT = BASE_PACKAGE+".CALLBACK_INTENT";
		CALLBACK_INTENT_ERROR = BASE_PACKAGE+".CALLBACK_INTENT_ERROR";
		CALLBACK_INTENT_SUCCESS = BASE_PACKAGE+".CALLBACK_INTENT_SUCCESS";		
	}
	
	public RestfulClient(Context ctx, LinkedHashMap<Integer, Class> m, ResponseHandler<S> rh, String name) {
		mMethods = m;
		mContext = ctx;
		mResponseHandler = rh;	
		mName = name;
		init();
	}
	
	public static Parcelable getParcelable(Bundle data, String name) {
		boolean invocation = data.containsKey(name);
		if (!invocation) {
			return null;
		}
		return data.getParcelable(name);
	}

	public static <T> T getParcelable(Bundle data, Class<T> type) {
		String name = type.getClass().getCanonicalName();
		boolean invocation = data.containsKey(name);
		if (!invocation) {
			return null;
		}
		return (T) data.getParcelable(name);
	}
	

	public void execute(Bundle b) {
		if(b == null){
			return;
		}
		Parcelable invoke = getParcelable(b, XTRA_METHOD);
		int type = invoke.describeContents();
		RestfulMethod method;
		try {
			method = (RestfulMethod) mMethods.get(type).newInstance();			
			method.go(b);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}	

	public S sendRequest(String request) throws IOException,
			MalformedURLException, HttpException {
		//Log.d(LOG_TAG, "request: " + request);
		URI reqURI = URI.create(request);
		HttpGet get = new org.apache.http.client.methods.HttpGet(reqURI);
		DefaultHttpClient httpClient = new DefaultHttpClient();
		S response = httpClient.execute(get, mResponseHandler);
		//Log.d(LOG_TAG, " response: " + response.toString());
		return response;
	}
	
	private void onExecuteError(Exception e) {

	}

	//generic restful execution
	public void execute(final String request, final Bundle data) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				S response = null;
				try {
					response = sendRequest(request);
				} catch (Exception e) {
					e.printStackTrace();
					onExecuteError(e);
				}

				// process response
				
				Intent processIntent = new Intent(mContext, RestfulResponseProcessor.class);
				processIntent.setAction(INTENT_PROCESS_RESPONSE);					
				processIntent.putExtra(XTRA_RESPONSE, response);	
				processIntent.putExtras(data);
				mContext.startService(processIntent);
				
			}
		});
	}
	


	/**
	 * Sends an Intent callback to the original caller
	 * @param ctx
	 * @param data
	 */
	public static void broadcastCallback(Context ctx, Bundle data, String action) {
		if (ctx != null ){			
			Log.d(LOG_TAG,"broadcastCallback");
			Intent i = new Intent(action);
			i.putExtras(data);
			ctx.sendBroadcast(i);
		}
	}
	

	public static abstract class RestfulMethod {
		public int callId = 0;
		abstract void go(Bundle b);
	}
	
	static class RestfulResponse implements Parcelable{

		String data;
		
		public RestfulResponse(String response) {
			data = response;
		}
		
		@Override
		public int describeContents() {			
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(data);
		}
		
		public static final Parcelable.Creator<RestfulResponse> CREATOR = new Creator<RestfulResponse>() {
			
			@Override
			public RestfulResponse[] newArray(int size) {
				return null;
			}
			
			@Override
			public RestfulResponse createFromParcel(Parcel source) {
				return new RestfulResponse(source.readString());
			}
		};
		
	}
	
	
}
