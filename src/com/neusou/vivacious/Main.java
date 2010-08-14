package com.neusou.vivacious;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentProducer;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.OnGestureListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.neusou.vivacious.Flickr.Photo;
import com.neusou.vivacious.Flickr.Photo.Size;
import com.neusou.vivacious.RestfulClient.RestfulMethod;
import com.neusou.vivacious.RestfulClient.RestfulResponse;

public class Main extends Activity {
	
	public static final String LOG_TAG = Main.class.getCanonicalName();
    
	RestfulCallback cb;
	GestureDetector gd;
	Flickr.Photo[] photos; 
	int currentPhotoIndex = 0;
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {	
		super.onConfigurationChanged(newConfig);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);       	
		bindViews();
		
        Flickr flickr = Flickr.getInstance();
        
        cb =  new RestfulCallback<RestfulResponse>(flickr.restfulClient) {
    	    	
			@Override
			public void onCallback(RestfulMethod restMethod, RestfulResponse response, String error) {
				
				if(error != null){
					Log.d(LOG_TAG,"error ######: "+error+" #####");
					return;
				}
				
				if(restMethod instanceof Flickr.FlickrGetFrob){
					
					Intent rest = new Intent(Main.this, FlickrIntentService.class);
	    			Flickr.FlickrGroupsSearch method = new Flickr.FlickrGroupsSearch();
	    			method.text = "wallpaper";
	    			Flickr flickr = Flickr.getInstance();
	    	    	rest.putExtra(flickr.restfulClient.XTRA_METHOD, method);
	    	    	//rest.putExtra(flickr.restfulClient.INTENE, "process response from groupsSearch");
	    	    	startService(rest);
	    	    						
	    	    	/*
	    	    	Intent rest = new Intent(Main.this, FlickrIntentService.class);
	    			Flickr.FlickrPhotosSearch method = new Flickr.FlickrPhotosSearch();
	    			method.tags = "geometry";
	    			method.text = "";
	    			method.sort = Flickr.FlickrPhotosSearch.Sort.Relevance.value;
	    			method.in_gallery = false;
	    			method.tag_mode = Flickr.FlickrPhotosSearch.TagMode.all.value;
	    			method.content_type = Flickr.FlickrPhotosSearch.ContentType.other_only.value;
	    			Flickr flickr = Flickr.getInstance();
	    	    	rest.putExtra(flickr.restfulClient.XTRA_METHOD, method);
	    	    	startService(rest);
	    	    	*/
	    	    	
	    	    	
				}
				
				if(restMethod instanceof Flickr.FlickrGroupsSearch){
					try {
						JSONObject json = new JSONObject(response.data);
						//Log.d(Main.LOG_TAG,json.toString(2));
						JSONArray groups = json.getJSONObject("groups").getJSONArray("group");
						JSONObject group = groups.getJSONObject(0);
						String nsid = group.optString("nsid");
						String name = group.optString("name");
					
						Log.d(Main.LOG_TAG,nsid+", "+name);
						nsid = "1030135@N20";
							
						Intent restService = new Intent(Main.this, FlickrIntentService.class);
		    			Flickr.FlickrGroupsPoolsGetPhotos method = new Flickr.FlickrGroupsPoolsGetPhotos();
		    			method.group_id = nsid;
		    			method.tags = null;
		    			method.paging.page = 7;
		    			method.paging.perPage = 30;
		    			Flickr flickr = Flickr.getInstance();    			
		    	    	restService.putExtra(flickr.restfulClient.XTRA_METHOD, method);
		    	    	Log.d(Main.LOG_TAG, "startingService");
		    	    	startService(restService);
		    	    	
					} catch (JSONException e) {		
						e.printStackTrace();
					}
					
				}
				
				if(restMethod instanceof Flickr.FlickrGroupsPoolsGetPhotos){
					try{
						JSONObject json = new JSONObject(response.data);
						//Log.d(Main.LOG_TAG,"GroupsPoolsGetPhotos: "+json.toString(2));						
						JSONArray jsArrayPhotos = json.getJSONObject("photos").getJSONArray("photo");						
						photos = Flickr.Photo.parseArray(jsArrayPhotos);			
						
						
						//http://farm4.static.flickr.com/3520/3238594890_8e0a6a45c4_b.jpg
					}catch(JSONException e){
						e.printStackTrace();
					}
					
				}
				
				if(restMethod instanceof Flickr.FlickrPhotosSearch){
					try{
						JSONObject json = new JSONObject(response.data);
						//Log.d(Main.LOG_TAG,"PhotosSearch: "+json.toString(2));						
						JSONArray jsArrayPhotos = json.getJSONObject("photos").getJSONArray("photo");						
						photos = Flickr.Photo.parseArray(jsArrayPhotos);			

					}catch(JSONException e){
						e.printStackTrace();
					}
					
				}
				
				
    			Toast.makeText(Main.this, restMethod.getClass().getSimpleName(), 1000).show();
    	    	    	    	
			}

			
    	};		
    	
    	
    	gd = new GestureDetector(new OnGestureListener() {
			
			@Override
			public boolean onSingleTapUp(MotionEvent e) {

				return false;
			}
			
			@Override
			public void onShowPress(MotionEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public void onLongPress(MotionEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
			
				
				if(velocityX < 0){
					currentPhotoIndex++;	
				}else{
					currentPhotoIndex--;
				}

				if(photos == null || photos.length == 0){
					return true;
				}
				
				if(currentPhotoIndex < 0){
					currentPhotoIndex = photos.length - 1;
				}else{
					currentPhotoIndex = currentPhotoIndex % photos.length;	
				}
				
				Photo currentPhoto = photos[currentPhotoIndex];
				
				URL imageUrl;
				
				try {
					imageUrl = currentPhoto.createImageUrl(Size.normal);
				} catch (MalformedURLException e3) {							
					e3.printStackTrace();
					return true;
				}						
				
				Log.d(LOG_TAG,"imageUrl: "+imageUrl.toString());
				
				showImage(imageUrl);
				
				return false;
			}
			
			@Override
			public boolean onDown(MotionEvent e) {
				// TODO Auto-generated method stub
				return false;
			}
		});
   
    }    
    
