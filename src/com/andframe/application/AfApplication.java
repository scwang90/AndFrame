package com.andframe.application;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Environment;
import android.os.Looper;

import com.andframe.BuildConfig;
import com.andframe.activity.framework.AfActivity;
import com.andframe.broadcast.ConnectionChangeReceiver;
import com.andframe.caches.AfImageCaches;
import com.andframe.caches.AfSharedPreference;
import com.andframe.fragment.AfFragment;
import com.andframe.helper.android.AfDeviceInfo;
import com.andframe.network.AfFileService;
import com.andframe.network.AfImageService;
import com.andframe.thread.AfDispatch;
import com.andframe.thread.AfTask;
import com.andframe.thread.AfThreadWorker;
import com.andframe.util.DatabaseUtil;
import com.andframe.util.android.AfNetwork;
import com.andframe.util.java.AfMD5;
import com.andframe.util.java.AfVersion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * AfApplication 抽象类 （继承使用必须继承并使用，其他框架功能依赖于 AfApplication）
 * @author 树朾 负责 全局数据的存储 和 事件通知
 * 
 *        必须实现 指定工程中 的主页面 的类 getForegroundClass
 * 
 *        提供静态全局接口 public static getApp() 获取全局App public static getAppContext()
 *        获取App public static getLooper() 获取全局消息循环对象（用于构造UI Handler） public
 *        static postTask(AfTask task) 抛送全局任务（AfTask） public static
 *        postHandle(AfUIHandle handle) 抛送UIHandle任务（AfUIHandle） public static
 *        getNetworkStatus() 获取网络状态 值类型在AfNetworkEnum中 public static
 *        getDebugMode() 获取当前调试模式 public static setDebugMode(int mode) 设置调试模式
 *        public static getVersion() 获取当前App版本 public static getVersionCode()
 *        获取当前App版本Code
 * 
 *        非静态接口 public exitForeground(Object power) 退出前台 public
 *        startForeground(Activity activity) 启动前台 public getCachesPath(String
 *        type) 获取并创建缓存路劲 public getWorkspacePath(String type) 获取并创建工作路劲
 * 
 *        继承之后根据需要的功能 重写 相应的函数 public getExceptionHandler() //全局异常处理 public
 *        getAppSetting() //全局设置 public getImageService() //图片服务 public
 *        getFileService() //文件服务 public getUpdateService() //更新服务 public
 *        getLogoId() //App Logo（更新等功能会用到） public getAppName() //App 名称
 *
 * 			public boolean isBackground() //判断是否在后台允许（按HOME之后）
 * 			public Random getRandom()//获取统一随机数
 */
@SuppressWarnings("deprecation")
public abstract class AfApplication extends Application {

	/**
	 * interface INotifyNeedUpdate
	 * @author 树朾 需要更新通知接口
	 */
	public interface INotifyNeedUpdate {

		void onNotifyNeedUpdate(String curver, String server);
	}
	/**
	 * interface INotifyUpdate
	 * @author 树朾 需要更新通知接口
	 */
	public interface INotifyUpdate {

		void onNotifyUpdate(String curver, String server, String describe);
	}
	/**
	 * interface INotifyNetworkStatus
	 * @author 树朾 网络状态改变通知接口
	 */
	public static interface INotifyNetworkStatus {

		void onNetworkStatusChanged(int networkStatus);
	}
	public static final int DEBUG_NONE = 0;

	public static final int DEBUG_TESTDATA = 1;
	public static final int DEBUG_TEST = 2;
	public static final String STATE_RUNNING = "APP_CACHE_RUNNING";
	protected static final String STATE_TIME = "STATE_TIME";

	protected static final String STATE_NETWORKSTATUS = "STATE_NETWORKSTATUS";
	protected static final String STATE_FIXEDPOSITION = "STATE_FIXEDPOSITION";
	protected static final String STATE_DEBUGMODE = "STATE_DEBUGMODE";
	protected static final String STATE_VERSION = "STATE_VERSION";
	protected static final String STATE_SERVERVERSION = "STATE_SERVERVERSION";
	protected static final String STATE_ISINITIALIZED = "STATE_ISINITIALIZED";
	protected static final String STATE_ISFORERUNNING = "STATE_ISFORERUNNING";
	public static AfApplication mApp = null;

