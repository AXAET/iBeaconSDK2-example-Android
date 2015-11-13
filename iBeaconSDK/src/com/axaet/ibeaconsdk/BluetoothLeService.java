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

import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
@SuppressLint("NewApi")
public class BluetoothLeService extends Service {

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothGatt mBluetoothGatt;

	public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	private final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
	public final static String ACTION_EXTRA_UUID_DATA = "com.example.bluetooth.le.EXTRA_UUID_DATA";
	public final static String ACTION_EXTRA_OTHER_DATA = "com.example.bluetooth.le.EXTRA_OTHER_DATA";
	public final static String ACTION_EXTRA_SENIOR_DATA = "com.example.bluetooth.le.EXTRA_SENIOR_DATA";
	public final static String ACTION_EXTRA_PASSWORD_MISTAKE = "com.example.bluetooth.le.ACTION_EXTRA_PASSWORD_MISTAKE";
	public final static String ACTION_EXTRA_PASSWORD_RIGHT = "com.example.bluetooth.le.ACTION_EXTRA_PASSWORD_RIGHT";

	public final static String UUID_DATA = "com.example.bluetooth.le.UUID_DATA";
	public final static String MAJOR_DATA = "com.example.bluetooth.le.MAJOR_DATA";
	public final static String MINOR_DATA = "com.example.bluetooth.le.MINOR_DATA";
	public final static String PERIOD_DATA = "com.example.bluetooth.le.PERIOD_DATA";
	public final static String TXPOWER_DATA = "com.example.bluetooth.le.TXPOWER_DATA";

	public final static UUID UUID_LOST_SERVICE = UUID.fromString(SampleGattAttributes.LOST_SERVICE);
	public final static UUID UUID_LOST_WRITE = UUID.fromString(SampleGattAttributes.LOST_WRITE);
	public final static UUID UUID_LOST_ENABLE = UUID.fromString(SampleGattAttributes.LOST_ENABLE);

