package org.raspen.basicbluetoothreader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    List<String> deviceNames = new ArrayList<>();
    final List<String> devices = new ArrayList<>();

    String selectedDeviceAddress;
    BluetoothSocket socket;

    boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mSelectButton = (Button) findViewById(R.id.select_device_button);
        mSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSelectDialog();
            }
        });

        Button mConnectButton = (Button) findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
                if (socket.isConnected()) {
                    initOBD();

                    RPMCommand command = new RPMCommand();
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            command.run(socket.getInputStream(), socket.getOutputStream());
                            Log.d(TAG, "RPM: " + command.getFormattedResult());
                        } catch (IOException | InterruptedException e) {
                            Log.e(TAG, "Failed to run command.");
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private void connect() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice  device  = adapter.getRemoteDevice(selectedDeviceAddress);

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();
            Log.d(TAG, "Connected to Bluetooth device.");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Could not connect to Bluetooth device.");
        }
    }

    private void initOBD() {
        if (socket.isConnected()) {
            try {
                new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
                new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(),
                        socket.getOutputStream());

                isInitialized = true;
                Log.d(TAG, "Intialized OBD.");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "Could not initialize OBD.");
            }
        }
    }

    private void showSelectDialog() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceNames.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }
        } else {
            showToast("No paired devices found.");
        }

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice,
                deviceNames.toArray(new String[deviceNames.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                selectedDeviceAddress = devices.get(position);

                TextView selectedDevice = (TextView) findViewById(R.id.selected_device);
                selectedDevice.setText(getResources().getString(R.string.selected_device,
                        selectedDeviceAddress));
            }
        });

        alertDialog.setTitle("Select a device");
        alertDialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
