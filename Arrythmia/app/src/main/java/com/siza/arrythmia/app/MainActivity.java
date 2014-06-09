package com.siza.arrythmia.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends ActionBarActivity {

    // Message types sent from the BluetoothRfcommClient Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothRfcommClient Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // bt-uart constants
    private static final int MAX_SAMPLES = 1080;
    private static final int  MAX_LEVEL	= 240;
    private static final int  DATA_START = (MAX_LEVEL + 1);
    private static final int  DATA_END = (MAX_LEVEL + 2);

    private static final byte  REQ_DATA = 0x00;
    private static final byte  ADJ_HORIZONTAL = 0x01;
    private static final byte  ADJ_VERTICAL = 0x02;
    private static final byte  ADJ_POSITION = 0x03;

    private static final byte  CHANNEL1 = 0x01;
    private static final byte  CHANNEL2 = 0x02;

    // Run/Pause status
    private boolean bReady = false;
    // receive data
    private int[] ch1_data = new int[MAX_SAMPLES];
    //private int[] ch2_data = new int[MAX_SAMPLES/2];
    private int dataIndex=0, dataIndex1=0, dataIndex2=0;
    private boolean bDataAvailable=false;

    // Time delay parameters
    public static long timeStart;
    public static long timeEnd;


    public SignalView mWaveform = null;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    private String mConnectedAddress = null;

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the RFCOMM services
    private BluetoothRfCommClient mRfcommClient = null;

    static String[] timebase = {"5us", "10us", "20us", "50us", "100us", "200us", "500us", "1ms", "2ms", "5ms", "10ms", "20ms", "50ms" };
    static String[] ampscale = {"10mV", "20mV", "50mV", "100mV", "200mV", "500mV", "1V", "2V", "GND"};
    static byte timebase_index = 5;
    static byte ch1_index = 4, ch2_index = 5;
    static byte ch1_pos = 24, ch2_pos = 17;	// 0 to 40

    // Layout Views
    private TextView conStatus;
    private TextView localAddress;
    private TextView remoteAddress;
    private TextView debugMessages;
    private Button sendButton;
    private ToggleButton runButton;
    private RadioGroup rbGroup;
    private Button upButton;
    private Button downButton;
//    private RadioButton radioButtonRealTime;
//    private RadioButton radioButtonAlert;

    // stay awake
    protected PowerManager.WakeLock mWakeLock;


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

        // Prevent phone from sleeping
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
        rbGroup = (RadioGroup) findViewById(R.id.radioGroup);
//        radioButtonRealTime = (RadioButton) findViewById(R.id.rbRealtime);
//        radioButtonAlert = (RadioButton) findViewById(R.id.rbAlert);
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
    public synchronized void onResume(){
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mRfcommClient != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mRfcommClient.getState() == BluetoothRfCommClient.STATE_NONE) {
                // Start the Bluetooth  RFCOMM services
                mRfcommClient.start();
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        // Stop the Bluetooth RFCOMM services
        if (mRfcommClient != null) mRfcommClient.stop();
        // release screen being on
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
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
                    timeEnd = System.currentTimeMillis();
                    long totalTime = timeEnd - timeStart;
                    Toast.makeText(this, Long.toString(totalTime), Toast.LENGTH_LONG).show();
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
        localAddress = (TextView) findViewById(R.id.localAddress);
        remoteAddress = (TextView)findViewById(R.id.remoteAddress);

//        debugMessages = (TextView)findViewById(R.id.tvDebug);

        // Send button
        sendButton = (Button)findViewById(R.id.bSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendMessage("r");
                //sendMessage(new String(new byte[] {REQ_DATA}));
            }
        });

        // Clear button
        sendButton = (Button)findViewById(R.id.bClear);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //debugMessages.setText("");
                sendMessage("w");
            }
        });

        // Up button
        upButton = (Button)findViewById(R.id.btUp);
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //debugMessages.setText("");
                sendMessage("u");
            }
        });

        // Down button
        downButton = (Button)findViewById(R.id.btDown);
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //debugMessages.setText("");
                sendMessage("d");
            }
        });

        runButton = (ToggleButton) findViewById(R.id.btRun);
        runButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                switch (rbGroup.getCheckedRadioButtonId())
                {
                    case R.id.rbAlert:
                        if (runButton.isChecked()) {
                            sendMessage("R");
                            bReady = true;
                        } else
                            bReady = false;
                        break;
                    case R.id.rbRealtime:
                        if (runButton.isChecked()) {
                            sendMessage("r");
                            bReady = true;
                        } else
                            bReady = false;
                        break;
                }
            }
        });

        // Initialize the BluetoothRfcommClient to perform bluetooth connections
        mRfcommClient = new BluetoothRfCommClient(this, mHandler);

        // waveform / plot area
        mWaveform = (SignalView)findViewById(R.id.signalViewArea);
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
                            remoteAddress.setText(mConnectedAddress);
                            localAddress.setText(mBluetoothAdapter.getAddress());
                            //debugMessages.append("\n" + mConnectedDeviceName + "Connected");
                            break;
                        case BluetoothRfCommClient.STATE_CONNECTING:
                            //debugMessages.append("\n" + "Connecting...");
                            break;
                        case BluetoothRfCommClient.STATE_NONE:
                            conStatus.setText("Not Connected");
                            remoteAddress.setText("");
                            localAddress.setText("");
                            //debugMessages.append("\n" + "Disconnected");
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    int data_length = msg.arg1;
                    for(int x=0; x<data_length; x++){
                        int raw = UByte(readBuf[x]);
                        if( raw>MAX_LEVEL ){
                            if( raw==DATA_START ){
                                bDataAvailable = true;
                                dataIndex = 0; dataIndex1=0; dataIndex2=0;
                            }
                            else if( (raw==DATA_END) || (dataIndex>=MAX_SAMPLES) ){
                                bDataAvailable = false;
                                dataIndex = 0; dataIndex1=0; dataIndex2=0;
                                mWaveform.set_data(ch1_data);
                                if(bReady){ // send "REQ_DATA" again
                                    //MainActivity.this.sendMessage( new String(new byte[] {REQ_DATA}) );
                                    MainActivity.this.sendMessage("r");
                                }
                                //break;
                            }
                        }
                        else if( (bDataAvailable) && (dataIndex<MAX_SAMPLES) ){ // valid data
//                            if((dataIndex++)%2==0) ch1_data[dataIndex1++] = raw;	// even data
//                            else ch2_data[dataIndex2++] = raw;	// odd data
                            ch1_data[dataIndex1++] = raw;
                            //Log.d("Main", String.valueOf(dataIndex1));
                        }
                        else{
                            bDataAvailable = true;
                            dataIndex = 0; dataIndex1=0; dataIndex2=0;
                        }
                    }
//                    String strIncom = new String(readBuf, 0, msg.arg1);                 // create string from bytes array
//                    debugMessages.append(strIncom);                                     // append string
//                    int endOfLineIndex = sb.indexOf("\r\n");                            // determine the end-of-line
//                    if (endOfLineIndex > 0) {                                            // if end-of-line,
//                        String sbprint = sb.substring(0, endOfLineIndex);               // extract string
//                        sb.delete(0, sb.length());                                      // and clear
//                        txtArduino.setText("Data from Arduino: " + sbprint);            // update TextView
//                        btnOff.setEnabled(true);
//                        btnOn.setEnabled(true);
//                    }

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
