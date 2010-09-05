package com.neusou.artsy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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

import com.neusou.bioroid.restful.RestfulClient;
import com.neusou.bioroid.restful.RestfulClient.RestfulResponse;
import com.neusou.artsy.R;

public class Flickr {

	public static final String LOG_TAG = Flickr.class.getCanonicalName();

	public static final String param_method = "method";
	public static final String param_api_key = "api_key";
	public static final String param_api_sig = "api_sig";
	public static final String param_format = "format";
	public static final String param_date = "date";
	public static final String param_per_page = "per_page";
	public static final String param_page = "page";
	public static final String param_text = "text";
	public static final String param_group_id = "group_id";
	public static final String param_tags = "tags";
	public static final String param_sort = "sort";
	public static final String param_tag_mode = "tag_mode";
	public static final String param_in_gallery = "in_gallery";
	public static final String param_content_type = "content_type";
	public static final String param_photoset_id = "photoset_id";
	public static final String param_photo_id = "photo_id";
	public static final String param_secret = "secret";
	
	public static final String format_json = "json";

	public static final int METHOD_AUTH_GETFROB = R.id.METHOD_AUTH_GETFROB;
	public static final int METHOD_GROUPS_SEARCH = R.id.METHOD_GROUPS_SEARCH;
	public static final int METHOD_GROUPS_POOLS_GETPHOTOS = R.id.METHOD_GROUPS_POOLS_GETPHOTOS;
	public static final int METHOD_GROUPS_POOLS_GETCONTEXT = R.id.METHOD_GROUPS_POOLS_GETCONTEXT;
	public static final int METHOD_PHOTOS_SEARCH = R.id.METHOD_PHOTOS_SEARCH;
	public static final int METHOD_INTERESTINGNESS_GETLIST = R.id.METHOD_INTERESTINGNESS_GETLIST;
	public static final int METHOD_PHOTOSETS_GETPHOTOS = R.id.METHOD_PHOTOSETS_GETPHOTOS;
	public static final int METHOD_PHOTOS_GETALLCONTEXTS = R.id.METHOD_PHOTOS_GETALLCONTEXTS;
	public static final int METHOD_PHOTOS_GETINFO = R.id.METHOD_PHOTOS_GETINFO;

	public static final String API_KEY = "d910ca1fe2936899118dd5d32caabaf6";
	public static final String API_SECRET = "47c7fe3023da99b1";
	public static final String JSON_CALLBACK_METHOD = "jsonFlickrApi";
	public static final String SIGNATURE_DIGEST_ALGORITHM = "MD5";
	public static final String BASE_ENDPOINT = "http://api.flickr.com/services/rest/?";
	
	RestfulClient<RestfulResponse> restfulClient;
	
	static LinkedHashMap<Integer, Class<?>> methodsMap = new LinkedHashMap<Integer, Class<?>>(1);
	static LinkedHashMap<Integer, String> restfulMethodMap = new LinkedHashMap<Integer, String>(1);
	
