//
// Copyright (C) 2011-2012 Carnegie Mellon University
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of version 2 of the GNU General Public License as published
// by the Free Software Foundation.  A copy of the GNU General Public License
// should have been distributed along with this program in the file
// LICENSE.GPL.

// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
// or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// for more details.
package edu.cmu.cs.cloudlet.android;
import java.util.ArrayList;

import org.teleal.cling.android.AndroidUpnpServiceImpl;

import edu.cmu.cs.cloudlet.android.application.CloudletCameraActivity;
import edu.cmu.cs.cloudlet.android.application.graphics.GraphicsClientActivity;
import edu.cmu.cs.cloudlet.android.data.VMInfo;
import edu.cmu.cs.cloudlet.android.discovery.CloudletDiscovery;
import edu.cmu.cs.cloudlet.android.network.CloudletConnector;
import edu.cmu.cs.cloudlet.android.discovery.CloudletDevice;
import edu.cmu.cs.cloudlet.android.util.CloudletEnv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class CloudletActivity extends Activity {
	public static String GLOBAL_DISCOVERY_SERVER = "http://hail.elijah.cs.cmu.edu:8000/api/v1/Cloudlet/search/?n=3";	
	public static String SYNTHESIS_SERVER_IP = "cloudlet.krha.kr";
	public static int SYNTHESIS_SERVER_PORT = 8021;

	private static final int SYNTHESIS_MENU_ID_SETTINGS = 11123;
	private static final int SYNTHESIS_MENU_ID_CLEAR = 12311;

	protected Button startConnectionButton;
	protected CloudletConnector connector;
	protected int selectedOveralyIndex;
	private CloudletDiscovery cloudletDiscovery;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		loadPreferneces();

		// Initiate Environment Settings
		CloudletEnv.instance();

		// Cloudlet discovery
		this.cloudletDiscovery = new CloudletDiscovery(this, CloudletActivity.this, discoveryHandler);		

		// Performance Button
		findViewById(R.id.testSynthesis).setOnClickListener(clickListener);
	}

	private void showDialogSelectOverlay(final ArrayList<VMInfo> vmList) {
		String[] nameList = new String[vmList.size()];
		for (int i = 0; i < nameList.length; i++) {
			nameList[i] = new String(vmList.get(i).getAppName());
		}

		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle("Overlay List");
		ab.setIcon(R.drawable.ic_launcher);
		ab.setSingleChoiceItems(nameList, 0, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				selectedOveralyIndex = position;
			}
		}).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				if (position >= 0) {
					selectedOveralyIndex = position;
				}
				VMInfo overlayVM = vmList.get(selectedOveralyIndex);
				runConnection(SYNTHESIS_SERVER_IP, SYNTHESIS_SERVER_PORT, overlayVM);
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				return;
			}
		});
		ab.show();
	}

	/*
	 * Synthesis initiation through HTTP Post
	 */
	protected void runConnection(String address, int port, VMInfo overlayVM) {
		if (this.connector != null) {
			this.connector.close();
		}
		this.connector = new CloudletConnector(this, CloudletActivity.this);
		this.connector.startConnection(address, port, overlayVM);
	}


	/*
	 * Launch Application as a Standalone
	 */
	private void showDialogSelectApp(final String[] applications) {
		// Show Dialog
		AlertDialog.Builder ab = new AlertDialog.Builder(this);
		ab.setTitle("Application List");
		ab.setIcon(R.drawable.ic_launcher);
		ab.setSingleChoiceItems(applications, 0, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				selectedOveralyIndex = position;
			}
		}).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				if (position >= 0) {
					selectedOveralyIndex = position;
				} else if (applications.length > 0 && selectedOveralyIndex == -1) {
					selectedOveralyIndex = 0;
				}
				String application = applications[selectedOveralyIndex];
				runStandAlone(application);
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int position) {
				return;
			}
		});
		ab.show();
	}

	public void runStandAlone(String application) {
		application = application.trim();

		if (application.equalsIgnoreCase("moped") || application.equalsIgnoreCase("moped_disk")) {
			Intent intent = new Intent(CloudletActivity.this, CloudletCameraActivity.class);
			intent.putExtra("address", SYNTHESIS_SERVER_IP);
			intent.putExtra("port", TEST_CLOUDLET_APP_MOPED_PORT);
			startActivityForResult(intent, 0);
		} else if (application.equalsIgnoreCase("graphics")) {
			Intent intent = new Intent(CloudletActivity.this, GraphicsClientActivity.class);
			intent.putExtra("address", SYNTHESIS_SERVER_IP);
			intent.putExtra("port", TEST_CLOUDLET_APP_GRAPHICS_PORT);
			startActivityForResult(intent, 0);
		} else {
			showAlert("Error", "NO such Application : " + application);
		}
	}

	View.OnClickListener clickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// show
			// serviceDiscovery.showDialogSelectOption();

			// This buttons are for MobiSys test
			if (v.getId() == R.id.testSynthesis) {
				// Find All overlay and let user select one of them.
				CloudletEnv.instance().resetOverlayList();
				ArrayList<VMInfo> vmList = CloudletEnv.instance().getOverlayDirectoryInfo();
				if (vmList.size() > 0) {
					showDialogSelectOverlay(vmList);
				} else {
					showAlert("Error", "We found No Overlay");
				}
			}
		}
	};

	public void showAlert(String type, String message) {
		new AlertDialog.Builder(CloudletActivity.this).setTitle(type).setMessage(message)
				.setIcon(R.drawable.ic_launcher).setNegativeButton("Confirm", null).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == 0) {
				String ret = data.getExtras().getString("message");
			}
		}

		// send close VM message
		this.connector.closeRequest();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			new AlertDialog.Builder(CloudletActivity.this).setTitle("Exit").setMessage("Finish Application")
					.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							moveTaskToBack(true);
							finish();
						}
					}).setNegativeButton("Cancel", null).show();
		}
		return super.onKeyDown(keyCode, event);
	}

	public void loadPreferneces() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		CloudletActivity.SYNTHESIS_SERVER_IP = prefs.getString(getString(R.string.synthesis_pref_address),
				getString(R.string.synthesis_default_ip_address));
		CloudletActivity.SYNTHESIS_SERVER_PORT = Integer.parseInt(prefs.getString(getString(R.string.synthesis_pref_port),
				getString(R.string.synthesis_default_port)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, SYNTHESIS_MENU_ID_SETTINGS, 0, getString(R.string.synthesis_config_memu_setting));
		menu.add(0, SYNTHESIS_MENU_ID_CLEAR, 1, getString(R.string.synthesis_config_memu_clear));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onResume() {
		loadPreferneces();
		super.onResume();
	}

	@Override
	public void onDestroy() {
		if (this.cloudletDiscovery != null){
			this.cloudletDiscovery.close();
		}
		if (this.connector != null)
			this.connector.close();
		
		super.onDestroy();
	}

	/*
	 * Service Discovery Handler
	 */
	Handler discoveryHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == CloudletDiscovery.DEVICE_SELECTED) {
				CloudletDevice device = (CloudletDevice) msg.obj;
				String ipAddress = device.getIPAddress();
				SYNTHESIS_SERVER_IP = ipAddress;
			} else if (msg.what == CloudletDiscovery.USER_CANCELED) {
				showAlert("Info", "Select UPnP Server for Cloudlet Service");
			}
		}
	};
	
	 
	// TO BE DELETED (only for test purpose)
	public static final String[] applications = { "MOPED", "GRAPHICS"};
	public static final int TEST_CLOUDLET_APP_MOPED_PORT = 9092; // 19092
	public static final int TEST_CLOUDLET_APP_GRAPHICS_PORT = 9093;
}
