package hk.ust.cse.fyp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;




/*
    Appear as Dialog. It lists any paired devices
    and devices detected in the area after discovery.
    When device is chosen by user, the MAC address of the device is sent back to
    parent Activity in the result Intent
 */

public class DeviceActivity extends Activity {

    private static final String TAG ="DeviceListActivity";

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothAdapter mAdapter;

    private ArrayAdapter<String> mNewDeviceArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up window feature
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device_list);

        //set result canceled in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        //Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doDiscovery();
                view.setVisibility(View.GONE);
            }
        });




        //Initialize array adapter, one for already paired, one for newly discovered
        ArrayAdapter<String> pairedDeviceArrayAdapter = new ArrayAdapter<String>(this,R.layout.device_name);
        mNewDeviceArrayAdapter = new ArrayAdapter<String>(this,R.layout.device_name);

        //Find and set up ListView for paired device
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDeviceArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);


        //Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDeviceArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        //Register for broadcasts when device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        //register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver,filter);

        //get local bluetooth adapter
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();

        if(pairedDevices.size() > 0){
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            pairedDeviceArrayAdapter.add(noDevices);
        }




    }

    private void doDiscovery() {
        Log.d(TAG,"do discovery()");

        //Indicate scanning in title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);


        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        //stop if already discovering
        if(mAdapter.isDiscovering()){
            mAdapter.cancelDiscovery();
        }

        //request discover
        mAdapter.startDiscovery();

    }

    //Listen for all devices in the ListViews
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View v, int i, long l) {
            //cancel discovery before connect
            mAdapter.cancelDiscovery();

            //Get device MAC address, the last 17 char in view
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            //create result intent
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS,address);

            //set result and finish the activity
            setResult(Activity.RESULT_OK,intent);
            finish();

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mAdapter != null){
            mAdapter.cancelDiscovery();
        }

        this.unregisterReceiver(mReceiver);
    }

    //Listen for discovered devices and change the tile when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG,BluetoothDevice.ACTION_FOUND);
            Log.v(TAG,BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            Log.v(TAG,action);

            //when find new device
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                //get bluetooth device object from the Intent
                Log.d(TAG,"new device found");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // if already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDeviceArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
            //when discovery finished change the title
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                setProgressBarVisibility(false);
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if(mNewDeviceArrayAdapter.getCount() == 0){
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDeviceArrayAdapter.add(noDevices);
                }
            }

        }
    };

}
