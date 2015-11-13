/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.axaet.ibeaconsdk;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements OnClickListener {
	private final static String TAG = "DeviceControlActivity";

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private TextView text_state;
	private EditText text_uuid, text_Major, text_Minor, text_Period, password,
			deviceName;
	private Spinner spinner;

	private String mDeviceName;
	private String mDeviceAddress;
	private BluetoothLeService mBluetoothLeService;
	private boolean mConnected = false;

	private Button but_enable;

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			mBluetoothLeService.connect(mDeviceAddress);
		}
		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
		
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device. This can be a
	// result of read
	// or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				text_state.setText("connected");
				invalidateOptionsMenu();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
				invalidateOptionsMenu();
				text_state.setText("disconnected");
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
			} else if (BluetoothLeService.ACTION_EXTRA_UUID_DATA.equals(action)) {
				String uuid = intent
						.getStringExtra(BluetoothLeService.UUID_DATA);
				Log.i(TAG, "uuid=" + uuid);
				text_uuid.setText(uuid);
			} else if (BluetoothLeService.ACTION_EXTRA_OTHER_DATA
					.equals(action)) {
				int Major = intent.getIntExtra(BluetoothLeService.MAJOR_DATA,
						-1);
				int Minor = intent.getIntExtra(BluetoothLeService.MINOR_DATA,
						-1);
				int Period = intent.getIntExtra(BluetoothLeService.PERIOD_DATA,
						-1);
				int txPower = intent.getIntExtra(
						BluetoothLeService.TXPOWER_DATA, -1);
				txPowerTemp=txPower;
				text_Major.setText(Major + "");
				text_Minor.setText(Minor + "");
				text_Period.setText(Period + "");
				Log.i(TAG, Minor+Major+Period+"OTHER");
				switch (txPower) {
				case 0:
					spinner.setSelection(0);
					break;

				case 1:
					spinner.setSelection(1);
					break;
				case 2:
					spinner.setSelection(2);
					break;
				case 3:
					spinner.setSelection(3);
					break;
				}
			} else if (BluetoothLeService.ACTION_EXTRA_SENIOR_DATA
					.equals(action)) {
				int Major = intent.getIntExtra(BluetoothLeService.MAJOR_DATA,
						-1);
				int Minor = intent.getIntExtra(BluetoothLeService.MINOR_DATA,
						-1);
				int Period = intent.getIntExtra(BluetoothLeService.PERIOD_DATA,
						-1);
				int txPower = intent.getIntExtra(
						BluetoothLeService.TXPOWER_DATA, -1);
				txPowerTemp=txPower;
				text_Major.setText(Major + "");
				text_Minor.setText(Minor + "");
				text_Period.setText(Period + "");
				Log.i(TAG, Minor+Major+Period+"SENIOR");
				switch (txPower) {
				case 0:
					spinner.setSelection(0);
					break;
				case 1:
					spinner.setSelection(1);
					break;
				case 2:
					spinner.setSelection(2);
					break;
				case 3:
					spinner.setSelection(3);
					break;
				}
			} else if (BluetoothLeService.ACTION_EXTRA_PASSWORD_MISTAKE
					.equals(action)) {
				Message message = handler.obtainMessage();
				message.what = 0;
				message.sendToTarget();
			} else if (BluetoothLeService.ACTION_EXTRA_PASSWORD_RIGHT
					.equals(action)) {
				Message message = handler.obtainMessage();
				message.what = 1;
				message.sendToTarget();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gatt_services_characteristics);
		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		text_state = (TextView) findViewById(R.id.text_state);
		text_uuid = (EditText) findViewById(R.id.text_uuid);
		text_Major = (EditText) findViewById(R.id.text_Major);
		text_Minor = (EditText) findViewById(R.id.text_Minor);
		text_Period = (EditText) findViewById(R.id.text_Period);
		password = (EditText) findViewById(R.id.text_Password);
		deviceName = (EditText) findViewById(R.id.text_Name);
		if (deviceName != null) {
			deviceName.setText(mDeviceName);
		}
		spinner = (Spinner) findViewById(R.id.spinner);

		List<String> list = new ArrayList<String>();
		list.add("-23");
		list.add("-6");
		list.add("0");
		list.add("4");

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				switch (arg2) {
				case 0:
					txPowerTemp = 0;
					break;

				case 1:
					txPowerTemp = 1;
					break;
				case 2:
					txPowerTemp = 2;
					break;
				case 3:
					txPowerTemp = 3;
					break;
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});

		but_enable = (Button) findViewById(R.id.but_enable);
		but_enable.setOnClickListener(this);
		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	
	private int txPowerTemp;
	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver,
				BluetoothLeService.makeGattUpdateIntentFilter());
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			mBluetoothLeService.connect(mDeviceAddress);
			return true;
		case R.id.menu_disconnect:
			mBluetoothLeService.disconnect();
			return true;
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View view) {
		if (but_enable == view) {
			mBluetoothLeService.validatePassword(password.getText().toString());
		}
	}

	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			Log.i(TAG, msg.what + "");
			switch (msg.what) {
			case 0:
				Toast.makeText(DeviceControlActivity.this, "error password",
						Toast.LENGTH_LONG).show();
				break;

			case 1:
				mBluetoothLeService.modifyUUID(text_uuid.getText().toString());
				mBluetoothLeService.modifyOtherParameter(text_Major.getText()
						.toString(), text_Minor.getText().toString(),
						text_Period.getText().toString(),txPowerTemp);
				if (!mDeviceName.equals(deviceName.getText().toString())) {
					mBluetoothLeService.modifyDeviceName(deviceName.getText()
							.toString());
				}
				mBluetoothLeService.closeBLE();
				Toast.makeText(DeviceControlActivity.this, "Modify the success", Toast.LENGTH_SHORT).show();;
				DeviceControlActivity.this.finish();
				break;
			}
		};
	};

}
