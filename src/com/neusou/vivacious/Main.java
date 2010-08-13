package com.neusou.vivacious;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class Main extends Activity {
	
	public static final String LOG_TAG = "Main";
    
	RestfulCallback cb;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);       	
		
        Flickr flickr = Flickr.getInstance();
        
        cb =  new RestfulCallback(flickr.restfulClient.CALLBACK_INTENT) {
    		@Override
    		public void onReceive(Context context, Intent intent) {		
    			super.onReceive(context, intent);    			
    			Intent rest = new Intent(Main.this, FlickrIntentService.class);
    			Flickr.FlickrGroupsSearch method = new Flickr.FlickrGroupsSearch();
    			method.text = "japan";
    			Flickr flickr = Flickr.getInstance();
    	    	rest.putExtra(flickr.restfulClient.XTRA_METHOD, method);
    	    	rest.putExtra(flickr.restfulClient.INTENT_PROCESS_RESPONSE, "process response from groupsSearch");
    	    	startService(rest);
    		}
    	};		
   
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
    	Intent rest = new Intent(this, FlickrIntentService.class);
    	//rest.setAction(getPackageName()+".REST");
    	Flickr flickr = Flickr.getInstance();
    	rest.putExtra(flickr.restfulClient.XTRA_METHOD, new Flickr.FlickrGetFrob());
    	rest.putExtra(flickr.restfulClient.INTENT_PROCESS_RESPONSE, "process response from getFrob");
    	startService(rest);
    	
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {    
    	super.onRestoreInstanceState(savedInstanceState);
    	
    }

}