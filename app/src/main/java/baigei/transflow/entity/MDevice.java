package baigei.transflow.entity;


import java.net.InetAddress;
import java.util.List;

import baigei.transflow.core.Transfile;

public class MDevice {
    private String name="null";
    private InetAddress ip;
    private int port;
    private boolean connection;
    private List<Transfile> transfiles;
    public MDevice(){}
    public void setName(String name){
        this.name=name;
    }
    public String getName(){
        return name;
    }
    public void setIp(InetAddress ip){
        this.ip=ip;
    }
    public InetAddress getIp(){
        return ip;
    }
    public void setConnection(boolean connection){
        this.connection=connection;
    }
    public boolean getConnection(){
        return connection;
    }
    public void setPort(int port){ this.port=port; }
    public int getPort(){return port;}
    public void setTransfiles(List<Transfile> files){
        transfiles=files;
    }
    public List<Transfile> getTransfiles(){
        return transfiles;
    }
}
