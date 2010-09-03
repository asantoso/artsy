package com.neusou.artsy;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

public class MyTextView extends TextView {

	private static final String LOG_TAG = Logger.registerLog(MyTextView.class);
	
	public MyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		Logger.l(Logger.DEBUG, LOG_TAG, "onLayout "+left+" "+top+" "+right+" "+bottom);
		
		super.onLayout(changed, left, top, right, bottom);
	}
	
	
	
}

class MyViewGroup extends ViewGroup {

	public MyViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		
		
	}
	
	
}
