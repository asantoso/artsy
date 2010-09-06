package com.neusou.artsy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.app.Application;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.admob.android.ads.AdManager;

public class App extends Application{ 
	
	private static final String LOG_TAG = Logger.registerLog(App.class);
	public static WebView mWebView;
	private static final String PREFS = "pref";
	private static final String PREFS_SEARCH_KEYWORD = "search.keyword";
	private static final String PREFS_SEARCH_KEYWORD_DEFAULT = "wallpaper";
	
	MediaScannerConnectionClient mMediaScannerClient = new MediaScannerConnectionClient() {
		
		@Override
		public void onScanCompleted(String path, Uri uri) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onScanCompleted "+path);
		}
		
		@Override
		public void onMediaScannerConnected() {
			Logger.l(Logger.DEBUG, LOG_TAG, "onMediaScannerConnected ");
		}
	};
	
	public MediaScannerConnection mMediaScannerConn = new MediaScannerConnection(this, mMediaScannerClient);
	
	
	 static final FrameLayout.LayoutParams FILL = 
	        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 
	                         ViewGroup.LayoutParams.FILL_PARENT);
	 static final FrameLayout.LayoutParams WRAP = 
	        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 
	                         ViewGroup.LayoutParams.WRAP_CONTENT);
	
	public static MyImageLoaderService mImageLoaderService;
	 
	public ServiceConnection mImageLoaderServiceConn = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onServiceDisconnected "+name.toString());			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onServiceConnected "+ name.toString());
			mImageLoaderService = ((MyImageLoaderService.ServiceBinder) service).getService();			
		}
	};
	
	public ServiceConnection mFlickrServiceConn = new ServiceConnection() {		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onServiceDisconnected "+name.toString());
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {	
			Logger.l(Logger.DEBUG, LOG_TAG, "onServiceConnected "+ name.toString());
		}
	};
	
	static public float pixelDensity;
	
	public static int toDip(int px){
		return (int)(pixelDensity * px);
	}
	
	@Override
	public void onCreate() {	
		super.onCreate();	
		Logger.l(Logger.DEBUG, LOG_TAG, "onCreate");
		AdManager.setAllowUseOfLocation(true);	
		Flickr.getInstance(this);
		pixelDensity = getResources().getDisplayMetrics().density;
		
//		bindService(new Intent(App.this,MediaScannerConnection.class), mMediaScannerConn, Context.BIND_AUTO_CREATE);
		bindService(new Intent(App.this,FlickrService.class), mFlickrServiceConn, Context.BIND_AUTO_CREATE);
		bindService(new Intent(App.this,MyImageLoaderService.class), mImageLoaderServiceConn, Context.BIND_AUTO_CREATE);
		
		mMediaScannerConn.connect();
	}	
	
	@Override
	public void onTerminate() {	
		super.onTerminate();
		unbindService(mFlickrServiceConn);
		unbindService(mImageLoaderServiceConn);
	}
	
	public static void setupWebView(Context ctx){
		App.mWebView = new WebView(ctx);		
		App.mWebView.setVerticalScrollBarEnabled(false);
		App.mWebView.setHorizontalScrollBarEnabled(false);
		App.mWebView.setSoundEffectsEnabled(true);
		App.mWebView.setLayoutParams(FILL);	
	}
	
	public static Uri saveImageToGallery(Context ctx, Bitmap bmp, String filename, String displayName, String title, int sizeBytes)
	throws FileNotFoundException{
		String storageState = Environment.getExternalStorageState();
		
		if(storageState.equals(Environment.MEDIA_MOUNTED)){
			File externalDirectory = Environment.getExternalStorageDirectory();
			File savedImageDirectory = new File(externalDirectory, "/vivacious");
			savedImageDirectory.mkdirs();			
			File savedImageFile = new File(savedImageDirectory, filename);
			
			FileOutputStream fos = new FileOutputStream(savedImageFile);
			boolean successCompress = bmp.compress(CompressFormat.JPEG, 100, fos);				
			if(successCompress){
				try {
					fos.flush();					
				} catch (IOException e) {				
					e.printStackTrace();
				}
			}
			
			//((App)ctx.getApplicationContext()).mMediaScannerConn.scanFile(savedImageFile.getAbsolutePath(), "image/jpeg");
			
			
			Log.d(LOG_TAG," successCompress?: "+successCompress);		
			ContentValues cv = new ContentValues();	
			cv.put(MediaStore.MediaColumns.DATA, savedImageFile.getAbsolutePath());
			cv.put(MediaStore.MediaColumns.DISPLAY_NAME,displayName);
			cv.put(MediaStore.MediaColumns.TITLE,title);
			cv.put(MediaStore.MediaColumns.SIZE, sizeBytes);		
			cv.put(MediaStore.MediaColumns.DATE_ADDED, new Date().getTime()/1000);
			
			try{
				MediaStore.Images.Media.insertImage(ctx.getContentResolver(),bmp,title,displayName);
			}catch(Exception e){				
			}finally{
				if(bmp!=null){
					//bmp.recycle();					
				}
			}
			/*
			Uri newRow = ctx.getContentResolver().insert(
					MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					cv);*/
			//Log.d(LOG_TAG," inserted row in gallery: "+newRow.toString());	
			//return newRow;
			return null;
			
		}
		return null;
			
	}
	
	public static void logout(Context ctx){
		Toast.makeText(ctx, "logout", 2000).show();
		App.mWebView.clearHistory();
		App.mWebView.clearCache(true);
		App.mWebView.clearFormData();
		//CookieManager.getInstance().removeAllCookie();
	}
	
	public SharedPreferences getPrefs(){
		return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
	}
	
	public void saveLastSearch(String keyword){		
		getPrefs().edit().putString(PREFS_SEARCH_KEYWORD, keyword).commit();		
	}
	
	public String getLastSearch(){
		return getPrefs().getString(PREFS_SEARCH_KEYWORD, PREFS_SEARCH_KEYWORD_DEFAULT);
	}
	
}