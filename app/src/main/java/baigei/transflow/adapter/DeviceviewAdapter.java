package baigei.transflow.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.InetAddress;
import java.util.ArrayList;

import baigei.example.transflow.R;
import baigei.transflow.UI.UIDialogFragment;
import baigei.transflow.core.ConnectionService;
import baigei.transflow.core.NsdMgr;
import baigei.transflow.entity.MConnection;
import baigei.transflow.entity.MDevice;
import baigei.transflow.entity.NetInfo;

public class DeviceviewAdapter extends ArrayAdapter<MDevice> {
    private Context contextConnectActivity;
    ArrayList<MDevice> devices;
    public DeviceviewAdapter(Context context, ArrayList<MDevice> devices){
        super(context,0,devices);
        contextConnectActivity=context;
        this.devices=devices;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MDevice device=devices.get(position);
        if(convertView==null){
            convertView= LayoutInflater.from(getContext()).inflate(R.layout.item_device,parent,false);
        }
        if(device==null){
            return convertView;
        }
        final Switch conswitch=(Switch)convertView.findViewById(R.id.device_switch);
        final TextView textView=(TextView)convertView.findViewById(R.id.device_name);
        textView.setText(device.getName());
        convertView.setTag(device.getIp());
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                UIDialogFragment uiDialogFragment=new UIDialogFragment();
                if(MConnection.getTargetDevice()==null){
                    DialogInterface.OnClickListener posonclick=new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConnectionService.transferHandler.sendMessage(Message.obtain(ConnectionService.transferHandler,NsdMgr.MSG_DEVICECONNECT,(InetAddress)v.getTag()));
                        }
                    };
                    DialogInterface.OnClickListener negonclick=new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    };
                    uiDialogFragment.setDialog("sure to connect to "+textView.getText(),"Yes","No",posonclick,negonclick);
                    NsdMgr.foregroundActivity.showdialog(uiDialogFragment);
                }
                else if(v.getTag().equals(MConnection.getTargetDevice().getIp())) {
                    DialogInterface.OnClickListener posonclick=new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConnectionService.transferHandler.sendEmptyMessage(NsdMgr.MSG_DEVICEDISCONNECT);
                        }
                    };
                    DialogInterface.OnClickListener negonclick=new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    };
                    uiDialogFragment.setDialog("sure to disconnect to "+textView.getText(),"Yes","No",posonclick,negonclick);
                    NsdMgr.foregroundActivity.showdialog(uiDialogFragment);
                }
                else {
                    DialogInterface.OnClickListener posonclick=new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConnectionService.transferHandler.sendEmptyMessage(NsdMgr.MSG_DEVICEDISCONNECT);
                            ConnectionService.transferHandler.sendMessage(Message.obtain(ConnectionService.transferHandler,NsdMgr.MSG_DEVICECONNECT,(InetAddress)v.getTag()));
                        }
                    };
                    DialogInterface.OnClickListener negonclick=new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    };
                    uiDialogFragment.setDialog("sure to switch connection to "+textView.getText(),"Yes","No",posonclick,negonclick);
                }
            }
        });
        if(device.equals(MConnection.getTargetDevice())){
            conswitch.setChecked(true);
        }else {
            conswitch.setChecked(false);
        }
        return convertView;
    }
    public void resetDate(ArrayList<MDevice> devices){
        this.devices=devices;
    }
}
