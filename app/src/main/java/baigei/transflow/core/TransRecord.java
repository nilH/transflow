package baigei.transflow.core;

import java.util.List;

public class TransRecord {
    String deviceName;
    List<String> files;
    public TransRecord(String device, List<String> files){
        this.deviceName=device;
        this.files=files;
    }
    public String getDeviceName(){
        return deviceName;
    }
    public List<String> getFiles(){
        return files;
    }
}
