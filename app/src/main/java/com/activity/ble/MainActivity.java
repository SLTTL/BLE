package com.activity.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /* TAG for Log */
    private final static String TAG = "MainActivity";

    /* Request code for activating bluetooth */
    private final static int REQUEST_ENABLE_BT = 0;

    /* Request code for ACESS_CORASE_LOCATION Permission */
    private final static int REQUEST_COARSE_LOCATION = 1;

    /* Request code for LOCATION Services */
    private final static int REQUEST_LOCATION_SERVICE = 2;

    /* set scan period */
    private final static int SCAN_PERIOD = 1000;


    /* flag used for LE scanning */
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothAdapter mBtAdapter;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mSettings;
    private List<ScanFilter> mFilters;
    private BluetoothGatt mBtGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final BluetoothManager bluetoothManager = (BluetoothManager)
                getSystemService(Context.BLUETOOTH_SERVICE);

        mHandler = new Handler();
        mBtAdapter = bluetoothManager.getAdapter();
        mLeScanner = mBtAdapter.getBluetoothLeScanner();
        mSettings = new ScanSettings.Builder().setScanMode
                (ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        mFilters = new ArrayList<ScanFilter>();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                            REQUEST_COARSE_LOCATION);
        if (!isLocationEnabled(this)){
            Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            this.startActivityForResult(enableLocationIntent, REQUEST_LOCATION_SERVICE);

        }


        Button scanButton = (Button)findViewById(R.id.scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* check if null */
                if (mLeScanner != null){
                    mLeScanner.startScan(mScanCallback);

                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permission[], int[] grantResults){

        switch(requestCode){
            case REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i(TAG, "onRequestPermissionResult: Permission granted");
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (mBtAdapter != null && mBtAdapter.isEnabled()){
            startScanning(false);
        }
    }

    @Override
    protected void onDestroy(){
        if (mBtGatt != null) {
           mBtGatt.close();
            mBtGatt = null;
        }
        /* must call on destroy at end, using broadcast receivers?? */
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_CANCELED){
                    finish();
                }
                else{
                    mLeScanner = mBtAdapter.getBluetoothLeScanner();
                }
                break;
            case REQUEST_COARSE_LOCATION:
                if (resultCode == Activity.RESULT_CANCELED || !isLocationEnabled(this)){
                    finish();
                }
                break;
        }
//        if (requestCode == REQUEST_ENABLE_BT){
//            if (resultCode == Activity.RESULT_CANCELED){
//                finish();
//            }else{
//                mLeScanner = mBtAdapter.getBluetoothLeScanner();
//            }
//        }
    }

    private void startScanning(final boolean enable){
        if (enable){
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mLeScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mLeScanner.startScan(mScanCallback);
        } else{
            mLeScanner.stopScan(mScanCallback);
            mScanning = false;
        }

    }

    private ScanCallback mScanCallback = new ScanCallback() {
        final static String TAG = "mScanDoneCallback";
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i(TAG, String.valueOf(callbackType));
            Log.i(TAG, "result: " + result.toString());

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.i(TAG, "onBatchResult: ");
            for (ScanResult sr: results){
                Log.i(TAG, sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.i(TAG, "scan failed" + errorCode);
        }
    };

    /* Copied from: http://stackoverflow.com/questions/10311834/how-to-check-if-location-services-are-enabled */
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }



}
