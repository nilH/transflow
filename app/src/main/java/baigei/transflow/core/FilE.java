package baigei.transflow.core;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FilE {
    public static final int MSG_STARTMULTISELECT=0x600;
    public static final int MSG_CHANGEACTION=0x610;
    public static final int MSG_SENDFILES=0x400;
    public static final int MSG_FILEERROR=0x611;
    public static final String FILESTOSEND="send selected files";
    String path;
    String name;
    public enum FileType{
        FILE,
        FOLDER
    }
    FileType fileType;
    int size;
    public enum SizeUnit{
        kb,
        mb,
        undefined
    }
    Date lastdate;
    SizeUnit sizeUnit;
    public FilE(String back){
        path=back;
    }
    public FilE(String path, String name, FileType fileType, int size, Date date,SizeUnit sizeUnit){
        this.fileType=fileType;
        this.lastdate=date;
        this.name=name;
        this.size=size;
        this.sizeUnit=sizeUnit;
    }
    public String getPath() {return path;}
    public String getName(){return name;}
    public FileType getFileType(){return fileType;}
    public int getSize(){return size;}
    public Date getLastdate() {return lastdate;}
    public SizeUnit getSizeUnit() {return sizeUnit;}


    public static List<FilE> getfilEsfrompath(String path){
//        check exist first before
        File[] files=(new File(path)).listFiles();
        List<FilE> mfiles=new ArrayList<FilE>();
        for(File file:files){
            Date lastmodified=new Date(file.lastModified());
            if(file.isDirectory()){
                FilE mfile=new FilE(file.getPath(),file.getName(),FileType.FOLDER,0,lastmodified,SizeUnit.undefined);
                mfiles.add(mfile);
                continue;
            }
            SizeUnit sizeUnit;
            long size=file.length()/1024;
            if(size>1024){
                size=size/1024;
                sizeUnit=SizeUnit.mb;
            }
            else {
                sizeUnit=SizeUnit.kb;
            }
            FilE mfile=new FilE(file.getPath(),file.getName(),FileType.FILE,(int)size,lastmodified,sizeUnit);
            mfiles.add(mfile);
        }
        return mfiles;
    }

    public static String currentfilePath;
}
