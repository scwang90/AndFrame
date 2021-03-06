package com.andframe.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.andframe.view.pulltorefresh.AfPullToRefreshBase;

public abstract class AfRefreshListView<T extends ListView> extends AfPullToRefreshBase<T> {
	protected ListAdapter mAdapter = null;
	protected boolean mIsNeedFooter = false;
	protected boolean mIsOpenRefresh = true;
	protected T mListView;

	public AfRefreshListView(Context context) {
		super(context);
	}

	public AfRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AfRefreshListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public final Object getData(int position) {
		if (mAdapter != null) {
			return mAdapter.getItem(getDataIndex(position));
		}
		return mTargetView.getItemAtPosition(getDataIndex(position));
	}
	

	public final <TT> TT getData(int position,Class<TT> clazz) {
		Object item = new Object();
		if (mAdapter != null) {
			item = mAdapter.getItem(getDataIndex(position));
		}else{
			item = mTargetView.getItemAtPosition(getDataIndex(position));
		}
		if(clazz.isInstance(item)){
			return clazz.cast(item);
		}
		return null;
	}

	public void setAdapter(ListAdapter adapter) {
		mTargetView.setAdapter(mAdapter = adapter);
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mTargetView.setOnItemClickListener(listener);
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		mTargetView.setOnItemLongClickListener(listener);
	}

	protected abstract T onCreateListView(Context context, AttributeSet attrs);

	@Override
	@SuppressLint("NewApi")
	protected T onCreateRefreshableView(Context context, AttributeSet attrs) {
		mListView = onCreateListView(context,attrs);//new ListView(context)
//		View view = new View(context);
//		mListView.addHeaderView(view, null, true);
//		mListView.addFooterView(view, null, true);
//		mListView.setHeaderDividersEnabled(true);
//		mListView.setFooterDividersEnabled(false);

		if(VERSION.SDK_INT >= 16){
			mListView.setDivider(getDividerDrawable());
		}else if(VERSION.SDK_INT >= 14){
			mListView.setDividerHeight(getDividerPadding());
			mListView.setDivider(new BitmapDrawable(getResources(),
					getDrawingCache()));
		}else{
			mListView.setDividerHeight(1);
			mListView.setDivider(new BitmapDrawable(getResources(),
					getDrawingCache()));
		}
		
		// 解决listview在拖动的时候背景图片消失变成黑色背景
		mListView.setCacheColorHint(0);
		mListView.setScrollingCacheEnabled(false);
		// 解决listview的上边和下边有黑色的阴影
		mListView.setFadingEdgeLength(0);
		// TypedArray array =
		// context.obtainStyledAttributes(attrs,android.R.style.DeviceDefault_Light_ButtonBar);
		// listview.setCacheColorHint(array.getInteger(R.styleable.PullToRefresh_cacheColorHint,0));
		// listview.setDividerHeight((int)array.getDimension(R.styleable.PullToRefresh_dividerHeight,
		// 1));
		// listview.setDivider(array.getDrawable(R.styleable.PullToRefresh_divider));
		// array.recycle();

		// ScrollBarUtil.bindScrollBar(listview, R.drawable.shape_scrollbar);
		return mListView;
	}

	public void setRefreshable(boolean able) {
		mIsOpenRefresh = able;
	}
	
//	@Override
//	protected final boolean isReadyForPullDown() {
//		return mIsOpenRefresh 
//				&&mTargetView.getFirstVisiblePosition() == 0
//				&& mTargetView.getScrollY() <= 0;
//	}
//	
//	@Override
//	protected final boolean isReadyForPullUp() {
//		return mIsNeedFooter &&
//				mTargetView.getLastVisiblePosition() 
//				== mTargetView.getCount() - 1;
//	}
	
//	@Override
//	protected final boolean isReadyForPullDown() {
//		// targetview.getOverScrollMode();
//		return mIsOpenRefresh
//				&& 5 >= Math.abs(getFirstPositionDistanceGuess(mTargetView)
//						- mTargetView.getTop());
//	}
//
//	@Override
//	protected final boolean isReadyForPullUp() {
//		return mIsNeedFooter
//				&& 5 >= Math.abs(getLastPositionDistanceGuess(mTargetView)
//						- mTargetView.getBottom());
//	}
//
//	int getFirstPositionDistanceGuess(AbsListView view) {
//		Field field;
//		// 获取ContextWrapper对象中的mBase变量。该变量保存了ContextImpl对象
//		try {
//			field = AbsListView.class
//					.getDeclaredField("mFirstPositionDistanceGuess");
//			field.setAccessible(true);
//			return (Integer) field.get(view);
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
//		return -1;
//	}
//
//	int getLastPositionDistanceGuess(AbsListView view) {
//		Field field;
//		// 获取ContextWrapper对象中的mBase变量。该变量保存了ContextImpl对象
//		try {
//			field = AbsListView.class
//					.getDeclaredField("mLastPositionDistanceGuess");
//			field.setAccessible(true);
//			return (Integer) field.get(view);
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
//		return -1;
//
//	}

