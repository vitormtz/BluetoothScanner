package com.example.bluetoothscanner;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_LOCATION = 2;
    private static final int REQUEST_PERMISSION_BLUETOOTH = 3;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothAdapter bluetoothAdapter;
    private boolean isScanning = false;
    private Handler handler = new Handler();
    private Button scanButton;
    private TextView statusTextView;
    private ListView deviceListView;
    private List<BluetoothDeviceInfo> deviceList;
    private DeviceListAdapter deviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scanButton = findViewById(R.id.scanButton);
        statusTextView = findViewById(R.id.statusTextView);
        deviceListView = findViewById(R.id.deviceListView);

        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceListAdapter(this, deviceList);
        deviceListView.setAdapter(deviceAdapter);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Este dispositivo não suporta Bluetooth", Toast.LENGTH_LONG).show();
            statusTextView.setText("Status: Bluetooth não suportado");
            scanButton.setEnabled(false);
            return;
        }

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndStartScan();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBluetoothStatus();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    private void updateBluetoothStatus() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                statusTextView.setText("Status: Bluetooth ativado");
                scanButton.setEnabled(true);
            } else {
                statusTextView.setText("Status: Bluetooth desativado");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (checkBluetoothPermission()) {
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    }

    private boolean checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                        },
                        REQUEST_PERMISSION_BLUETOOTH);
                return false;
            }
        }
        return true;
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);
            return false;
        }
        return true;
    }

    private void checkPermissionsAndStartScan() {
        if (checkBluetoothPermission() && checkLocationPermission()) {
            startBluetoothScan();
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startBluetoothScan() {
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();

        if (!bluetoothAdapter.isEnabled()) {
            statusTextView.setText("Status: Bluetooth desativado");
            return;
        }

        if (isScanning) {
            isScanning = false;
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            scanButton.setText("Iniciar Busca");
            statusTextView.setText("Status: Escaneamento parado");
        } else {
            statusTextView.setText("Status: Escaneando...");
            scanButton.setText("Parar Busca");
            isScanning = true;

            bluetoothAdapter.startDiscovery();

            handler.postDelayed(new Runnable() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
                @Override
                public void run() {
                    if (isScanning) {
                        isScanning = false;
                        bluetoothAdapter.cancelDiscovery();
                        scanButton.setText("Iniciar Busca");
                        statusTextView.setText("Status: Escaneamento concluído");
                    }
                }
            }, SCAN_PERIOD);
        }
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                String deviceName = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT)
                            == PackageManager.PERMISSION_GRANTED) {
                        deviceName = device.getName();
                    }
                } else {
                    deviceName = device.getName();
                }

                BluetoothDeviceInfo deviceInfo = new BluetoothDeviceInfo(deviceName, rssi);

                boolean deviceExists = false;
                for (int i = 0; i < deviceList.size(); i++) {
                    BluetoothDeviceInfo existingDevice = deviceList.get(i);
                    if (existingDevice.equals(deviceInfo)) {
                        existingDevice.setSignalStrength(rssi);
                        deviceExists = true;
                        deviceAdapter.notifyDataSetChanged();
                        break;
                    }
                }

                if (!deviceExists) {
                    deviceList.add(deviceInfo);
                    deviceAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                statusTextView.setText("Status: Escaneamento iniciado...");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (isScanning) {
                    bluetoothAdapter.startDiscovery();
                } else {
                    scanButton.setText("Iniciar Busca");
                    statusTextView.setText("Status: Escaneamento concluído");
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissionsAndStartScan();
                } else {
                    Toast.makeText(this, "Permissão de localização necessária para escaneamento Bluetooth", Toast.LENGTH_LONG).show();
                }
                break;

            case REQUEST_PERMISSION_BLUETOOTH:
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    updateBluetoothStatus();
                } else {
                    Toast.makeText(this, "Permissões de Bluetooth necessárias", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                statusTextView.setText("Status: Bluetooth ativado");
            } else {
                statusTextView.setText("Status: Bluetooth desativado");
                Toast.makeText(this, "O Bluetooth precisa estar ativado para usar este aplicativo", Toast.LENGTH_LONG).show();
            }
        }
    }
}