package com.mike.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class L {

	private static final String TAG = "IMAGE LOADER : ";

	public static void m(String message) {

		Log.d(TAG, message);

	}

	public static void s(Context context, String message) {

		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

	}

}
