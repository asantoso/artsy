package com.neusou.vivacious;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.neusou.vivacious.RestfulClient.RestfulResponse;

public class Flickr {

	public static final String LOG_TAG = Flickr.class.getCanonicalName();

	public static final String param_method = "method";
	public static final String param_api_key = "api_key";
	public static final String param_api_sig = "api_sig";
	public static final String param_format = "format";
	public static final String param_per_page = "per_page";
	public static final String param_page = "page";
	public static final String param_text = "text";
	public static final String param_group_id = "group_id";
	public static final String param_tags = "tags";
	public static final String param_sort = "sort";
	public static final String param_tag_mode = "tag_mode";
	public static final String param_in_gallery = "in_gallery";
	public static final String param_content_type = "content_type";
	
	public static final String format_json = "json";
	

	public static final String[] methods = new String[] {
			"flickr.auth.getFrob",
			"flickr.groups.search",
			"flickr.groups.pools.getPhotos", 
			"flickr.groups.pools.getContext",
			"flickr.photos.search",
			"flickr.interestingness.getList"
			};

	public static final int METHOD_AUTH_GETFROB = 0;
	public static final int METHOD_GROUPS_SEARCH = 1;
	public static final int METHOD_GROUPS_POOLS_GETPHOTOS = 2;
	public static final int METHOD_GROUPS_POOLS_GETCONTEXT = 3;
	public static final int METHOD_PHOTOS_SEARCH = 4;
	public static final int METHOD_INTERESTINGNESS_GETLIST = 5;

	public static final String API_KEY = "d910ca1fe2936899118dd5d32caabaf6";
	public static final String API_SECRET = "47c7fe3023da99b1";
	public static final String JSON_CALLBACK_METHOD = "jsonFlickrApi";
	public static final String SIGNATURE_DIGEST_ALGORITHM = "MD5";
	public static final String BASE_ENDPOINT = "http://api.flickr.com/services/rest/?";
	
	RestfulClient<RestfulResponse> restfulClient;
	