	public static synchronized AfApplication getApp() {
		return mApp;
	}

	public static synchronized Context getAppContext() {
		return mApp.getApplicationContext();
	}

	public static synchronized Looper getLooper() {
		return mApp.mLooper;
	}

	public static synchronized AfTask postTask(AfTask task) {
		if (mApp.mWorker != null) {
			return mApp.mWorker.postTask(task);
		}
		return AfDaemonThread.postTask(task);
	}

	public static synchronized AfTask postTaskDelayed(AfTask task, int delay) {
		if (mApp.mWorker != null) {
			return mApp.mWorker.postTaskDelayed(task, delay);
		}
		return AfDaemonThread.postTaskDelayed(task,delay);
	}

	public static synchronized AfDispatch dispatch(AfDispatch handle) {
		handle.dispatch(mApp.mLooper);
		return handle;
	}

	public static synchronized int getNetworkStatus() {
		return AfNetwork.getNetworkState(mApp);
		// return mApp.mNetworkStatus;
	}

	public static synchronized int getDebugMode() {
		return mApp.mDebugMode;
	}

	public static synchronized void setDebugMode(int mode) {
		mApp.mDebugMode = mode;
	}

	/**
	 * 获取内部版本
	 * @return
	 */
	public static synchronized String getVersion() {
		return mApp.mVersion;
	}

	/**
	 * 获取内部版本代码
	 * @return
	 */
	public static synchronized int getVersionCode() {
		return AfVersion.transformVersion(mApp.mVersion);
	}

	/**
	 * 获取 activity 主页面 类
	 * @return
	 */
	public abstract Class<? extends AfActivity> getForegroundClass();

	// Debug Mode
	protected int mDebugMode = DEBUG_NONE;

	// 当前网络连接类型 默认为 未连接
	protected int mNetworkStatus = AfNetwork.TYPE_NONE;
	// 当前主页面
	protected AfActivity mCurActivity = null;
	// 当前主页面
	protected AfFragment mCurFragment = null;
	// 主页面
	protected AfActivity mMainActivity = null;
	// 当前版本
	protected String mVersion = "0.0.0.0";
	// 最新版本
	protected String mServerVersion = "0.0.0.0";
	// 最新版本描述
	protected String mUpdateDescribe = "";
	//随机数生成器
	protected Random mRandom = new Random();
	// 主程序的线程Worker
	protected Looper mLooper = null;

	protected AfThreadWorker mWorker = null;

	protected boolean mIsExiting = false;

	//	protected boolean mIsGoingHome = false;
	protected boolean mIsInitialized = false;
	// 标记前台是否在运行
	protected boolean mIsForegroundRunning = false;
	// 保存数据
	protected Date mStateTime = new Date();
	protected AfSharedPreference mRunningState = null;
	private PackageInfo mPackageInfo;
	private ApplicationInfo mApplicationInfo;

	//全局单例模式MAP
	protected Map<String,Object> mSingletonMap = new LinkedHashMap<String,Object>(); 

