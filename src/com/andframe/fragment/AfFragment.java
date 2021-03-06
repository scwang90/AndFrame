package com.andframe.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.andframe.activity.framework.AfActivity;
import com.andframe.activity.framework.AfPageable;
import com.andframe.activity.framework.AfView;
import com.andframe.annotation.inject.interpreter.Injecter;
import com.andframe.application.AfApplication;
import com.andframe.application.AfDaemonThread;
import com.andframe.application.AfExceptionHandler;
import com.andframe.exception.AfException;
import com.andframe.exception.AfToastException;
import com.andframe.feature.AfBundle;
import com.andframe.feature.AfDailog;
import com.andframe.feature.AfIntent;
import com.andframe.feature.AfSoftInputer;
import com.andframe.feature.AfViewBinder;
import com.andframe.thread.AfTask;
import com.andframe.thread.AfThreadWorker;
import com.andframe.util.java.AfStackTrace;

/**
 * 框架 AfFragment
 *
 * @author 树朾
 *         <p/>
 *         以下是 AfFragment 像子类提供的 功能方法
 *         <p/>
 *         protected void buildThreadWorker()
 *         为本页面开启一个独立后台线程 供 postTask 的 任务(AfTask)运行 注意：开启线程之后 postTask
 *         任何任务都会在该线程中运行。 如果 postTask 前一个任务未完成，后一个任务将等待
 *         <p/>
 *         protected AfTask postTask(AfTask task)
 *         抛送任务到Worker执行
 *         <p/>
 *         AfPageable 接口中的方法
 *         public Activity getActivity();
 *         public void makeToastLong(String tip);
 *         public void makeToastShort(String tip);
 *         public void makeToastLong(int resid);
 *         public void makeToastShort(int resid);
 *         public boolean getSoftInputStatus();
 *         public boolean getSoftInputStatus(View view);
 *         public void setSoftInputEnable(EditText editview, boolean enable);
 *         public void showProgressDialog(String message);
 *         public void showProgressDialog(String message, boolean cancel);
 *         public void showProgressDialog(String message, boolean cancel,int textsize);
 *         public void showProgressDialog(String message, listener);
 *         public void showProgressDialog(String message, listener, int textsize);
 *         public void hideProgressDialog();
 *         public void startActivity(Class<? extends AfActivity> tclass);
 *         public void startActivityForResult(Class<AfActivity> tclass,int request);
 *         <p/>
 *         public void doShowDialog(String title, String message);
 *         public void doShowDialog(String title, String message,OnClickListener);
 *         public void doShowDialog(String title, String message,String ,OnClickListener);
 *         public void doShowDialog(String, String,String,OnClickListener,String,OnClickListener);
 *         public void doShowDialog(int,String,String,String,OnClickListener,String,OnClickListener);
 *         public void doShowDialog(int,String,String,String,Listener,String,Listener,String,Listener);
 *         <p/>
 *         public void doShowViewDialog(title, View view,String positive, OnClickListener );
 *         public void doShowViewDialog(title, View view,String positive, OnClickListener , String negative,OnClickListener );
 *         public void doShowViewDialog(title,view,String,Listener,String,Listener,String,Listener);
 *         public void doShowViewDialog(int iconres, title,  view,String, OnClickListener,String,OnClickListener );
 *         public void doShowViewDialog(int iconres,title,view,String,Listener,String,Listener,String,Listener);
 *         <p/>
 *         public void doSelectItem(String title,String[] items,OnClickListener);
 *         public void doSelectItem(String title,String[] items,OnClickListener,cancel);
 *         public void doSelectItem(String title,String[] items,OnClickListener,oncancel);
 *         <p/>
 *         public void doInputText(String title,InputTextListener listener);
 *         public void doInputText(String title,int type,InputTextListener listener);
 *         public void doInputText(String title,String defaul,int type,InputTextListener listener);
 *         <p/>
 *         AfPageListener 接口中的方法
 *         public void onSoftInputShown();
 *         public void onSoftInputHiden();
 *         public void onQueryChanged();
 *         }
 */
