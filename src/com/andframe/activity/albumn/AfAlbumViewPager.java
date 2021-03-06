package com.andframe.activity.albumn;

import com.andframe.exception.AfException;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class AfAlbumViewPager extends ViewPager{

	private boolean mHorizontalScrollBarEnabled;
	
	public AfAlbumViewPager(Context context) {
		super(context);
	}
	
	public AfAlbumViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		if (!mHorizontalScrollBarEnabled) {
			return false;
		}
		return super.onTouchEvent(arg0);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent arg0) {
		if (!mHorizontalScrollBarEnabled) {
			return false;
		}
		try {
			return super.onInterceptTouchEvent(arg0);
		} catch (Throwable e) {
			AfException.handle(e, "AfAlbumViewPager.onInterceptTouchEvent 捕捉异常！");
			return false;
		}
	}

	@Override
	public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
		mHorizontalScrollBarEnabled = horizontalScrollBarEnabled;
		super.setHorizontalScrollBarEnabled(horizontalScrollBarEnabled);
	}
}
