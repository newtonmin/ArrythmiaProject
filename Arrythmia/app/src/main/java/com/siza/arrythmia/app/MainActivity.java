package com.siza.arrythmia.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    // Address of the connected device
    private String mConnectedAddress = null;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the RFCOMM services
    private BluetoothRfCommClient mRfcommClient = null;

    // Message types sent from the BluetoothRfcommClient Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothRfcommClient Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Layout Views
    private TextView conStatus;
    private TextView btAddress;
    private TextView debugMessages;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            //return;
        }
    }

    @Override
    public void onStart(){
        super.onStart();

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        //Otherwise, setup the Oscillosope session
        else{
            if (mRfcommClient == null) setup();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id)      //Checking which menu pressed
        {
            case R.id.action_connect:

                return true;
            case R.id.action_device:
                bluetoothDevices();
                return true;
            case R.id.action_settings:

                return true;
            case R.id.action_help:

                return true;
            case R.id.action_about:

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK){
            switch (requestCode) {
                case REQUEST_CONNECT_DEVICE:
                    // When DeviceListActivity returns with a device to connect
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Toast.makeText(this, address, Toast.LENGTH_LONG).show();
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mRfcommClient.connect(device);
                    mConnectedAddress = address;
                    break;
                case REQUEST_ENABLE_BT:
                    // When the request to enable Bluetooth returns
                    setup();
                    break;
            }
        }else{
            // User did not enable Bluetooth or an error occured
            Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        // Stop the Bluetooth RFCOMM services
        if (mRfcommClient != null) mRfcommClient.stop();
    }

    private void bluetoothDevices(){
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message){
        // Check that we're actually connected before trying anything
        if (mRfcommClient.getState() != BluetoothRfCommClient.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothRfcommClient to write
            byte[] send = message.getBytes();
            mRfcommClient.write(send);
        }
    }

    private void setup(){
        conStatus = (TextView) findViewById(R.id.tvConState);
        btAddress = (TextView) findViewById(R.id.tvAddress);
        debugMessages = (TextView)findViewById(R.id.tvDebug);
        sendButton = (Button)findViewById(R.id.bSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage("Test");
            }
        });

        // Initialize the BluetoothRfcommClient to perform bluetooth connections
        mRfcommClient = new BluetoothRfCommClient(this, mHandler);
    }

    // The Handler that gets information back from the BluetoothRfcommClient
    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1){
                        case BluetoothRfCommClient.STATE_CONNECTED:
                            conStatus.setText("Connected");
                            btAddress.setText(mConnectedAddress);
                            debugMessages.append("\n" + mConnectedDeviceName + "Connected");
                            break;
                        case BluetoothRfCommClient.STATE_CONNECTING:
                            debugMessages.append("\n" + "Connecting...");
                            break;
                        case BluetoothRfCommClient.STATE_NONE:
                            conStatus.setText("Not Connected");
                            debugMessages.append("\n" + "Disconnected");
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    debugMessages.append("\n" + msg);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
        // signed to unsigned
        private int UByte(byte b){
            if(b<0) // if negative
                return (int)( (b&0x7F) + 128 );
            else
                return (int)b;
        }
    };
}
