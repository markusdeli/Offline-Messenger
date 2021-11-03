package com.example.offlinemessenger;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceManager extends AppCompatActivity {

    public static final String EXTRA_ADDRESS = "device_address";
    private static final String TAG = "DeviceListActivity";

    private Button mBackBtn, mScanBtn;
    private ListView mDeviceListView;

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> devicesArrayAdapter;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if (deviceName == null) {
                    return;
                }
                devicesArrayAdapter.add(deviceName + "\n" + device.getAddress());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (devicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    devicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_manager);

        mScanBtn = findViewById(R.id.scan_bttn);
        devicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        initDevicesListView();

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(receiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(receiver, filter);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void initDiscovery() {
        Log.d(TAG, "discovering");
        setProgressBarIndeterminateVisibility(true);
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    public void scanButtonClicked(View v) {
        initDiscovery();
    }

    private void initDevicesListView() {
        mDeviceListView = findViewById(R.id.devices_list_view);
        mDeviceListView.setAdapter(devicesArrayAdapter);
        mDeviceListView.setOnItemClickListener((adapterView, view, i, l) -> {
            mBluetoothAdapter.cancelDiscovery();

            String info = ((TextView) view).getText().toString();
            // TODO: Find another way than using a magic number
            String address = info.substring(info.length()-17);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, "Trying to pair with " + info);
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                device.createBond();
            }


            Intent intent = new Intent();
            intent.putExtra(EXTRA_ADDRESS, address);
            setResult(RESULT_OK, intent);
            finish();
        });
    }
}



