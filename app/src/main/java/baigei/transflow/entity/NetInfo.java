package baigei.transflow.entity;

import android.os.Handler;

import java.net.InetAddress;
import java.util.ArrayList;

import baigei.transflow.core.NsdMgr;

public class NetInfo {
    public ArrayList<MDevice> mDevices;
    private static NetInfo netInfo=new NetInfo();
    private static Handler connecthandler;
    private NetInfo(){
        mDevices=new ArrayList<>();
    }
    public static void setHandler(Handler handler){
        connecthandler=handler;
    }
    public static NetInfo getNetInfo(){
        return netInfo;
    }
    public void addDevice(InetAddress inetAddress, String name, int port){
        for(MDevice e:mDevices){
            if(e.getIp().equals(inetAddress)){
                return;
            }
        }
        MDevice mDevice=new MDevice();
        mDevice.setConnection(false);
        mDevice.setIp(inetAddress);
        mDevice.setName(name);
        mDevice.setPort(port);
        mDevices.add(mDevice);
        connecthandler.sendEmptyMessage(NsdMgr.MSG_DEVICEUPDATE);
    }
    public void addOwnDevice(InetAddress inetAddress, String name){
        for(MDevice e:mDevices){
            if(e.getIp().equals(inetAddress)){
                return;
            }
        }
        MDevice mDevice=new MDevice();
        mDevice.setName(name);
        mDevice.setConnection(false);
        mDevices.add(mDevice);
        connecthandler.sendEmptyMessage(NsdMgr.MSG_DEVICEUPDATE);
    }
    public void removeDevice(InetAddress inetAddress){
        for(MDevice e:mDevices){
            if(e.getIp().equals(inetAddress)){
                mDevices.remove(e);
            }
        }
    }
}
