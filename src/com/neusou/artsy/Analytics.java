package com.neusou.artsy;

import com.flurry.android.FlurryAgent;

public class Analytics {
	public static final String FLURRY_KEY = "8ZT96E9SX43JUDYHI9HN";
	public static final String FLURRY_EVENT_SETWALLPAPER = "set_wallpaper";
	public static final String FLURRY_EVENT_VIEWINTERESTINGNESS = "interestingness";
	public static final String FLURRY_EVENT_SAVETOGALLERY = "save_to_gallery";
	public static final String FLURRY_EVENT_NEXTPAGE = "nextpage";
	public static final String FLURRY_EVENT_PREVPAGE = "prevpage";
	public static final String FLURRY_EVENT_SEARCH = "search";
	public static final String FLURRY_EVENT_VIEWNEXTIMAGE = "viewnextimage";
	
	public static void init(){		
		FlurryAgent.setReportLocation(true);	
		FlurryAgent.setContinueSessionMillis(30000);
	}	
}