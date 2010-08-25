package com.neusou.vivacious;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.OnGestureListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.neusou.bioroid.restful.RestfulCallback;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.restful.RestfulClient.RestfulResponse;
import com.neusou.vivacious.Flickr.FlickrPhotosGetInfo;
import com.neusou.vivacious.Flickr.Photo;
import com.neusou.vivacious.Flickr.Photo.Size;

public class Main extends BaseActivity {

	public static final String LOG_TAG = Main.class.getCanonicalName();

	RestfulCallback<RestfulResponse> mFlickerRestfulCallback;
	GestureDetector mScrollViewGestureDetector;
	GestureDetector mImageGestureDetector;
	GestureDetector mCaptionGestureDetector; 
	Flickr.Photo[] photos;
	int currentPhotoIndex = 0;

	ImageView mImage;
	TextView mCaption;
	TextView mCaption2;
	TextView mCaption3;
	CountDownLatch waitServiceLatch;
	String query = "japan";
	FrameLayout mCaptionContainer;
	
	HashMap<Integer,Long> mCallIds = new HashMap<Integer, Long>();

	BroadcastReceiver wakeupbr = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onReceive action: "
					+ intent.getAction());
		}
	};

	Configuration config;
	int w,h;
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		config = newConfig;	
		
		updateDisplayDimension();
		updateDueToOrientationChange();
	}
	
	int mCaptionContainer_height = 200;
	
	void updateDueToOrientationChange(){
		updateLayoutParams(R.id.captioncontainer, currentPage*-w,0,0,0);
		updateLayoutParams(R.id.content1, currentPage*-w,0,0,0);
	}
	
	private void updateDisplayDimension(){
		View v = findViewById(R.id.root);
		int mW = v.getMeasuredWidth();
		int mH = v.getMeasuredHeight();
		int rootH = v.getHeight();
		int rootW = v.getWidth();
		Log.d(LOG_TAG," root. mW:"+mW+" mH:"+mH+" rW:"+rootW+", rH:"+rootH);
		
		DisplayMetrics metrics = new DisplayMetrics();
		
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		if(config != null && config.orientation == Configuration.ORIENTATION_LANDSCAPE){
			w = metrics.widthPixels;
			h = metrics.heightPixels;
			mCaptionContainer_height = 100;
		}else{
			w = metrics.widthPixels;
			h = metrics.heightPixels;
			mCaptionContainer_height = 100;
		}		
		if(config != null){
			Log.d(LOG_TAG,config.orientation+", displaydim: "+w+", "+h);
		}
	}

	FlickrService mService;

	public ServiceConnection mFlickrServiceConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onServiceDisconnected "
					+ name.toString());
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onServiceConnected "
					+ name.toString() + " binder: "
					+ service.getClass().getCanonicalName());
			mService = ((FlickrService.FlickrBinder) service).getService();
			waitServiceLatch.countDown();
		}
	};

	IRemoteService mRemoteService;
	public ServiceConnection mRemoteConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onServiceDisconnected");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onServiceConnected "
					+ name.toString() + " binder: "
					+ service.getClass().getCanonicalName());
			mRemoteService = IRemoteService.Stub.asInterface(service);
			try {
				int result = mRemoteService.processData(1);
				Logger.l(Logger.DEBUG, LOG_TAG, "remote process result: "
						+ result);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	Flickr flickr;
	boolean isCaptionContainerExpanded = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		bindViews();
		initObjects();
		initViews();
		
		waitServiceLatch = new CountDownLatch(1);

		bindService(new Intent(getApplicationContext(), FlickrService.class),
				mFlickrServiceConn, Context.BIND_AUTO_CREATE);
		bindService(new Intent(getApplicationContext(),
				FlickrRemoteService.class), mRemoteConnection,
				Context.BIND_AUTO_CREATE);

		Flickr flickr = Flickr.getInstance();

		mFlickerRestfulCallback = new RestfulCallback<RestfulResponse>(
				flickr.restfulClient) {

			@Override
			public void onCallback(RestfulMethod restMethod,
					RestfulResponse response, String error) {

				int methodId = restMethod.describeContents();
				
				if (error != null) {
					Log.d(LOG_TAG, "error ######: " + error + " #####");
					Toast.makeText(Main.this, error, 2000).show();
					return;
				}

				if (restMethod instanceof Flickr.FlickrGetFrob) {
					

				}

				if (restMethod instanceof Flickr.FlickrGroupsSearch) {
					try {
						JSONObject json = new JSONObject(response.getData());
						Log.d(Main.LOG_TAG, json.toString(2));
						JSONArray groups = json.getJSONObject("groups")
								.getJSONArray("group");
						JSONObject group = groups.getJSONObject(0);
						String nsid = group.optString("nsid");
						String name = group.optString("name");
						Log.d(Main.LOG_TAG, nsid + ", " + name);

						doGroupsPoolsGetPhotos(getAndIncrementCallId(Flickr.METHOD_GROUPS_POOLS_GETPHOTOS),nsid);
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}

				if (restMethod instanceof Flickr.FlickrGroupsPoolsGetPhotos) {
					try {
						JSONObject json = new JSONObject(response.getData());
						Log.d(Main.LOG_TAG, "GroupsPoolsGetPhotos: "
								+ json.toString(2));
						JSONArray jsArrayPhotos = json.getJSONObject("photos")
								.getJSONArray("photo");
						photos = Flickr.Photo.parseArray(jsArrayPhotos);
						showImage(0);
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}

				if (restMethod instanceof Flickr.FlickrPhotosSearch) {
					try {
						JSONObject json = new JSONObject(response.getData());
						// Log.d(Main.LOG_TAG,"PhotosSearch: "+json.toString(2));
						JSONArray jsArrayPhotos = json.getJSONObject("photos")
								.getJSONArray("photo");
						photos = Flickr.Photo.parseArray(jsArrayPhotos);
						showImage(0);
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}

				if (methodId == Flickr.METHOD_PHOTOS_GETINFO) {
					try {
						JSONObject json = new JSONObject(response.getData());
						Log.d(Main.LOG_TAG,"PhotoGetInfo: "+json.toString(2));
						
						long callId = restMethod.getCallId();
						boolean isLatest = isLatestCall(Flickr.METHOD_PHOTOS_GETINFO, callId);
						if(isLatest){
						JSONObject photoInfo = json.optJSONObject("photo");
						photoInfo.remove("tags");
						Log.d(Main.LOG_TAG, "PhotoGetInfo: "+ json.toString(2));
						String title = photoInfo.optJSONObject("title")
								.getString("_content");
						String description = photoInfo.optJSONObject("description").getString("_content");						
						String views = photoInfo.optString("views");
						String realname = photoInfo.optJSONObject("owner").getString("realname");
						String username = photoInfo.optJSONObject("owner").getString("username");
						String dateUploaded = photoInfo.optString("dateuploaded");
						mCaption.setText(title + "\n\n" + realname + "\n"+ views + " views");
						mCaption2.setText(description.trim());						
						mCaption3.setText(dateUploaded);
						//mWebView.setBackgroundColor(Color.TRANSPARENT);
						//String data = "<html><head><style type=\"text/css\">body{background:#transparent;}</style></head><body>"+description+"</body></html>";
						//mWebView.loadData(data,"text/html", "utf-8");
						
						}						
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}

			}

		};

		
		
	}

	protected void onDestroy() {
		super.onDestroy();
		unbindService(mFlickrServiceConn);
		unbindService(mRemoteConnection);
	};
	@Override
	protected void onStart() {
		super.onStart();
		flickr = Flickr.getInstance();
	}

	@Override
	protected void onStop() {
		super.onStop();

	}

	@Override
	protected void onPause() {
		super.onPause();
		mFlickerRestfulCallback.unregister(this);
		unregisterReceiver(wakeupbr);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		handleIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		//updateDueToOrientationChange();
		new WaitLatchThread(waitServiceLatch).start();
		mFlickerRestfulCallback.register(this);
		registerReceiver(wakeupbr,	new IntentFilter(FlickrRemoteService.WAKE_UP));		
	}
	
	@Override
	protected void onPostResume() {
		super.onPostResume();
		updateDisplayDimension();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		query = savedInstanceState.getString("query");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("query", query);
	}

	public void bindViews() {
		mImage = (ImageView) findViewById(R.id.image);
		mCaption = (TextView) findViewById(R.id.caption);
		mCaption2 = (TextView) findViewById(R.id.caption2);
		mCaption3 = (TextView) findViewById(R.id.caption3);
		mCaptionContainer = (FrameLayout) findViewById(R.id.captioncontainer);
		
		mContent1 = (MyScrollView) findViewById(R.id.content1);
		mContent2 = (MyScrollView) findViewById(R.id.content2);
		mContent3 = (MyScrollView) findViewById(R.id.content3);
	}

	public void initObjects(){
		mScrollViewGestureDetector = new GestureDetector(new OnGestureListener() {
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
				Logger.l(Logger.DEBUG, LOG_TAG, "scrollView. onScroll dx"+distanceX);
				final double minThreshold = 8;
				if(Math.abs(distanceX) > minThreshold){
					return false;
				}
				return true;
			}
	
			@Override
			public boolean onDown(MotionEvent e) {		
				Logger.l(Logger.DEBUG, LOG_TAG, "scrollView. onDown");
				return true;
			}
	
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				// TODO Auto-generated method stub
				return false;
			}
	
			@Override
			public void onLongPress(MotionEvent e) {

				Logger.l(Logger.DEBUG, LOG_TAG, "scrollView. onLongPress");
			}
	
			@Override
			public void onShowPress(MotionEvent e) {
				Logger.l(Logger.DEBUG, LOG_TAG, "scrollView. onShowPress");
				
			}
	
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				Logger.l(Logger.DEBUG, LOG_TAG, "scrollView. onSingleTapUp");
				return false;
			}
		});
		
		mCaptionGestureDetector = new GestureDetector(new OnGestureListener() {
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {				
				Logger.l(Logger.DEBUG, LOG_TAG, "CaptionGesture. onScroll. dX:"+distanceX+", dY:"+distanceY);
				mContent1.scrollBy(0, (int) distanceY);
				mContent2.scrollBy(0, (int) distanceY);
				mContent3.scrollBy(0, (int) distanceY);
				return false;
			}
	
			@Override
			public boolean onDown(MotionEvent e) {				
				return true;
			}
	
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				Logger.l(Logger.DEBUG, LOG_TAG, "CaptionGesture. onFling. vX:"+velocityX+" , vY:"+velocityY);
				
				
				return false;
			}
	
			@Override
			public void onLongPress(MotionEvent e) {
				Logger.l(Logger.DEBUG, LOG_TAG, "scrollView. onLongPress");
			}
	
			@Override
			public void onShowPress(MotionEvent e) {
				Logger.l(Logger.DEBUG, LOG_TAG, "scrollView. onShowPress");
			}
	
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				Logger.l(Logger.DEBUG,LOG_TAG,"onSingleTapUp");
				
				if(isCaptionContainerExpanded){
					mCaptionContainer_height = 200;	
				}
				else{
					mCaptionContainer_height = 400;
				}
				
				isCaptionContainerExpanded = !isCaptionContainerExpanded;
				return false;
			}
		});
	
		
		mImageGestureDetector = new GestureDetector(new OnGestureListener() {
	
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
	
				return false;
			}
	
			@Override
			public void onShowPress(MotionEvent e) {
				// TODO Auto-generated method stub
	
			}
	
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				// TODO Auto-generated method stub
				return true;
			}
	
			@Override
			public void onLongPress(MotionEvent e) {
				// TODO Auto-generated method stub
	
			}
	
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
	
				if (velocityX < 0) {
					currentPhotoIndex++;
				} else {
					currentPhotoIndex--;
				}
	
				if (photos == null || photos.length == 0) {
					return true;
				}
	
				if (currentPhotoIndex < 0) {
					currentPhotoIndex = photos.length - 1;
				} else {
					currentPhotoIndex = currentPhotoIndex % photos.length;
				}
	
				showImage(currentPhotoIndex);
	
				return false;
			}
	
			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}
		});
	
	}

	public void initViews(){
		mContent1.setGestureDetector(mScrollViewGestureDetector);
		mContent2.setGestureDetector(mScrollViewGestureDetector);
		mContent3.setGestureDetector(mScrollViewGestureDetector);
		
		/*
		mContent2.setOnTouchListener(new View.OnTouchListener() {			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Logger.l(Logger.DEBUG, LOG_TAG, "onTouch ");
				return mScrollViewGestureDetector.onTouchEvent(event);
			}
		});
		*/
				
		//mWebView = new WebView(this);
		//mWebView.setFocusable(false);
		//mWebView.setWebViewClient(new WebViewClient());
		/*
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.FILL_PARENT,				
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		*/		
		//mWebView.setLayoutParams(lp);
		
		//mContent2.addView(mWebView);
		
		LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
				w,mCaptionContainer_height);
				clp.gravity = Gravity.TOP | Gravity.LEFT;
				mContent1.setLayoutParams(clp);
				mContent2.setLayoutParams(clp);
				mContent3.setLayoutParams(clp);
				
				
		mCaptionContainer.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Logger.l(Logger.DEBUG, LOG_TAG, "onTouch");
				mCaptionGestureDetector.onTouchEvent(event);
				
				int action = event.getAction();
				if(action == MotionEvent.ACTION_DOWN){
					lastTouchX = event.getX();
					lastViewLeft = v.getLeft();
					return true;
				}else if(action == MotionEvent.ACTION_MOVE){
					int dx = (int)(event.getX() - lastTouchX);
					lastViewLeft += dx;
					float newViewLeft = lastViewLeft;
					if(newViewLeft > 0){
						newViewLeft = 0;
					}
					else if(newViewLeft < -w*maxPage){
						newViewLeft = -w*maxPage;
					}					   
					
					updateLayoutParams(R.id.captioncontainer,(int)newViewLeft,0,0,0);
					
					return true;
				}else if(action == MotionEvent.ACTION_UP){					
					float viewLeft = v.getLeft();
					Logger.l(Logger.DEBUG, LOG_TAG, "actionUp viewleft:"+viewLeft);
					
					lastViewLeft = 0;
					int dist = (int)(-viewLeft - (currentPage * w ));
					
					
					//int d = (int)(viewLeft + w*(-currentPage+0.5));
					Logger.l(Logger.DEBUG,LOG_TAG,"dist:"+dist+" "+w/2);
					
					if(Math.abs(dist) > w/3){
						if(dist > 0){
							//scroll to right
							if(currentPage < maxPage){
								currentPage++;
								//viewPage(v, currentPage);
							}							
						}else if(dist < 0){
							//scroll to left
							if(currentPage > 0){
								currentPage--;
								//viewPage(v, currentPage);
							}
						}
					}
					viewPage(mCaptionContainer, currentPage);
				}
				return false;
				
			}
		});
		
		
		mImage.setOnTouchListener(
			new View.OnTouchListener() {			
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					Logger.l(Logger.DEBUG, LOG_TAG, "onTouch "+event.getAction());
					boolean consumed = mImageGestureDetector.onTouchEvent(event);					
					return consumed;
				}
				
		});
		
	}

	MyScrollView mContent1,mContent2,mContent3;
	
	int lastViewLeft = 0;

	float lastTouchX = 0;

	int maxPage = 2;

	int currentPage = 0;

	//WebView mWebView;
	
	//TouchDelegate mTouchDelegate;

	private void updateLayoutParams(int id,int left, int top, int right, int bottom){
		if(id == R.id.captioncontainer){			
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.FILL_PARENT, mCaptionContainer_height);
			lp.addRule(RelativeLayout.ABOVE, R.id.actionbar);
			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			lp.setMargins(left,top,right,bottom);
			mCaptionContainer.setLayoutParams(lp);
		}
		
		else if(id == R.id.content1 || id == R.id.content2 || id == R.id.content3){
			LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(w,mCaptionContainer_height);
			clp.gravity=Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;		
			mContent1.setLayoutParams(clp);
			mContent2.setLayoutParams(clp);
			mContent3.setLayoutParams(clp);
		}
		
	}
	
	private void viewPage(View v, int page){
		updateLayoutParams(R.id.content1, currentPage*-w,0,0,0);
		updateLayoutParams(R.id.captioncontainer,currentPage*-w,0,0,0);		
	}
	

	
	//WebView mWebView;
	
	private void handleIntent(Intent intent) {
		Toast.makeText(this, "handleIntent " + intent.getAction(), 1000).show();
		if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			query = intent.getStringExtra(SearchManager.USER_QUERY);
			Toast.makeText(this, query, 1000).show();
		}
	}

	/*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);		
	}
	*/

	class WaitLatchThread extends Thread {
		public CountDownLatch latch;

		public WaitLatchThread(CountDownLatch wait) {
			latch = wait;
		}

		public void start() {
			super.start();

		}

		public void run() {
			Logger.l(Logger.DEBUG, "# thread wait for service", "waiting..");
			try {
				latch.await();
			} catch (InterruptedException e) {
			}
			Logger.l(Logger.DEBUG, "# thread wait for service", "continues..");
			//doGroupsSearch(0,query);
			doPhotoSearch(0, query, "ubud");
		}
	};

	public void getFrob() {

		Intent rest = new Intent(
				Flickr.getInstance().restfulClient.INTENT_EXECUTE_REQUEST);
		Flickr flickr = Flickr.getInstance();

		Bundle data = new Bundle();
		data.putParcelable(flickr.restfulClient.XTRA_METHOD,
				new Flickr.FlickrGetFrob());
		rest.putExtras(data);

		startService(rest);
		// mService.execute(data);
	}

	int groupPoolsGetPhotos_callId = 0;

	public void doPhotosGetInfo(long callId, String photoId){		
		Intent getPhotoInfo = new Intent(flickr.restfulClient.INTENT_EXECUTE_REQUEST);
		FlickrPhotosGetInfo method = new Flickr.FlickrPhotosGetInfo();
		method.photo_id = photoId;
		method.setCallId(callId);
		Bundle b = new Bundle();
		b.putParcelable(flickr.restfulClient.XTRA_METHOD, method);
		getPhotoInfo.putExtras(b);
		startService(getPhotoInfo);
	}
	
	public void doPhotoSearch(long callId, String tags, String text) {
		Intent rest = new Intent(Flickr.getInstance().restfulClient.INTENT_EXECUTE_REQUEST);
		Flickr.FlickrPhotosSearch method = new Flickr.FlickrPhotosSearch();
		method.tags = tags==null?"":tags;
		method.text = text==null?"":text;
		method.sort = Flickr.FlickrPhotosSearch.Sort.Relevance.value;
		method.in_gallery = false;
		method.tag_mode = Flickr.FlickrPhotosSearch.TagMode.all.value;
		method.content_type = Flickr.FlickrPhotosSearch.ContentType.other_only.value;
		method.setCallId(callId);
		rest.putExtra(flickr.restfulClient.XTRA_METHOD, method);
		startService(rest);
	}

	public void doGroupsSearch(long callId, String text) {
	
		Intent rest = new Intent(Main.this,FlickrIntentService.class);
		rest.setAction(flickr.restfulClient.INTENT_EXECUTE_REQUEST);
		Flickr.FlickrGroupsSearch method = new Flickr.FlickrGroupsSearch(); 
		method.text = text;
		method.setCallId(callId);
		rest.putExtra(flickr.restfulClient.XTRA_METHOD, method);
		startService(rest);
		
	}
	
	public void doGroupsPoolsGetPhotos(long callId, String nsid) {
		
		Intent restService = new Intent(Main.this, FlickrIntentService.class);
		restService.setAction(flickr.restfulClient.INTENT_EXECUTE_REQUEST);
		Flickr.FlickrGroupsPoolsGetPhotos method = new Flickr.FlickrGroupsPoolsGetPhotos();
		method.group_id = nsid;
		method.tags = null;
		method.paging.page = 1;
		method.paging.perPage = 10;
		
		method.setCallId(callId);
		
		restService.putExtra(flickr.restfulClient.XTRA_METHOD, method);
		Log.d(Main.LOG_TAG, "startingService");
		startService(restService);
	
	}

	private long getAndIncrementCallId(int methodId){
		Long callId = mCallIds.get(methodId);
		if(callId == null){
			callId = 0l;
		}else{
			callId++;
		}
		mCallIds.put(methodId, callId);
		return callId;
	}
	
	private boolean isLatestCall(int methodId, long id){
		Log.d(LOG_TAG,""+id+" "+mCallIds.get(methodId));
		return mCallIds.get(methodId).equals(id);
	}
	
	public void showImage(int i) {
		if (photos.length == 0 || i > photos.length) {
			return;
		}

		mCaption.setText("");

		Photo currentPhoto = photos[i];

		URL imageUrl;

		doPhotosGetInfo(getAndIncrementCallId(Flickr.METHOD_PHOTOS_GETINFO), currentPhoto.id);

		try {
			imageUrl = currentPhoto.createImageUrl(Size.normal);
		} catch (MalformedURLException e3) {
			e3.printStackTrace();
			return;
		}

		Log.d(LOG_TAG, "imageUrl: " + imageUrl.toString());

		showImage(imageUrl);
	};

	class ImageLoadingThread implements Runnable{
		private URL mBitmapUrl; 
		
		public ImageLoadingThread(URL bitmapUrl) {
			mBitmapUrl = mBitmapUrl;
		}
		
		public void run(){
		HttpGet httpRequest = null;

		try {
			httpRequest = new HttpGet(mBitmapUrl.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;
		try {
			response = (HttpResponse) httpclient.execute(httpRequest);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {

		}

		HttpEntity entity = response.getEntity();
		BufferedHttpEntity bufHttpEntity;
		try {
			bufHttpEntity = new BufferedHttpEntity(entity);
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
	}
	
	public void showImage(URL bitmapUrl) {
		HttpGet httpRequest = null;

		try {
			httpRequest = new HttpGet(bitmapUrl.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response;
		try {
			response = (HttpResponse) httpclient.execute(httpRequest);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} finally {

		}

		HttpEntity entity = response.getEntity();
		BufferedHttpEntity bufHttpEntity;
		try {
			bufHttpEntity = new BufferedHttpEntity(entity);
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
		Log.d(LOG_TAG, "onSearchRequested");
		Bundle data = new Bundle();
		startSearch(query, false, data, false);
		return true;
	}
	
	public static final int OPTIONS_SET_WALLPAPER = 0;
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0,OPTIONS_SET_WALLPAPER,0,"Set as wallpaper");
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch(id){
			case OPTIONS_SET_WALLPAPER:{
				
				break;
			}	
		}
		return super.onOptionsItemSelected(item);
	}
}