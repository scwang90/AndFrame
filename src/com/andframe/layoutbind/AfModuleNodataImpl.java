package com.andframe.layoutbind;

import android.view.View;
import android.widget.TextView;

import com.andframe.R;
import com.andframe.activity.framework.AfPageable;
import com.andframe.activity.framework.AfViewable;

public class AfModuleNodataImpl extends AfModuleNodata {
	
	public static final int ID_BUTTON = R.id.module_nodata_button;

	public AfModuleNodataImpl(AfPageable page) {
		super(page);
	}
	
	@Override
	protected View findLayout(AfViewable view) {
		View layout = view.findViewById(R.id.module_nodata_button);
		if(layout != null){
			layout = (View)layout.getParent();
		}
		return layout;
	}

	@Override
	protected View findRefreshButton(AfViewable view) {
		return view.findViewById(R.id.module_nodata_button);
	}

	@Override
	protected TextView findDescription(AfViewable view) {
		return view.findViewByID(R.id.module_nodata_description);
	}
}
