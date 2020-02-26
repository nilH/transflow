package baigei.transflow.adapter;

import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import baigei.example.transflow.R;
import baigei.transflow.UI.MainActivity;
import baigei.transflow.UI.TransactionActivity;
import baigei.transflow.core.FilE;
import baigei.transflow.core.Transfile;
import baigei.transflow.entity.MConnection;

public class FileviewAdapter extends RecyclerView.Adapter<FileviewAdapter.ViewHolder> {
    class ViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView filename;
        TextView filesize;
        ImageView imgSelected;
        public ViewHolder(View itemview){
            super(itemview);
            imageView=itemview.findViewById(R.id.file_img);
            filename=itemview.findViewById(R.id.file_name);
            filesize=itemview.findViewById(R.id.file_size);
            imgSelected=itemview.findViewById(R.id.file_selected);
            if(tosend){
                itemview.setOnClickListener(new MOnclickListener(getAdapterPosition(),imgSelected));
                itemview.setOnLongClickListener(new MOnlongclickListener(getAdapterPosition(),imgSelected));
            }else {
                itemview.setOnClickListener(new MStoreOnclickListener(getAdapterPosition()));
            }
        }
    }

    List<FilE> filEList;
    boolean tosend;
    public boolean multiselectMode;
    public List<FilE> filesSelected;
    public FileviewAdapter(List<FilE> filEList,boolean mode){
        this.filEList=filEList;
        filesSelected=new ArrayList<>();
        this.tosend=mode;
    }

    public void updateData(List<FilE> filEList){
        this.filEList=filEList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater=LayoutInflater.from(parent.getContext());
        View fileview=layoutInflater.inflate(R.layout.item_file,parent,false);
        return new ViewHolder(fileview);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FilE filE=filEList.get(position);
        holder.filename.setText(filE.getName());
        if(filE.getFileType()== FilE.FileType.FOLDER){
            holder.filesize.setVisibility(View.GONE);
            holder.imageView.setImageResource(R.drawable.folder);
        }
        else if(filE.getFileType()== FilE.FileType.FILE){
            String unit=filE.getSizeUnit()== FilE.SizeUnit.kb?" kb":" mb";
            String size=filE.getSize()+unit;
            holder.filesize.setText(size);
            holder.imageView.setImageResource(R.drawable.file);
        }
    }

    class MStoreOnclickListener implements View.OnClickListener{
        int position;
        MStoreOnclickListener(int position){
            this.position=position;
        }

        @Override
        public void onClick(View v) {
            FilE file=filEList.get(position);
            if(file.getFileType()==FilE.FileType.FOLDER){
                FilE.currentfilePath=file.getPath();
                updateData(FilE.getfilEsfrompath(FilE.currentfilePath));
            }
        }
    }

    class MOnclickListener implements View.OnClickListener {
        int position;
        ImageView imgselected;
        MOnclickListener(int position, ImageView imageView){
            this.position=position;
            this.imgselected=imageView;
        }
        @Override
        public void onClick(View v) {
            FilE filE=filEList.get(position);
            if(multiselectMode){
                if(filesSelected.contains(filE)){
                    filesSelected.remove(filE);
                    imgselected.setVisibility(View.GONE);
                    if(filesSelected.size()==0){
                        changeAction();
                    }
                }
                else {
                    filesSelected.add(filE);
                    changeAction();
                    imgselected.setVisibility(View.VISIBLE);
                }
            }
            else {
                if(filE.getFileType()== FilE.FileType.FILE){
                    filesSelected.add(filE);
                    sendfile();
                }
                else if(filE.getFileType()== FilE.FileType.FOLDER){
                    FilE.currentfilePath=filE.getPath();
                    updateData(FilE.getfilEsfrompath(FilE.currentfilePath));
                }
            }
        }
    }

    class MOnlongclickListener implements View.OnLongClickListener{
        int position;
        ImageView imgselected;
        MOnlongclickListener(int position, ImageView imageView){
            this.position=position;
            this.imgselected=imageView;
        }
        @Override
        public boolean onLongClick(View v) {
            if(multiselectMode){
                return false;
            }
            FilE filE=filEList.get(position);
            if(filE.getFileType()== FilE.FileType.FOLDER){
                MainActivity.mainHandler.sendMessage(Message.obtain(MainActivity.mainHandler,FilE.MSG_FILEERROR,new String("can not send folders")));
                return false;
            }
            multiselectMode=true;
            imgselected.setVisibility(View.VISIBLE);
            return true;
        }
    }

    @Override
    public int getItemCount() {
        return filEList.size();
    }

    private void changeAction(){
        MainActivity.mainHandler.sendEmptyMessage(FilE.MSG_CHANGEACTION);
    }

    public void sendfile(){
        List<Transfile> nwSendFiles=new ArrayList<>();
        for(FilE filE:filesSelected){
            Transfile transfile=new Transfile(filE.getPath(),0,true,filE.getName());
            int size=filE.getSize();
            size=filE.getSizeUnit()== FilE.SizeUnit.mb?size*1024:size;
            transfile.setFulBuf(size/80);
            transfile.setState(true);
            nwSendFiles.add(transfile);
        }
        List<Transfile> nwAllfiles=MConnection.getTargetDevice().getTransfiles();
        nwAllfiles.addAll(nwSendFiles);
        TransactionActivity.transactHandler.sendEmptyMessage(Transfile.MSG_SAVERECORD);
        MainActivity.mainHandler.sendEmptyMessage(FilE.MSG_SENDFILES);
    }

    public void doclear(){
        filesSelected.clear();
        multiselectMode=false;
    }

}
