package baigei.transflow.core;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import baigei.transflow.UI.MainActivity;
import baigei.transflow.UI.MbaseActivity;
import baigei.transflow.entity.NetInfo;

public class NsdMgr {
    public static final int MSG_REGISTERED_SUCCESS=0x010;
    public static final int MSG_REGISTERED_ERROR=0x011;
    public static final int MSG_DISCOVERY_FAILED=0x211;
    public static final int MSG_DEVICEUPDATE=0x220;
    public static final int MSG_DEVICECONNECT=0x230;
    public static final int MSG_DEVICEDISCONNECT=0x231;
    public static MbaseActivity foregroundActivity;
    public static final String TAG="Transflow";
    private static final String SERVICE_TYPE="_socket._udp";
    private String SERVICE_NAME="transflowCell undefined";
    public void setServiceName(String serviceName){
        SERVICE_NAME=serviceName;
    }
    private int MPORT;
    private static NsdMgr nsdMgr=new NsdMgr();
    private NsdMgr(){}
    private NsdServiceInfo myserviceinfo;
    private NsdManager nsdManager;
    NsdManager.RegistrationListener registrationListener;
    public static NsdMgr getInstance(){
        return nsdMgr;
    }
    public void setport(int port){
        MPORT=port;
    }
     public void registerService(Context context){
        myserviceinfo=new NsdServiceInfo();
        myserviceinfo.setPort(MPORT);
        myserviceinfo.setServiceName(SERVICE_NAME);
        myserviceinfo.setServiceType(SERVICE_TYPE);
        nsdManager=(NsdManager) context.getSystemService(Context.NSD_SERVICE);
        registrationListener= new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG,"registerfailed::"+errorCode);
                MainActivity.mainHandler.sendMessage(Message.obtain(MainActivity.mainHandler,MSG_REGISTERED_ERROR,new String("registerfailed")));
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG,"unregisterfailed"+errorCode);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                MainActivity.mainHandler.sendEmptyMessage(MSG_REGISTERED_SUCCESS);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            }
        };
        if(nsdManager==null){
            MainActivity.mainHandler.sendMessage(Message.obtain(MainActivity.mainHandler,MSG_REGISTERED_ERROR,new String("Nsd service not found")));
        }else {
            nsdManager.registerService(myserviceinfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
        }
    }
    public void unregister(){
        nsdManager.unregisterService(registrationListener);
    }
    class MResolveListener implements NsdManager.ResolveListener{
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG,"resolveservice failed"+errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG,"resolveservice success"+serviceInfo);
            NetInfo.getNetInfo().addDevice(serviceInfo.getHost(),serviceInfo.getServiceName(),serviceInfo.getPort());
        }
    }
    public void initdiscover(final Handler connecthandler){
        final MResolveListener mResolveListener=new MResolveListener();
        NsdManager.DiscoveryListener discoveryListener=new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG,"startdiscovery failed"+errorCode);
                connecthandler.sendEmptyMessage(MSG_DISCOVERY_FAILED);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG,"stopdiscovery failed"+errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.e(TAG,"startdiscovery");
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.e(TAG,"stopdiscovery");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.e(TAG,"service found");
                if(serviceInfo.getServiceType().equals(SERVICE_TYPE)){
                    if(serviceInfo.getServiceName().equals(SERVICE_NAME)){
                        Log.e(TAG,"own device found");
                        NetInfo.getNetInfo().addOwnDevice(myserviceinfo.getHost(),SERVICE_NAME);
                    }
                    else {
                        Log.e(TAG,"new device found");
                        nsdManager.resolveService(serviceInfo,mResolveListener);
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG,"service lost");
                if(serviceInfo.getServiceType().equals(SERVICE_TYPE)){
                    Log.e(TAG,"device lost");
                    NetInfo.getNetInfo().removeDevice(serviceInfo.getHost());
                }
            }
        };
        nsdManager.discoverServices(SERVICE_TYPE,NsdManager.PROTOCOL_DNS_SD,discoveryListener);
    }
}
