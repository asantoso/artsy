package com.neusou.artsy;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.AdapterView.OnItemSelectedListener;

public class MyGallery extends Gallery{

		public MyGallery(Context context, AttributeSet attrs) {
			super(context, attrs);
			init();
		}
		private float modifiedVelocityX;

		private void init() {
			Display display = ((WindowManager) getContext().getSystemService(
					Context.WINDOW_SERVICE)).getDefaultDisplay();
			int width = display.getWidth();
			int height = display.getHeight();
		

			modifiedVelocityX = (float) (width * 0);
			setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {

					int pos = getSelectedItemPosition();		
					if (pos == 0) {		
						setSelection(1);
					} else {
						int count = getCount();
						if (pos == count - 1) {							
							setSelection(count - 2);
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
				} 

			});
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			float mod = velocityX < 0 ? -modifiedVelocityX : modifiedVelocityX;

			if (getSelectedItemPosition() == 1
					|| getSelectedItemPosition() == getAdapter().getCount() - 2) {
				mod = velocityX < 0 ? -1 : 1;
			}
			mod = 0;
			
			return super.onFling(e1, e2, mod, 0);
		}

		
	}