package com.neusou.artsy;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;

	public class MyScrollView extends ScrollView{
		
		GestureDetector mGestureDetector;
				
		private static final String LOG_TAG = "MyScrolLView";
		public MyScrollView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		public void setGestureDetector(GestureDetector gestureDetector){
			mGestureDetector = gestureDetector;			
			
		}
		
		boolean bypassEvent = false; 
		
		
		int lastDownX;
		@Override
		public boolean onTouchEvent(MotionEvent ev) {
			
			int actionId = ev.getAction();
			switch(actionId){
				case MotionEvent.ACTION_DOWN:{
					lastDownX = (int) ev.getX();
					bypassEvent = false;
					break;
				}
				case MotionEvent.ACTION_MOVE:{
					int adx = Math.abs(lastDownX - (int)ev.getX());
					Logger.l(Logger.DEBUG, LOG_TAG, "onTouchEvent adx:"+adx);
					if(adx > 5){
						bypassEvent = true;
					}else{
						bypassEvent = false;
					}
					break;
				}
				case MotionEvent.ACTION_UP:{					
					bypassEvent = false;
					break;
				}
				case MotionEvent.ACTION_CANCEL:{
					bypassEvent = false;
					break;
				}
				case MotionEvent.ACTION_OUTSIDE:{
					bypassEvent = false;
					break;
				}
			}
			
			super.onTouchEvent(ev);
			
			return false;
			/*			
			if(bypassEvent){
				return false;
			}
			return false;
			*/
		}
	
	}
	