	public AfApplication() {
		mApp = this;
		mLooper = Looper.myLooper();
		AfFileService.setServer("/FileService", "/FileServlet");
//		AfSoapService.setServer("http://tempuri.org/", "MobileService.svc",
//				"IMobileService/");
//		AfSoapServiceOld.setServer("http://tempuri.org/", "MobileService.svc",
//				"IMobileService/");
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

	@Override
	public void onCreate() {
		super.onCreate();
		try {
			AfExceptionHandler.register();
			
			mRunningState = new AfSharedPreference(this, STATE_RUNNING);
			// 初始化设备信息
			AfDeviceInfo.initialize(getAppContext());
			// 初始化震动控制台
			AfVibratorConsole.initialize(getAppContext());
			// 初始化AppSettings
			AfAppSettings.initialize(getAppContext());
			// 设置图片缓存路径
			AfImageCaches.initialize(this, getCachesPath("image"));
			// 初始化 图片服务
			AfImageService.initialize(getAppContext());
			// 初始化 文件服务
			AfFileService.initialize(getAppContext());
			// 初始化 更新服务
			AfUpdateService.initialize(getAppContext());
			// App 启动的时候 检查网络相关设置
			mNetworkStatus = AfNetwork.getNetworkState(this);
			// 设置服务器
			AfAppSettings set = AfAppSettings.getInstance();
			AfFileService.setServer(set.getFileServerIP(),
					set.getFileServerPort());
//			AfSoapService.setServer(set.getDataServerIP(),
//					set.getDataServerPort());
			mDebugMode = set.getDebugMode();
			// 初始化通知中心
			AfNotifyCenter.initailize(getAppContext());
			// 初始化版本号
			getPackageVersion();
			// 检查数据库
			new DatabaseUtil(getAppContext()).checkDataBaseVersion();
		} catch (Throwable e) {
			e.printStackTrace();// handled
			AfExceptionHandler.handler(e, "AfApplication.onCreate");
		}
	}
	
	public boolean isDebug() {
		return BuildConfig.DEBUG;
	}
	
	private void getPackageVersion() throws Exception {
		int get = PackageManager.GET_CONFIGURATIONS;
		String tPackageName = getPackageName();
		PackageManager magager = getPackageManager();
		mPackageInfo = magager.getPackageInfo(tPackageName, get);
		mVersion = mPackageInfo.versionName;
	}

	/**
	 * 在每次程序启动的时候初始化一遍
	 * @deprecated 已经弃用
	 * @param power
	 *            用于权限验证
	 */
	public synchronized void initialize(AfActivity power) {
		if (power instanceof AfActivity && !mIsInitialized) {
			try {
				// 标识初始化完成
				mIsInitialized = true;
			} catch (Throwable e) {
				AfExceptionHandler.handler(e, "AfApplication.initialize");
			}
		}
	}

	/**
	 * 获取App工作目录
	 * @param type
	 */
	public synchronized String getPrivatePath(String type) {
		File file = new File(getCacheDir(), type);
		if (!file.exists()) {
			file.mkdir();
		}
		return file.getPath();
	}

	/**
	 * 获取App工作目录
	 * @param type
	 */
	public synchronized String getWorkspacePath(String type) {
		File workspace = null;
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			String sdcard = Environment.getExternalStorageDirectory().getPath();
			workspace = new File(sdcard + "/" + getAppName() + "/" + type);
			if (!workspace.exists()) {
				workspace.mkdir();
			}
		} else {
			return getPrivatePath(type);
		}
		return workspace.getPath();
	}

	/**
	 * 获取caches目录
	 * @param type
	 */
	public synchronized String getCachesPath(String type) {
		File caches = new File(getWorkspacePath("caches"));
		caches = new File(caches, type);
		if (!caches.exists()) {
			caches.mkdir();
		}
		return caches.getPath();
	}

	/**
	 * 获取包信息
	 * @return PackageInfo
	 */
	public PackageInfo getPackageInfo() {
		return mPackageInfo;
	}

	/**
	 * 获取 App信息
	 * @return ApplicationInfo
	 */
	@Override
	public ApplicationInfo getApplicationInfo() {
		if (mApplicationInfo == null) {
//			mApplicationInfo = super.getApplicationInfo();
//			if (mApplicationInfo == null){
			String name = this.getPackageName();
			int type = PackageManager.GET_META_DATA;
			PackageManager manager = this.getPackageManager();
			try {
				mApplicationInfo = manager.getApplicationInfo(name, type);
			} catch (Throwable e) {
				AfExceptionHandler.handler(e, "PackageManager.getApplicationInfo");
			}
//			}
		}
		return mApplicationInfo;
	}

