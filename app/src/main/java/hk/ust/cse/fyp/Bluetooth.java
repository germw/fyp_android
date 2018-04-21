package hk.ust.cse.fyp;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {

    //Debugging
    private static final String TAG = "BluetoothService";
    private static final boolean D = true;

    //name for the SDP record when creating server socket
    private static final String NAME = "Bluetooth";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Activity mActivity;

    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private BluetoothSocket BTsocket;

    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private boolean BTconnected;
    private boolean BTthreadStop;

    //initialize constant
    private final static int REQUEST_ENABLE_BT = 1; //any number greater than one


    //Constructor
    public Bluetooth(MainActivity mainActivity, Handler mHandler) {
        mActivity = mainActivity;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mHandler = mHandler;
    }

    //check the connection of Bluetooth by adapter
    public void connect(){
        if(mAdapter == null){
            Toast.makeText(mActivity.getApplicationContext(),"Device NOT support bluetooth",Toast.LENGTH_LONG).show();
        }
        else{
            //querying paired device,
            final Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
            final List<String> deviceList = new ArrayList<String>();


            if(pairedDevices.size() > 0){
                // There are paired devices. Get the name and address of each paired device.
                for(BluetoothDevice device : pairedDevices){
                    deviceList.add(device.getName());
                    Log.d("devicelist",device.getName()+"  " + device.getAddress());
                }
            }

            //set up the dialog display list
            String [] showList = new String[deviceList.size()];
            for(int i = 0 ; i < deviceList.size(); i++ ){
                showList[i] = deviceList.get(i);

            }


            //select the bluetooth device
            AlertDialog.Builder connectionDialog = new AlertDialog.Builder(mActivity);
            DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int select) {
                    for(BluetoothDevice device : pairedDevices){
                        if(device.getName().equals(deviceList.get(select))){
                            String address = device.getAddress();
                            Log.d("devicelist","address: " + address);
                            BluetoothDevice BTdevice = mAdapter.getRemoteDevice(address);
                            try {
                                try{
                                    BTsocket = BTdevice.createRfcommSocketToServiceRecord(MY_UUID);
                                }catch (IOException e){
                                    Toast.makeText(mActivity,"Wrong UUID", Toast.LENGTH_SHORT).show();
                                }

                                try{
                                    BTsocket.connect();
                                    Log.d("devicelist","could not connect");
                                }catch (IOException e) {
                                    try {
                                        BTsocket =(BluetoothSocket) BTsocket.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(BTdevice,1);
                                        BTsocket.connect();
                                    } catch (IllegalAccessException e1) {
                                        Log.d("devicelist","still could not connect");
                                    } catch (InvocationTargetException e1) {
                                        Log.d("devicelist","still could not connect");
                                    } catch (NoSuchMethodException e1) {
                                        Log.d("devicelist","still could not connect");
                                    }

                                }


                                outputStream = BTsocket.getOutputStream();
                                inputStream = BTsocket.getInputStream();

                                BTconnected = true;

                                Toast.makeText(mActivity,"Connected", Toast.LENGTH_SHORT).show();

                            } catch (IOException e) {
                                BTconnected = false;
                                Toast.makeText(mActivity,"Connection failed, close and try again", Toast.LENGTH_LONG).show();
                            }

                            if(BTconnected){
                                BTthreadStop = false;
                                BTread();

                            }
                        }
                    }
                }
            };

            connectionDialog.setTitle("Select Bluetooth")
                    .setItems(showList,clickListener);
            connectionDialog.show();

        }
    }

    public void disconnect(){
        BTthreadStop = true;
        try {
            Thread.sleep(500);
            BTsocket.close();
            BTconnected = false;

            Toast.makeText(mActivity, "Disconnected", Toast.LENGTH_SHORT).show();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Toast.makeText(mActivity, "Failed to disconnect", Toast.LENGTH_LONG).show();
        }

    }

    public boolean isConnected(){
        return BTconnected;
    }

    public void isEnable(boolean[] b){
        if(!mAdapter.isEnabled()){
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }else{
            b[0] = true;
        }
    }

    //continue to read input data
    void BTread(){
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !BTthreadStop){
                    try {
                        int byteCount = inputStream.available();
                        if(byteCount > 0){
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String value = new String(rawBytes,"UTF-8");
                            mHandler.obtainMessage(1,value).sendToTarget();
                        }
                    } catch (IOException e) {
                        BTthreadStop = true;
                    }

                }
            }
        });
        thread.start();
    }

    //send data
    public void sendData(String msg){
        byte[] msgBuffer = msg.getBytes();

        try {
            outputStream.write(msgBuffer);
            outputStream.flush();
        } catch (IOException e) {

        }
    }


}
