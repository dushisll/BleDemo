package com.dushisll.bledemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import dushisll.blelibrary.BleGattCharacteristic;
import dushisll.blelibrary.BleGattService;
import dushisll.blelibrary.BleService;
import dushisll.blelibrary.IBle;

/**
 * Created by we on 2016/12/15.
 */

public class DeviceControlActivity  extends Activity {
    private final static String TAG = DeviceControlActivity.class
            .getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private ArrayList<ArrayList<BleGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BleGattCharacteristic>>();
    private boolean mConnected = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    protected IBle mBle;

    private final BroadcastReceiver mBleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            if (!mDeviceAddress.equals(extras.getString(BleService.EXTRA_ADDR))) {
                return;
            }

            String action = intent.getAction();
            Log.i(TAG, "mBleReceiver: "+action);
            if (BleService.BLE_GATT_CONNECTED.equals(action)) {

                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();

            } else if (BleService.BLE_GATT_DISCONNECTED.equals(action)) {
                onDeviceDisconnected();
            } else if (BleService.BLE_SERVICE_DISCOVERED.equals(action)) {
                displayGattServices(mBle.getServices(mDeviceAddress));
//				BleGattService service = mBle.getService(mDeviceAddress,UUID.fromString("0000ffb0-0000-1000-8000-00805f9b34fb"));
//				if(service == null){
//					return;
//				}
//				mCharacteristic = service.getCharacteristic(UUID.fromString("0000FFB5-0000-1000-8000-00805F9B34FB"));
//				isread = true;
//				myThread t = new myThread();
//				t.start();
            }else if(BleService.BLE_CHARACTERISTIC_READ.equals(action)){
//				byte[] val = extras.getByteArray(BleService.EXTRA_VALUE);
//				System.out.println(new String(val));
//				System.out.println(new String(Hex.encodeHex(val)));

            }else if(BleService.BLE_REQUEST_FAILED.equals(action)){

            }
        }
    };


    private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v,
                                    int groupPosition, int childPosition, long id) {
            Log.d(TAG, "onChildClick " + groupPosition + " " + childPosition);
            if (mGattCharacteristics != null) {
                final BleGattCharacteristic characteristic = mGattCharacteristics
                        .get(groupPosition).get(childPosition);
                Intent intent = new Intent(DeviceControlActivity.this,
                        CharacteristicActivity.class);
                intent.putExtra("address", mDeviceAddress);
                Log.d(TAG, "service size " + mBle.getServices(mDeviceAddress).size());
                intent.putExtra("service", mBle.getServices(mDeviceAddress)
                        .get(groupPosition).getUuid().toString());
                intent.putExtra("characteristic", characteristic.getUuid()
                        .toString().toUpperCase());
                intent.putExtra("deviceName", mDeviceName);
                startActivity(intent);
                return true;
            }
            return false;
        }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        Log.e(TAG, "mDeviceAddress="+mDeviceAddress);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        BleApplication app = (BleApplication) getApplication();
        mBle = app.getIBle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBleReceiver, BleService.getIntentFilter());
        ArrayList<BleGattService> services = mBle.getServices(mDeviceAddress);
        if (services == null || services.size() == 0) {
            onDeviceDisconnected();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBleReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBle != null) {
            mBle.disconnect(mDeviceAddress);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBle.requestConnect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBle.disconnect(mDeviceAddress);
                onDeviceDisconnected();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    // 演示如何遍历支持关贸总协定服务/特性。在此示例中，我们填充绑定到UI上的ExpandableListView的数据结构。
    private void displayGattServices(List<BleGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        String unknownServiceString = getResources().getString(
                R.string.unknown_service);
        String unknownCharaString = getResources().getString(
                R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BleGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BleGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString().toUpperCase();
            currentServiceData.put(LIST_NAME, Utils.BLE_SERVICES
                    .containsKey(uuid) ? Utils.BLE_SERVICES.get(uuid)
                    : unknownServiceString);
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BleGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();
            ArrayList<BleGattCharacteristic> charas = new ArrayList<BleGattCharacteristic>();

            // Loops through available Characteristics.
            for (BleGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString().toUpperCase();
                currentCharaData
                        .put(LIST_NAME,
                                Utils.BLE_CHARACTERISTICS.containsKey(uuid) ? Utils.BLE_CHARACTERISTICS
                                        .get(uuid) : unknownCharaString);
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this, gattServiceData,
                android.R.layout.simple_expandable_list_item_2, new String[] {
                LIST_NAME, LIST_UUID }, new int[] { android.R.id.text1,
                android.R.id.text2 }, gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2, new String[] {
                LIST_NAME, LIST_UUID }, new int[] { android.R.id.text1,
                android.R.id.text2 });
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private void onDeviceDisconnected() {
        mConnected = false;
        updateConnectionState(R.string.disconnected);
        invalidateOptionsMenu();
        clearUI();
    }
}