	static {
		methodsMap.put(METHOD_AUTH_GETFROB, FlickrGetFrob.class);
		methodsMap.put(METHOD_GROUPS_SEARCH, FlickrGroupsSearch.class);
		methodsMap.put(METHOD_GROUPS_POOLS_GETPHOTOS, FlickrGroupsPoolsGetPhotos.class);
		methodsMap.put(METHOD_GROUPS_POOLS_GETCONTEXT, FlickrGroupsPoolsGetContext.class);
		methodsMap.put(METHOD_PHOTOS_SEARCH, FlickrPhotosSearch.class);
		methodsMap.put(METHOD_INTERESTINGNESS_GETLIST, FlickrInterestingnessGetList.class);
		methodsMap.put(METHOD_PHOTOSETS_GETPHOTOS, FlickrPhotosetsGetPhotos.class);
		methodsMap.put(METHOD_PHOTOS_GETALLCONTEXTS, FlickrPhotosGetAllContexts.class);
		methodsMap.put(METHOD_PHOTOS_GETINFO, FlickrPhotosGetInfo.class);
		
		restfulMethodMap.put(METHOD_AUTH_GETFROB, "flickr.auth.getFrob");
		restfulMethodMap.put(METHOD_GROUPS_SEARCH, "flickr.groups.search");
		restfulMethodMap.put(METHOD_GROUPS_POOLS_GETPHOTOS, "flickr.groups.pools.getPhotos");
		restfulMethodMap.put(METHOD_GROUPS_POOLS_GETCONTEXT, "flickr.groups.pools.getContext");
		restfulMethodMap.put(METHOD_PHOTOS_SEARCH, "flickr.photos.search");
		restfulMethodMap.put(METHOD_INTERESTINGNESS_GETLIST, "flickr.interestingness.getList");
		restfulMethodMap.put(METHOD_PHOTOSETS_GETPHOTOS, "flickr.photosets.getPhotos");
		restfulMethodMap.put(METHOD_PHOTOS_GETALLCONTEXTS, "flickr.photos.getAllContexts");
		restfulMethodMap.put(METHOD_PHOTOS_GETINFO, "flickr.photos.getInfo");
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
		restfulClient.setContext(ctx);
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

	public <T extends HttpRequestBase> void execute(HttpRequestBase httpMethod,  Bundle b){		
		restfulClient.execute(httpMethod, b);
	}

	public static final Paging parsePaging(JSONObject resultset, Paging paging){
		//{"photos":{"page":1, "pages":0, "perpage":298, "total":"0", "photo":[]}, "stat":"ok"}
		JSONObject photo = resultset.optJSONObject("photos");
		if(paging == null){
			paging = new Paging();	
		}		
		if(photo == null){
			return paging;
		}
		paging.page = photo.optInt("page");
		paging.pages = photo.optInt("pages");
		paging.perPage = photo.optInt("perpage");
		paging.total = photo.optInt("total");
		return paging;
	}
	
	public static class Photo implements Parcelable{
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

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
		
		}
		
		
	}
	
	public static class FlickrGetFrob extends RestfulClient.BaseRestfulMethod{
		
		public static final Parcelable.Creator<FlickrGetFrob> CREATOR = new Creator<FlickrGetFrob>() {			
			@Override
			public FlickrGetFrob[] newArray(int size) {
				return null;
			}
			
			@Override
			public FlickrGetFrob createFromParcel(Parcel source) {
				FlickrGetFrob obj = new FlickrGetFrob();
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);
				return obj;
			}
		};
		