	/**
	 * 获取在Application 中定义的 meta-data
	 * @param key
	 * @return meta-data or null
	 */
	public String getMetaData(String key) {
		try {
//			String name = this.getPackageName();
//			int type = PackageManager.GET_META_DATA;
//			PackageManager manager = this.getPackageManager();
//			ApplicationInfo info = manager.getApplicationInfo(name, type);
			ApplicationInfo info = getApplicationInfo();
			Object data = info.metaData.get(key);
			if (data == null) {
				throw new Exception("getMetaData null");
			}
			key = String.valueOf(data);
//			key = info.metaData.getString(key);
			return key;
		} catch (Throwable e) {
			AfExceptionHandler.handler(e, "AfApplication.getMetaData");
		}
		return null;
	}

	/**
	 * 获取服务器最新版本
	 * @return verson
	 */
	public String getServerVersion() {
		return mServerVersion;
	}

	/**
	 * 获取服务器最新版本更新描述
	 * @return describe
	 */
	public String getUpdateDescribe() {
		return mUpdateDescribe;
	}

	/**
	 * 获取App是否 需要更新
	 * @return need
	 */
	public boolean isNeedUpdate() {
		int curver = AfVersion.transformVersion(mVersion);
		int server = AfVersion.transformVersion(mServerVersion);
		return curver < server;
	}

	/**
	 * 获取App是否 执行过 initialize
	 * @deprecated 已经弃用
	 * @return inited
	 */
	public synchronized boolean isInitialize() {
		return mIsInitialized;
	}

	/**
	 * 获取全局单例实例
	 * @param key
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T> T getSingleton(String key){
		T singleton = null;
		try {
			singleton = (T) mSingletonMap.get(key);
		} catch (Exception e) {
			AfExceptionHandler.handler(e, "AfApplication.getSingleton");
		}
		return singleton;
	}

	/**
	 * 设置全局单例实例
	 * @param key
	 * @param value
	 */
	public synchronized void setSingleton(String key,Object value){
		mSingletonMap.put(key, value);
	}

	/**
	 * 获取CurActivity
	 * @return AfActivity or null
	 */
	public synchronized AfActivity getCurActivity() {
		return mCurActivity;
	}

	/**
	 * 获取AfMainActivity
	 * @return AfMainActivity or null
	 */
	public synchronized AfActivity getMainActivity() {
		return mMainActivity;
	}

	/**
	 * 设置主页面
	 * @param activity
	 *            主页面
	 */
	public synchronized void setMainActivity(AfActivity activity) {
		mMainActivity = activity;
		mIsForegroundRunning = true;
	}

	/**
	 * 设置当前的页面
	 * @param power
	 *            用于权限验证
	 * @param activity
	 *            当前的 Activity
	 */
	public synchronized void setCurActivity(Object power, AfActivity activity) {
		if (power instanceof AfActivity) {
			//AfActivity 退出
			if (activity == null){
				if (power == mMainActivity){
					notifyForegroundClosed(mMainActivity);
				}
			}
			// 如果正在返回主页面
			else if (mIsExiting) {
				// 如果已经到达主页面
				mCurActivity = activity;
				if (getForegroundClass().isInstance(activity)) {
					// 关闭返回主页面功能
					if (mIsExiting) {
						activity.finish();
					}
					mIsExiting = false;
				} else {
					// 关不当前页面回到主页面
					activity.finish();
					return;
				}
			} if(getForegroundClass().isInstance(activity) && !mIsExiting && mMainActivity==null){
				setMainActivity(activity);
				mCurActivity = activity;
			}
		}
	}

	/**
	 * 设置当前的页面
	 * @param power
	 *            用于权限验证
	 * @param fragment
	 *            当前的 Fragment
	 */
	public synchronized void setCurFragment(Object power, AfFragment fragment) {
		if (power instanceof AfFragment) {
			mCurFragment = fragment;
		}
	}

	/**
	 * 设置App网络状态
	 * @param power
	 *            传入this指针 用于验证权限
	 * @param networkState
	 *            指定的网络状态
	 */
	public synchronized void setNetworkStatus(Object power, int networkState) {
		if (power instanceof ConnectionChangeReceiver) {
			mNetworkStatus = networkState;

			notifyNetworkStatus(mCurActivity, networkState);
			notifyNetworkStatus(mCurFragment, networkState);

			// 如果网络连接上
			if (mNetworkStatus != AfNetwork.TYPE_NONE) {
				// 如果还没有成功定位
			}
		}
	}