	public final static UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);
	// Implements callback methods for GATT events that the app cares about. For
	// example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			String intentAction;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				intentAction = ACTION_GATT_CONNECTED;
				broadcastUpdate(intentAction);
				// Attempts to discover services after successful connection.
				mBluetoothGatt.discoverServices();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				intentAction = ACTION_GATT_DISCONNECTED;
				broadcastUpdate(intentAction);
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
				sendtNotify();
			} else {
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
		}

		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

			// Can get rssi here
		};

	};

	private void broadcastUpdate(final String action) {
		final Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
		final Intent intent = new Intent();
		// This is special handling for the Heart Rate Measurement profile. Data
		// parsing is
		// carried out as per profile specifications:
		// http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
		if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
			intent.setAction(action);
			int flag = characteristic.getProperties();
			int format = -1;
			if ((flag & 0x01) != 0) {
				format = BluetoothGattCharacteristic.FORMAT_UINT16;
			} else {
				format = BluetoothGattCharacteristic.FORMAT_UINT8;
			}
			final int heartRate = characteristic.getIntValue(format, 1);
			intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
		} else {
			// For all other profiles, writes the data formatted in HEX.
			final byte[] data = characteristic.getValue();
			String string = Conversion.bytesToHexString(data);
			try {
				if (data[0] == 17) {
					string = string.substring(2, 34);
					intent.setAction(ACTION_EXTRA_UUID_DATA);
					intent.putExtra(UUID_DATA, string);
				} else if (data[0] == 18) {
					int Major = Integer.parseInt(string.substring(2, 6), 16);
					int Minor = Integer.parseInt(string.substring(6, 10), 16);
					int txPower = Integer.parseInt(string.substring(10, 12), 16);
					int Period = Integer.parseInt(string.substring(12, 16), 16);
					intent.setAction(ACTION_EXTRA_OTHER_DATA);
					intent.putExtra(MAJOR_DATA, Major);
					intent.putExtra(MINOR_DATA, Minor);
					intent.putExtra(PERIOD_DATA, Period);
					intent.putExtra(TXPOWER_DATA, txPower);
				} else if (data[0] == 11) {
					int Major = Integer.parseInt(string.substring(2, 6), 16);
					int Minor = Integer.parseInt(string.substring(6, 10), 16);
					int txPower = Integer.parseInt(string.substring(10, 12), 16);
					int Period = Integer.parseInt(string.substring(12, 16), 16);
					intent.setAction(ACTION_EXTRA_SENIOR_DATA);
					intent.putExtra(MAJOR_DATA, Major);
					intent.putExtra(MINOR_DATA, Minor);
					intent.putExtra(PERIOD_DATA, Period);
					intent.putExtra(TXPOWER_DATA, txPower);
				} else if (data[0] == 5) {
					intent.setAction(ACTION_EXTRA_PASSWORD_RIGHT);
				} else if (data[0] == 10) {
					intent.setAction(ACTION_EXTRA_PASSWORD_MISTAKE);
				}
			} catch (Exception e) {
			}

		}
		sendBroadcast(intent);
	}

	public class LocalBinder extends Binder {
		public BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize() {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through
		// BluetoothManager.
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		if (mBluetoothAdapter == null || address == null) {
			return false;
		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			return false;
		}
		// We want to directly connect to the device, so we are setting the
		// autoConnect
		// parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Enables or disables notification on a give characteristic.
	 * 
	 * @param characteristic
	 *            Characteristic to act on.
	 * @param enabled
	 *            If true, enable notification. False otherwise.
	 */
	private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		// This is specific to Heart Rate Measurement.
		if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic
					.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}
		if (UUID_LOST_ENABLE.equals(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic
					.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			mBluetoothGatt.writeDescriptor(descriptor);
		}
	}

	/**
	 * Send enable notification
	 */
	private void sendtNotify() {
		// boolean result = false;
		BluetoothGattService nableService = mBluetoothGatt.getService(UUID_LOST_SERVICE);
		if (nableService == null) {
			return;
		}
		BluetoothGattCharacteristic TxPowerLevel = nableService.getCharacteristic(UUID_LOST_ENABLE);
		if (TxPowerLevel == null) {
			return;
		}
		setCharacteristicNotification(TxPowerLevel, true);
	}

	/**
	 * Write data to the bluetooth device ,Pwm_data_buf length less than 20
	 * 
	 * @param pwm_data_buf
	 */
	private void WriteData(byte[] data) {

		BluetoothGattService alertService = mBluetoothGatt.getService(UUID_LOST_SERVICE);
		if (alertService == null) {
			return;
		}
		BluetoothGattCharacteristic alertLevel = alertService.getCharacteristic(UUID_LOST_WRITE);
		if (alertLevel == null) {
			return;
		}
		alertLevel.getWriteType();
		alertLevel.setValue(data);
		alertLevel.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
		mBluetoothGatt.writeCharacteristic(alertLevel);
	}

	/**
	 * All action registered
	 * 
	 * @return
	 */
	public static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.ACTION_EXTRA_OTHER_DATA);
		intentFilter.addAction(BluetoothLeService.ACTION_EXTRA_SENIOR_DATA);
		intentFilter.addAction(BluetoothLeService.ACTION_EXTRA_UUID_DATA);
		intentFilter.addAction(BluetoothLeService.ACTION_EXTRA_PASSWORD_MISTAKE);
		intentFilter.addAction(BluetoothLeService.ACTION_EXTRA_PASSWORD_RIGHT);
		return intentFilter;
	}

	/**
	 * Send password authentication, verify that success or failure will receive
	 * broadcast
	 * 
	 * @param password
	 *            ASCII code visible character
	 */
	public void validatePassword(String password) {

		byte[] bs = Conversion.str2Byte(password, (byte) 0x04);
		WriteData(bs);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * To send a Bluetooth off device command, you must implement the method in
	 * order to write parameters to the Bluetooth device at each time you modify
	 * the parameters
	 */
	public void closeBLE() {
		byte[] data = new byte[1];
		data[0] = (byte) 0x03;
		WriteData(data);
	}

	/**
	 * modify the uuid
	 * 
	 * @param uuid
	 *            Each character in the UUID string must be or A~F or a~f 0~9
	 */
	public void modifyUUID(String uuid) {
		byte[] bs2 = Conversion.hex2Byte(uuid);
		WriteData(bs2);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Modify other parameters
	 * 
	 * @param major
	 *            Integer.parseInt(major)<65535
	 * @param minor
	 *            Integer.parseInt(minor)<65535
	 * @param period
	 *            100 < Integer.parseInt(period)<9800
	 * @param txPower
	 *            This parameter is more special, when spinner is selected, the
	 *            txPower should be 0 when -23 is -6; txPower is 1; the txPower
	 *            is 2; the is 4; the txPower should be 3.
	 */
	public void modifyOtherParameter(String major, String minor, String period, int txPower) {
		int majorNum = Integer.parseInt(major);
		int minorNum = Integer.parseInt(minor);
		int periodNum = Integer.parseInt(period);
		if (majorNum > 65535 || majorNum < 0) {
			majorNum = 10004;
		}
		if (minorNum > 65535 || minorNum < 0) {
			minorNum = 54480;
		}
		if (periodNum > 9800 || periodNum < 100) {
			periodNum = 1000;
		}
		byte[] data = new byte[8];
		data[0] = (byte) 0x02;
		data[1] = (byte) (majorNum / 256);
		data[2] = (byte) (majorNum % 256);
		data[3] = (byte) (minorNum / 256);
		data[4] = (byte) (minorNum % 256);
		data[5] = (byte) txPower;
		data[6] = (byte) (periodNum / 256);
		data[7] = (byte) (periodNum % 256);

		WriteData(data);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Change the password, ASCII code visible character
	 * 
	 * @param oldPassword
	 *            Length must for 6
	 * @param newPassword
	 *            Length must for 6
	 */
	public void modifyNewPassword(String oldPassword, String newPassword) {
		byte[] bs2 = Conversion.str2Byte(oldPassword + newPassword, (byte) 0x0c);
		WriteData(bs2);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		bs2[0] = (byte) 0x03;
		WriteData(bs2);
	}

	/**
	 * read rssi
	 * 
	 * @return
	 */
	public boolean readRssi() {
		if (mBluetoothGatt != null) {
			return mBluetoothGatt.readRemoteRssi();
		}
		return false;
	}

	/**
	 * Used to modify the device name, each character in the deviceName must be
	 * ASCII code visible characters
	 * 
	 * @param deviceName
	 *            ASCII code visible character
	 */
	public void modifyDeviceName(String deviceName) {
		byte[] Namebs = Conversion.str2ByteDeviceName(deviceName);
		WriteData(Namebs);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
