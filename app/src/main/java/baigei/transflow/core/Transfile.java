package baigei.transflow.core;

public class Transfile {
    public final static int MSG_STARTTRANSACTION=0x700;
    public final static int MSG_CHANGEACTION=0x710;
    public final static int MSG_SENDUPDATELIST=0x720;
    public final static int MSG_CHANGESTATE=0x730;
    public final static int MSG_SAVERECORD=0x800;
    public final static int MSG_UPDATETRANSVIEW=0x810;
    public final static String SENDUPDATE="sendupdatetransfile";
    public final static String MSG_RESSENDUPDATE="respondtosendupdatetransfile";
    public final static String MSG_UPDATERECIEVED="updatetransfilerecieved";
    public final static int ACTION_STATE=0x71010;
    public final static int ACTION_UNSTATE=0x71011;
    public final static int ACTION_TOP=0x71020;
    public final static int ACTION_UNTOP=0x71021;
    public final static int ACTION_CANCEL=0x71030;
    public final static int ACTION_UNCANCEL=0x71031;
//    identifier is name, can not be duplicated.
    String path;
    int recordBuf;
    boolean tosend;
    String name;
    int fulBuf=0;
    boolean state;
    public Transfile(String path, int recordBuf, boolean tosend,String name){
        this.path=path;
        this.recordBuf=recordBuf;
        this.tosend=tosend;
        this.name=name;
    }
    public void setPath(String locpath){
        path=locpath;
    }
    public String getPath(){return path;}
    public void setRecordBuf(int buf){
        recordBuf=buf;
    }
    public int getRecordBuf() {return  recordBuf;}
    public boolean getTosend(){return tosend;}
    public String getName(){return name;}
    public void setFulBuf(int sizebuf){
        fulBuf=sizebuf;
    }
    public int getFulBuf(){return fulBuf;}
    public void setState(boolean state){this.state=state;}
    public boolean getSate(){return state;}
}

