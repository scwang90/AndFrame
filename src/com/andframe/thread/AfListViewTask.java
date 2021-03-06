package com.andframe.thread;

import android.os.Handler;
import android.os.Message;

import com.andframe.activity.framework.AfActivity;
import com.andframe.adapter.AfListAdapter;
import com.andframe.application.AfApplication;
import com.andframe.bean.Page;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 各种数据加载任务handler分流
 * @param <T>
 */
public abstract class AfListViewTask<T> extends AfListTask<T>
{
    //枚举任务类型
    public static final int TASK_NULL = 1000;    //下拉刷新
	/**
	 * 调用这个构造函数可以触发 TASK_LOAD 任务
	 * 加载上次缓存数据，如果过期将触发 TASK_REFRESH 加载新数据
	 * 缓存使用的 KEY_CACHELIST = this.getClass().getName()
	 * 		KEY_CACHELIST 为缓存的标识
	 * 缓存的过期 间隔为 AfListTask.CACHETIMEOUTSECOND
	 * 		这个时间间隔可以在App初始化时候更改
	 * @param clazz 数据Model的类对象（json要用到）
	 */
	public AfListViewTask(Class<T> clazz) {
		super(clazz);
	}

	/**
	 * 调用这个构造函数可以触发 TASK_LOAD 任务
	 * 加载上次缓存数据，如果过期将触发 TASK_REFRESH 加载新数据
	 * 缓存使用的 KEY_CACHELIST 可以自定义
	 * 缓存的过期 间隔为 AfListTask.CACHETIMEOUTSECOND
	 * 		这个时间间隔可以在App初始化时候更改
	 * @param clazz 数据Model的类对象（json要用到）
	 * @param KEY_CACHELIST 缓存的KEY标识
	 */
	public AfListViewTask(Class<T> clazz, String KEY_CACHELIST) {
		super(clazz, KEY_CACHELIST);
	}


	public AfListViewTask(int task) {
		super(task);
	}
	
	public AfListViewTask(int task,int first) {
		super(task,first);
	}
	
	public AfListViewTask(Handler handle, int task) {
		super(handle, task);
	}
	

	public AfListViewTask(List<T> list) {
		super(list);
	}

	public AfListViewTask(AfListAdapter<T> adapter) {
		super(adapter);
	}

	/**
	 * 数据分页加载（在异步线程中执行，不可以更改页面操作）
	 * @param page 分页对象
	 * @param task 任务id
	 * @return 加载到的数据列表
	 * @throws Exception
	 */
	protected abstract List<T> onListByPage(Page page, int task) throws Exception ;

	@Override
	protected List<T> onRefresh(Page page) throws Exception{
		return onListByPage(page,mTask);
	}

	@Override
	protected List<T> onMore(Page page) throws Exception {
		return onListByPage(page,mTask);
	}

	@Override
	protected boolean onHandle(Message msg) {
		boolean isfinish = isFinish();
		boolean dealerror = false;
		if (mTask == TASK_LOAD) {
			dealerror = this.onLoaded(isfinish,mltData,getCacheTime());
		}else if (mTask == TASK_MORE) {
			dealerror = this.onMored(isfinish,mltData,mltData.size() < PAGE_SIZE);
		}else if (mTask == TASK_REFRESH) {
			dealerror = this.onRefreshed(isfinish,mltData);
		}else {
			dealerror = this.onWorked(mTask,isfinish,mltData);
		}
		if (!dealerror && !isfinish) {
			this.onDealError(mTask,mErrors,mException);
		}
		return false;
	}

	/**
	 * 任务执行出错 之后统一（各种任务）处理错误提示
	 * 	如果在 onRefreshed、onMored、onTaskWorked、onLoaded 返回true 表示已经错误处理提示
	 * 矿井唉将不在调用 onDealError 
	 * @param task
	 * @param rrrors
	 * @param exception
	 */
	protected void onDealError(int task, String rrrors, Throwable exception) {
		AfActivity activity = AfApplication.getApp().getCurActivity();
		if (activity != null && !activity.isRecycled()) {
			if (exception instanceof IOException) {
				activity.makeToastLong("网络出现异常");
			}else {
				activity.makeToastLong(rrrors);
			}
		}
	}

	/**
	 * 刷新任务执行结束 后页面处理工作
	 * @param isfinish true 成功执行数据刷新 false 失败
	 * @param ltdata 刷新的数据
	 * @return 返回 true 表示 isfinish=false 时候 已经做了失败提示处理 将不在调用 onDealError
	 */
	protected abstract boolean onRefreshed(boolean isfinish, List<T> ltdata);

	/**
	 * 加载更多任务执行结束 后页面处理工作
	 * @param isfinish true 成功执行数据刷新 false 失败
	 * @param ltdata 刷新的数据
	 * @param ended true 表示是否已经加载完毕 用于控制更多按钮的显示
	 * @return 返回 true 表示 isfinish=false 时候 已经做了失败提示处理 将不在调用 onDealError
	 */
	protected abstract boolean onMored(boolean isfinish, List<T> ltdata,boolean ended);

	/**
	 * 其他任务执行结束 后页面处理工作
	 * @param task 任务标识
	 * @param isfinish true 成功执行数据刷新 false 失败
	 * @param ltdata 刷新的数据
	 * @return 返回 true 表示 isfinish=false 时候 已经做了失败提示处理 将不在调用 onDealError
	 */
	protected boolean onWorked(int task, boolean isfinish, List<T> ltdata) {
		return false;
	}

	/**
	 * 加载缓存结束 后页面处理工作
	 * @param isfinish true 成功执行数据刷新 false 失败
	 * @param ltdata 刷新的数据
	 * @return 返回 true 表示 isfinish=false 时候 已经做了失败提示处理 将不在调用 onDealError
	 */
	protected boolean onLoaded(boolean isfinish, List<T> ltdata,Date cachetime) {
		return onRefreshed(isfinish, ltdata);
	}
	
	@SuppressWarnings("rawtypes")
	public static AfListViewTask getTask(Message msg) {
		if (msg.obj instanceof AfListViewTask) {
			return (AfListViewTask) msg.obj;
		}
		return null;
	}
}