public abstract class AfFragment extends Fragment implements AfPageable, AdapterView.OnItemClickListener {

    public static final String EXTRA_DATA = "EXTRA_DATA";
    public static final String EXTRA_INDEX = "EXTRA_INDEX";
    public static final String EXTRA_RESULT = "EXTRA_RESULT";

    public static final int LP_MP = LayoutParams.MATCH_PARENT;
    public static final int LP_WC = LayoutParams.WRAP_CONTENT;
    // 根视图
    protected View mRootView = null;

    protected AfThreadWorker mWorker = null;
    protected ProgressDialog mProgress = null;
    protected boolean mIsRecycled = false;

    /**
     * 获取LOG日志 TAG 是 AfFragment 的方法
     * 用户也可以重写自定义TAG,这个值AfActivity在日志记录时候会使用
     * 子类实现也可以使用
     *
     * @return
     * @author 树朾
     */
    protected String TAG() {
        return "AfFragment(" + getClass().getName() + ")";
    }

    protected String TAG(String tag) {
        return "AfFragment(" + getClass().getName() + ")." + tag;
    }

    /**
     * 判断是否被回收
     *
     * @return true 已经被回收
     */
    @Override
    public boolean isRecycled() {
        return mIsRecycled;
    }

    /**
     * 为本页面开启一个独立后台线程 供 postTask 的 任务(AfTask)运行 注意：开启线程之后 postTask
     * 任何任务都会在该线程中运行。 如果 postTask 前一个任务未完成，后一个任务将等待
     */
    protected void buildThreadWorker() {
        if (mWorker == null) {
            mWorker = new AfThreadWorker(this.getClass().getSimpleName());
        }
    }

