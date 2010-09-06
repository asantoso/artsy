package com.neusou.artsy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector.OnGestureListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout; 
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.admob.android.ads.AdListener;
import com.admob.android.ads.AdView;
import com.flurry.android.FlurryAgent;
import com.neusou.bioroid.thread.DecoupledHandlerThread;
import com.neusou.artsy.Flickr.FlickrPhotosGetInfo;
import com.neusou.artsy.Flickr.Photo;
import com.neusou.artsy.Flickr.Photo.Size;
import com.neusou.artsy.GaleryAdapter.GalleryImageTag;
import com.neusou.bioroid.image.ImageLoader;
import com.neusou.bioroid.restful.RestfulCallback;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.restful.RestfulClient.RestfulResponse;

public class Main extends BaseActivity {

	public static final String LOG_TAG = Main.class.getCanonicalName();

	RestfulCallback<RestfulResponse> mFlickerRestfulCallback;
	GestureDetector mScrollViewGestureDetector;
	GestureDetector mImageGestureDetector;
	GestureDetector mCaptionGestureDetector;
	Flickr.Photo[] photos;
	int currentPhotoIndex = 0;
	MyGallery mGallery;
	int interestingnessIntervalDays = 365 * 1;

	public static final int OPTIONS_SAVETOGALLERY = 0;
	public static final int OPTIONS_SET_WALLPAPER = 1;
	public static final int OPTIONS_INTERESTINGNESS = 2;

	TextView mCaption;
	TextView mCaption2;
	TextView mCaption3;
	CountDownLatch waitServiceLatch;
	String query;
	FrameLayout mCaptionContainer;
	HashMap<Integer, Long> mCallIds = new HashMap<Integer, Long>();
	Configuration config;
	int w, h;
	int mCaptionContainer_height = mCaptionContainer_height_normal;
	static final int mCaptionContainer_height_normal = 24 * 2 + 5;
	static final int mCaptionContainer_height_expanded = 180;