    ImageView mImage;
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	boolean consumed = gd.onTouchEvent(event);
    	if(!consumed){
    		return super.onTouchEvent(event);
    	}
    	return consumed;
    }
    public void bindViews(){
    	mImage = (ImageView) findViewById(R.id.image);	
    }
    
    @Override
    protected void onStop() {    
    	super.onStop();    	
    	
    }
    
    @Override
    protected void onPause() {
       	super.onPause();
       	cb.unregister(this);
    }
    
    @Override
    protected void onResume() {    
    	super.onResume();    	
    	
    	cb.register(this);
    	
    	
    	
    	//Uri PhotoUri = Uri.fromParts("content", "//ssp",null);
    	
		getFrob();	
    }
    
    public void getFrob(){
    	Intent rest = new Intent(this, FlickrIntentService.class);
    	//rest.setAction(getPackageName()+".REST");
    	Flickr flickr = Flickr.getInstance();
    	rest.putExtra(flickr.restfulClient.XTRA_METHOD, new Flickr.FlickrGetFrob());
    	rest.putExtra(flickr.restfulClient.INTENT_PROCESS_RESPONSE, "process response from getFrob");
    	startService(rest);
    }

    public void test2(RestfulResponse response){
    	try {
			JSONObject json = new JSONObject(response.data);
			Log.d(Main.LOG_TAG,json.toString(2));
			JSONArray groups = json.getJSONObject("groups").getJSONArray("group");
			JSONObject group = groups.getJSONObject(0);
			String nsid = group.optString("nsid");
			String name = group.optString("name");
		
			Log.d(Main.LOG_TAG,nsid+", "+name);
			nsid = "1030135@N20";
				
			Intent restService = new Intent(Main.this, FlickrIntentService.class);
			Flickr.FlickrGroupsPoolsGetPhotos method = new Flickr.FlickrGroupsPoolsGetPhotos();
			method.group_id = nsid;
			method.tags = null;
			method.paging.page = 7;
			method.paging.perPage = 30;
			Flickr flickr = Flickr.getInstance();    			
	    	restService.putExtra(flickr.restfulClient.XTRA_METHOD, method);
	    	Log.d(Main.LOG_TAG, "startingService");
	    	startService(restService);
	    	
		} catch (JSONException e) {		
			e.printStackTrace();
		}
			
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {    
    	super.onRestoreInstanceState(savedInstanceState);
    	
    }
    
    public void showImage(URL bitmapUrl){
    	 HttpGet httpRequest = null;

         try {
                 httpRequest = new HttpGet(bitmapUrl.toURI());
         } catch (URISyntaxException e) {
                 e.printStackTrace();
         }

         HttpClient httpclient = new DefaultHttpClient();
         HttpResponse response;
		try {
			response = (HttpResponse) httpclient.execute
			(httpRequest);
		} catch (ClientProtocolException e) {			
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally{
			
		}

         HttpEntity entity = response.getEntity();
         BufferedHttpEntity bufHttpEntity;
		try {
			bufHttpEntity = new BufferedHttpEntity
(entity);
		} catch (IOException e) {		
			e.printStackTrace();
			return;
		}
		
        InputStream instream;
		try {
			instream = bufHttpEntity.getContent();
			Bitmap bm = BitmapFactory.decodeStream(instream);
	         mImage.setImageBitmap(bm);
		} catch (IOException e) {		
			e.printStackTrace();
			return;
		}
		
		
    }
    
      
    
    @Override
    public boolean onSearchRequested() {   
    	Log.d(LOG_TAG,"onSearchRequested");
    	Bundle data = new Bundle();
    	startSearch("queryFill", false, data, false); 
    	return true;
    }
    
}