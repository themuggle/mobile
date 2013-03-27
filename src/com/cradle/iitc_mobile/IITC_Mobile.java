package com.cradle.iitc_mobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.cradle.iitc_mobile.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class IITC_Mobile extends Activity {

	private IITC_WebView iitc_view;
	private boolean back_button_pressed = false;

	static String[] plugins_list;
	static SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		initPluginsList();

		setContentView(R.layout.activity_main);

		// we do not want to reload our page every time we switch orientations...
		// so restore state if activity was already created
		if (savedInstanceState != null) {
			((IITC_WebView) findViewById(R.id.webview)).restoreState(savedInstanceState);
		} else {
			// load new iitc web view with ingress intel page
			iitc_view = (IITC_WebView) findViewById(R.id.webview);
			Intent intent = getIntent();
			String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action)) {
				Uri uri = intent.getData();
				String url = uri.toString();
				// TODO Why does "if(intent.getScheme() == "http")" not work?
				if (url.contains("http://"))
					url = url.replace("http://", "https://");
					Log.d("Intent received", "url: " + url);
				if (url.contains("ingress.com")) {
					Log.d("Intent received", "loading url...");
					iitc_view.loadUrl(url);
				}
			} else {
				Log.d("No Intent call", "loading https://www.ingress.com/intel");
				iitc_view.loadUrl("https://www.ingress.com/intel");
			}
		}
	}

	private void initPluginsList() {
		try {
		String[] assets = getAssets().list("");

		ArrayList<String> plugins = new ArrayList<String>();

		for (int i = 0; i < assets.length; i += 1) {
			if (!assets[i].equals("iitc-debug.user.js") && assets[i].substring(assets[i].length() - 3).equals(".js")) {
				plugins.add(assets[i]);
				Log.d(this.getClass().getSimpleName(), "plugin " + i + " :" + assets[i]);
			}
		}

		plugins_list = new String[plugins.size()];
		plugins.toArray(plugins_list);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// save instance state to avoid reloading on orientation change
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		iitc_view.saveState(outState);
	}

	// we want a self defined behavior for the back button
	@Override
	public void onBackPressed() {
		if (this.back_button_pressed) {
			super.onBackPressed();
			return;
		}

		iitc_view.loadUrl("javascript: window.goBack();");
		this.back_button_pressed = true;
		Toast.makeText(this, "Press twice to exit", Toast.LENGTH_SHORT).show();

		// reset back button after 0.5 seconds
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				back_button_pressed = false;
			}
		}, 500);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.reload_button:
				reload();
				return true;

			case R.id.select_plugins:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder
					.setTitle(R.string.select_plugins)
					.setMultiChoiceItems(plugins_list, getSelectedItems(),
						new DialogInterface.OnMultiChoiceClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which, boolean isChecked) {
								mPrefs.edit().putBoolean(plugins_list[which], isChecked).commit();
							}
						})

					.setPositiveButton(R.string.reload, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							Toast.makeText(IITC_Mobile.this, "Reloading...", Toast.LENGTH_SHORT).show();
							reload();
						}
					})
					.setNegativeButton(R.string.cancel, null);

				AlertDialog dialog = builder.create();
				dialog.show();

				return true;
				// print version number
			case R.id.version_num:
				PackageInfo pinfo;
				try {
					pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					Toast.makeText(this, "Build version: " + pinfo.versionName, Toast.LENGTH_SHORT).show();
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
				return true;
				// clear cache
			case R.id.cache_clear:
				iitc_view.clearHistory();
				iitc_view.clearFormData();
				iitc_view.clearCache(true);
				return true;
				// get the users current location and focus it on map
			case R.id.locate:
				iitc_view.loadUrl("javascript: window.map.locate({setView : true, maxZoom: 13});");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private boolean[] getSelectedItems() {
		boolean[] selected_plugins = new boolean[plugins_list.length];
		for (int i = 0; i < plugins_list.length; i++) {
			selected_plugins[i] = mPrefs.getBoolean(plugins_list[i], true);
		}
		return selected_plugins;
	}

	private void reload() {
		iitc_view.reload();
		try {
			iitc_view.getWebViewClient().loadIITC_JS(this);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