	/**
	 * 设置 服务器 App 版本
	 * @param power
	 *            传入this指针 用于验证权限
	 * @param version
	 *            服务器版本
	 */
	public synchronized void setServerVersion(Object power, String version) {
		mServerVersion = version;
		if (isNeedUpdate()) {
			notifyNeedUpdate(mCurActivity, mVersion, mServerVersion);
			notifyNeedUpdate(mCurFragment, mVersion, mServerVersion);
		}
	}

	/**
	 * 设置 服务器 App 版本 更新描述
	 * @param power
	 *            传入this指针 用于验证权限
	 * @param describe
	 *            服务器版本
	 */
	public synchronized void setServerVersion(Object power, String version,
			String describe) {
		mServerVersion = version;
		mUpdateDescribe = describe;
		if (isNeedUpdate()) {
			notifyUpdate(mCurActivity, mVersion, mServerVersion, describe);
			notifyUpdate(mCurFragment, mVersion, mServerVersion, describe);
		}
	}

	/**
	 * 向 power 发送 需要更新通知
	 * @param power
	 *            通知的对象 必须实现 INotifyNeedUpdate 接口
	 */
	private void notifyUpdate(Object power, String curver, String server,
			String describe) {
		if (power instanceof INotifyUpdate) {
			try {
				INotifyUpdate tINotify = (INotifyUpdate) power;
				tINotify.onNotifyUpdate(curver, server, describe);
			} catch (Throwable e) {
				e.printStackTrace();// handled
				AfExceptionHandler.handler(e, "AfApplication.notifyUpdate");
			}
		}
	}

	/**
	 * 向 power 发送 需要更新通知
	 * @param power
	 *            通知的对象 必须实现 INotifyNeedUpdate 接口
	 *            当前网络状态
	 */
	private void notifyNeedUpdate(Object power, String curver, String server) {
		if (power instanceof INotifyNeedUpdate) {
			try {
				INotifyNeedUpdate tINotify = (INotifyNeedUpdate) power;
				tINotify.onNotifyNeedUpdate(curver, server);
			} catch (Throwable e) {
				e.printStackTrace();// handled
				AfExceptionHandler.handler(e, "AfApplication.notifyNeedUpdate");
			}
		}
	}

	/**
	 * 向 power 发送 网络状态改变通知
	 * @param power
	 *            通知的对象 必须实现 INotifyNetworkStatus 接口
	 * @param networkState
	 *            当前网络状态
	 */
	private synchronized void notifyNetworkStatus(Object power, int networkState) {
		if (power instanceof INotifyNetworkStatus) {
			try {
				INotifyNetworkStatus tINotify = (INotifyNetworkStatus) power;
				tINotify.onNetworkStatusChanged(networkState);
			} catch (Throwable e) {
				e.printStackTrace();// handled
				AfExceptionHandler.handler(e, "AfApplication.notifyNetworkStatus");
			}
		}
	}

	/**
	 * 通知APP 启动天台页面
	 */
	public synchronized void startForeground() {
		if (mIsForegroundRunning == false) {
			mIsForegroundRunning = true;
			mMainActivity = null;
			Intent intent = new Intent(this, getForegroundClass());
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(intent);
		}
	}

	/**
	 * 退出前台
	 * @param power
	 */
	public synchronized void exitForeground(Object power) {
		/** (2014-7-30 注释 只有当notifyForegroundClosed时才设为false) **/
		// mIsForegroundRunning = false;
		if (mCurActivity != null) {
			if (mCurActivity instanceof AfActivity) {
				mCurActivity.finish();
			} else {
				mIsExiting = true;
				mCurActivity.finish();
			}
		}
	}

	/**
	 * 获取 前台页面是否在运行
	 * @return isforeground
	 */
	public synchronized boolean isForegroundRunning() {
		return mIsForegroundRunning;
	}

