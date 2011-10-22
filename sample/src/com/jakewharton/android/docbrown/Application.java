package com.jakewharton.android.docbrown;

import android.os.Build;
import com.jakewharton.android.docbrown.DocBrown;

public class Application extends android.app.Application {
	@Override
	public void onCreate() {
		super.onCreate();
		
		DocBrown.from("ActionProvider.jar")
		        .load("android.view.ActionProvider")
		        .before(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		        .into(this);
	}
}