		@Override
		public int describeContents() {
			return METHOD_AUTH_GETFROB;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);			
		}
		
		public void go(Bundle b) {
						
			Bundle data = new Bundle();
			data.putString(param_method, restfulMethodMap.get(METHOD_AUTH_GETFROB));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			
			Flickr flickr = Flickr.getInstance();			
			try {
				String sig = flickr.createRequestSignature(API_SECRET, data);
				data.putString(param_api_sig, sig);
				HttpGet get = new HttpGet();
				
				String request = flickr.createRequestString(data);
				HttpGet httpGet = new HttpGet();
				URI requestUri = URI.create(request);
				httpGet.setURI(requestUri);
				flickr.execute(httpGet, b);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

		}

	}
	
	
	public static class FlickrPhotosSearch extends RestfulClient.BaseRestfulMethod{

		/**
		 *  (Optional) A comma-delimited list of tags. Photos with one or more of the tags listed will be returned. You can exclude results that match a term by prepending it with a - character.
		 */
		public String tags;
		public String text;
		public String tag_mode = TagMode.any.value;
		public String sort = Sort.InterestingnessDesc.value;
		public boolean in_gallery = true;
		public int content_type = ContentType.photos_only.value;
		public Paging paging = new Paging();
	    
		public static final Parcelable.Creator<FlickrPhotosSearch> CREATOR = new Creator<FlickrPhotosSearch>() {			
			@Override
			public FlickrPhotosSearch[] newArray(int size) {
				return null;
			}
			
			@Override
			public FlickrPhotosSearch createFromParcel(Parcel source) {
				FlickrPhotosSearch obj = new FlickrPhotosSearch();
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);
				obj.paging = Paging.CREATOR.createFromParcel(source);
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
			super.writeToParcel(dest, flags);
			paging.writeToParcel(dest, flags);
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
			data.putString(param_method, restfulMethodMap.get(METHOD_PHOTOS_SEARCH));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			tags = tags.trim().replace(" ", ",");
			if(tags != null && tags.trim().length() > 0){
				data.putString(param_tags, tags);
			}
			data.putString(param_tag_mode, tag_mode);
			data.putString(param_sort, sort);
			if(text != null && text.trim().length() > 0){
				data.putString(param_text, text);
			}
			data.putString(param_in_gallery,"false"); 
			data.putString(param_content_type, Integer.toString(content_type));
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));			
			
			Flickr flickr = Flickr.getInstance();			
			try {
				String request = flickr.createRequestString(data);	
				HttpGet get = new HttpGet();
				URI reqURI = URI.create(request);
				get.setURI(reqURI);
				flickr.execute(get, b);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

		}

	}
		
	public static class FlickrGroupsSearch extends RestfulClient.BaseRestfulMethod{
		
		public String text = "background cute";
		public Paging paging = new Paging();
				
		public static final Parcelable.Creator<FlickrGroupsSearch> CREATOR = new Creator<FlickrGroupsSearch>() {

			@Override
			public FlickrGroupsSearch createFromParcel(Parcel source) {
				FlickrGroupsSearch obj = new FlickrGroupsSearch();
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);
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
			super.writeToParcel(dest, flags);
			
			dest.writeString(text);
			paging.writeToParcel(dest, flags);
		}
		
		public void go(Bundle b) {
					
			Bundle data = new Bundle();				
			data.putString(param_method, restfulMethodMap.get(METHOD_GROUPS_SEARCH));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			text = text.trim().replace(" ",",");
			data.putString(param_text, text);
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();
			
			try {
				String request = flickr.createRequestString(data);
				HttpGet httpGet = new HttpGet();				
				URI requestUri = URI.create(request);
				httpGet.setURI(requestUri);
				flickr.execute(httpGet, b);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}	

	public static class FlickrGroupsPoolsGetPhotos extends RestfulClient.BaseRestfulMethod{
		
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
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);
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
			super.writeToParcel(dest, flags);
			
			dest.writeString(group_id);
			dest.writeString(tags);
			dest.writeString(user_id);
			dest.writeString(extras);
			paging.writeToParcel(dest, flags);
		}
		
		public void go(Bundle b) {
			Bundle data = new Bundle();
			data.putString(param_method, restfulMethodMap.get(METHOD_GROUPS_POOLS_GETPHOTOS));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			data.putString(param_group_id, group_id);
			//data.putString(param_tags, tags);
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();			
			try {				
				String request = flickr.createRequestString(data);
				HttpGet httpGet = new HttpGet();
				URI requestUri = URI.create(request);
				httpGet.setURI(requestUri);
				flickr.execute(httpGet, b);
				
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}	
	
	public static class FlickrInterestingnessGetList extends RestfulClient.BaseRestfulMethod{
		
		public Paging paging = new Paging();
		public Date date;
				
		private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
		
		public static final Parcelable.Creator<FlickrInterestingnessGetList> CREATOR = new Creator<FlickrInterestingnessGetList>() {
			
			@Override
			public FlickrInterestingnessGetList[] newArray(int size) {
				return null;
			}
			
			@Override
			public FlickrInterestingnessGetList createFromParcel(Parcel source) {				
				FlickrInterestingnessGetList obj = new FlickrInterestingnessGetList();
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);
				obj.paging = Paging.CREATOR.createFromParcel(source);
				obj.date =  new Date(source.readLong());
				//obj.extras = "description, license, date_upload, date_taken, owner_name, icon_server, original_format, last_update, geo, tags, machine_tags, o_dims, views, media, path_alias, url_sq, url_t, url_s, url_m, url_o";
				Logger.l(Logger.DEBUG,LOG_TAG,"createFromParcel: time:"+obj.date.toString());
				return obj;
			}
		}; 
		
		@Override
		public void go(Bundle b) {
			Bundle data = new Bundle();
			data.putString(param_method, restfulMethodMap.get(METHOD_INTERESTINGNESS_GETLIST));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);			
			data.putString(param_date, DATE_FORMAT.format(date));
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();			
			try {
				String request = flickr.createRequestString(data);	
				HttpGet get = new HttpGet();
				URI reqURI = URI.create(request);
				get.setURI(reqURI);
				flickr.execute(get, b);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}

		@Override
		public int describeContents() {
			return METHOD_INTERESTINGNESS_GETLIST;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			Logger.l(Logger.DEBUG,LOG_TAG,"writeToParcel: time:"+date.toString());
			super.writeToParcel(dest, flags);
			paging.writeToParcel(dest, flags);
			dest.writeLong(date.getTime());
		}	
		
	}
	
	public static class FlickrPhotosetsGetPhotos extends RestfulClient.BaseRestfulMethod{

		public String photoset_id = "";
		public Paging paging = new Paging();
				
		public static final Parcelable.Creator<FlickrPhotosetsGetPhotos> CREATOR = new Creator<FlickrPhotosetsGetPhotos>() {

			@Override
			public FlickrPhotosetsGetPhotos createFromParcel(Parcel source) {
				FlickrPhotosetsGetPhotos obj = new FlickrPhotosetsGetPhotos();
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);
				obj.photoset_id = source.readString();
				obj.paging = Paging.CREATOR.createFromParcel(source);
				return obj;
			}

			@Override
			public FlickrPhotosetsGetPhotos[] newArray(int size) {
				return null;
			}			
		
		};
		
		@Override
		public int describeContents() {
			return METHOD_PHOTOSETS_GETPHOTOS;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeString(photoset_id);
			paging.writeToParcel(dest, flags);
		}
		
		public void go(Bundle b) {
					
			Bundle data = new Bundle();				
			data.putString(param_method, restfulMethodMap.get(METHOD_PHOTOSETS_GETPHOTOS));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			data.putString(param_photoset_id, photoset_id);
			data.putString(param_page, Integer.toString(paging.getPage()));
			data.putString(param_per_page, Integer.toString(paging.getPerPage()));
			
			Flickr flickr = Flickr.getInstance();
			
			try {
				String request = flickr.createRequestString(data);	
				HttpGet get = new HttpGet();
				URI reqURI = URI.create(request);
				get.setURI(reqURI);
				flickr.execute(get, b);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}	
	
	
	public static class FlickrPhotosGetAllContexts extends RestfulClient.BaseRestfulMethod{

		public String photo_id = "";
		public Paging paging = new Paging();
				
		public static final Parcelable.Creator<FlickrPhotosGetAllContexts> CREATOR = new Creator<FlickrPhotosGetAllContexts>() {

			@Override
			public FlickrPhotosGetAllContexts createFromParcel(Parcel source) {
				FlickrPhotosGetAllContexts obj = new FlickrPhotosGetAllContexts();
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);
				obj.photo_id = source.readString();
				obj.paging = Paging.CREATOR.createFromParcel(source);
				return obj;
			}

			@Override
			public FlickrPhotosGetAllContexts[] newArray(int size) {
				return null;
			}			
		
		};
		
		@Override
		public int describeContents() {
			return METHOD_PHOTOS_GETALLCONTEXTS;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			
			dest.writeString(photo_id);
			paging.writeToParcel(dest, flags);
		}
		
		public void go(Bundle b) {
					
			Bundle data = new Bundle();				
			data.putString(param_method, restfulMethodMap.get(METHOD_PHOTOS_GETALLCONTEXTS));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			data.putString(param_photo_id, photo_id);
			
			Flickr flickr = Flickr.getInstance();
			
			try {
				
				String request = flickr.createRequestString(data);
				HttpGet httpGet = new HttpGet();
				URI requestUri = URI.create(request);
				httpGet.setURI(requestUri);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}


	public static class FlickrPhotosGetInfo extends RestfulClient.BaseRestfulMethod{

		public String photo_id = "";		
		public String secret = "";
				
		public static final Parcelable.Creator<FlickrPhotosGetInfo> CREATOR = new Creator<FlickrPhotosGetInfo>() {

			@Override
			public FlickrPhotosGetInfo createFromParcel(Parcel source) {				
				FlickrPhotosGetInfo obj = new FlickrPhotosGetInfo();
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);				
				obj.photo_id = source.readString();
				obj.secret = source.readString();
				return obj;
			}

			@Override
			public FlickrPhotosGetInfo[] newArray(int size) {
				return null;
			}			
		
		};
		
		@Override
		public int describeContents() {
			return METHOD_PHOTOS_GETINFO;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			
			dest.writeString(photo_id);	
			dest.writeString(secret);
		}
		
		public void go(Bundle b) {
			Logger.l(Logger.DEBUG, LOG_TAG, "getPhotoInfo");
			Bundle data = new Bundle();				
			data.putString(param_method, restfulMethodMap.get(METHOD_PHOTOS_GETINFO));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			data.putString(param_photo_id, photo_id);
			data.putString(param_secret, secret);
			Flickr flickr = Flickr.getInstance();
			
			try {				
				String request = flickr.createRequestString(data);	
				HttpGet get = new HttpGet();
				URI reqURI = URI.create(request);
				get.setURI(reqURI);
				flickr.execute(get, b);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}
	
	

	public static class FlickrGroupsPoolsGetContext extends RestfulClient.BaseRestfulMethod{

		public String photo_id = "";		
		public String group_id = "";
				
		public static final Parcelable.Creator<FlickrGroupsPoolsGetContext> CREATOR = new Creator<FlickrGroupsPoolsGetContext>() {

			@Override
			public FlickrGroupsPoolsGetContext createFromParcel(Parcel source) {
				FlickrGroupsPoolsGetContext obj = new FlickrGroupsPoolsGetContext();
				RestfulClient.BaseRestfulMethod.createFromParcel(obj, source);
				obj.photo_id = source.readString();
				obj.group_id = source.readString();
				return obj;
			}

			@Override
			public FlickrGroupsPoolsGetContext[] newArray(int size) {
				return null;
			}			
		
		};
		
		@Override
		public int describeContents() {
			return METHOD_GROUPS_POOLS_GETCONTEXT;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			
			dest.writeString(photo_id);	
			dest.writeString(group_id);
		}
		
		public void go(Bundle b) {
					
			Bundle data = new Bundle();				
			data.putString(param_method, restfulMethodMap.get(METHOD_GROUPS_POOLS_GETCONTEXT));
			data.putString(param_api_key, API_KEY);
			data.putString(param_format, format_json);
			data.putString(param_photo_id, photo_id);
			data.putString(param_group_id, group_id);
			Flickr flickr = Flickr.getInstance();
			
			try {				
				String request = flickr.createRequestString(data);	
				HttpGet get = new HttpGet();
				URI reqURI = URI.create(request);
				get.setURI(reqURI);
				flickr.execute(get, b);
			} catch (Exception e) {
				e.printStackTrace();
			}		
		}
	}
	
	
	 
	
}