	/**
	 * 通知APP 前台已经关闭
	 * @param activity
	 *            权限对象 传入this
	 */
	public synchronized void notifyForegroundClosed(AfActivity activity) {
		if (activity == mMainActivity && mIsForegroundRunning) {
			// 清空相关信息
			mCurActivity = null;
			mMainActivity = null;
			mCurFragment = null;
			mServerVersion = "0.0.0.0";
			mUpdateDescribe = "";
			mIsForegroundRunning = false;
		}
	}

	/**
	 * 获取 ExceptionHandler
	 * @return handler
	 */
	public AfExceptionHandler getExceptionHandler() {
		return new AfExceptionHandler();
	}

	/**
	 * 获取 AfAppSettings
	 * @return setting
	 */
	public AfAppSettings getAppSetting() {
		return new AfAppSettings(this);
	}

	/**
	 * 获取 AfImageService
	 * @return service
	 */
	public AfImageService getImageService() {
		return new AfImageService();
	}

	/**
	 * 获取 AfFileService
	 * @return fileservice
	 */
	public AfFileService getFileService() {
		return new AfFileService();
	}

	/**
	 * 获取 AfUpdateService
	 * @return updateservice
	 */
	public AfUpdateService getUpdateService() {
		return new AfUpdateService(this);
	}

	/**
	 * 获取APP图标ID,子类可以继承返回R.drawable.app_logo
	 * @return APP图标ID
	 */
	public int getLogoId() {
		return android.R.drawable.zoom_plate;
	}

	/**
	 * 获取APP名称,子类可以继承返回getString(R.string.app_name);
	 * @return APP名称
	 */
	public String getAppName() {
		return "AndFrame";
	}

	/**
	 * 获取 Des默认加密密钥
	 * @return key
	 */
	public String getDesKey() {
		return AfMD5.getMD5("");
	}

	/**
	 * 密码加密
	 * @param password
	 *            密码明文
	 * @return 加密的密文 子类可重写这个方法更改加密算法（默认 MD5）
	 */
	public String encryptionPassword(String password) {
		return AfMD5.getMD5(password);
	}

	/**
	 * 处理触发事件
	 * @param eventId
	 */
	public void onEvent(String eventId) {
		this.onEvent(eventId,new Object(),"");
	}

	/**
	 * 处理触发事件
	 * @param eventId
	 * @param tag
	 */
	public void onEvent(String eventId, String tag) {
		this.onEvent(eventId,new Object(),tag);
	}

	/**
	 * 处理触发事件
	 * @param eventId
	 * @param tag
	 */
	public void onEvent(String eventId,Object tag, String remark) {

	}

	/**
	 * 更新App相关信息 接口事件
	 * (各个框架组件中会调用触发)
	 */
	public void onUpdateAppinfo(){
	}

	/**
	 * 当APP被临时销毁时保存App 状态 在AfActivity 中调用
	 */
	public final void onRestoreInstanceState() {
		Date date = mRunningState.getDate(STATE_TIME);
		if (date != null) {
			onRestoreInstanceState(mRunningState);
			mRunningState.clear();
		}
	}

	/**
	 * 当APP被临时销毁时保存App 状态 在onRestoreInstanceState() 中调用
	 * @param state
	 */
	protected void onRestoreInstanceState(AfSharedPreference state) {
		mDebugMode = state.getInt(STATE_DEBUGMODE, mDebugMode);
		mNetworkStatus = state.getInt(STATE_NETWORKSTATUS, mNetworkStatus);
		mVersion = state.getString(STATE_VERSION, mVersion);
		mServerVersion = state.getString(STATE_SERVERVERSION, mServerVersion);
		mIsInitialized = state.getBoolean(STATE_ISINITIALIZED, mIsInitialized);
		mIsForegroundRunning = state.getBoolean(STATE_ISFORERUNNING,
				mIsForegroundRunning);
	}

	/**
	 * 更新保存转台时间，如果已经保存下次也可执行 onSaveInstanceState
	 */
	public void updateStateTime() {
		mStateTime = new Date();
	}

