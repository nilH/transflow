package baigei.transflow.core;

//use RandomAccessFile class. read records first. buffer size 80k.
//record file hierarchy:  FILES (stringset) XXX; RECORDBUF (int) xxx; RECORDPATH (String) xxx; TOSEND (boolean);
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import baigei.transflow.UI.TransactionActivity;
import baigei.transflow.adapter.TransviewAdapter;
import baigei.transflow.entity.MConnection;

public class BaseTransfer implements Runnable{
    public final static int BUFFER=80;
    public static volatile List<Transfile> tosaveFileslist;
    static volatile Transfile currentTrans;
    ArrayList<Transfile> loads;
    private final Object LOCK=new Object();
    private volatile boolean topause;
    private volatile boolean toend;
    Socket socket;
    DataOutputStream dataOutputStream;
    DataInputStream dataInputStream;

    public static Transfile getcurrentTrans(){
        return currentTrans;
    }

    @Override
    public void run() {
        try{
            dataOutputStream=new DataOutputStream(socket.getOutputStream());
            dataInputStream=new DataInputStream(socket.getInputStream());
            while (loads.size()>0){
                currentTrans=loads.get(0);
                loads.remove(0);
                if(currentTrans.getTosend()){
                    Sender sender=new Sender(this);
                    sender.run();
                }
                else {
                    Receiver receiver=new Receiver(this);
                    receiver.run();
                }
                changestate();
                save();
            }
            dataInputStream.close();
            dataOutputStream.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    class Sender implements Runnable{
        BaseTransfer baseTransfer;
        Sender(BaseTransfer master){
            baseTransfer=master;
        }
        @Override
        public void run() {
            try{
                long startTime=System.currentTimeMillis();
                long endTime=startTime;
                long reCordstartTime=startTime;
                RandomAccessFile file=new RandomAccessFile(currentTrans.getPath(),"r");
                int offset;
                offset=currentTrans.getRecordBuf();
                file.seek(offset*BUFFER*1024);
                byte[] buf=new byte[BUFFER*1024];
                while (offset<currentTrans.getFulBuf()){
                    synchronized (LOCK){
                        if(topause){
                            LOCK.wait();
                        }
                    }
                    if(toend){
                        file.close();
                        toend=false;
                        return;
                    }
                    endTime=System.currentTimeMillis();
                    file.read(buf);
                    dataOutputStream.write(buf);
                    offset=offset+1;
                    if(endTime==startTime+2000){
//                        update transactionActivity view every 2s
                        currentTrans.setRecordBuf(offset);
                        loads.set(0,currentTrans);
                        baseTransfer.updateView();
                        startTime=endTime;
                    }
                    if(endTime==reCordstartTime+300000){
//                        record the progress to local disk every 5 min
                        baseTransfer.save();
                        reCordstartTime=endTime;
                    }
                }
                file.close();
            }catch (InterruptedException|IOException fe){
                fe.printStackTrace();
            }
        }
    }

    class Receiver implements Runnable{
        BaseTransfer baseTransfer;
        Receiver(BaseTransfer master){
            baseTransfer=master;
        }
        @Override
        public void run() {
            try{
                long startTime=System.currentTimeMillis();
                long reCordstartTime=startTime;
                long endTime=startTime;
                RandomAccessFile file=new RandomAccessFile(currentTrans.getPath()+".tmp","w");
                int offset;
                offset=currentTrans.getRecordBuf();
                file.seek(offset*BUFFER*1024);
                byte[] buf=new byte[BUFFER*1024];
                while (dataInputStream.read(buf)!=-1){
                    synchronized (LOCK){
                        if(topause){
                            LOCK.wait();
                        }
                    }
                    if(toend){
                        file.close();
                        toend=false;
                        return;
                    }
                    endTime=System.currentTimeMillis();
                    file.write(buf);
                    offset=offset+1;
                    if(endTime==startTime+2000){
//                        update transactionActivity view every 2s
                        currentTrans.setRecordBuf(offset);
                        loads.set(0,currentTrans);
                        baseTransfer.updateView();
                        startTime=endTime;
                    }
                    if(endTime==reCordstartTime+300000){
//                        record the progress to local disk every 5 min
                        baseTransfer.save();
                        reCordstartTime=endTime;
                    }
                }
                File nwfile=new File(currentTrans.getPath()+".tmp");
                if(nwfile.exists()){
                    nwfile.renameTo(new File(currentTrans.getPath()));
                }
            }catch (IOException|InterruptedException ie){
                ie.printStackTrace();
            }

        }
    }

    void changestate(){
//        resume or pause. also called to notify of changes in local transifles list
        loads=(ArrayList<Transfile>) MConnection.getTargetDevice().getTransfiles();
        currentTrans=loads.get(0);
        if(currentTrans.getSate()){
            synchronized (LOCK){
                topause=false;
                LOCK.notifyAll();
            }
        }
        else {
            synchronized (LOCK){
                topause=true;
                LOCK.notifyAll();
            }
        }
    }

    public void resetTrans(ArrayList<Transfile> transfiles){
//        send from target device to update transfile list, remotely
        if(tosaveFileslist==null){
            tosaveFileslist=new ArrayList<>();
        }else {
            tosaveFileslist.clear();
        }
        loads.clear();
        for(Transfile transfile:transfiles){
            boolean isnew=true;
            for(Transfile localtransfile:MConnection.getTargetDevice().getTransfiles()){
                if(localtransfile.getName().equals(transfile.getName())){
                    isnew=false;
                    transfile.setPath(localtransfile.getPath());
                }
            }
            if(isnew){
                tosaveFileslist.add(transfile);
            }
            else {
                loads.add(transfile);
            }
        }
        currentTrans=loads.get(0);
        save();
        toend=true;
    }

    void resetTrans(){
//        call from transactionActivity or StoreActivity to update the transfilelist, locally
        loads=(ArrayList<Transfile>) MConnection.getTargetDevice().getTransfiles();
        currentTrans=loads.get(0);
        save();
        toend=true;
    }

    void save(){
//        save current transfile list to local records and disk file, and update transactionActivity
        MConnection.getTargetDevice().setTransfiles(loads);
        TransactionActivity.transactHandler.sendEmptyMessage(Transfile.MSG_SAVERECORD);
    }

    void updateView(){
//        save currrent transfile list to local record and update transactionActivity
        MConnection.getTargetDevice().setTransfiles(loads);
        TransactionActivity.transactHandler.sendEmptyMessage(Transfile.MSG_UPDATETRANSVIEW);
    }
}
