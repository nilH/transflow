package baigei.transflow.UI;

import androidx.annotation.NonNull;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import baigei.example.transflow.R;
import baigei.transflow.adapter.DeviceviewAdapter;
import baigei.transflow.core.NsdMgr;
import baigei.transflow.entity.NetInfo;

public class ConnectionActivity extends MbaseActivity {
    DeviceviewAdapter deviceviewAdapter;
    TextView scantext=findViewById(R.id.scanText);
    ListView devicelist=findViewById(R.id.devicelist);
    public static class ConnectHandler extends Handler{
        private WeakReference<Activity> mtarget;
        ConnectHandler(Activity target){
            mtarget=new WeakReference<Activity>(target);
        }
        public void setMtarget(Activity target){
            mtarget.clear();
            mtarget=new WeakReference<Activity>(target);
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what==NsdMgr.MSG_DISCOVERY_FAILED){
                ConnectionActivity activity=(ConnectionActivity)mtarget.get();
                activity.showError("start discovery failed");
            }
            else if(msg.what==NsdMgr.MSG_DEVICEUPDATE){
                ConnectionActivity activity=(ConnectionActivity)mtarget.get();
                activity.updataList();
            }
        }
    }
    public static ConnectHandler connectHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        if(connectHandler==null){
            connectHandler=new ConnectHandler(this);
        }
        else {
            connectHandler.setMtarget(this);
        }
        NetInfo.setHandler(connectHandler);
        NsdMgr.getInstance().initdiscover(connectHandler);
        initiate();
    }
    void initiate(){
        if(NetInfo.getNetInfo().mDevices.isEmpty()){
            scantext.setVisibility(View.VISIBLE);
            scantext.setText(R.string.connection_no_devices);
            devicelist.setVisibility(View.GONE);
        }
        else {
            scantext.setVisibility(View.GONE);
            devicelist.setVisibility(View.VISIBLE);
            deviceviewAdapter=new DeviceviewAdapter(this,NetInfo.getNetInfo().mDevices);
            devicelist.setAdapter(deviceviewAdapter);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        NsdMgr.foregroundActivity=this;
    }

    void updataList(){
        if(NetInfo.getNetInfo().mDevices.isEmpty()){
            scantext.setVisibility(View.VISIBLE);
            scantext.setText(R.string.connection_no_devices);
            devicelist.setVisibility(View.GONE);
        }
        else {
            scantext.setVisibility(View.GONE);
            devicelist.setVisibility(View.VISIBLE);
            devicelist.setAdapter(deviceviewAdapter);
        }
        deviceviewAdapter.resetDate(NetInfo.getNetInfo().mDevices);
        deviceviewAdapter.notifyDataSetChanged();
    }
    void showError(String error){
        Toast toast=Toast.makeText(this,error,Toast.LENGTH_SHORT);
        toast.show();
    }

}
