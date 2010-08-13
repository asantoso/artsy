package com.neusou.vivacious;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.vivacious.RestfulClient.RestfulResponse;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Flickr {

	public static final String LOG_TAG = "Flickr";

	public static final String param_method = "method";
	public static final String param_api_key = "api_key";
	public static final String param_api_sig = "api_sig";
	public static final String param_format = "format";
	public static final String param_per_page = "per_page";
	public static final String param_page = "page";
	public static final String param_text = "text";
	public static final String param_group_id = "group_id";
	public static final String param_tags = "tags";

	public static final String format_json = "json";

	public static final String[] methods = new String[] {
			"flickr.auth.getFrob", "flickr.groups.search",
			"flickr.groups.pools.getPhotos", "flickr.groups.pools.getContext" };

	public static final int METHOD_AUTH_GETFROB = 0;
	public static final int METHOD_GROUPS_SEARCH = 1;
	public static final int METHOD_GROUPS_POOLS_GETPHOTOS = 2;
	public static final int METHOD_GROUPS_POOLS_GETCONTEXT = 3;

	public static final String FLICKR_API_KEY = "d910ca1fe2936899118dd5d32caabaf6";
	public static final String FLICKR_API_SECRET = "47c7fe3023da99b1";
	public static final String FLICKR_JSON_METHOD_CALLBACK = "jsonFlickrApi";
	public static final String SIGNATURE_DIGEST_ALGORITHM = "MD5";
	public static final String BASE_ENDPOINT = "http://api.flickr.com/services/rest/?";
	
	RestfulClient<RestfulResponse> restfulClient;
	
	static LinkedHashMap<Integer, Class> methodsMap = new LinkedHashMap<Integer, Class>(1);
	static {
		methodsMap.put(METHOD_AUTH_GETFROB, FlickrGetFrob.class);
		methodsMap.put(METHOD_GROUPS_SEARCH, FlickrGroupsSearch.class);
		methodsMap.put(METHOD_GROUPS_POOLS_GETPHOTOS, FlickrGroupsPoolsGetPhotos.class);
	}

	private Flickr() {
		restfulClient = new RestfulClient<RestfulResponse>(mContext, methodsMap, new DefaultResponseHandler(),"flickr");
	}

	final static Flickr INSTANCE = new Flickr();
	
	Context mContext;

	public static Flickr getInstance() {
		return INSTANCE;
	}
	
	public void setContext(Context ctx){
		mContext = ctx;
		restfulClient.mContext = ctx;
	}

	class DefaultResponseHandler implements ResponseHandler<RestfulResponse> {
		
		@Override
		public RestfulResponse handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			String a = response.getStatusLine().toString();
			String b = response.toString();
			// BufferedInputStream bis = new BufferedInputStream();
			BufferedReader responseReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			StringBuffer responseBuffer = new StringBuffer();
			while (true) {
				int bytesRead = responseReader.read();
				if (bytesRead == -1) {
					break;
				}
				responseBuffer.append(responseReader.readLine());
			}

			String tmp = responseBuffer.toString();
			String tmp2 = tmp.substring(FLICKR_JSON_METHOD_CALLBACK
					.length(), tmp.length() - 1);

			JSONObject jsonResponse;
			try {
				jsonResponse = new JSONObject(tmp2);
			//	Log.d(LOG_TAG, "json response: "+ jsonResponse.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return new RestfulResponse(tmp2);
		}
	};
	
	public String createRequestString(Bundle data) throws Exception {
		
			String equals = "=";
			StringBuffer sb = new StringBuffer(BASE_ENDPOINT);
			Set<String> keys = data.keySet();
			boolean start = true;
			for (String key : keys) {
				if (!start) {
					sb.append("&");
				} else {
					start = false;
				}
				sb.append(key);
				sb.append(equals);
				sb.append(data.getString(key));
			}
			return sb.toString();
		
		
	}

	public String createRequestSignature(String secret, Bundle data)
			throws NoSuchAlgorithmException {
		Set<String> keys = data.keySet();
		String[] keysArray = new String[keys.size()];
		keys.toArray(keysArray);
		Arrays.sort(keysArray);

		StringBuffer sb = new StringBuffer();
		sb.append(secret);
		for (String key : keysArray) {
			sb.append(key);
			sb.append(data.get(key));
		}

		MessageDigest md = MessageDigest
				.getInstance(SIGNATURE_DIGEST_ALGORITHM);
		md.update(sb.toString().getBytes());
		byte[] digested = md.digest();
		BigInteger bi = new BigInteger(1, digested);
		String signature = bi.toString(16);
		return signature;
	}

	//public static final String XTRA_CALLBACK_INTENT = Flickr.class.getPackage()+".REST_CALLBACK_INTENT";



	
	private void onExecuteSuccess(Exception e) {
	}


	public void execute(String request, Bundle b){		
		restfulClient.execute(request, b);
	}
	
	public static class FlickrGetFrob extends RestfulClient.RestfulMethod implements Parcelable{
		
		public static final Parcelable.Creator<FlickrGetFrob> CREATOR = new Creator<FlickrGetFrob>() {			
			@Override
			public FlickrGetFrob[] newArray(int size) {
				return null;
			}
			
			@Override
			public FlickrGetFrob createFromParcel(Parcel source) {
				return new FlickrGetFrob();
			}
		};
		
		@Override
		public int describeContents() {
			return METHOD_AUTH_GETFROB;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {

		}
		
		public void go(Bundle b) {
						
			Bundle data = new Bundle();
			data.putString(param_method, methods[METHOD_AUTH_GETFROB]);
			data.putString(param_api_key, FLICKR_API_KEY);
			data.putString(param_format, format_json);

			
			Flickr flickr = Flickr.getInstance();			
			try {
				String sig = flickr.createRequestSignature(FLICKR_API_SECRET, data);
				data.putString(param_api_sig, sig);
				String request = flickr.createRequestString(data);				
				flickr.execute(request, b);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

		}

	}
	
	
	public static class FlickrGroupsSearch extends RestfulClient.RestfulMethod implements Parcelable{
		
		public String text = "japan";
		public Paging paging = new Paging();
		
		
		public static final Parcelable.Creator<FlickrGroupsSearch> CREATOR = new Creator<FlickrGroupsSearch>() {

			@Override
			public FlickrGroupsSearch createFromParcel(Parcel source) {
				FlickrGroupsSearch obj = new FlickrGroupsSearch();
				obj.text = source.readString();
				obj.paging = Paging.CREATOR.createFromParcel(source);
				return obj;
			}

			@Override
			public FlickrGroupsSearch[] newArray(int size) {
				return null;
			}			
		
		};
		
		@Override
		public int describeContents() {
			return METHOD_GROUPS_SEARCH;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(text);
			paging.writeToParcel(dest, flags);
		}
		
		public void go(Bundle b) {
					
			Bundle data = new Bundle();				
			data.putString(param_method, methods[METHOD_GROUPS_SEARCH]);
			data.putString(param_api_key, FLICKR_API_KEY);
			data.putString(param_format, format_json);
			data.putString(param_text, text);
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();
			
			try {
				String request = flickr.createRequestString(data);
				flickr.execute(request, b);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}	


	public static class FlickrGroupsPoolsGetPhotos extends RestfulClient.RestfulMethod implements Parcelable{
		
		public Paging paging = new Paging();
		
		/**
		 *  The id of the group who's pool you which to get the photo list for. required
		 */
		public String group_id = "";

		/**
		 * tags (Optional) A tag to filter the pool with. At the moment only one tag at a time is supported.
		 */
		public String tags;
			
		/**
		 * (Optional)
		    The nsid of a user. Specifiying this parameter will retrieve for you only those photos that the user has contributed to the group pool.
		 */
		public String user_id;

		/**
		 *  (Optional) A comma-delimited list of extra information to fetch for each returned record. Currently supported fields are: description, license, date_upload, date_taken, owner_name, icon_server, original_format, last_update, geo, tags, machine_tags, o_dims, views, media, path_alias, url_sq, url_t, url_s, url_m, url_o
		 */
		public String extras;
		
		public int per_page; 
		   
		public int page;
		
		public static final Parcelable.Creator<FlickrGroupsPoolsGetPhotos> CREATOR = new Creator<FlickrGroupsPoolsGetPhotos>() {

			@Override
			public FlickrGroupsPoolsGetPhotos createFromParcel(Parcel source) {
				FlickrGroupsPoolsGetPhotos obj = new FlickrGroupsPoolsGetPhotos();
				obj.group_id = source.readString();
				obj.tags = source.readString();
				obj.user_id = source.readString();
				obj.extras = source.readString();				
				obj.paging = Paging.CREATOR.createFromParcel(source);
				return obj;
			}

			@Override
			public FlickrGroupsPoolsGetPhotos[] newArray(int size) {
				return null;
			}			
		
		};
		
		@Override
		public int describeContents() {
			return METHOD_GROUPS_SEARCH;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(group_id);
			dest.writeString(tags);
			dest.writeString(user_id);
			dest.writeString(extras);
			paging.writeToParcel(dest, flags);
		}
		
		public void go(Bundle b) {
					
			Bundle data = new Bundle();				
			data.putString(param_method, methods[METHOD_GROUPS_POOLS_GETPHOTOS]);
			data.putString(param_api_key, FLICKR_API_KEY);
			data.putString(param_format, format_json);
			data.putString(param_group_id, group_id);
			data.putString(param_tags, tags);
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();			
			try {
				String request = flickr.createRequestString(data);
				flickr.execute(request, b);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}	
		
}
