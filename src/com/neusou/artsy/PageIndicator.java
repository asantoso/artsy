package com.neusou.artsy;

import com.neusou.artsy.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

class PageIndicator extends RelativeLayout {

	private int totalSteps;
	private int step;
	private int futureStep;

	View bg;
	View indicator;

	View mRoot;

	RelativeLayout.LayoutParams main_lp;
	RelativeLayout.LayoutParams bg_lp;
	RelativeLayout.LayoutParams indicator_lp;

	int bg_w = LayoutParams.FILL_PARENT;
	int bg_h = 11;
	int indicator_size = 16;

	Context mContext;

	private void createLayoutsParams() {
		main_lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, App
				.toDip(bg_h));
		bg_lp = new RelativeLayout.LayoutParams(bg_w, App.toDip(bg_h));
		indicator_lp = new RelativeLayout.LayoutParams(App
				.toDip(indicator_size), App.toDip(indicator_size));
	}

	private void updateLayoutParams() {
		// setLayoutParams(main_lp);
		bg.setLayoutParams(bg_lp);
		indicator.setLayoutParams(indicator_lp);
	}

	public PageIndicator(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		mContext = ctx;
		makeControllerView();
		addView(mRoot);
	}

	@Override
	public void onFinishInflate() {
		if (mRoot != null) {
			initControllerView(mRoot);
		}
	}

	private View makeControllerView() {
		LayoutInflater inflate = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mRoot = inflate.inflate(R.layout.pageindicator, null);
		initControllerView(mRoot);
		return mRoot;
	}

	private void initControllerView(View v) {
		indicator = v.findViewById(R.id.indicator);
		bg = v.findViewById(R.id.bg);
	}

	public void setTotalSteps(int total) {
		this.totalSteps = total;
	}

	public void setTotalPages(int total){
		this.totalSteps = total;
	}
	
	public void nextStep(){
		this.step++;
	}
	
	public void previousStep(){
		this.step--;
	}
	
	public interface PageIndicatorController{
		public void nextPage();			
		public void prevPage();
		public void firstPage();
		public void lastPage();	
	}

}
