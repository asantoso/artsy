package com.neusou.vivacious;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.ContentObservable;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

public class BaseContentProvider extends ContentProvider{
	public static final String LOG_TAG = Logger.registerLog(BaseContentProvider.class);
	
	ContentObservable mObservable = new ContentObservable();
	
	public static final String AUTHORITY = BaseContentProvider.class.getPackage().getName()+".basecontentprovider";
	
	static class Photo{
		public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/photo");		
	}
	static class PhotoSets{
		public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/photosets");		
	}
	static class Group{
		public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/group");		
	}
	
	public void registerObserver(ContentObserver observer){	
		mObservable.registerObserver(observer);
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if(uri.compareTo(Photo.CONTENT_URI) == 0){
			
		}
		
		return null;
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Logger.l(Logger.DEBUG, LOG_TAG, "query "+uri.toString());
		if(uri.compareTo(Photo.CONTENT_URI) == 0){
			
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}
	
	
}