    /**
     * 抛送任务到Worker执行
     *
     * @param task
     */
    public AfTask postTask(AfTask task) {
        if (mWorker != null) {
            return mWorker.postTask(task);
        }
        return AfDaemonThread.postTask(task);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void startActivity(Class<? extends AfActivity> tclass) {
        startActivity(new Intent(getActivity(), tclass));
    }

    public void startActivityForResult(Class<? extends AfActivity> tclass, int request) {
        startActivityForResult(new Intent(getActivity(), tclass), request);
    }

    /**
     * (non-Javadoc)
     *
     * @see android.support.v4.app.FragmentActivity#onActivityResult(int, int, android.content.Intent)
     * final 重写 onActivityResult 使用 try-catch 调用
     * onActivityResult(AfIntent intent, int requestcode,int resultcode)
     * @see AfFragment#onActivityResult(AfIntent intent, int requestcode, int resultcode)
     * {@link AfFragment#onActivityResult(AfIntent intent, int requestcode, int resultcode)}
     */
    @Override
    public final void onActivityResult(int requestcode, int resultcode, Intent data) {
        try {
            onActivityResult(new AfIntent(data), requestcode, resultcode);
        } catch (Throwable e) {
            if (!(e instanceof AfToastException)) {
                AfExceptionHandler.handler(e, TAG("onActivityResult"));
            }
            makeToastLong("反馈信息读取错误！", e);
        }
    }

    /**
     * 安全 onActivityResult(AfIntent intent, int requestcode,int resultcode)
     * 在onActivityResult(int requestcode, int resultCode, Intent data) 中调用
     * 并使用 try-catch 提高安全性，子类请重写这个方法
     *
     * @param intent
     * @param requestcode
     * @param resultcode
     * @see AfFragment#onActivityResult(int, int, android.content.Intent)
     * {@link AfFragment#onActivityResult(int, int, android.content.Intent)}
     */
    protected void onActivityResult(AfIntent intent, int requestcode, int resultcode) {
        super.onActivityResult(requestcode, resultcode, intent);
    }

    /**
     * 自定义 View onCreate(Bundle)
     */
    protected abstract void onCreated(AfView rootView, AfBundle bundle) throws Exception;

    /**
     * 自定义 View onCreateView(LayoutInflater, ViewGroup)
     */
    protected abstract View onCreateView(LayoutInflater inflater,
                                         ViewGroup container);

    @Override
    public void onResume() {
        super.onResume();
        this.onQueryChanged();
    }

    @Override
    public final void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    /**
     * 锁住 上级的 View onCreateView(LayoutInflater, ViewGroup, Bundle)
     */
    @Override
    public final View onCreateView(LayoutInflater inflater,
                                   ViewGroup container, Bundle bundle) {
        mRootView = onCreateView(inflater, container);
        if (mRootView == null) {
            mRootView = super.onCreateView(inflater, container, bundle);
        }
        try {
//			mRootView.getViewTreeObserver().addOnGlobalLayoutListener(this);
            AfViewBinder binder = new AfViewBinder(this);
            binder.doBind(mRootView);
            Injecter injecter = new Injecter(this);
            injecter.doInject(getActivity());
            AfSoftInputer inputer = new AfSoftInputer(getActivity());
            inputer.setBindListener(mRootView, this);
            onCreated(new AfView(mRootView), new AfBundle(getArguments()));
        } catch (Throwable e) {
            if (!(e instanceof AfToastException)) {
                AfExceptionHandler.handler(e, TAG("onCreateView"));
            }
            makeToastLong("页面初始化异常！", e);
        }
        return mRootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//		mRootView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsRecycled = true;
        if (mWorker != null) {
            mWorker.quit();
        }
    }

    /**
     * 第一次切换到本页面
     */
    protected void onFirstSwitchOver() {
    }

    /**
     * 每次切换到本页面
     *
     * @param count 切换序号
     */
    protected void onSwitchOver(int count) {
    }

    /**
     * 离开本页面
     */
    protected void onSwitchLeave() {
    }

    /**
     * 查询系统数据变动
     */
    public void onQueryChanged() {
        new Injecter(this).doInjectQueryChanged();
    }


    @Override
    public boolean getSoftInputStatus() {
        return new AfSoftInputer(getActivity()).getSoftInputStatus();
    }

    @Override
    public boolean getSoftInputStatus(View view) {
        return new AfSoftInputer(getActivity()).getSoftInputStatus(view);
    }

    @Override
    public void setSoftInputEnable(EditText editview, boolean enable) {
        new AfSoftInputer(getActivity()).setSoftInputEnable(editview, enable);
    }

    @Override
    public Context getContext() {
        Activity activity = getActivity();
        if (activity == null) {
            return AfApplication.getAppContext();
        }
        return activity;
    }

    @Override
    public void makeToastLong(String tip) {
        Toast.makeText(getContext(), tip, Toast.LENGTH_LONG).show();
    }

    @Override
    public void makeToastShort(String tip) {
        Toast.makeText(getContext(), tip, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void makeToastLong(int resid) {
        Toast.makeText(getContext(), resid, Toast.LENGTH_LONG).show();
    }

    @Override
    public void makeToastShort(int resid) {
        Toast.makeText(getContext(), resid, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void makeToastLong(String tip, Throwable e) {
        tip = AfException.handle(e, tip);
        Toast.makeText(getContext(), tip, Toast.LENGTH_LONG).show();
    }


    @Override
    public final View findViewById(int id) {
        if (mRootView != null) {
            return mRootView.findViewById(id);
        }
        return null;
    }

    @Override
    public <T extends View> T findViewById(int id, Class<T> clazz) {
        View view = findViewById(id);
        if (clazz.isInstance(view)) {
            return clazz.cast(view);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends View> T findViewByID(int id) {
        try {
            return (T) findViewById(id);
        } catch (Exception e) {
            AfExceptionHandler.handler(e, TAG("findViewByID"));
        }
        return null;
    }

    /**
     * 显示 进度对话框
     *
     * @param message 消息
     */
    public final void showProgressDialog(String message) {
        showProgressDialog(message, false, 25);
    }

    /**
     * 显示 进度对话框
     *
     * @param message 消息
     * @param cancel  是否可取消
     */
    public final void showProgressDialog(String message, boolean cancel) {
        showProgressDialog(message, cancel, 25);
    }

    /**
     * 显示 进度对话框
     *
     * @param message  消息
     * @param cancel   是否可取消
     * @param textsize 字体大小
     */
    public final void showProgressDialog(String message, boolean cancel,
                                         int textsize) {
        try {
            mProgress = new ProgressDialog(getActivity());
            mProgress.setMessage(message);
            mProgress.setCancelable(cancel);
            mProgress.setOnCancelListener(null);
            mProgress.show();

            setDialogFontSize(mProgress, textsize);
        } catch (Exception e) {
            //进过日志验证，这个异常会发送，但是概率非常小，注释掉异常通知
//			AfExceptionHandler.handler(e, "AfActivity.showProgressDialog");
        }
    }

    /**
     * 显示 进度对话框
     *
     * @param message  消息
     * @param listener 字体大小
     */
    public final void showProgressDialog(String message,
                                         OnCancelListener listener) {
        try {
            mProgress = new ProgressDialog(getActivity());
            mProgress.setMessage(message);
            mProgress.setCancelable(true);
            mProgress.setOnCancelListener(listener);
            mProgress.show();

            setDialogFontSize(mProgress, 25);
        } catch (Exception e) {
            //进过日志验证，这个异常会发送，但是概率非常小，注释掉异常通知
//			AfExceptionHandler.handler(e, "AfActivity.showProgressDialog");
        }
    }

    /**
     * 显示 进度对话框
     *
     * @param message  消息
     * @param listener 是否可取消
     * @param textsize 字体大小
     */
    public final void showProgressDialog(String message,
                                         OnCancelListener listener, int textsize) {
        try {
            mProgress = new ProgressDialog(getActivity());
            mProgress.setMessage(message);
            mProgress.setCancelable(true);
            mProgress.setOnCancelListener(listener);
            mProgress.show();

            setDialogFontSize(mProgress, textsize);
        } catch (Exception e) {
            //进过日志验证，这个异常会发送，但是概率非常小，注释掉异常通知
//			AfExceptionHandler.handler(e, "AfActivity.showProgressDialog");
        }
    }

    @Override
    public void onSoftInputHiden() {
    }

    @Override
    public void onSoftInputShown() {
    }

    /**
     * 隐藏 进度对话框
     */
    public final void hideProgressDialog() {
        try {
            if (mProgress != null && !isRecycled()) {
                mProgress.dismiss();
                mProgress = null;
            }
        } catch (Exception e) {
            AfExceptionHandler.handler(e, "AfActivity.hideProgressDialog");
        }
    }

    /**
     * 显示对话框 并添加默认按钮 "我知道了"
     *
     * @param title   显示标题
     * @param message 显示内容
     */
    public void doShowDialog(String title, String message) {
        doShowDialog(0, title, message, "我知道了", null, "", null);
    }

    /**
     * 显示对话框 并添加默认按钮 "我知道了"
     *
     * @param title     显示标题
     * @param message   显示内容
     * @param lpositive 点击  "我知道了" 响应事件
     */
    public void doShowDialog(String title, String message, OnClickListener lpositive) {
        doShowDialog(0, title, message, "我知道了", lpositive, "", null);
    }

    /**
     * 显示对话框
     *
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     */
    public void doShowDialog(String title, String message, String positive, OnClickListener lpositive) {
        doShowDialog(0, title, message, positive, lpositive, "", null);
    }

    /**
     * 显示对话框
     *
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    public void doShowDialog(String title, String message,
                             String positive, OnClickListener lpositive, String negative,
                             OnClickListener lnegative) {
        doShowDialog(0, title, message, positive, lpositive, negative, lnegative);
    }

    /**
     * 显示对话框
     *
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    @Override
    public void doShowDialog(String title, String message,
                             String positive, OnClickListener lpositive,
                             String neutral, OnClickListener lneutral,
                             String negative, OnClickListener lnegative) {
        doShowDialog(0, title, message, positive, lpositive, neutral, lneutral, negative, lnegative);
    }

    /**
     * 显示对话框
     *
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    @Override
    public void doShowDialog(int iconres, String title, String message,
                             String positive, OnClickListener lpositive, String negative,
                             OnClickListener lnegative) {
        doShowDialog(iconres, title, message, positive, lpositive, "", null, negative, lnegative);
    }

    /**
     * 显示对话框
     *
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    public void doShowDialog(int iconres, String title, String message,
                             String positive, OnClickListener lpositive,
                             String neutral, OnClickListener lneutral,
                             String negative, OnClickListener lnegative) {
        doShowDialog(-1, iconres, title, message, positive, lpositive, neutral, lneutral, negative, lnegative);
    }

    /**
     * 显示视图对话框
     *
     * @param theme     主题
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    @SuppressLint("NewApi")
    @Override
    public void doShowDialog(int theme, int iconres,
                             String title, String message,
                             String positive, OnClickListener lpositive,
                             String neutral, OnClickListener lneutral,
                             String negative, OnClickListener lnegative) {
        new AfDailog(getActivity()).doShowDialog(theme, iconres, title, message, positive, lpositive, neutral, lneutral, negative, lnegative);
    }

    /**
     * 显示视图对话框
     *
     * @param title     显示标题
     * @param view      显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     */
    @Override
    public void doShowViewDialog(String title, View view, String positive,
                                 OnClickListener lpositive) {
        doShowViewDialog(title, view, positive, lpositive, "", null);
    }

    /**
     * 显示视图对话框
     *
     * @param title     显示标题
     * @param view      显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    @Override
    public void doShowViewDialog(String title, View view, String positive,
                                 OnClickListener lpositive, String negative,
                                 OnClickListener lnegative) {
        doShowViewDialog(0, title, view, positive, lpositive, negative, lnegative);
    }

    /**
     * 显示视图对话框
     *
     * @param title     显示标题
     * @param view      显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    @Override
    public void doShowViewDialog(String title, View view,
                                 String positive, OnClickListener lpositive,
                                 String neutral, OnClickListener lneutral,
                                 String negative, OnClickListener lnegative) {
        doShowViewDialog(0, title, view, positive, lpositive, neutral, lneutral, negative, lnegative);
    }

    /**
     * 显示视图对话框
     *
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param view      显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    @Override
    public void doShowViewDialog(int iconres, String title, View view,
                                 String positive, OnClickListener lpositive,
                                 String negative, OnClickListener lnegative) {
        doShowViewDialog(0, title, view, positive, lpositive, "", null, negative, lnegative);
    }

    /**
     * 显示视图对话框
     *
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param view      显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    @Override
    public void doShowViewDialog(int iconres, String title, View view,
                                 String positive, OnClickListener lpositive,
                                 String neutral, OnClickListener lneutral,
                                 String negative, OnClickListener lnegative) {
        doShowViewDialog(-1, iconres, title, view, positive, lpositive, neutral, lneutral, negative, lnegative);
    }

    /**
     * 显示视图对话框
     *
     * @param theme     主题
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param view      显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    @SuppressLint("NewApi")
    @Override
    public void doShowViewDialog(int theme,
                                 int iconres, String title, View view,
                                 String positive, OnClickListener lpositive,
                                 String neutral, OnClickListener lneutral,
                                 String negative, OnClickListener lnegative) {
        new AfDailog(getActivity()).doShowViewDialog(theme, iconres, title, view, positive, lpositive, neutral, lneutral, negative, lnegative);
    }

    /**
     * 显示一个单选对话框 （设置可取消）
     *
     * @param title    对话框标题
     * @param items    选择菜单项
     * @param listener 选择监听器
     * @param cancel   取消选择监听器
     */
    public void doSelectItem(String title, String[] items, OnClickListener listener,
                             boolean cancel) {
        new AfDailog(getActivity()).doSelectItem(title, items, listener, cancel);
    }

    /**
     * 显示一个单选对话框
     *
     * @param title    对话框标题
     * @param items    选择菜单项
     * @param listener 选择监听器
     * @param oncancel 取消选择监听器
     */
    public void doSelectItem(String title, String[] items, OnClickListener listener,
                             final OnClickListener oncancel) {
        new AfDailog(getActivity()).doSelectItem(title, items, listener, oncancel);
    }

    /**
     * 显示一个单选对话框 （默认可取消）
     *
     * @param title    对话框标题
     * @param items    选择菜单项
     * @param listener 选择监听器
     */
    public void doSelectItem(String title, String[] items, OnClickListener listener) {
        doSelectItem(title, items, listener, null);
    }

    /**
     * 弹出一个文本输入框
     *
     * @param title    标题
     * @param listener 监听器
     */
    public void doInputText(String title, InputTextListener listener) {
        doInputText(title, "", InputType.TYPE_CLASS_TEXT, listener);
    }

    /**
     * 弹出一个文本输入框
     *
     * @param title    标题
     * @param type     android.text.InputType
     * @param listener 监听器
     */
    public void doInputText(String title, int type, InputTextListener listener) {
        doInputText(title, "", type, listener);
    }

    /**
     * 弹出一个文本输入框
     *
     * @param title    标题
     * @param defaul   默认值
     * @param type     android.text.InputType
     * @param listener 监听器
     */
    public void doInputText(String title, String defaul, int type, InputTextListener listener) {
        new AfDailog(getActivity()).doInputText(title, defaul, type, listener);
    }

    /**
     * 显示对话框 并添加默认按钮 "我知道了"
     *
     * @param key     不再显示KEY
     * @param title   显示标题
     * @param message 显示内容
     */
    public void doShowDialog(String key, String title, String message) {
        doShowDialog(key, 0, 0, title, message, "我知道了", null, "", null);
    }

    /**
     * 显示对话框
     *
     * @param key       不再显示KEY
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     */
    public void doShowDialog(String key, String title, String message, String positive, OnClickListener lpositive) {
        doShowDialog(key, 0, 0, title, message, positive, lpositive, "", null);
    }

    /**
     * 显示对话框
     *
     * @param key       不再显示KEY
     * @param defclick  不再显示之后默认执行index
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    public void doShowDialog(String key, int defclick,
                             String title, String message,
                             String positive, OnClickListener lpositive,
                             String negative, OnClickListener lnegative) {
        doShowDialog(key, defclick, 0, title, message, positive, lpositive, negative, lnegative);
    }

    /**
     * 显示对话框
     *
     * @param key       不再显示KEY
     * @param defclick  不再显示之后默认执行index
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    public void doShowDialog(String key, int defclick,
                             String title, String message,
                             String positive, OnClickListener lpositive,
                             String neutral, OnClickListener lneutral,
                             String negative, OnClickListener lnegative) {
        doShowDialog(key, defclick, 0, title, message, positive, lpositive, neutral, lneutral, negative, lnegative);
    }

    /**
     * 显示对话框
     *
     * @param key       不再显示KEY
     * @param defclick  不再显示之后默认执行index
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    public void doShowDialog(String key, int defclick,
                             int iconres, String title, String message,
                             String positive, OnClickListener lpositive,
                             String negative, OnClickListener lnegative) {
        if (defclick == 1) {
            defclick = 2;
        }
        doShowDialog(key, defclick, iconres, title, message, positive, lpositive, "", null, negative, lnegative);
    }

    /**
     * 显示对话框
     *
     * @param key       不再显示KEY
     * @param defclick  不再显示之后默认执行index
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    public void doShowDialog(String key, int defclick,
                             int iconres, String title, String message,
                             String positive, OnClickListener lpositive,
                             String neutral, OnClickListener lneutral,
                             String negative, OnClickListener lnegative) {
        doShowDialog(key, defclick, -1, iconres, title, message, positive, lpositive, neutral, lneutral, negative, lnegative);
    }


    /**
     * 显示视图对话框
     *
     * @param key       不再显示KEY
     * @param defclick  不再显示之后默认执行index
     * @param theme     主题
     * @param iconres   对话框图标
     * @param title     显示标题
     * @param message   显示内容
     * @param positive  确认 按钮显示信息
     * @param lpositive 点击  确认 按钮 响应事件
     * @param neutral   详细 按钮显示信息
     * @param lneutral  点击  详细 按钮 响应事件
     * @param negative  按钮显示信息
     * @param lnegative 点击  拒绝 按钮 响应事件
     */
    public void doShowDialog(String key, int defclick,
                             int theme, int iconres,
                             String title, String message,
                             String positive, OnClickListener lpositive,
                             String neutral, OnClickListener lneutral,
                             String negative, OnClickListener lnegative) {
        new AfDailog(getActivity()).doShowDialog(key, defclick, theme, iconres, title, message, positive, lpositive, neutral, lneutral, negative, lnegative);
    }


    protected void setProgressDialogText(ProgressDialog dialog, String text) {
        Window window = dialog.getWindow();
        View view = window.getDecorView();
        setViewFontText(view, text);
    }

    private void setViewFontText(View view, String text) {
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                setViewFontText(parent.getChildAt(i), text);
            }
        } else if (view instanceof TextView) {
            TextView textview = (TextView) view;
            textview.setText(text);
        }
    }

    private void setDialogFontSize(Dialog dialog, int size) {
        Window window = dialog.getWindow();
        View view = window.getDecorView();
        setViewFontSize(view, size);
    }

    private void setViewFontSize(View view, int size) {
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                setViewFontSize(parent.getChildAt(i), size);
            }
        } else if (view instanceof TextView) {
            TextView textview = (TextView) view;
            textview.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        }
    }

    /**
     * 按下返回按键
     *
     * @return 返回 true 表示已经处理 否则 Activity 会处理
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * 按键按下事件
     *
     * @return 返回 true 表示已经处理 否则 Activity 会处理
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * 按键弹起事件
     *
     * @return 返回 true 表示已经处理 否则 Activity 会处理
     */
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * 按键重复事件
     *
     * @return 返回 true 表示已经处理 否则 Activity 会处理
     */
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return false;
    }

    /**
     * 按键onKeyShortcut事件
     *
     * @return 返回 true 表示已经处理 否则 Activity 会处理
     */
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * 按键onKeyLongPress事件
     *
     * @return 返回 true 表示已经处理 否则 Activity 会处理
     */
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * final 包装 onItemClick 事件处理 防止抛出异常崩溃
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     * @author 树朾
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        try {
            if (AfStackTrace.isLoopCall()) {
                //System.out.println("递归检测");
                return;
            }
            this.onItemClick(parent, view, id, position);
        } catch (Exception e) {
            AfExceptionHandler.handler(e, TAG() + ".onItemClick");
        }
    }

    /**
     * 安全onItemClick框架会捕捉异常防止崩溃
     *
     * @param parent
     * @param item
     * @param id
     * @param index
     * @author 树朾
     */
    protected void onItemClick(AdapterView<?> parent, View item, long id, int index) {

    }
}