	BroadcastReceiver wakeupbr = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Logger.l(Logger.DEBUG, LOG_TAG, "onReceive action: "
					+ intent.getAction());
		}
	};

	public void toggleCaptionContainer() {
		if (isCaptionContainerExpanded) {
			mCaptionContainer_height = mCaptionContainer_height_normal;
		} else {
			mCaptionContainer_height = mCaptionContainer_height_expanded;
		}

		isCaptionContainerExpanded = !isCaptionContainerExpanded;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		config = newConfig;

		updateDisplayDimension();
		updateCaptionContainerView();

	}

	void updateCaptionContainerView() {
		updateLayoutParams(R.id.captioncontainer, currentPage * -w, 0, 0, 0);
		updateLayoutParams(R.id.content1, currentPage * -w, 0, 0, 0);
	}

	private void updateDisplayDimension() {
		View v = findViewById(R.id.root);
		int mW = v.getMeasuredWidth();
		int mH = v.getMeasuredHeight();
		int rootH = v.getHeight();
		int rootW = v.getWidth();

		DisplayMetrics metrics = new DisplayMetrics();

		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		if (config != null
				&& config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			w = metrics.widthPixels;
			h = metrics.heightPixels;
			// mCaptionContainer_height /= 2;
		} else {
			w = metrics.widthPixels;
			h = metrics.heightPixels;
			// mCaptionContainer_height *= 2;
		}
		if (config != null) {

		}
	}

	FlickrService mService;

	private int browseOperation = BROWSEOPERATION_NOOP;
	private int browseMode = BROWSEMODE_SEQUENTIAL;
	private static final int BROWSEOPERATION_NEXT = 1;
	private static final int BROWSEOPERATION_PREV = 2;
	private static final int BROWSEOPERATION_NOOP = 0;
	private static final int BROWSEMODE_JUMP = 1;
	private static final int BROWSEMODE_SEQUENTIAL = 2;

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
				// e.printStackTrace();
			}
		}
	};

	Flickr flickr;
	boolean isCaptionContainerExpanded = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.main);

		bindViews();
		initObjects();
		initViews();

		waitServiceLatch = new CountDownLatch(1);

		bindService(new Intent(getApplicationContext(), FlickrService.class),
				mFlickrServiceConn, Context.BIND_AUTO_CREATE);

		/*
		 * bindService(new Intent(getApplicationContext(),
		 * FlickrRemoteService.class), mRemoteConnection,
		 * Context.BIND_AUTO_CREATE);
		 */

	}

	protected void onDestroy() {
		super.onDestroy();
		unbindService(mFlickrServiceConn);
		// unbindService(mRemoteConnection);
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
		mAsyncExecutionHandlerThread.waitInit();
		new WaitLatchThread(waitServiceLatch).start();
		mFlickerRestfulCallback.register(this);
		registerReceiver(wakeupbr,
				new IntentFilter(FlickrRemoteService.WAKE_UP));
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		updateDisplayDimension();
		updateCaptionContainerView();
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

	AdView mAdView;
	View mAdViewWrapper;
	ImageButton mShowInfo;
	ImageButton mSaveToGallery;
	ImageButton mSetAsWallpaper;
	ImageButton mSearch;
	ImageButton mInterestingness;
	ImageButton mPrevPage;
	ImageButton mNextPage;

	public void bindViews() {
		mNextPage = (ImageButton) findViewById(R.id.actionbar_nextpage);
		mPrevPage = (ImageButton) findViewById(R.id.actionbar_prevpage);

		mAdViewWrapper = findViewById(R.id.adview_wrapper);
		mAdView = (AdView) findViewById(R.id.ad);
		mAdView.setRequestInterval(30);
		mGallery = (MyGallery) findViewById(R.id.gallery);

		mCaption = (TextView) findViewById(R.id.caption);
		mCaption2 = (TextView) findViewById(R.id.caption2);
		mCaption3 = (TextView) findViewById(R.id.caption3);
		mCaptionContainer = (FrameLayout) findViewById(R.id.captioncontainer);

		mContent1 = (MyScrollView) findViewById(R.id.content1);
		mContent2 = (MyScrollView) findViewById(R.id.content2);
		mContent3 = (MyScrollView) findViewById(R.id.content3);

		mShowInfo = (ImageButton) findViewById(R.id.actionbar_viewinfo);
		mSaveToGallery = (ImageButton) findViewById(R.id.actionbar_savetogallery);
		mSetAsWallpaper = (ImageButton) findViewById(R.id.actionbar_setwallpaper);
		mSearch = (ImageButton) findViewById(R.id.actionbar_search);
		mInterestingness = (ImageButton) findViewById(R.id.actionbar_interestingness);
	}

	AnimationListener mCaptionContainerAnimationListener;
	GalleryImageContextualAnimationListener mGalleryImageShowAnimationListener;

	Paging resultPaging = new Paging();

	GaleryAdapter mGalleryAdapter;
	View.OnClickListener mLoadNextPageOnClick;
	View.OnClickListener mLoadPrevPageOnClick;

	public void initObjects() {
		query = ((App) getApplicationContext()).getLastSearch();

		flickr = Flickr.getInstance();
		mGalleryAdapter = new GaleryAdapter();
		mGallery.setAdapter(mGalleryAdapter);

		mAsyncExecutionHandlerThread.start();
		mGalleryImageShowAnimationListener = new GalleryImageContextualAnimationListener(
				null, View.INVISIBLE, View.VISIBLE);
		mGalleryImageShowAnimationListener = new GalleryImageContextualAnimationListener(
				null, View.VISIBLE, View.INVISIBLE);
		mCaptionContainerAnimationListener = new AnimationListener() {

			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// TODO Auto-generated method stub

			}
		};

		mScrollViewGestureDetector = new GestureDetector(
				new OnGestureListener() {
					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						// Logger.l(Logger.DEBUG, LOG_TAG,
						// "scrollView. onScroll dx"+distanceX);
						final double minThreshold = 8;
						if (Math.abs(distanceX) > minThreshold) {
							return false;
						}
						return true;
					}

					@Override
					public boolean onDown(MotionEvent e) {
						// Logger.l(Logger.DEBUG, LOG_TAG,
						// "scrollView. onDown");
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
						// Logger.l(Logger.DEBUG, LOG_TAG,
						// "scrollView. onLongPress");
					}

					@Override
					public void onShowPress(MotionEvent e) {
						// Logger.l(Logger.DEBUG, LOG_TAG,
						// "scrollView. onShowPress");

					}

					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						// Logger.l(Logger.DEBUG, LOG_TAG,
						// "scrollView. onSingleTapUp");
						return false;
					}
				});

		mCaptionGestureDetector = new GestureDetector(new OnGestureListener() {
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				// Logger.l(Logger.DEBUG, LOG_TAG,
				// "CaptionGesture. onScroll. dX:"+distanceX+", dY:"+distanceY);
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
				// Logger.l(Logger.DEBUG, LOG_TAG,
				// "CaptionGesture. onFling. vX:"+velocityX+" , vY:"+velocityY);

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
				toggleCaptionContainer();
				return true;
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
					// currentPhotoIndex = photos.length - 1;
					currentPhotoIndex++;
					fetchPreviousResultPage();
					return true;
				} else if (currentPhotoIndex >= photos.length) {
					currentPhotoIndex--;
					// currentPhotoIndex = currentPhotoIndex % photos.length;
					fetchNextResultPage();
					return true;
				}

				// showPagingOnTitleBar();
				showImage(currentPhotoIndex);
				return true;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}
		});

		mFlickerRestfulCallback = new RestfulCallback<RestfulResponse>(
				flickr.restfulClient) {

			@Override
			public void onCallback(RestfulMethod restMethod,
					RestfulResponse response, String error) {

				mBlockAsyncRequest = false;
				Resources res = getResources();

				int methodId = restMethod.describeContents();

				if (error != null) {
					// Log.d(LOG_TAG, "error ######: " + error + " #####");
					// check if there is data connection
					ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo ni = cm.getActiveNetworkInfo();
					if (ni != null) {
						if (!ni.isConnected()) {
							Toast.makeText(Main.this,
									getResources().getString(R.string.no_data),
									2000).show();
						} else {
							Toast.makeText(
									Main.this,
									getResources().getString(
											R.string.please_try_again), 2000)
									.show();
						}
					} else {
						Toast.makeText(
								Main.this,
								getResources().getString(
										R.string.no_active_network), 2000)
								.show();
					}

					return;
				}

				if (restMethod instanceof Flickr.FlickrGetFrob) {
				}

				if (restMethod instanceof Flickr.FlickrInterestingnessGetList) {
					try {
						JSONObject json = new JSONObject(response.getData());
						// Log.d(Main.LOG_TAG, json.toString(2));
						JSONObject jsObjectPhotos = json
								.getJSONObject("photos");
						JSONArray jsArrayPhotos = jsObjectPhotos
								.getJSONArray("photo");
						resultPaging = Flickr.parsePaging(json, resultPaging);
						photos = Flickr.Photo.parseArray(jsArrayPhotos);
						processBrowse();
						showImage(currentPhotoIndex);
					} catch (Exception e) {
					}
				}

				if (restMethod instanceof Flickr.FlickrGroupsSearch) {
					try {
						JSONObject json = new JSONObject(response.getData());
						// Log.d(Main.LOG_TAG, json.toString(2));
						JSONArray groups = json.getJSONObject("groups")
								.getJSONArray("group");
						resultPaging = Flickr.parsePaging(json, resultPaging);
						JSONObject group = groups.getJSONObject(0);
						String nsid = group.optString("nsid");
						String name = group.optString("name");
						// Log.d(Main.LOG_TAG, nsid + ", " + name);

						doGroupsPoolsGetPhotos(
								getAndIncrementCallId(Flickr.METHOD_GROUPS_POOLS_GETPHOTOS),
								nsid);
					} catch (JSONException e) {
						// e.printStackTrace();
					}
				}

				if (restMethod instanceof Flickr.FlickrGroupsPoolsGetPhotos) {
					try {
						JSONObject json = new JSONObject(response.getData());
						// Log.d(Main.LOG_TAG, "GroupsPoolsGetPhotos: "
						// + json.toString(2));
						JSONArray jsArrayPhotos = json.getJSONObject("photos")
								.getJSONArray("photo");
						photos = Flickr.Photo.parseArray(jsArrayPhotos);

					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				if (restMethod instanceof Flickr.FlickrPhotosSearch) {
					try {
						JSONObject json = new JSONObject(response.getData());
						// Log.d(Main.LOG_TAG,"PhotosSearch: "+json.toString(2));
						JSONObject jsObjectPhotos = json
								.getJSONObject("photos");
						JSONArray jsArrayPhotos = jsObjectPhotos
								.getJSONArray("photo");
						resultPaging = Flickr.parsePaging(json, resultPaging);
						photos = Flickr.Photo.parseArray(jsArrayPhotos);
						processBrowse();
						showImage(currentPhotoIndex);
					} catch (JSONException e) {
						e.printStackTrace();
					}

				}

				if (restMethod instanceof Flickr.FlickrPhotosGetInfo) {
					try {
						JSONObject json = new JSONObject(response.getData());
						// Log.d(Main.LOG_TAG,"PhotoGetInfo: "+json.toString(2));

						long callId = restMethod.getCallId();
						boolean isLatest = isLatestCall(
								Flickr.METHOD_PHOTOS_GETINFO, callId);
						if (isLatest) {
							JSONObject photoInfo = json.optJSONObject("photo");
							JSONObject tagsWrapper = (JSONObject) photoInfo
									.remove("tags");
							JSONArray tags;
							String tagsListStr = "";
							if (tagsWrapper != null) {
								tags = tagsWrapper.optJSONArray("tag");
								if (tags != null) {
									StringBuffer sb = new StringBuffer();
									for (int i = 0, n = tags.length(); i < n; i++) {
										try {
											sb.append(tags.optJSONObject(i)
													.optString("raw"));
											if (i < n - 1) {
												sb.append(", ");
											}
										} catch (Exception e) {
										}
									}
									tagsListStr = sb.toString();
								}
							}

							// Log.d(Main.LOG_TAG, "PhotoGetInfo: "+
							// json.toString(2));

							String realname = "";
							String username = "";
							String location = "";
							String dateuploaded = "";
							String description = "";
							String title = "";
							String views = "";

							try {
								title = photoInfo.optJSONObject("title")
										.getString("_content");
							} catch (NullPointerException e) {
							}
							try {
								description = photoInfo.optJSONObject(
										"description").getString("_content");
							} catch (NullPointerException e) {
							}
							try {
								views = photoInfo.optString("views");
							} catch (NullPointerException e) {
							}

							try {
								realname = photoInfo.optJSONObject("owner")
										.optString("realname");
							} catch (NullPointerException e) {
							}
							try {
								username = photoInfo.optJSONObject("owner")
										.optString("username");
							} catch (NullPointerException e) {
							}
							try {
								location = photoInfo.optJSONObject("owner")
										.optString("location");
							} catch (NullPointerException e) {
							}
							try {
								dateuploaded = photoInfo
										.optString("dateuploaded");
							} catch (NullPointerException e) {
							}

							mCaption.setText(title + "\n\n"
									+ description.trim());
							StringBuffer sb = new StringBuffer();
							sb.append(res.getString(R.string.uploaded_by)+": ").append(username)
									.append(
											realname.length() > 0 ? (" ("
													+ realname + ")") : "")
									.append("\n\n"+res.getString(R.string.location)+": ").append(location)
									// .append("\n\ndate uploaded: "+dateuploaded)
									.append("\n\n"+res.getString(R.string.tags) +": "+ tagsListStr);
							mCaption2.setText(sb.toString());
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					return;
				}

				mGalleryAdapter.setData(Main.this, resultPaging, photos);
				mGalleryAdapter.notifyDataSetChanged();
				mGallery.setSelection(getFirstImageIndex(), true);
				// Logger.l(Logger.DEBUG, LOG_TAG,
				// "num photos: "+(photos==null?0:photos.length));

				// showPagingOnTitleBar();
			}
		};
	}

	int mFirstImageIndex = 0;

	private int getFirstImageIndex() {
		if (resultPaging.page > 1) {
			return 1;
		} else {
			return 0;
		}
	}

	private void processBrowse() {
		if (browseMode == BROWSEMODE_SEQUENTIAL) {
			currentPhotoIndex = 0;
			if (browseOperation == BROWSEOPERATION_NEXT) {
				currentPhotoIndex = 0;
			} else if (browseOperation == BROWSEOPERATION_PREV) {
				if (photos != null && photos.length > 0) {
					currentPhotoIndex = photos.length - 1;
				}
			} else {
				currentPhotoIndex = 0;
			}
			browseOperation = BROWSEOPERATION_NOOP;
		} else if (browseMode == BROWSEMODE_JUMP) {
			currentPhotoIndex = 0;
			if (browseOperation == BROWSEOPERATION_NEXT) {
				if (photos != null && photos.length > 0) {
					currentPhotoIndex = photos.length - 1;
				}
			} else if (browseOperation == BROWSEOPERATION_PREV) {
			} else {
				currentPhotoIndex = 0;
			}
			browseOperation = BROWSEOPERATION_NOOP;
		}
		browseOperation = BROWSEOPERATION_NOOP;
	}

	private void showPagingOnTitleBar() {
		StringBuffer sb = new StringBuffer();

		sb.append("page:").append(resultPaging.page).append("/").append(
				resultPaging.pages).append(" i:").append(currentPhotoIndex + 1)
				.append("/").append(photos.length).append(
						"  total:" + resultPaging.total);

		setTitle(sb.toString());
	}

	private void fetchPreviousResultPage() {
		FlurryAgent.onEvent(Analytics.FLURRY_EVENT_PREVPAGE);

		if (mBlockAsyncRequest) {
			return;
		}
		mBlockAsyncRequest = true;
		if (resultPaging.page > 1) {
			resultPaging.page--;
			browseOperation = BROWSEOPERATION_PREV;
			uiHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(Main.this, getResources().getString(R.string.fetching_prevpage), 2000)
							.show();
				}
			});
			executeCurrentOperation();

		} else {
			uiHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(Main.this, getResources().getString(R.string.no_more_images), 700).show();
				}
			});
		}
	}

	Handler uiHandler = new Handler();

	private void getRandomInterestingness() {
		mCurrentOperation = R.id.METHOD_INTERESTINGNESS_GETLIST;
		Calendar cal = Calendar.getInstance();
		Date now = cal.getTime();
		int backward = (int) (Math.random() * interestingnessIntervalDays);
		cal.set(Calendar.DATE, now.getDate() - backward);
		now = cal.getTime();
		Logger.l(Logger.DEBUG, LOG_TAG, now.toString());
		doInterestingnessGetList(0, now);
	}

	private void fetchNextResultPage() {
		FlurryAgent.onEvent(Analytics.FLURRY_EVENT_NEXTPAGE);
		if (mBlockAsyncRequest) {
			return;
		}
		mBlockAsyncRequest = true;
		if (resultPaging.page < resultPaging.pages) {
			resultPaging.page++;
			browseOperation = BROWSEOPERATION_NEXT;

			uiHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(Main.this, getResources().getString(R.string.fetching_nextpage), 2000)
							.show();
				}
			});

			executeCurrentOperation();
		} else {
			uiHandler.post(new Runnable() {
				public void run() {
					Toast.makeText(Main.this, getResources().getString(R.string.no_more_images), 700).show();
				}
			});
		}
	}

	public void initViews() {
		// registerForContextMenu(mGallery);
		// mGallery.setSpacing(10);

		mNextPage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// Toast.makeText(Main.this, "loading next page",
						// 2000).show();
					}
				});
				fetchNextResultPage();
			}
		});

		mPrevPage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						// Toast.makeText(Main.this, "loading prev page",
						// 2000).show();
					}
				});
				fetchPreviousResultPage();
			}
		});

		mShowInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// toggleCaptionContainer();
				// updateCaptionContainerView();
				if (mCaptionContainer.getVisibility() != View.VISIBLE) {
					mCaptionContainer.setVisibility(View.VISIBLE);
					// mCaptionContainer_height =
					// mCaptionContainer_height_normal;
				} else {
					mCaptionContainer.setVisibility(View.GONE);
					// mCaptionContainer_height =
					// mCaptionContainer_height_normal;
				}
			}
		});

		mSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onSearchRequested();
			}
		});

		mInterestingness.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getRandomInterestingness();
			}
		});

		mSetAsWallpaper.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				confirmWallpaper();
			}
		});

		mAdView.setAdListener(new AdListener() {
			@Override
			public void onReceiveRefreshedAd(AdView arg0) {
				mAdViewWrapper.setVisibility(View.VISIBLE);

			}

			@Override
			public void onReceiveAd(AdView arg0) {
				mAdViewWrapper.setVisibility(View.VISIBLE);

			}

			@Override
			public void onFailedToReceiveRefreshedAd(AdView arg0) {
				mAdViewWrapper.setVisibility(View.GONE);
			}

			@Override
			public void onFailedToReceiveAd(AdView arg0) {
				mAdViewWrapper.setVisibility(View.GONE);

			}
		});

		mGalleryAdapter
				.setPageListener(new GaleryAdapter.GalleryAdapterListener() {
					@Override
					public void onLoadPrevPage() {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// Toast.makeText(Main.this,
								// "loading prev page", 2000).show();
							}
						});
						fetchPreviousResultPage();
					}

					@Override
					public void onLoadNextPage() {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								// Toast.makeText(Main.this,
								// "loading next page", 2000).show();
							}
						});
						fetchNextResultPage();
					}
				});

		mGallery.setFadingEdgeLength(0);
		mGallery.setAnimationDuration(300);
		mGallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long arg3) {
				Logger.l(Logger.DEBUG, LOG_TAG, "clicked view: " + position);
			}
		});
		mGallery
				.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> arg0, View view,
							int position, long arg3) {
						Logger.l(Logger.DEBUG, LOG_TAG, "selected view: "
								+ position);
						FlurryAgent
								.onEvent(Analytics.FLURRY_EVENT_VIEWNEXTIMAGE);
						mCurrentImageView = view;
						clearCaptions();
						GalleryImageTag tag = (GalleryImageTag) view
								.getTag(R.id.tag_galleryadapter_image_data);
						try {
							doPhotosGetInfo(
									getAndIncrementCallId(Flickr.METHOD_PHOTOS_GETINFO),
									photos[tag.dataIndexPosition].id);
						} catch (Exception e) {
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {

					}

				});

		LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(w, App
				.toDip(mCaptionContainer_height));
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
				if (action == MotionEvent.ACTION_DOWN) {
					lastTouchX = event.getX();
					lastViewLeft = v.getLeft();
					return true;
				} else if (action == MotionEvent.ACTION_MOVE) {
					int dx = (int) (event.getX() - lastTouchX);
					lastViewLeft += dx;
					float newViewLeft = lastViewLeft;
					if (newViewLeft > 0) {
						newViewLeft = 0;
					} else if (newViewLeft < -w * maxPage) {
						newViewLeft = -w * maxPage;
					}

					updateLayoutParams(R.id.captioncontainer,
							(int) newViewLeft, 0, 0, 0);

					return true;
				} else if (action == MotionEvent.ACTION_UP) {
					float viewLeft = v.getLeft();
					// Logger.l(Logger.DEBUG, LOG_TAG,
					// "actionUp viewleft:"+viewLeft);

					lastViewLeft = 0;
					int dist = (int) (-viewLeft - (currentPage * w));

					// int d = (int)(viewLeft + w*(-currentPage+0.5));
					Logger.l(Logger.DEBUG, LOG_TAG, "dist:" + dist + " " + w
							/ 2);

					if (Math.abs(dist) > w / 3) {
						if (dist > 0) {
							// scroll to right
							if (currentPage < maxPage) {
								currentPage++;
								// viewPage(v, currentPage);
							}
						} else if (dist < 0) {
							// scroll to left
							if (currentPage > 0) {
								currentPage--;
								// viewPage(v, currentPage);
							}
						}
					}
					viewPage(mCaptionContainer, currentPage);
				}
				return false;

			}
		});

		/*
		 * mImage.setOnTouchListener( new View.OnTouchListener() {
		 * 
		 * @Override public boolean onTouch(View v, MotionEvent event) {
		 * Logger.l(Logger.DEBUG, LOG_TAG, "onTouch "+event.getAction());
		 * boolean consumed = mImageGestureDetector.onTouchEvent(event); return
		 * consumed; }
		 * 
		 * });
		 */

	}

	MyScrollView mContent1, mContent2, mContent3;

	int lastViewLeft = 0;

	float lastTouchX = 0;

	int maxPage = 1;

	int currentPage = 0;

	// WebView mWebView;

	// TouchDelegate mTouchDelegate;

	Handler adViewHandler = new Handler() {

	};

	private void updateLayoutParams(int id, int left, int top, int right,
			int bottom) {
		if (id == R.id.captioncontainer) {
			RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.FILL_PARENT, App
							.toDip(mCaptionContainer_height));

			lp.addRule(RelativeLayout.ABOVE, R.id.actionbar);
			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			lp.setMargins(left, top, right, bottom);
			mCaptionContainer.setLayoutParams(lp);
		}

		else if (id == R.id.content1 || id == R.id.content2
				|| id == R.id.content3) {
			LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(w,
					App.toDip(mCaptionContainer_height));
			clp.gravity = Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
			mContent1.setLayoutParams(clp);
			mContent2.setLayoutParams(clp);
			mContent3.setLayoutParams(clp);
		}

	}

	private void viewPage(View v, int page) {
		updateLayoutParams(R.id.content1, currentPage * -w, 0, 0, 0);
		updateLayoutParams(R.id.captioncontainer, currentPage * -w, 0, 0, 0);
	}

	// WebView mWebView;

	private void handleIntent(Intent intent) {
		// Toast.makeText(this, "handleIntent " + intent.getAction(),
		// 1000).show();
		if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
			query = intent.getStringExtra(SearchManager.USER_QUERY);
			query = query.replaceAll(" ", ",");
			((App) getApplicationContext()).saveLastSearch(query);
			Toast.makeText(this, getResources().getString(R.string.searching_for) + query, 1000).show();
			mCurrentOperation = R.id.METHOD_PHOTOS_SEARCH;
			resetPaging();
		}
	}

	private void resetPaging() {
		resultPaging.reset();
	}

	/*
	 * @Override public boolean onTouchEvent(MotionEvent event) { return
	 * super.onTouchEvent(event); }
	 */

	private int mCurrentOperation = R.id.METHOD_INTERESTINGNESS_GETLIST;

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
			executeCurrentOperation();
		}
	};

	// CountDownLatch mWaitAsyncRequest;

	DecoupledHandlerThread mAsyncExecutionHandlerThread = new DecoupledHandlerThread();

	volatile boolean mBlockAsyncRequest;

	private void executeCurrentOperation() {
		mAsyncExecutionHandlerThread.h.post(new Runnable() {
			@Override
			public void run() {
				switch (mCurrentOperation) {
				case R.id.METHOD_INTERESTINGNESS_GETLIST: {
					getRandomInterestingness();
					break;
				}
				case R.id.METHOD_PHOTOS_SEARCH: {
					doPhotoSearch(0, null, query);
					break;
				}			
				}

			}
		});

	}

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

	public void doPhotosGetInfo(long callId, String photoId) {
		Intent getPhotoInfo = new Intent(
				flickr.restfulClient.INTENT_EXECUTE_REQUEST);
		FlickrPhotosGetInfo method = new Flickr.FlickrPhotosGetInfo();
		method.photo_id = photoId;
		method.setCallId(callId);
		Bundle b = new Bundle();
		b.putParcelable(flickr.restfulClient.XTRA_METHOD, method);
		getPhotoInfo.putExtras(b);
		startService(getPhotoInfo);
	}

	public void doInterestingnessGetList(long callId, Date time) {
		Intent rest = new Intent(
				Flickr.getInstance().restfulClient.INTENT_EXECUTE_REQUEST);
		Flickr.FlickrInterestingnessGetList method = new Flickr.FlickrInterestingnessGetList();
		method.setCallId(callId);
		method.date = time;
		method.paging = resultPaging;
		rest.putExtra(flickr.restfulClient.XTRA_METHOD, method);
		startService(rest);
	}

	public void doPhotoSearch(long callId, String tags, String text) {
		Intent rest = new Intent(
				Flickr.getInstance().restfulClient.INTENT_EXECUTE_REQUEST);
		Flickr.FlickrPhotosSearch method = new Flickr.FlickrPhotosSearch();
		method.tags = tags == null ? "" : tags;
		method.text = text == null ? "" : text;
		method.sort = Flickr.FlickrPhotosSearch.Sort.InterestingnessDesc.value;
		method.in_gallery = false;
		method.paging = resultPaging;
		method.tag_mode = Flickr.FlickrPhotosSearch.TagMode.all.value;
		method.content_type = Flickr.FlickrPhotosSearch.ContentType.photos_and_other.value;
		method.setCallId(callId);
		rest.putExtra(flickr.restfulClient.XTRA_METHOD, method);
		startService(rest);
	}

	public void doGroupsSearch(long callId, String text) {

		Intent rest = new Intent(Main.this, FlickrIntentService.class);
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
		// Log.d(Main.LOG_TAG, "startingService");
		startService(restService);

	}

	private long getAndIncrementCallId(int methodId) {
		Long callId = mCallIds.get(methodId);
		if (callId == null) {
			callId = 0l;
		} else {
			callId++;
		}
		mCallIds.put(methodId, callId);
		return callId;
	}

	private boolean isLatestCall(int methodId, long id) {
		// Log.d(LOG_TAG,""+id+" "+mCallIds.get(methodId));
		try {
			return mCallIds.get(methodId).equals(id);
		} catch (NullPointerException e) {
			return false;
		}
	}

	public static final int DIALOG_SETWALLPAPERCONFIRM = 0;
	public static final int DIALOG_SETWALLPAPERAPPLYING = 1;

	protected Dialog onCreateDialog(int id) {
		Resources res = getResources();
		switch (id) {
		case DIALOG_SETWALLPAPERAPPLYING: {
			ProgressDialog swpd = new ProgressDialog(this);
			swpd.setMessage(res.getString(R.string.applying_wallpaper));
			swpd.setCancelable(false);
			return swpd;
		}
		case DIALOG_SETWALLPAPERCONFIRM: {
			
			AlertDialog swad = new AlertDialog.Builder(this).create();
			swad.setCancelable(true);
			swad.setMessage(res.getString(R.string.confirm_setwallpaper));
			swad.setButton(res.getString(R.string.yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_SETWALLPAPERCONFIRM);
					showDialog(DIALOG_SETWALLPAPERAPPLYING);
					setAsWallpaper();
				}
			});
			swad.setButton2(res.getString(R.string.no), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			return swad;
		}
		}
		return null;
	};

	public void confirmWallpaper() {
		showDialog(DIALOG_SETWALLPAPERCONFIRM);
	}

	private void setAsWallpaper() {

		if (mCurrentImageView != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					boolean success = false;
					// final Intent intent = new
					// Intent("com.android.camera.action.CROP");
					GalleryImageTag tag = (GalleryImageTag) mCurrentImageView
							.getTag(R.id.tag_galleryadapter_image_data);
					Photo currentPhoto = photos[tag.dataIndexPosition];
					URL imageUrl = null;

					try {
						imageUrl = currentPhoto.createImageUrl(Size.normal);
						imageUrl.getContent();
					} catch (MalformedURLException e3) {
						e3.printStackTrace();
					} catch (IOException e) {/*
						try {
							imageUrl = currentPhoto.createImageUrl(Size.normal);
							imageUrl.getContent();
						} catch (MalformedURLException e4) {
							e4.printStackTrace();
						} catch (IOException e5) {
							e5.printStackTrace();
						}*/
					}

					if (imageUrl == null) {
						success = false;
					} else {
						String imagePath = imageUrl.toString();
						Log.d("Main", imagePath);
						Bitmap img = App.mImageLoaderService.loadImage(imagePath, false);
						// Bitmap img = ((WeakReference<Bitmap>)
						// tag.imageView.get().getTag()).get();
						if (img == null) {
							success = false;
						} else {
							try {
								setWallpaper(img);
								//img.recycle(); cant recyle compress bitmap will be thrown if you set the image twice
								success = true;
							} catch (IOException e) {
								success = false;
							}
						}
					}

					final String msg;
					if (success) {
						msg = getResources().getString(R.string.set_wallpaper_successful);
					} else {
						msg = "Internal error: image can not be set as wallpaper.";
					}
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(Main.this, msg, 2000).show();
							dismissDialog(DIALOG_SETWALLPAPERAPPLYING);
						}
					});
				}
			}).start();
		}
	}

	public void clearCaptions() {
		
		
		mCaption.setText("");
		mCaption2.setText("");
		mCaption3.setText("");
	}

	public void clearContents() {
		mContent1.scrollTo(0, 0);
		mContent2.scrollTo(0, 0);
		mContent3.scrollTo(0, 0);

		clearCaptions();
		currentPage = 0;
		viewPage(mCaptionContainer, currentPage);
	}

	public void showImage(int i) {
		if (photos.length == 0 || i > photos.length) {
			return;
		}
		clearContents();
		Photo currentPhoto = photos[i];
		URL imageUrl;
		doPhotosGetInfo(getAndIncrementCallId(Flickr.METHOD_PHOTOS_GETINFO), currentPhoto.id);

		try {
			imageUrl = currentPhoto.createImageUrl(Size.small);
		} catch (MalformedURLException e3) {
			e3.printStackTrace();
			return;
		}

		Logger.l(Logger.DEBUG, LOG_TAG, "imageUrl: " + imageUrl.toString());

		showImage(imageUrl);
	};

	public void showImage(URL bitmapUrl) {
		// ImageLoader.AsyncLoaderInput input = new
		// ImageLoader.AsyncLoaderInput();
		// input.imageView = mImage;
		// input.imageUri = bitmapUrl.toString();
		// App.mImageLoaderService.loadImage(input, mImageLoadingListener);

	}

	private void doSetWallpaper() {
		confirmWallpaper();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Log.d(LOG_TAG, "createContextMenu: " + v.toString());
		menu.add("Set as wallpaper");
		menu.add("Save to gallery");
		menu.setHeaderTitle("Artful");
	}

	@Override
	public boolean onSearchRequested() {
		Logger.l(Logger.DEBUG, LOG_TAG, "onSearchRequested");
		FlurryAgent.onEvent(Analytics.FLURRY_EVENT_SEARCH);
		Bundle data = new Bundle();
		startSearch(query, false, data, false);
		return true;
	}

	View mCurrentImageView;

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		/*
		// menu.add(0,OPTIONS_SAVETOGALLERY,0,"Save to gallery").setIcon(android.R.drawable.ic_menu_gallery);
		menu.add(0, OPTIONS_SET_WALLPAPER, 0, "Set as wallpaper").setIcon(
				android.R.drawable.ic_menu_set_as);
		menu.add(0, OPTIONS_INTERESTINGNESS, 0, "Interestingness").setIcon(
				android.R.drawable.ic_menu_view);
		
		*/
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case OPTIONS_SAVETOGALLERY: {
			FlurryAgent.onEvent(Analytics.FLURRY_EVENT_SAVETOGALLERY);

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						GalleryImageTag tag = (GalleryImageTag) mCurrentImageView
								.getTag(R.id.tag_galleryadapter_image_data);
						Bitmap img = ((WeakReference<Bitmap>) tag.imageView
								.get().getTag()).get();
						App.saveImageToGallery(Main.this, img, Long
								.toString(new Date().getTime()), "Flickr",
								"Vivacious", 100);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast
										.makeText(
												Main.this,
												"Image has been saved in gallery",
												2000).show();
							}

						});
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				}
			}).start();

			break;
		}
		case OPTIONS_INTERESTINGNESS: {
			FlurryAgent.onEvent(Analytics.FLURRY_EVENT_VIEWINTERESTINGNESS);

			getRandomInterestingness();
			resetPaging();
			break;
		}

		case OPTIONS_SET_WALLPAPER: {
			FlurryAgent.onEvent(Analytics.FLURRY_EVENT_SETWALLPAPER);
			confirmWallpaper();
			break;
		}
		}
		return super.onOptionsItemSelected(item);
	}
}

