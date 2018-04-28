package hk.ust.cse.fyp;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class BluetoothFragment extends Fragment {
    private static final String TAG = "BluetoothFragment";


    //constant
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    private BluetoothAdapter mAdapter;
    private Bluetooth mBluetooth = null;
    private ArrayAdapter<String> mArrayAdapter = null;
    private String mConnectedDeviceName;

    private ListView mConversationView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //get local adapter
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        //check if the device support bluetooth
        if(mAdapter == null){
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();

        }

    }

    @Override
    public void onStart() {
        super.onStart();


        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

            // Otherwise, setup the chat session
        } else if (mBluetooth == null) {
            setupBluetooth();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mBluetooth != null){
            if(mBluetooth.getState() == Bluetooth.STATE_NONE){
                mBluetooth.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mBluetooth != null){
            mBluetooth.stop();
        }
    }

    private void setupBluetooth() {
        Log.d(TAG,"setupChat()");


        mArrayAdapter = new ArrayAdapter<String>(getActivity(),R.layout.message);
        mConversationView.setAdapter(mArrayAdapter);
        mBluetooth = new Bluetooth(getActivity(),mHandler);


    }


    //Handler that gets information back from bluetooth
    private final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Bluetooth.STATE_CONNECTED:

                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));

                            mArrayAdapter.clear();
                            break;
                        case Bluetooth.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case Bluetooth.STATE_LISTEN:
                        case Bluetooth.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mArrayAdapter.add("Me:  " + writeMessage);
                    break;
                //GET the result
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    mArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupBluetooth();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), "Bluetooth not enabled",
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }


    /*---------------------MENU------------------------------- */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.secure_connect_scan: {
                // Launch the DeviceActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                Log.v(TAG,"connect secure");
                return true;
            }
            case R.id.insecure_connect_scan:{
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                Log.v(TAG,"connect insecure");
                return true;

            }
            case R.id.discoverable:{
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }


    /*--------------Function---------------------------------*/
    // make the device discoverable for 300 seconds (5 minutes)
    private void ensureDiscoverable() {
        if (mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    //Updates the status on the action bar
    //@param resId a string resource ID
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    //Updates the status on the action bar
    // @param subTitle status
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    //Establish connection with other device
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceActivity.EXTRA_DEVICE_ADDRESS);
        Log.v(TAG,"address: "+address);
        // Get the BluetoothDevice object
        BluetoothDevice device = mAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBluetooth.connect(device, secure);
    }
}
