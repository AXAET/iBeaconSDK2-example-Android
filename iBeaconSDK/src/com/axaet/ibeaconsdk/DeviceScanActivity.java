
package com.axaet.ibeaconsdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.axaet.ibeaconsdk.iBeaconClass.iBeacon;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@SuppressLint("NewApi")
public class DeviceScanActivity extends ListActivity {

	private LeDeviceListAdapter mLeDeviceListAdapter;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;

	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 10000;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(R.string.title_devices);
		mHandler = new Handler();
		/**
		 * Check if the current mobile phone supports ble Bluetooth, if you do
		 * not support the exit program
		 */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
			finish();
		}
		/**
		 * Adapter Bluetooth, get a reference to the Bluetooth adapter (API),
		 * which must be above android4.3 or above.
		 */
		final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		mBluetoothAdapter = bluetoothManager.getAdapter();
		/**
		 * Check whether the device supports Bluetooth
		 */
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		if (!mScanning) {
			menu.findItem(R.id.menu_stop).setVisible(false);
			menu.findItem(R.id.menu_scan).setVisible(true);
			menu.findItem(R.id.menu_refresh).setActionView(null);
		} else {
			menu.findItem(R.id.menu_stop).setVisible(true);
			menu.findItem(R.id.menu_scan).setVisible(false);
			menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_scan:
			mLeDeviceListAdapter.clear();
			scanLeDevice(true);
			break;
		case R.id.menu_stop:
			scanLeDevice(false);
			break;
		}
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		/**
		 * In order to ensure that the device can be used in Bluetooth, if the
		 * current Bluetooth device is not enabled, the pop-up dialog box to the
		 * user to grant permissions to enable
		 */
		if (!mBluetoothAdapter.isEnabled()) {
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			}
		}

		// Initializes list view adapter.
		mLeDeviceListAdapter = new LeDeviceListAdapter();
		setListAdapter(mLeDeviceListAdapter);
		scanLeDevice(true);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		super.onPause();
		scanLeDevice(false);
		mLeDeviceListAdapter.clear();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final iBeacon device = mLeDeviceListAdapter.getDevice(position);
		if (device == null)
			return;
		if (!"pBeacon_n".equals(device.name)) {
			Toast.makeText(this, "Please enter the connection mode", Toast.LENGTH_SHORT).show();
			return;
		}
		final Intent intent = new Intent(this, DeviceControlActivity.class);
		intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name);
		intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.bluetoothAddress);
		// Bundle bundle = new Bundle();
		// bundle.putSerializable("iBeacon", device);
		// intent.putExtras(bundle);
		if (mScanning) {
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			mScanning = false;
		}
		startActivity(intent);
	}

	// ---------scan BLE-----------------------------------------
	private void scanLeDevice(final boolean enable) {
		if (enable) {
			// Stops scanning after a pre-defined scan period.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mScanning = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					invalidateOptionsMenu();
				}
			}, SCAN_PERIOD);

			mScanning = true;
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		} else {
			mScanning = false;
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
		invalidateOptionsMenu();
	}

	// ---------- Device scan callback-------------.
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			/**
			 * Package data into iBeacon
			 */
			final iBeacon ibeacon = iBeaconClass.fromScanData(device, rssi, scanRecord);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mLeDeviceListAdapter.addDevice(ibeacon);
					mLeDeviceListAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	// ----- Adapter for holding devices found through scanning.-------------
	private class LeDeviceListAdapter extends BaseAdapter {
		private ArrayList<iBeacon> mLeDevices;
		private LayoutInflater mInflator;

		public LeDeviceListAdapter() {
			super();
			mLeDevices = new ArrayList<iBeacon>();
			mInflator = DeviceScanActivity.this.getLayoutInflater();
		}

		public iBeacon getDevice(int position) {
			return mLeDevices.get(position);
		}

		public void clear() {
			mLeDevices.clear();
		}

		@Override
		public int getCount() {
			return mLeDevices.size();
		}

		@Override
		public Object getItem(int i) {
			return mLeDevices.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		/**
		 * Add data,And sort by RSSI
		 * 
		 * @param device
		 */
		public void addDevice(iBeacon device) {
			if (device == null)
				return;

			for (int i = 0; i < mLeDevices.size(); i++) {
				String btAddress = mLeDevices.get(i).bluetoothAddress;
				if (btAddress.equals(device.bluetoothAddress)) {
					mLeDevices.add(i + 1, device);
					mLeDevices.remove(i);
					return;
				}
			}
			mLeDevices.add(device);
			Collections.sort(mLeDevices, new Comparator<iBeacon>() {
				@Override
				public int compare(iBeacon h1, iBeacon h2) {
					return h2.rssi - h1.rssi;
				}
			});
		}

		@SuppressLint("InflateParams")
		@Override
		public View getView(int i, View view, ViewGroup viewGroup) {
			ViewHolder viewHolder;
			// General ListView optimization code.
			if (view == null) {
				view = mInflator.inflate(R.layout.listitem_device, null);
				viewHolder = new ViewHolder();
				viewHolder.txt_name = (TextView) view.findViewById(R.id.txt_name);
				viewHolder.txt_major = (TextView) view.findViewById(R.id.txt_major);
				viewHolder.txt_minor = (TextView) view.findViewById(R.id.txt_minor);
				viewHolder.txt_uuid = (TextView) view.findViewById(R.id.txt_uuid);
				viewHolder.txt_mac = (TextView) view.findViewById(R.id.txt_mac);
				viewHolder.txt_rssi = (TextView) view.findViewById(R.id.txt_rssi);
				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}
			iBeacon device = mLeDevices.get(i);
			final String deviceName = device.name;
			if (deviceName != null && deviceName.length() > 0)
				viewHolder.txt_name.setText("Name:" + device.name);
			else
				viewHolder.txt_name.setText("Name:" + R.string.unknown_device);

			viewHolder.txt_uuid.setText("UUID:" + device.proximityUuid);
			viewHolder.txt_major.setText("MajorId:" + device.major);
			viewHolder.txt_minor.setText("MinorId:" + device.minor);
			viewHolder.txt_rssi.setText("Rssi:" + device.rssi);
			viewHolder.txt_mac.setText("MAC:" + device.bluetoothAddress);

			return view;
		}
	}

	static class ViewHolder {
		TextView txt_name;
		TextView txt_major;
		TextView txt_minor;
		TextView txt_uuid;
		TextView txt_mac;
		TextView txt_rssi;

	}
}