class GaleryAdapter extends BaseAdapter {
	private Photo[] data;
	private Paging paging;
	private boolean hasPrevPage;
	private boolean hasNextPage;
	private Activity ctx;
	private LayoutInflater inflater;
	private View.OnClickListener mLoadNextPageOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (pageListener != null) {
				pageListener.onLoadNextPage();
			}
		}
	};
	private View.OnClickListener mLoadPrevPageOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (pageListener != null) {
				pageListener.onLoadPrevPage();
			}
		}
	};
	private View.OnTouchListener mLoadPrevPageOnTouch = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				if (pageListener != null) {
					pageListener.onLoadPrevPage();
				}
				return true;
			}
			return false;
		}
	};
	private View.OnTouchListener mLoadNextPageOnTouch = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				if (pageListener != null) {
					pageListener.onLoadNextPage();
				}
				return true;
			}
			return false;
		}
	};

	class GalleryImageTag {
		public int dataIndexPosition;
		public SoftReference<ImageView> imageView;
		public SoftReference<ImageView> loadingView;
	}

	class GalleryLoadPageTag {
	}

	ImageLoader.AsyncListener mImageLoadingListener = new ImageLoader.AsyncListener() {

		@Override
		public void onPublishProgress(ImageLoader.AsyncLoaderProgress progress) {
			progress.imageView.setImageBitmap(progress.bitmap);
			progress.imageView
					.setTag(new WeakReference<Bitmap>(progress.bitmap));

			Animation anim = AnimationUtils.loadAnimation(ctx, R.anim.fade_in);
			anim
					.setAnimationListener(new GalleryImageContextualAnimationListener(
							progress.imageView, View.INVISIBLE, View.VISIBLE));
			progress.imageView.startAnimation(anim);
		}

		@Override
		public void onPreExecute() {
		}

		@Override
		public void onPostExecute(ImageLoader.AsyncLoaderResult result) {
		}

		@Override
		public void onCancelled() {
		}
	};

	interface GalleryAdapterListener {
		public void onLoadNextPage();

		public void onLoadPrevPage();
	}

	private GalleryAdapterListener pageListener;

	public void setPageListener(GalleryAdapterListener pageListener) {
		this.pageListener = pageListener;
	}

	public void setData(Activity ctx, Paging paging, Photo... data) {
		this.data = data;
		this.paging = paging;
		this.ctx = ctx;
		this.inflater = ctx.getLayoutInflater();

		if (paging.page < paging.pages) {
			hasNextPage = true;
		} else {
			hasNextPage = false;
		}

		if (paging.page > 1) {
			hasPrevPage = true;
		} else {
			hasPrevPage = false;
		}

	}

	@Override
	public boolean isEmpty() {
		return data == null || data.length == 0;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		if (position == 0 && hasPrevPage) {
			return 0;
		} else if (position == getCount() - 1 && hasNextPage) {
			return 0;
		}
		return 1;
	}

	/**
	 * @ToDo there is a bug in Gallery class that convertView is null. i.e: no
	 *       recycling is implemented.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// compute offset
		int offset = 0;
		if (hasPrevPage) {
			offset++;
		}

		GalleryImageTag imageDataTag = null;
		GalleryLoadPageTag loadPageTag = null;
		try {
			imageDataTag = (GalleryImageTag) convertView
					.getTag(R.id.tag_galleryadapter_image_data);
		} catch (NullPointerException e) {

		}
		try {
			loadPageTag = (GalleryLoadPageTag) convertView
					.getTag(R.id.tag_galleryadapter_loadpage_data);
		} catch (NullPointerException e) {

		}

		if (position == 0 && hasPrevPage) {
			convertView = ctx.getLayoutInflater().inflate(
					R.layout.t_galleryimage_loadpage, parent, false);
			Button loadPrevPage = (Button) convertView
					.findViewById(R.id.loadpage);
			loadPrevPage.setText(ctx.getResources().getString(R.string.previous));
			Drawable prev = ctx.getResources().getDrawable(
					R.drawable.ic_menu_back);
			prev.setBounds(0, 0, prev.getIntrinsicWidth(), prev
					.getIntrinsicHeight());
			loadPrevPage.setCompoundDrawables(prev, null, null, null);
			loadPrevPage.setOnClickListener(mLoadPrevPageOnClick);
			// loadPrevPage.setOnTouchListener(mLoadPrevPageOnTouch);
			convertView.setTag(R.id.tag_galleryadapter_image_data, null);
			convertView.setTag(R.id.tag_galleryadapter_loadpage_data,
					loadPageTag);
			return convertView;
		}

		else if (position == getCount() - 1 && hasNextPage) {
			convertView = inflater.inflate(R.layout.t_galleryimage_loadpage,
					parent, false);
			Button loadNextPage = (Button) convertView
					.findViewById(R.id.loadpage);
			loadNextPage.setClickable(true);
			loadNextPage.setFocusable(true);
			loadNextPage.setText(ctx.getResources().getString(R.string.next));
			Drawable next = ctx.getResources().getDrawable(
					R.drawable.ic_menu_forward);
			next.setBounds(0, 0, next.getIntrinsicWidth(), next
					.getIntrinsicHeight());
			loadNextPage.setCompoundDrawables(null, null, next, null);
			loadNextPage.setOnClickListener(mLoadNextPageOnClick);
			// loadNextPage.setOnTouchListener(mLoadNextPageOnTouch);
			convertView.setTag(R.id.tag_galleryadapter_image_data, null);
			convertView.setTag(R.id.tag_galleryadapter_loadpage_data,
					loadPageTag);

			return convertView;
		}

		if (convertView == null || imageDataTag == null) {
			convertView = inflater.inflate(R.layout.t_galleryimage, parent,
					false);
		}

		if (imageDataTag == null) {
			// Log.d("crate","create new imagetag "+position);
			imageDataTag = new GalleryImageTag();
			imageDataTag.imageView = new SoftReference<ImageView>(
					(ImageView) convertView.findViewById(R.id.image));
			imageDataTag.loadingView = new SoftReference<ImageView>((ImageView) convertView.findViewById(R.id.loading));			
			convertView
					.setTag(R.id.tag_galleryadapter_image_data, imageDataTag);
		} else {
			// Log.d("crate","using existing new imagetag "+position);
		}

		//apply rotation animation
		/*
		 try{ 
			 Animation rotate = AnimationUtils.loadAnimation(ctx,
					R.anim.rotateright); 
			 rotate.setRepeatMode(Animation.INFINITE);
			 imageDataTag.loadingView.get().startAnimation(rotate);
		 }
		 catch(NullPointerException e){ }
		 */

		imageDataTag.dataIndexPosition = position - offset;

		ImageLoader.AsyncLoaderInput input = new ImageLoader.AsyncLoaderInput();
		input.imageView = (ImageView) convertView.findViewById(R.id.image);
		try {
			// Log.d("GALLERY","###### load data at position: " +
			// (position-offset));
			input.imageUri = data[position - offset].createImageUrl(
					Photo.Size.medium).toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		// registerForContextMenu(convertView);
		App.mImageLoaderService.loadImage(input, mImageLoadingListener);

		return convertView;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public int getCount() {
		// Logger.l(Logger.DEBUG, LOG_TAG,
		// "Gallery getCount "+(data==null?0:data.length));
		if (data == null || data.length == 0) {
			return 0;
		}
		int add = 0;
		if (hasPrevPage) {
			add++;
		}
		if (hasNextPage) {
			add++;
		}
		return data.length + add;
	}
};

class GalleryImageContextualAnimationListener extends
		ContextualAnimationListener {
	private int mStartVisibility;
	private int mEndVisibility;

	public GalleryImageContextualAnimationListener(View v, int startVisibility,
			int endVisibility) {
		super(v);
		mStartVisibility = startVisibility;
		mEndVisibility = endVisibility;
	}

	public void setView(View v) {
		mView = v;
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		mView.setVisibility(mEndVisibility);
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
	}

	@Override
	public void onAnimationStart(Animation animation) {
		mView.setVisibility(mStartVisibility);
	}

};
