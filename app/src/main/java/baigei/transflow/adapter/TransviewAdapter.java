package baigei.transflow.adapter;

import android.content.Context;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import baigei.example.transflow.R;
import baigei.transflow.UI.TransactionActivity;
import baigei.transflow.core.BaseTransfer;
import baigei.transflow.core.ConnectionService;
import baigei.transflow.core.FilE;
import baigei.transflow.core.Transfile;

public class TransviewAdapter extends ArrayAdapter<Transfile> {
    public boolean onAction;
    private Context contextTransaction;
    private List<Transfile> selectedTrans;
    private List<Transfile> allTrans;
    public TransviewAdapter(Context context, ArrayList<Transfile> transfiles){
        super(context,0,transfiles);
        contextTransaction=context;
        selectedTrans=new ArrayList<>();
        allTrans=transfiles;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final Transfile transfile=allTrans.get(position);
        if(convertView==null){
            convertView= LayoutInflater.from(getContext()).inflate(R.layout.item_trans,parent,false);
        }
        if(transfile==null){
            return convertView;
        }
        TextView nametext=convertView.findViewById(R.id.trans_name);
        nametext.setText(transfile.getName());
        TextView progresstext=convertView.findViewById(R.id.trans_progress);
        TextView statetext=convertView.findViewById(R.id.trans_state);
        final ImageView imgselected=convertView.findViewById(R.id.trans_selected);
        FilE.SizeUnit sizeUnit;
        String state;
        if(transfile.getFulBuf()!=0){
            int fulsize=transfile.getFulBuf()* BaseTransfer.BUFFER;
            int size=transfile.getRecordBuf();
            if(fulsize>1024){
                sizeUnit= FilE.SizeUnit.mb;
                fulsize=fulsize/1024;
                size=size/1024;
            }
            else {
                sizeUnit=FilE.SizeUnit.kb;
            }
            String unit=sizeUnit== FilE.SizeUnit.kb?" kb":" mb";
            progresstext.setText(new String(size+unit+"/"+fulsize+unit));
            if(transfile.getSate()){
                if(transfile.getTosend()){
                    state="sending";
                }
                else {
                    state="receiving";
                }
            }
            else {
                state="pause";
            }
            statetext.setText(state);
        }
        else {
            progresstext.setText(new String("undefined"));
            statetext.setText(new String("undefined"));
        }
        convertView.setOnClickListener(new MyOnclickListener(transfile,imgselected));
        convertView.setOnLongClickListener(new MyOnlongclickListener(transfile,imgselected));
        return convertView;
    }

    @Override
    public int getCount() {
        return allTrans.size();
    }

    class MyOnclickListener implements View.OnClickListener{
        Transfile file;
        ImageView imageView;
        MyOnclickListener(Transfile transfile,ImageView imgselected){
            file=transfile;
            imageView=imgselected;
        }
        @Override
        public void onClick(View v) {
            if(onAction){
                if(!selectedTrans.contains(file)){
                    if(selectedTrans.size()==0){
                        TransactionActivity.transactHandler.sendMessage(Message.obtain(TransactionActivity.transactHandler,Transfile.MSG_CHANGEACTION,Transfile.ACTION_CANCEL));
                        TransactionActivity.transactHandler.sendMessage(Message.obtain(TransactionActivity.transactHandler,Transfile.MSG_CHANGEACTION,Transfile.ACTION_TOP));
                    }
                    imageView.setVisibility(View.VISIBLE);
                    selectedTrans.add(file);
                    if(file.getPath().equals(BaseTransfer.getcurrentTrans().getPath())){
                        TransactionActivity.transactHandler.sendMessage(Message.obtain(TransactionActivity.transactHandler,Transfile.MSG_CHANGEACTION,Transfile.ACTION_STATE));
                    }
                }
                else {
                    if(selectedTrans.size()==1){
                        TransactionActivity.transactHandler.sendMessage(Message.obtain(TransactionActivity.transactHandler,Transfile.MSG_CHANGEACTION,Transfile.ACTION_UNCANCEL));
                        TransactionActivity.transactHandler.sendMessage(Message.obtain(TransactionActivity.transactHandler,Transfile.MSG_CHANGEACTION,Transfile.ACTION_UNTOP));
                    }
                    imageView.setVisibility(View.GONE);
                    selectedTrans.remove(file);
                    if(file.getPath().equals(BaseTransfer.getcurrentTrans().getPath())){
                        TransactionActivity.transactHandler.sendMessage(Message.obtain(TransactionActivity.transactHandler,Transfile.MSG_CHANGEACTION,Transfile.ACTION_UNSTATE));
                    }
                }
            }
        }
    }
    class MyOnlongclickListener implements View.OnLongClickListener{
        Transfile file;
        ImageView imageView;
        MyOnlongclickListener(Transfile transfile,ImageView imgselected){
            file=transfile;
            imageView=imgselected;
        }

        @Override
        public boolean onLongClick(View v) {
            if(!onAction){
                onAction=true;
                TransactionActivity.transactHandler.sendEmptyMessage(Transfile.MSG_STARTTRANSACTION);
            }
            return false;
        }
    }

    public void pauseOrresume(){
        List<Transfile> newall=new ArrayList<>();
        for(Transfile file:allTrans){
            if(selectedTrans.contains(file)){
                file.setState(!file.getSate());
            }
            newall.add(file);
        }
        allTrans=newall;
    }

    public void removefile(){
        for(Transfile file:selectedTrans){
            allTrans.remove(file);
        }
        selectedTrans.clear();
    }
    public void topup(){
        for(Transfile file:selectedTrans){
            allTrans.remove(file);
        }
        selectedTrans.addAll(allTrans);
        allTrans=selectedTrans;
        selectedTrans.clear();
    }

    public void update(List<Transfile> newlist){
        allTrans=newlist;
    }
}
