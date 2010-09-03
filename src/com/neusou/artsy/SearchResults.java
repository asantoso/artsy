package com.neusou.artsy;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;

public class SearchResults extends Activity{
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {	
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		setTitle(SearchResults.class.getSimpleName());
	}
	
	@Override
	protected void onResume() {	
		super.onResume();
		Toast.makeText(this, "searchResults", 2000).show();
	}
	
	
}