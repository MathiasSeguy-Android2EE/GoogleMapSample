package com.android2ee.formation.librairies.google.map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class LauncherActivity extends Activity {
	private static final String TAG = "MainActivity";
	private Intent target = new Intent();
	private int activityResult = 11021974;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int gpsuStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (gpsuStatusCode != ConnectionResult.SUCCESS) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(gpsuStatusCode, this, activityResult);
			dialog.show();
		} else {
			// Else GoogleService are installed => launch your app
			target.setClass(getApplicationContext(), MainActivity.class);
			startActivity(target);
			finish();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == activityResult && resultCode == RESULT_OK) {
			// GoogleService are NOW installed => launch your app
			target.setClass(getApplicationContext(), MainActivity.class);
			startActivity(target);
		}else {
			Toast.makeText(this, "GooglePlayService not avaialble. Application can't work.", Toast.LENGTH_LONG).show();
		}
		// what ever happens, die
		finish();
	}
}
