package baigei.transflow.core;

import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import baigei.transflow.UI.MainActivity;
import baigei.transflow.UI.UIDialogFragment;
import baigei.transflow.entity.MConnection;
import baigei.transflow.entity.MDevice;

public class ConnectionService extends Service{
    private static final String MSG_CONNECT="letsconnect";
    private static final String MSG_NOCONNECT="refuseconnect";
    private static final String TRANSNAME="nameoftransfile";
    private static final String TRANSFULSIZE="fullsizeoftransfile";
    private static final String TRANSBUFSIZE="pastsizeoftransfile";
    private static final String TRANSTOSEND="iftransfiletosend";
    public static final int MSG_SERVICESTARTED=0x000;
    private MDevice targetMDevice;
    private static final int SYNCTIMEOUT=200000;
    private int localport;
    private int remoteport;
    private int localfileport;
    private int remotefileport;
    private DatagramSocket greetSocket;
    private DatagramSocket listenerSocket;
    private Socket socket;
    private ServerSocket serverSocket;
    private ThreadPoolExecutor threadPoolExecutor;
    private BaseTransfer baseTransfer;
    public static TransferHandler transferHandler;
    public static class TransferHandler extends Handler{
        private WeakReference<ConnectionService> mtarget;
        TransferHandler(ConnectionService target){
            mtarget=new WeakReference<ConnectionService>(target);
        }
        public void setMtarget(ConnectionService target){
            mtarget.clear();
            mtarget=new WeakReference<ConnectionService>(target);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            ConnectionService service=mtarget.get();
            if(msg.what==NsdMgr.MSG_DEVICECONNECT){
                if(MConnection.doconnect((InetAddress) msg.obj)){
                    service.targetMDevice=MConnection.getTargetDevice();
//                    next time do revoke work here, prevent foregroundactivity destroyed
                    NsdMgr.foregroundActivity.showstates("connecting to "+service.targetMDevice.getName());
                    service.handshake();
                }
                else {
                    NsdMgr.foregroundActivity.showstates("already in connection with one device");
                }
            }
            else if(msg.what==NsdMgr.MSG_DEVICEDISCONNECT){
                try {
                    service.disConnect();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
            else if(msg.what==Transfile.MSG_SENDUPDATELIST){
                service.sendTranslist();
            }
            else if(msg.what==Transfile.MSG_CHANGESTATE){
                service.baseTransfer.changestate();
            }
        }
    }
    void disConnect() throws IOException{
        socket.close();
        MConnection.disconnect();
    }
    @Override
    public void onCreate() {
        if(transferHandler==null){
            transferHandler=new TransferHandler(this);
        }
        else{
            transferHandler.setMtarget(this);
        }
        baseTransfer=new BaseTransfer();
        threadPoolExecutor=new ThreadPoolExecutor(2,4,1, TimeUnit.MINUTES,new LinkedBlockingDeque<Runnable>());
        try {
            listenerSocket=new DatagramSocket();
            localport=listenerSocket.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
            localport=0;
        }
        NsdMgr.foregroundActivity.showstates("disconnected");
        NsdMgr.getInstance().setport(localport);
        ListennerTask listennerTask=new ListennerTask(this);
        threadPoolExecutor.execute(listennerTask);
        MainActivity.mainHandler.sendEmptyMessage(MSG_SERVICESTARTED);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.e(NsdMgr.TAG,"connecitonservice destroyed");
    }

    public void startStoreActivity(){
        Intent intent=new Intent(this,MainActivity.class);
        intent.putExtra("tosend",true);
        startActivity(intent);
    }

    class ListennerTask implements Runnable{
        ConnectionService master;
        ListennerTask(ConnectionService service){
            master=service;
        }
        //        task1: listen to be connected
//        task2: listen for transfilelist update
        byte[] recvData;
        byte[] sendData;
        String response;
        DatagramPacket recvPacket;
        DatagramPacket sendPacket;

        @Override
        public void run() {
            while (true) {
                recvData=new byte[512];
                recvPacket = new DatagramPacket(recvData, recvData.length);
                try {
                    listenerSocket.receive(recvPacket);
                }catch (IOException e){
                    e.printStackTrace();
                    return;
                }
                response = new String(recvPacket.getData(), StandardCharsets.UTF_8).trim();
                if (response.split(" ")[0].equals(MSG_CONNECT)) {
                    if (MConnection.doconnect(recvPacket.getAddress())) {
                        remotefileport = Integer.valueOf(response.split(" ")[1].trim());
                        remoteport=recvPacket.getPort();
                        UIDialogFragment uiDialogFragment = new UIDialogFragment();
                        DialogInterface.OnClickListener posclick = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                            listener end is client socket
                                try {
                                    onagreeConnect();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        DialogInterface.OnClickListener negclick=new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    ondisagreeConnect();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        uiDialogFragment.setDialog(targetMDevice.getName()+" wants to connect","Yes","No",posclick,negclick);
                        NsdMgr.foregroundActivity.showdialog(uiDialogFragment);
                        NsdMgr.getInstance().unregister();
                    }
                }
                else if(response.split(" ")[0].equals(Transfile.SENDUPDATE)){
                    try {
                        listenerSocket.setSoTimeout(SYNCTIMEOUT);
                    }catch (SocketException so){
                        so.printStackTrace();
                    }
                    sendData=Transfile.MSG_RESSENDUPDATE.getBytes(StandardCharsets.UTF_8);
                    sendPacket=new DatagramPacket(sendData,sendData.length,MConnection.getTargetDevice().getIp(),remoteport);
                    try {
                        listenerSocket.send(sendPacket);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    recvData=new byte[4096];
                    recvPacket=new DatagramPacket(recvData,recvData.length);
                    try {
                        listenerSocket.receive(recvPacket);
                    }catch (SocketTimeoutException se){
                        break;
                    }catch (IOException e){
                        e.printStackTrace();
                        break;
                    }
                    JSONArray jsonArray;
                    try {
                        jsonArray = new JSONArray(new String(recvPacket.getData(),StandardCharsets.UTF_8).trim());
                        ArrayList<Transfile> transfiles=new ArrayList<>();
                        for(int i=0;i<jsonArray.length();i++){
                            JSONObject jsonObject=jsonArray.getJSONObject(i);
                            boolean tosend=!jsonObject.getBoolean(TRANSTOSEND);
                            Transfile transfile=new Transfile("",jsonObject.getInt(TRANSBUFSIZE),tosend,jsonObject.getString(TRANSNAME));
                            transfile.setFulBuf(jsonObject.getInt(TRANSFULSIZE));
                            transfiles.add(transfile);
                            master.startStoreActivity();
                        }
                        baseTransfer.resetTrans(transfiles);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        void onagreeConnect() throws IOException {
            socket=new Socket(MConnection.getTargetDevice().getIp(),remotefileport);
            sendData=MSG_CONNECT.getBytes(StandardCharsets.UTF_8);
            sendPacket=new DatagramPacket(sendData,sendData.length,recvPacket.getAddress(),remoteport);
            listenerSocket.send(sendPacket);
            NsdMgr.foregroundActivity.showstates("connected to "+MConnection.getTargetDevice().getName());
        }
        void ondisagreeConnect() throws IOException {
            sendData=MSG_NOCONNECT.getBytes(StandardCharsets.UTF_8);
            sendPacket=new DatagramPacket(sendData,sendData.length,recvPacket.getAddress(),remoteport);
            listenerSocket.send(sendPacket);
        }
    }


    void handshake(){
        Runnable runnable=new Runnable() {
            byte[] recvData=new byte[512];
            byte[] sendData;
            String response;
            DatagramPacket sendpacket;
            DatagramPacket recvPacket;
            @Override
            public void run() {
                try {
                    if(greetSocket==null){
                        greetSocket=new DatagramSocket();
                    }
                    greetSocket.setSoTimeout(SYNCTIMEOUT);
                    serverSocket=new ServerSocket();
                    socket=serverSocket.accept();
                    localfileport=serverSocket.getLocalPort();
                    sendData=(MSG_CONNECT+" "+localfileport).getBytes(StandardCharsets.UTF_8);
                    sendpacket=new DatagramPacket(sendData,sendData.length,MConnection.getTargetDevice().getIp(),MConnection.getTargetDevice().getPort());
                    greetSocket.send(sendpacket);
                    recvPacket=new DatagramPacket(recvData,recvData.length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                    while (true){
                        try{
                            greetSocket.receive(recvPacket);
                            response=new String(recvPacket.getData(),StandardCharsets.UTF_8).trim();
                            if(response.equals(MSG_CONNECT)){
                                NsdMgr.foregroundActivity.showstates("connected to "+targetMDevice.getName());
                                break;
                            }
                            else if(response.equals(MSG_NOCONNECT)){
                                NsdMgr.foregroundActivity.showstates(targetMDevice.getName()+" refuse to connect");
                                break;
                            }
                        }catch (SocketTimeoutException se){
                            break;
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;
                        }

                    }
            }
        };
        threadPoolExecutor.execute(runnable);
    }

    void sendTranslist(){
//        when initiate the app, or changed local transfile list, then notify the target device. pause/resume dont.
        baseTransfer.resetTrans();
        try {
            greetSocket.setSoTimeout(SYNCTIMEOUT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                ArrayList<Transfile> transfiles=(ArrayList<Transfile>) MConnection.getTargetDevice().getTransfiles();
                JSONArray jsonArray=new JSONArray();
                try {
                    for(Transfile transfile:transfiles){
                        JSONObject jsonObject=new JSONObject();
                        jsonObject.put(TRANSNAME,transfile.getName());
                        jsonObject.put(TRANSBUFSIZE,transfile.getRecordBuf());
                        jsonObject.put(TRANSFULSIZE,transfile.getFulBuf());
                        jsonObject.put(TRANSTOSEND,transfile.getTosend());
                        jsonArray.put(jsonObject);
                    }
                }catch (JSONException je){
                    je.printStackTrace();
                }

                byte[] recvData=new byte[512];
                byte[] sendData;
                String response;
                DatagramPacket sendpacket;
                DatagramPacket recvPacket;
                sendData=(Transfile.SENDUPDATE+" 0").getBytes(StandardCharsets.UTF_8);
                sendpacket=new DatagramPacket(sendData,sendData.length,MConnection.getTargetDevice().getIp(),MConnection.getTargetDevice().getPort());
                try {
                    greetSocket.send(sendpacket);
                    recvPacket=new DatagramPacket(recvData,recvData.length);
                }catch (IOException e){
                    e.printStackTrace();
                    return;
                }
                while (true){
                    try{
                        greetSocket.receive(recvPacket);
                    }catch (SocketTimeoutException so){
                        break;
                    }catch (IOException e){
                        e.printStackTrace();
                        break;
                    }
                    response=new String(recvPacket.getData(),StandardCharsets.UTF_8).trim();
                    if(response.equals(Transfile.MSG_RESSENDUPDATE)){
                        break;
                    }
                }
                sendData=(jsonArray.toString()).getBytes(StandardCharsets.UTF_8);
                sendpacket=new DatagramPacket(sendData,sendData.length,MConnection.getTargetDevice().getIp(),MConnection.getTargetDevice().getPort());
                try {
                    greetSocket.send(sendpacket);
                }catch (IOException e){
                    e.printStackTrace();
                    return;
                }
                while (true){
                    try {
                        greetSocket.receive(recvPacket);
                    }catch (SocketTimeoutException se){
                        break;
                    }catch (IOException e){
                        e.printStackTrace();
                        break;
                    }
                    response=new String(recvPacket.getData(),StandardCharsets.UTF_8).trim();
                    if(response.equals(Transfile.MSG_UPDATERECIEVED)){
                        NsdMgr.foregroundActivity.showstates("update transfer tasks successful");
                        break;
                    }
                }
            }
        };
        threadPoolExecutor.execute(runnable);
    }
}
