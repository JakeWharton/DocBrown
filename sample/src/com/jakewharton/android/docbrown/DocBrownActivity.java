package com.jakewharton.android.docbrown;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ActionProvider;
import android.view.View;

public class DocBrownActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Attempt to access the ICS-only class
		new MyActionProvider(this);

		// If you're here it worked. It's a celebration.
		setContentView(R.layout.main);
	}
	
	class MyActionProvider extends ActionProvider {
		public MyActionProvider(Context context) {
			super(context);
		}

		@Override
		public View onCreateActionView() {
			// TODO Auto-generated method stub
			return null;
		}
	}
}