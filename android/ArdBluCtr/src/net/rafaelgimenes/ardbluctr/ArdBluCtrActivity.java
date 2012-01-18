package net.rafaelgimenes.ardbluctr;

import net.rafaelgimenes.ardbluctr.BluetoothRfcommClient;
import net.rafaelgimenes.ardbluctr.DeviceListActivity;
import net.rafaelgimenes.ardbluctr.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class ArdBluCtrActivity extends Activity implements  Button.OnClickListener{
	
	// Message types sent from the BluetoothRfcommClient Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    String items[] = new String[100];
	// Key names received from the BluetoothRfcommClient Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    // receive data 
    
    // Layout Views
    private TextView mBTStatus;
    private Button mConnectButton;
    private ToggleButton run_buton;
    private Spinner spin;
    private EditText recebido;
  //  public WaveformView mWaveform = null;
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the RFCOMM services
    private BluetoothRfcommClient mRfcommClient = null;
    
    static byte timebase_index = 5;
    static byte ch1_index = 4, ch2_index = 5;
    static byte ch1_pos = 24, ch2_pos = 17;	// 0 to 40
    
    // stay awake
  //  protected PowerManager.WakeLock mWakeLock;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);        
        setContentView(R.layout.main);
        
        // combo
        spin = (Spinner) findViewById(R.id.spinner1);
        spin.setOnItemSelectedListener( new CustomSelectListner());
        for (int i = 0; i < 98; i++) {
			items[i] = ""+i;
		}
        
        ArrayAdapter<String> aa = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,items);
        aa.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spin.setAdapter(aa);
       
        //editDataRecebide
        recebido = (EditText) findViewById(R.id.editText1);
        
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
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
    	// Otherwise, setup the Oscillosope session
    	else{
    		if (mRfcommClient == null) setupScreen();
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
            if (mRfcommClient.getState() == BluetoothRfcommClient.STATE_NONE) {
              // Start the Bluetooth  RFCOMM services
              mRfcommClient.start();
            }
        }
    }
    
    @Override
    public void  onClick(View v){
    	int buttonID;
    	buttonID = v.getId();
    	switch (buttonID){
    	case R.id.tbtn_runtoggle :
    		if(run_buton.isChecked()){
    			//mBTStatus.setText(+""); 
    			sendMessage("!ON"+spin.getSelectedItem()+"&");
    		}else{
    			sendMessage("!OF"+spin.getSelectedItem()+"&");
    		}
    		break;
    	}
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	// Stop the Bluetooth RFCOMM services
        if (mRfcommClient != null) mRfcommClient.stop();
      
    }
        
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message){
    	// Check that we're actually connected before trying anything
    	if (mRfcommClient.getState() != BluetoothRfcommClient.STATE_CONNECTED) {
    		Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
    		mConnectButton.setText("Connect");
    		return;
    	}else{
    		mConnectButton.setText("Discconect");
    	}
    	// Check that there's actually something to send
    	if (message.length() > 0) {
    		// Get the message bytes and tell the BluetoothRfcommClient to write
    		byte[] send = message.getBytes();
    		mRfcommClient.write(send);
    	}
    }
    
    private void setupScreen(){
    	
    	mBTStatus = (TextView) findViewById(R.id.txt_status);
    	
    	mConnectButton = (Button) findViewById(R.id.button_connect);
    	mConnectButton.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				BTConnect();
				if (mRfcommClient.getState() ==  BluetoothRfcommClient.STATE_CONNECTED)
					mConnectButton.setText("Discconect");
			}    		
    	});
    	
        
        run_buton = (ToggleButton) findViewById(R.id.tbtn_runtoggle);
        run_buton.setOnClickListener(this);
        
    	// Initialize the BluetoothRfcommClient to perform bluetooth connections
        mRfcommClient = new BluetoothRfcommClient(this, mHandler);
        
    }
    
    private void BTConnect(){
    	Intent serverIntent = new Intent(this, DeviceListActivity.class);
    	startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }
  
    
    // The Handler that gets information back from the BluetoothRfcommClient
    private final Handler mHandler = new Handler(){
    	@Override
        public void handleMessage(Message msg){
    		switch (msg.what){
    		case MESSAGE_STATE_CHANGE:
    			switch (msg.arg1){
    			case BluetoothRfcommClient.STATE_CONNECTED:
    				mBTStatus.setText(R.string.title_connected_to);
                    mBTStatus.append("\n" + mConnectedDeviceName);
    				break;
    			case BluetoothRfcommClient.STATE_CONNECTING:
    				mBTStatus.setText(R.string.title_connecting);
    				break;
    			case BluetoothRfcommClient.STATE_NONE:
    				mBTStatus.setText(R.string.title_not_connected);
    				break;
    			}
    			break;
    		case MESSAGE_READ: //recebendo mensagem
    			 byte[] readBuf = (byte[]) msg.obj;
                 // construct a string from the valid bytes in the buffer
                 String readMessage = new String(readBuf, 0, msg.arg1);
                 recebido.setText(readMessage);
                 if(readMessage.length()>4){
	                 if (readMessage.subSequence(1, 3).equals("ON")){
	                	 run_buton.setChecked(true);
	                 }else if (readMessage.subSequence(1, 3).equals("OF")){
	                	 run_buton.setChecked(false);
	                 }
                 }
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
    
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	switch (requestCode) {
    	case REQUEST_CONNECT_DEVICE:
    		// When DeviceListActivity returns with a device to connect
    		if (resultCode == Activity.RESULT_OK){
    			// Get the device MAC address
    			String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
    			// Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mRfcommClient.connect(device);
    		}
    		break;
    	case REQUEST_ENABLE_BT:
    		// When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK){
            	// Bluetooth is now enabled, so set up the oscilloscope
            	setupScreen();
            }else{
            	// User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
    		break;
    	}
    }
}
