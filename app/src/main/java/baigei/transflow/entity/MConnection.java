package baigei.transflow.entity;

import android.util.Log;

import java.net.InetAddress;

import baigei.transflow.core.NsdMgr;

public class MConnection {
    private MDevice targetMDevice;
    private static MConnection MConnection=new MConnection();
    private MConnection(){}
    private static void setupConnection(MDevice MDevice){
        MConnection.targetMDevice = MDevice;
    }
    public static MDevice getTargetDevice(){
        return MConnection.targetMDevice;
    }
    public static boolean doconnect(InetAddress inetAddress){
        if(getTargetDevice()!=null){
            return false;
        }
        for(MDevice e:NetInfo.getNetInfo().mDevices){
            if(e.getIp().equals(inetAddress)){
                setupConnection(e);
                return true;
            }
        }
        Log.e(NsdMgr.TAG,"connect failed target not in list");
        return false;
    }
    public static void disconnect(){
        MConnection.targetMDevice=null;
        NsdMgr.foregroundActivity.showstates("disconnected");
    }
}
