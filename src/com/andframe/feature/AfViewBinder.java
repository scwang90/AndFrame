package com.andframe.feature;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CompoundButton;

import com.andframe.activity.framework.AfView;
import com.andframe.activity.framework.AfViewable;
import com.andframe.annotation.view.BindAfterViews;
import com.andframe.annotation.view.BindCheckedChange;
import com.andframe.annotation.view.BindClick;
import com.andframe.annotation.view.BindItemClick;
import com.andframe.annotation.view.BindItemLongClick;
import com.andframe.annotation.view.BindLongClick;
import com.andframe.annotation.view.BindView;
import com.andframe.application.AfExceptionHandler;
import com.andframe.feature.framework.EventListener;
import com.andframe.layoutbind.framework.AfViewDelegate;
import com.andframe.util.java.AfReflecter;


/**
 * 控件绑定器
 * @author 树朾
 */
public class AfViewBinder {

	private Object mHandler;

	public AfViewBinder(Object handler) {
		mHandler = handler;
	}
	
	protected String TAG(String tag) {
		return "AfViewBinder("+mHandler.getClass().getName()+")."+tag;
	}
	
	public void doBind() {
		if (mHandler instanceof AfViewable) {
			this.doBind((AfViewable)(mHandler));
		}
	}
	
	public void doBind(View root) {
		this.doBind(new AfView(root));
	}
	
	public void doBind(AfViewable root) {
		doBindClick(root);
		doBindLongClick(root);
		doBindItemClick(root);
		doBindItemLongClick(root);
		doBindCheckedChange(root);
		doBindView(root);
		doBindAfterView(root);
	}

	private Class<?> getStopType(){
		if (mHandler instanceof AfViewDelegate){
			return AfViewDelegate.class;
		}
		return Object.class;
	}

	public void doBindClick(AfViewable root) {
		for (Method method : AfReflecter.getMethodAnnotation(mHandler.getClass(), getStopType(), BindClick.class)) {
			try {
				BindClick bind = method.getAnnotation(BindClick.class);
				for (int id : bind.value()){
					View view = root.findViewById(id);
					view.setOnClickListener(new EventListener(mHandler).click(method));
				}
			} catch (Exception e) {
				AfExceptionHandler.handler(e,TAG("doBindClick.")+ method.getName());
			}
		}
	}

	public void doBindLongClick(AfViewable root) {
		for (Method method : AfReflecter.getMethodAnnotation(mHandler.getClass(), getStopType(), BindLongClick.class)) {
			try {
				BindLongClick bind = method.getAnnotation(BindLongClick.class);
				for (int id : bind.value()){
					View view = root.findViewById(id);
					view.setOnLongClickListener(new EventListener(mHandler).longClick(method));
				}
			} catch (Exception e) {
				AfExceptionHandler.handler(e,TAG("doBindLongClick.")+ method.getName());
			}
		}
	}

	public void doBindItemClick(AfViewable root) {
		for (Method method : AfReflecter.getMethodAnnotation(mHandler.getClass(), getStopType(), BindItemClick.class)) {
			try {
				BindItemClick bind = method.getAnnotation(BindItemClick.class);
				for (int id : bind.value()){
					AdapterView<?> view = root.findViewByID(id);
					if (view != null){
						view.setOnItemClickListener(new EventListener(mHandler).itemClick(method));
					}
				}
			} catch (Exception e) {
				AfExceptionHandler.handler(e,TAG("doBindLongClick.")+ method.getName());
			}
		}
	}

	public void doBindItemLongClick(AfViewable root) {
		for (Method method : AfReflecter.getMethodAnnotation(mHandler.getClass(), getStopType(), BindItemLongClick.class)) {
			try {
				BindItemLongClick bind = method.getAnnotation(BindItemLongClick.class);
				for (int id : bind.value()){
					AdapterView<?> view = root.findViewByID(id);
					if (view != null){
						view.setOnItemLongClickListener(new EventListener(mHandler).itemLongClick(method));
					}
				}
			} catch (Exception e) {
				AfExceptionHandler.handler(e,TAG("doBindLongClick.")+ method.getName());
			}
		}
	}

	public void doBindCheckedChange(AfViewable root) {
		for (Method method : AfReflecter.getMethodAnnotation(mHandler.getClass(), getStopType(), BindCheckedChange.class)) {
			try {
				BindCheckedChange bind = method.getAnnotation(BindCheckedChange.class);
				for (int id : bind.value()){
					CompoundButton view = root.findViewByID(id);
					if (view != null){
						view.setOnCheckedChangeListener(new EventListener(mHandler).checkedChange(method));
					}
				}
			} catch (Exception e) {
				AfExceptionHandler.handler(e,TAG("doBindLongClick.")+ method.getName());
			}
		}
	}

	public void doBindView(AfViewable root) {
		for (Field field : AfReflecter.getFieldAnnotation(mHandler.getClass(), getStopType(), BindView.class)) {
			try {
				BindView bind = field.getAnnotation(BindView.class);
				List<View> list = new ArrayList<View>();
				for (int id : bind.value()){
					int viewId = id;
					View view = root.findViewById(viewId);
					if (view != null){
						if (bind.click() && mHandler instanceof OnClickListener) {
							view.setOnClickListener((OnClickListener) mHandler);
						}
						list.add(view);
					}
				}
				if (list.size() > 0){
					field.setAccessible(true);
					if (field.getType().isArray()){
						Class<?> componentType = field.getType().getComponentType();
						Object[] array = list.toArray((Object[]) Array.newInstance(componentType, list.size()));
						field.set(mHandler, array);
					} else {
						field.set(mHandler, list.get(0));
					}
				}
			} catch (Exception e) {
				AfExceptionHandler.handler(e,TAG("doBindView.")+ field.getName());
			}
		}
	}

	private void doBindAfterView(AfViewable root) {
		List<SimpleEntry> methods = new ArrayList<SimpleEntry>();
		for (Method method : AfReflecter.getMethodAnnotation(mHandler.getClass(), getStopType(), BindAfterViews.class)) {
			BindAfterViews annotation = method.getAnnotation(BindAfterViews.class);
			methods.add(new SimpleEntry(method,annotation));
		}
		Collections.sort(methods, new Comparator<SimpleEntry>() {
			@Override
			public int compare(SimpleEntry lhs, SimpleEntry rhs) {
				return lhs.getValue().value()-rhs.getValue().value();
			}
		});
		for (SimpleEntry entry : methods) {
			try {
				invokeMethod(mHandler,entry.getKey());
			}catch(Throwable e){
				e.printStackTrace();
				if (!entry.getValue().exception()){
					throw new RuntimeException("调用视图初始化失败",e);
				}
				AfExceptionHandler.handler(e, TAG("doBindView.")+entry.getKey().getName());
			}
		}
	}

	private Object invokeMethod(Object handler, Method method, Object... params) throws Exception {
		if (handler != null && method != null){
			method.setAccessible(true);
			return method.invoke(handler, params);
		}
		return null;
	}
	
	public static class SimpleEntry implements Map.Entry<Method,BindAfterViews> {

		private final Method key;
		private BindAfterViews value;

		public SimpleEntry(Method theKey, BindAfterViews theValue) {
			key = theKey;
			value = theValue;
		}

		public Method getKey() {
			return key;
		}

		public BindAfterViews getValue() {
			return value;
		}

		public BindAfterViews setValue(BindAfterViews object) {
			BindAfterViews result = value;
			value = object;
			return result;
		}

		@Override public String toString() {
			return key + "=" + value;
		}
	}

}