	@Override
	protected boolean isReadyForPullDown() {
		return mIsOpenRefresh&&isFirstItemVisible();
	}

	@Override
	protected boolean isReadyForPullUp() {
		return mIsNeedFooter&&isLastItemVisible();
	}
	
	private boolean isFirstItemVisible() {
		final Adapter adapter = mTargetView.getAdapter();

		if (null == adapter || adapter.isEmpty()) {
			return true;
		} else {
			/**
			 * This check should really just be:
			 * mRefreshableView.getFirstVisiblePosition() == 0, but PtRListView
			 * internally use a HeaderView which messes the positions up. For
			 * now we'll just add one to account for it and rely on the inner
			 * condition which checks getTop().
			 */
			if (mTargetView.getFirstVisiblePosition() <= 1) {
				final View firstVisibleChild = mTargetView.getChildAt(0);
				if (firstVisibleChild != null) {
					return firstVisibleChild.getTop() >= mTargetView.getTop();
				}
			}
		}
		return false;
	}
	
	private boolean isLastItemVisible() {
		final Adapter adapter = mTargetView.getAdapter();
		if (null == adapter || adapter.isEmpty()) {
			return true;
		} else {
			final int lastItemPosition = mTargetView.getCount() - 1;
			final int lastVisiblePosition = mTargetView.getLastVisiblePosition();
			/**
			 * This check should really just be: lastVisiblePosition ==
			 * lastItemPosition, but PtRListView internally uses a FooterView
			 * which messes the positions up. For me we'll just subtract one to
			 * account for it and rely on the inner condition which checks
			 * getBottom().
			 */
			if (lastVisiblePosition >= lastItemPosition - 1) {
				final int childIndex = lastVisiblePosition - mTargetView.getFirstVisiblePosition();
				final View lastVisibleChild = mTargetView.getChildAt(childIndex);
				if (lastVisibleChild != null) {
					return lastVisibleChild.getBottom() <= mTargetView.getBottom();
				}
			}
		}
		return false;
	}
	
	public final void addMoreView() {
		mIsNeedFooter = true;
	}

	public final void removeMoreView() {
		onRefreshComplete();
		mIsNeedFooter = false;
	}

	/**
	 * Returns the number of header views in the list. Header views are special
	 * views at the top of the list that should not be recycled during a layout.
	 * @return
	 */
	public final int getHeaderViewsCount() {
		return mTargetView.getHeaderViewsCount() - 1;
	}

	/**
	 * 获取 OnItemClick 中的index 对应ListView 中的index 包含 HeaderView
	 * @param index
	 * @return
	 */
	public final int getIndex(int index) {
		return index - 0;//1;
	}

	/**
	 * 获取 OnItemClick 中的index 对应Adapter 中的index
	 * 主要用于当ListView中有Header的时候 可以排除Header产生的index偏移
	 * @param index
	 * @return index-headercount (小于0时代表点击的是header)
	 */
	public final int getDataIndex(int index) {
//		if (index < mTargetView.getHeaderViewsCount()) {
//			return index;
//		}
		return index - mTargetView.getHeaderViewsCount();
	}

	public void addHeaderView(View v) {
		mTargetView.addHeaderView(v);
	}
	
	public void addHeaderView(View v, Object data, boolean isSelectable) {
		mTargetView.addHeaderView(v, data, isSelectable);
	}

	public void addFooterView(View v) {
		mTargetView.addFooterView(v);
	}

	public void addFooterView(View v, Object data, boolean isSelectable) {
		mTargetView.addFooterView(v, data, isSelectable);
	}

	public boolean removeHeaderView(View v) {
		return mTargetView.removeHeaderView(v);
	}

	public boolean removeFooterView(View v) {
		return mTargetView.removeFooterView(v);
	}

	public int getCount() {
		return mTargetView.getCount();
	}

	public void smoothScrollToPosition(int position) {
		mTargetView.smoothScrollToPosition(position);
	}

}
