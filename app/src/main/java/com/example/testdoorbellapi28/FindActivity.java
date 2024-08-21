package com.example.testdoorbellapi28;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class FindActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final String DEVICE_ADDRESS = "08:3A:F2:BA:70:C2";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private TextView distanceTextView;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        distanceTextView = findViewById(R.id.distanceTextView);
        handler = new Handler();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (checkBluetoothPermissions()) {
            ensureBluetoothEnabled();
        } else {
            requestBluetoothPermissions();
        }
    }

    private void logRunningProcesses() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                Log.d("ActivityManager", "Process: " + processInfo.processName + " State: " + processInfo.importance);
            }
        }
    }

    private boolean checkBluetoothPermissions() {
        Log.d("FindActivity", "Checking Bluetooth permissions.");
        boolean hasBluetoothConnectPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        boolean hasBluetoothScanPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        boolean hasLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        Log.d("FindActivity", "Bluetooth Connect Permission: " + hasBluetoothConnectPermission);
        Log.d("FindActivity", "Bluetooth Scan Permission: " + hasBluetoothScanPermission);
        Log.d("FindActivity", "Location Permission: " + hasLocationPermission);

        return hasBluetoothConnectPermission && hasBluetoothScanPermission && hasLocationPermission;
    }


    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                REQUEST_BLUETOOTH_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Log.d("FindActivity", "All Bluetooth permissions granted.");
                ensureBluetoothEnabled();
            } else {
                Log.e("FindActivity", "Bluetooth permissions are required for this app to function.");
                Toast.makeText(this, "Bluetooth permissions are required for this app to function.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }



    private void ensureBluetoothEnabled() {
        Log.d("FindActivity", "Checking if Bluetooth is enabled.");

        logRunningProcesses();  // Logge laufende Prozesse zur Fehlersuche

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d("FindActivity", "Required permissions are not granted. Requesting permissions.");
            requestBluetoothPermissions();
            return;
        }

        if (bluetoothAdapter == null) {
            Log.e("FindActivity", "Bluetooth is not supported on this device.");
            Toast.makeText(this, "Bluetooth is not supported on this device.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.d("FindActivity", "Bluetooth is not enabled. Requesting to enable Bluetooth.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.d("FindActivity", "Bluetooth is enabled. Connecting to Bluetooth device.");
            connectToBluetoothDevice();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                connectToBluetoothDevice();
            } else {
                Toast.makeText(this, "Bluetooth is required for this app to function.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectToBluetoothDevice() {
        Log.d("FindActivity", "Attempting to connect to Bluetooth device with address: " + DEVICE_ADDRESS);

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
        if (device == null) {
            Log.e("FindActivity", "Bluetooth device not found.");
            Toast.makeText(this, "Bluetooth device not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Log.d("FindActivity", "Successfully connected to Bluetooth device.");
        } catch (IOException e) {
            Log.e("FindActivity", "Failed to connect to device: " + e.getMessage());
            Toast.makeText(this, "Failed to connect to device: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        sendBluetoothCommand("start");
    }

    @Override
    protected void onPause() {
        super.onPause();
        sendBluetoothCommand("stop");
    }


    private void sendBluetoothCommand(String command) {
        if (outputStream != null) {
            try {
                outputStream.write((command + "\n").getBytes());
            } catch (IOException e) {
                Log.e("FindActivity", "Failed to send command: " + e.getMessage());
                Toast.makeText(this, "Failed to send command: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Bluetooth connection is not established.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            Log.e("FindActivity", "Error closing resources: " + e.getMessage());
        }
    }

}
