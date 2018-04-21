package hk.ust.cse.fyp;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    Switch BTswitch;
    TextView data_textview;

    Bluetooth mBT;
    Handler BThandler;
    boolean[] BTpermission = new boolean[1];   //permission status to use the Bluetooth

    private final static int REQUEST_ENABLE_BT = 1; //any number greater than one

    private StringBuilder stringBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BTswitch = (Switch) findViewById(R.id.BTswitch);
        BTswitch.setOnCheckedChangeListener(this) ;

        data_textview = (TextView) findViewById(R.id.Data_TextView);
        data_textview.setMovementMethod(new ScrollingMovementMethod());

        mBT = new Bluetooth(this,BThandler);
        //check if already turn on bluetooth, if not request user permission
        mBT.isEnable(BTpermission);


    }

    //Check with the action of the button
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if(isChecked){
            if(BTpermission[0]){
                mBT.connect();
            }
            else{
                BTswitch.toggle();
                mBT.isEnable(BTpermission);
            }

        }
        //else if uncheck but bluetooth is on, turn it off
        else{
            if(mBT.isConnected()){
                mBT.disconnect();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_CANCELED){
                //user rejected to turn on bluetooth
                Toast.makeText(getApplicationContext(),"Application will stop without Bluetooth turn on",Toast.LENGTH_LONG).show();
                BTpermission[0] = false;
            }
            else if(resultCode == RESULT_OK){
                BTpermission[0] = true;
            }
        }
    }


    //handle input read
    static class handlerManage extends Handler{
        //prevent memory leak?
        private final WeakReference<MainActivity> mActivity;

        handlerManage(WeakReference<MainActivity> mActivity) {
            this.mActivity = mActivity;
        }

        @Override
        public void handleMessage(Message msg){
            MainActivity activity = mActivity.get();
            if(activity != null){
                activity.handleMessage(msg);
            }
        }
    }

    //deal with the return data
    private void handleMessage(Message msg){
        if(msg.what == 1){
            String readMsg = (String)msg.obj;
            stringBuilder.append(readMsg);
            int endOfLineIndex = stringBuilder.indexOf("~");
            if(endOfLineIndex > 0){
                String received = stringBuilder.substring(0,endOfLineIndex);
                data_textview.setText(received);

            }

            stringBuilder.delete(0,stringBuilder.length());

        }
    }

}