	static LinkedHashMap<Integer, Class<?>> methodsMap = new LinkedHashMap<Integer, Class<?>>(1);
	static {
		methodsMap.put(METHOD_AUTH_GETFROB, FlickrGetFrob.class);
		methodsMap.put(METHOD_GROUPS_SEARCH, FlickrGroupsSearch.class);
		methodsMap.put(METHOD_GROUPS_POOLS_GETPHOTOS, FlickrGroupsPoolsGetPhotos.class);
		methodsMap.put(METHOD_PHOTOS_SEARCH, FlickrPhotosSearch.class);
		methodsMap.put(METHOD_INTERESTINGNESS_GETLIST, FlickrInterestingnessGetList.class);
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
			String tmp2 = tmp.substring(JSON_CALLBACK_METHOD
					.length(), tmp.length() - 1);

			JSONObject jsonResponse;
			try {
				jsonResponse = new JSONObject(tmp2);
			//	Log.d(LOG_TAG, "json response: "+ jsonResponse.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			RestfulResponse rsp = new RestfulResponse(tmp2);
			return rsp;
			//return new IRestfulResponse(tmp2);
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
	
	private void onExecuteSuccess(Exception e) {
	}


	public <T extends HttpRequestBase> void execute(String request, Bundle b, Class<T> httpMethod){		
		restfulClient.execute(request, b, httpMethod);
	}
	
	public static class Photo {
		public String farm;
		public String id;
		public String secret;
		public String server;
		public String owner;
		
		enum Size{
			small ("_s"),
			medium ("_m"),
			big ("_b"),
			normal ("");
			
			String key;
			private Size(String key){
				this.key = key;
			}
		}
		
		public static Photo[] parseArray(JSONArray photos){
			int num = photos.length();
			Photo[] out = new Photo[num];
			for(int i=0;i<num;i++){
				try {
					out[i] = parseJSONObject(photos.getJSONObject(i));
				} catch (JSONException e) {				
					e.printStackTrace();
				}
			}	
			return out;
		}
		
		public static Photo parseJSONObject(JSONObject photo){
			Photo p = new Photo();
			p.farm = photo.optString("farm");
			p.id = photo.optString("id");
			p.owner = photo.optString("owner");
			p.secret = photo.optString("secret");
			p.server = photo.optString("server");
			return p;
		}
		
		public URL createImageUrl(Size size) throws MalformedURLException{
			StringBuffer sb = new StringBuffer();
			sb.append("http://farm").append(farm)
			.append(".static.flickr.com/")
			.append(server)
			.append("/")
			.append(id)
			.append("_")
			.append(secret)
			.append(size.key)
			.append(".jpg");
			return new URL(sb.toString());
		}
	}
	
	public static class FlickrGetFrob implements RestfulClient.RestfulMethod{
		
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
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			
			Flickr flickr = Flickr.getInstance();			
			try {
				String sig = flickr.createRequestSignature(API_SECRET, data);
				data.putString(param_api_sig, sig);
				String request = flickr.createRequestString(data);				
				flickr.execute(request, b, HttpGet.class);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

		}

	}
	
	
	public static class FlickrPhotosSearch implements RestfulClient.RestfulMethod{

		/**
		 *  (Optional) A comma-delimited list of tags. Photos with one or more of the tags listed will be returned. You can exclude results that match a term by prepending it with a - character.
		 */
		public String tags;
		public String text;
		public String tag_mode = TagMode.any.value;
		public String sort = Sort.DatePostedDesc.value;
		public boolean in_gallery = true;
		public int content_type = ContentType.photos_only.value;
	    
		public static final Parcelable.Creator<FlickrPhotosSearch> CREATOR = new Creator<FlickrPhotosSearch>() {			
			@Override
			public FlickrPhotosSearch[] newArray(int size) {
				return null;
			}
			
			@Override
			public FlickrPhotosSearch createFromParcel(Parcel source) {
				FlickrPhotosSearch obj = new FlickrPhotosSearch();
				obj.tags = source.readString();
				obj.text = source.readString();
				obj.in_gallery = source.readByte()==1;
				obj.sort = source.readString();
				obj.content_type = source.readInt();
				obj.tag_mode = source.readString();
				return obj;
			}
		};
		
		@Override
		public int describeContents() {
			return METHOD_PHOTOS_SEARCH;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(tags);
			dest.writeString(text);
			dest.writeByte(in_gallery?(byte)1:(byte)0);
			dest.writeString(sort);
			dest.writeInt(content_type);
			dest.writeString(tag_mode);
		}
		
		enum Sort{
			Relevance("relevance"),
			DatePostedDesc("date-posted-desc"),
			DatePostedAsc("date-posted-asc"),
			InterestingnessAsc("interestingness-asc"),
			InterestingnessDesc("interestingness-desc");
			String value;
			private Sort(String value){
				this.value = value;
			}
		};
		
		enum ContentType{
			photos_only(1),
			screenshots_only(2),
			other_only(3),
			photos_and_screenshots(4),
			screenshots_and_other(5),
			photos_and_other(6),
			all(7);
			int value;
		    private ContentType(int value) {
				this.value = value;
			}
		};
		
		enum TagMode{
			all("all"),
			any("any");
			String value;
			private TagMode(String value){
				this.value = value;
			}
		}
		
		public void go(Bundle b) {
						
			Bundle data = new Bundle();
			data.putString(param_method, methods[METHOD_PHOTOS_SEARCH]);
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			tags = tags.trim().replace(" ", ",");
			data.putString(param_tags, tags);
			data.putString(param_tag_mode, tag_mode);
			data.putString(param_sort, sort);
			//data.putString(param_text, text);
			data.putString(param_in_gallery,"false"); 
			data.putString(param_content_type, Integer.toOctalString(content_type));
			
			Flickr flickr = Flickr.getInstance();			
			try {
				String request = flickr.createRequestString(data);				
				flickr.execute(request, b, HttpGet.class);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

		}

	}
	
	
	
	public static class FlickrGroupsSearch implements RestfulClient.RestfulMethod{
		
		public String text = "background cute";
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
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			text = text.trim().replace(" ",",");
			data.putString(param_text, text);
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();
			
			try {
				String request = flickr.createRequestString(data);
				flickr.execute(request, b, HttpGet.class);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}	

	public static class FlickrGroupsPoolsGetPhotos implements RestfulClient.RestfulMethod{
		
		public Paging paging = new Paging();
		
		/**
		 *  The id of the group who's pool you which to get the photo list for. required
		 */
		public String group_id = "";// "45939032@N00";

		/**
		 * tags (Optional) A tag to filter the pool with. At the moment only one tag at a time is supported.
		 */
		public String tags = null;
			
		/**
		 * (Optional)
		    The nsid of a user. Specifiying this parameter will retrieve for you only those photos that the user has contributed to the group pool.
		 */
		public String user_id = "";

		/**
		 *  (Optional) A comma-delimited list of extra information to fetch for each returned record. Currently supported fields are: description, license, date_upload, date_taken, owner_name, icon_server, original_format, last_update, geo, tags, machine_tags, o_dims, views, media, path_alias, url_sq, url_t, url_s, url_m, url_o
		 */
		public String extras = "";
		
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
			return METHOD_GROUPS_POOLS_GETPHOTOS;
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
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			data.putString(param_group_id, group_id);
			//data.putString(param_tags, tags);
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();			
			try {
				String request = flickr.createRequestString(data);
				flickr.execute(request, b, HttpGet.class);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}	
	
	public static class FlickrInterestingnessGetList implements RestfulClient.RestfulMethod{

		@Override
		public void go(Bundle b) {
			Bundle data = new Bundle();
			data.putString(param_method, methods[METHOD_GROUPS_POOLS_GETPHOTOS]);
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			//data.putString(param_group_id, group_id);
			//data.putString(param_tags, tags);
			//data.putString(param_page, Integer.toString(paging.getPage()));
			//data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();			
			try {
				String request = flickr.createRequestString(data);
				flickr.execute(request, b, HttpGet.class);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {		
			
		}		
		
	}
	
}
