package com.neusou.vivacious;

import java.util.concurrent.TimeUnit;

import com.neusou.async.UserTaskExecutionScope;
import com.neusou.web.ImageUrlLoader2;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

public class App extends Application{
	
	private static final String LOG_TAG = Logger.registerLog(App.class);
	public static WebView mWebView;
	
	
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
		pixelDensity = getResources().getDisplayMetrics().density;
		Flickr.getInstance().setContext(this);
		bindService(new Intent(App.this,FlickrService.class), mFlickrServiceConn, Context.BIND_AUTO_CREATE);
		bindService(new Intent(App.this,MyImageLoaderService.class), mImageLoaderServiceConn, Context.BIND_AUTO_CREATE);
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
	public static void logout(Context ctx){
		Toast.makeText(ctx, "logout", 2000).show();
		App.mWebView.clearHistory();
		App.mWebView.clearCache(true);
		App.mWebView.clearFormData();
		
		//CookieManager.getInstance().removeAllCookie();
	}
	
}