	/**
	 * 当APP被还原时候还原原来状态 在AfActivity 中调用
	 */
	public final void onSaveInstanceState() {
		// 如果保存时间标记一直，则不用保存
		Date date = mRunningState.getDate(STATE_TIME);
		if (date != null && date.equals(mStateTime)) {
			return;
		}
		mRunningState.putDate(STATE_TIME, mStateTime);
		onSaveInstanceState(mRunningState);
	}

	/**
	 * 当APP被还原时候还原原来状态 在AfActivity 中调用
	 */
	protected void onSaveInstanceState(AfSharedPreference state) {
		Editor editor = mRunningState.getSharePrefereEditor();
		editor.putInt(STATE_NETWORKSTATUS, mNetworkStatus);
		editor.putInt(STATE_DEBUGMODE, mDebugMode);
		editor.putString(STATE_VERSION, mVersion);
		editor.putString(STATE_SERVERVERSION, mServerVersion);
		editor.putBoolean(STATE_ISINITIALIZED, mIsInitialized);
		editor.putBoolean(STATE_ISFORERUNNING, mIsForegroundRunning);
		editor.commit();
	}

	/**
	 * 获取签值信息
	 * @author allen
	 * @version 2013-8-27 下午4:15:04
	 * @return X509Certificate
	 */
	public X509Certificate getSignInfo() {
		try {
			PackageManager manager = getPackageManager();
			PackageInfo packageInfo = manager.getPackageInfo(getPackageName(),
					PackageManager.GET_SIGNATURES);
			Signature[] signs = packageInfo.signatures;
			CertificateFactory certFactory = CertificateFactory
					.getInstance("X.509");
			ByteArrayInputStream stream = new ByteArrayInputStream(
					signs[0].toByteArray());
			return (X509Certificate) certFactory.generateCertificate(stream);
			// String pubKey = cert.getPublicKey().toString();
			// String signNumber = cert.getSerialNumber().toString();
			// System.out.println("signName:" + cert.getSigAlgName());
			// System.out.println("pubKey:" + pubKey);
			// System.out.println("signNumber:" + signNumber);
			// System.out.println("subjectDN:" +
			// cert.getSubjectDN().toString());
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 判断是否在后台运行（按HOME之后）
	 * 需要额外权限 android.permission.GET_TASKS 
	 * @return isBackground
	 */
	public boolean isBackground() {
		ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		try {
			List<RunningTaskInfo> tasks = am.getRunningTasks(1);
			if (tasks.size() > 0) {
				ComponentName topActivity = tasks.get(0).topActivity;
				if (!topActivity.getPackageName().equals(getPackageName())) {
					return true;
				}
			}
		} catch (Throwable e) {
		}
		return false;
	}

	/**
	 * 获取统一随机数
	 * @return Random
	 */
	public Random getRandom(){
		return mRandom;
	}

	/**
	 * 用于转换接口通知
	 * @param object
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	protected  <T> T transform(Object object, Class<T> clazz) {
		if (clazz.isInstance(object)){
			return clazz.cast(object);
		}
		return null;
	}

	/**
	 * 用于转换接口通知
	 * @param objects
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	protected <T> List<T> transforms(Object[] objects, Class<T> clazz) {
		List<T> list = new ArrayList<T>();
		for (Object object : objects) {
			if (clazz.isInstance(object)){
				list.add(clazz.cast(object));
			}
		}
		return list;
	}

	/**
	 * 用于转换接口通知
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	protected <T> List<T> transformNotifys(Class<T> clazz){
		List<Object> list = new ArrayList<Object>();
		if (mMainActivity != mCurActivity
				&& mMainActivity != null
				&& !mMainActivity.isRecycled()){
			list.add(mMainActivity);
		}
		if (mCurActivity != null
				&& !mCurActivity.isRecycled()){
			list.add(mCurActivity);
		}
		if (mCurFragment != null
				&& !mCurFragment.isRecycled()){
			list.add(mCurFragment);
		}
		return transforms(list.toArray(new Object[0]),clazz);
	}
}
