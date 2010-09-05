package com.neusou.artsy;

import android.view.View;
import android.view.animation.Animation.AnimationListener;

public abstract class ContextualAnimationListener implements AnimationListener{
	
	protected View mView;
	
	public ContextualAnimationListener(View view) {
		mView = view;
	}

}