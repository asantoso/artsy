package com.neusou.vivacious;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class RestfulClient<S extends IRestfulResponse<?>> {
	
	public static final String LOG_TAG = RestfulClient.class.getCanonicalName();	
	
	public String BASE_PACKAGE = "";	
	public String INTENT_PROCESS_RESPONSE;
	public String CALLBACK_INTENT;
	public String CALLBACK_INTENT_ERROR;
	public String CALLBACK_INTENT_SUCCESS;
	
	/**
	 * The xtra key for restful method invocation, its value implements Parcelable and a subclass of RestfulMethod
	 */
	public String XTRA_METHOD;
	
	/**
	 * The xtra key for responses of the restful method invocation, its value implements Parcelable
	 */
	public String XTRA_RESPONSE;	
	
	public String XTRA_ERROR;
	
	String mName = "default";
	Context mContext;
	ThreadPoolExecutor executor = new ThreadPoolExecutor(1,5,1,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());
	
	LinkedHashMap<Integer, Class<?>> mMethods = new LinkedHashMap<Integer, Class<?>>(1);
	ResponseHandler<S> mResponseHandler;
	
	private void init(){
		BASE_PACKAGE = getClass().getPackage().getName()+"."+mName+".restful";
		XTRA_METHOD = BASE_PACKAGE+".CALL_METHOD";
		INTENT_PROCESS_RESPONSE = BASE_PACKAGE+".PROCESS_RESPONSE";
		XTRA_RESPONSE = BASE_PACKAGE+".RESPONSE";	
		XTRA_ERROR = BASE_PACKAGE+".ERROR";
		CALLBACK_INTENT = BASE_PACKAGE+".CALLBACK_INTENT";
		CALLBACK_INTENT_ERROR = BASE_PACKAGE+".CALLBACK_INTENT_ERROR";
		CALLBACK_INTENT_SUCCESS = BASE_PACKAGE+".CALLBACK_INTENT_SUCCESS";		
	}
	
	public RestfulClient(Context ctx, LinkedHashMap<Integer, Class<?>> m, ResponseHandler<S> rh, String name) {
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
		RestfulMethod invoke = (RestfulMethod) getParcelable(b, XTRA_METHOD);
		invoke.go(b);
		/*
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
		*/
	}	

	public <T extends HttpRequestBase> S sendRequest(String request, Class<T> method) throws Exception {
		Log.d(LOG_TAG, "request line: " + request);
		URI reqURI = URI.create(request);
		T httpMethod;
		try {
			httpMethod = (T) method.newInstance();
			httpMethod.setURI(reqURI);			
			//HttpGet get = new org.apache.http.client.methods.HttpGet(reqURI);
			DefaultHttpClient httpClient = new DefaultHttpClient();
			S response = httpClient.execute(httpMethod, mResponseHandler);
			Log.d(LOG_TAG, " response: " + response.toString());		
			return response;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw e;
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw e;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw e;
		} catch (SecurityException e) {
			e.printStackTrace();
			throw e;
		} catch (UnknownHostException e){
			e.printStackTrace();
			throw e;
		}
				
	}
	
	private void onExecuteError(Exception e) {

	}

	//generic restful execution
	public <T extends HttpRequestBase> void execute(final String request, final Bundle data, final Class<T> httpMethod) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				S response = null;
				String exceptionMessage = null;
				try {
					response = sendRequest(request, httpMethod);
				} catch (Exception e) {
					e.printStackTrace();
					onExecuteError(e);					
					exceptionMessage = e.getClass().getCanonicalName()+": "+e.getMessage();
				}

				// process response
								
				Log.d(LOG_TAG,"starting service with action: "+INTENT_PROCESS_RESPONSE);
				Intent processIntent = new Intent();
				processIntent.setAction(INTENT_PROCESS_RESPONSE);					
				processIntent.putExtra(XTRA_RESPONSE, response);
				processIntent.putExtra(XTRA_ERROR, exceptionMessage);
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
			Log.d(LOG_TAG,"broadcastCallback action:"+action);
			Intent i = new Intent(action);
			i.putExtras(data);
			ctx.sendBroadcast(i);
		}
	}
	
	/*
	public static abstract class RestfulMethod {
		public int callId = 0;
		abstract void go(Bundle b);
	}
	*/
	
	public static interface RestfulMethod extends Parcelable{		
		public void go(Bundle b);
	}
	

	/**
	 * Default implementation of IRestfulResponse
	 * @author asantoso
	 *
	 */
	public static class RestfulResponse implements IRestfulResponse<String>{
		
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
	
		@Override
		public void setData(String data) {
			this.data = data;
		}
		
	}
		
	
}


interface IRestfulResponse<D> extends Parcelable{
	public void setData(D data);
};

