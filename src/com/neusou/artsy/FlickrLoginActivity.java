package com.neusou.artsy;

import java.security.NoSuchAlgorithmException;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.neusou.bioroid.restful.RestfulCallback;
import com.neusou.bioroid.restful.RestfulClient.RestfulMethod;
import com.neusou.bioroid.restful.RestfulClient.RestfulResponse;

public class FlickrLoginActivity extends Activity {

	private static final String LOG_TAG = "FlickrLoginActivity";
	private String frob;
	private String perms = "write";
	private LinearLayout mContent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mContent = new LinearLayout(this);
		App.setupWebView(this);
		setupWebView();
		addContentView(mContent, App.FILL);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	protected void onDestroy() {
		super.onDestroy();
		mContent.removeAllViews();		
	};

	RestfulCallback<RestfulResponse> mCallback = new RestfulCallback<RestfulResponse>(
			Flickr.getInstance().restfulClient) {
		@Override
		public <T extends RestfulMethod> void onCallback(T restMethod,
				final RestfulResponse response, String error) {

			Logger.l(Logger.DEBUG, LOG_TAG, "response:"+response.getData());

			runOnUiThread(new Runnable(){
				@Override
				public void run() {
					try {
						JSONObject rsp = new JSONObject(response.getData());
						String frob = rsp.optJSONObject("frob").optString("_content");		
						App.mWebView.loadUrl(createUrl(Flickr.API_KEY, "read", frob));
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			});
			
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		mCallback.register(this);
		getFrob();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mCallback.unregister(this);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return super.onKeyUp(keyCode, event);
	}
	
	final int logoff = 0;
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		menu.add(0, logoff, 0, "logoff");
		return super.onPrepareOptionsMenu(menu);
	}
	
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		int id = item.getItemId();
		switch(id){
			case logoff:{
				App.logout(this);
				return true;

			}
		}
		return false;
	};

	private String createUrl(String apikey, String perms, String frob)
			throws Exception {

		Bundle data = new Bundle();
		data.putString("perms", perms);
		data.putString("frob", frob);
		data.putString("api_key", apikey);
		String sig = "";
		try {
			sig = Flickr.getInstance().createRequestSignature(
					Flickr.API_SECRET, data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new Exception();
		}
		StringBuffer sb = new StringBuffer("http://flickr.com/services/auth/?");
		sb.append("api_key=").append(apikey).append("&perms=").append(perms)
				.append("&frob=").append(frob).append("&api_sig=").append(sig);
		Log.d(LOG_TAG, sb.toString());
		return sb.toString();
	}

	WebViewClient wvc = new WebViewClient() {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.d(LOG_TAG, url);
			return false;
		}
	};

	private void setupWebView() {
		
		App.mWebView.setWebViewClient(wvc);
		//App.mWebView.getSettings().setUserAgentString("Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/533.3 (KHTML, like Gecko) Chrome/5.0.354.0 Safari/533.3");
		mContent.addView(App.mWebView);
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

}