package baigei.transflow.UI;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import baigei.example.transflow.R;
import baigei.transflow.adapter.TransviewAdapter;
import baigei.transflow.core.ConnectionService;
import baigei.transflow.core.NsdMgr;
import baigei.transflow.core.TransRecord;
import baigei.transflow.core.Transfile;
import baigei.transflow.entity.MConnection;
import baigei.transflow.entity.MDevice;

public class TransactionActivity extends MbaseActivity{
    ActionMode transAction;
    MDevice target;
    TransviewAdapter transviewAdapter;
    MenuItem action_Top;
    MenuItem action_State;
    MenuItem action_Remove;
    public static TransactHandler transactHandler;
    public static class TransactHandler extends Handler{
        private WeakReference<Activity> mtarget;
        TransactHandler(Activity target){
            mtarget=new WeakReference<>(target);
        }
        public void setMtarget(Activity target){
            mtarget.clear();
            mtarget=new WeakReference<>(target);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            TransactionActivity activity=(TransactionActivity) mtarget.get();
            if(msg.what==Transfile.MSG_STARTTRANSACTION){
                activity.startTransAction();
            }
            else if(msg.what==Transfile.MSG_CHANGEACTION){
                activity.changeAction((int)msg.obj);
            }
            else if(msg.what==Transfile.MSG_SAVERECORD){
                activity.saveRecords();
            }
            else if(msg.what==Transfile.MSG_UPDATETRANSVIEW){
                activity.updateView();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        loadRecords();
        if(transactHandler==null){
            transactHandler=new TransactHandler(this);
        }
        else {
            transactHandler.setMtarget(this);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_WRITE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_WRITE: {
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    initiate();
                }
                else {
                    finish();
                }
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        NsdMgr.foregroundActivity=this;
    }

    void initiate(){
        ListView listView=findViewById(R.id.list_trans);
        transviewAdapter=new TransviewAdapter(this,(ArrayList<Transfile>) target.getTransfiles());
        listView.setAdapter(transviewAdapter);
        transviewAdapter.onAction=false;

    }

    @Override
    protected void onStop() {
        super.onStop();
        saveRecords();
    }

    void saveRecords(){
        target=MConnection.getTargetDevice();
        SharedPreferences sharedPreferences=this.getSharedPreferences("DEVICE_"+target.getName(),Context.MODE_PRIVATE);
        List<Transfile> transfiles=target.getTransfiles();
        if(transfiles==null) {
            return;
        }
        SharedPreferences.Editor editor=sharedPreferences.edit();
        Set<String> fileset=new HashSet<>();
        for(Transfile file:transfiles){
            String name=file.getName();
            fileset.add(name);
            editor.putString(name+"_"+getString(R.string.recordpath),file.getPath());
            editor.putBoolean(name+"_"+getString(R.string.tosend),file.getTosend());
            editor.putInt(name+"_"+getString(R.string.recordbuf),file.getRecordBuf());
        }
        editor.putStringSet(getString(R.string.files),fileset);
        editor.apply();
        updateView();
    }

    void loadRecords(){
        MDevice target=MConnection.getTargetDevice();
        if(target==null){
            return;
        }
        Set<String> files=new HashSet<String>();
        SharedPreferences sharedPreferences=this.getSharedPreferences("DEVICE_"+target.getName(), Context.MODE_PRIVATE);
        files=sharedPreferences.getStringSet(getString(R.string.files),files);
        if(files.size()==0) {
            return;
        }
        List<Transfile> transfiles=new ArrayList<>();
        for(String file:files){
            boolean send=sharedPreferences.getBoolean(file+"_"+getString(R.string.tosend),true);
            String path=sharedPreferences.getString(file+"_"+getString(R.string.recordpath),"");
            int buf=sharedPreferences.getInt(file+"_"+getString(R.string.recordbuf),0);
            transfiles.add(new Transfile(path,buf,send,file));
        }
        target.setTransfiles(transfiles);
    }

    class TransActionModeCallback implements ActionMode.Callback{
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_trans,menu);
            action_Remove=findViewById(R.id.action_remove);
            action_State=findViewById(R.id.action_PauseorResume);
            action_Top=findViewById(R.id.action_topup);
            action_State.setEnabled(false);
            action_Top.setEnabled(false);
            action_Remove.setEnabled(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if(item.getItemId()==R.id.action_PauseorResume){
                transviewAdapter.pauseOrresume();
                ConnectionService.transferHandler.sendEmptyMessage(Transfile.MSG_CHANGESTATE);
            }
            else if(item.getItemId()==R.id.action_remove){
                transviewAdapter.removefile();
                ConnectionService.transferHandler.sendEmptyMessage(Transfile.MSG_SENDUPDATELIST);
            }
            else{
                transviewAdapter.topup();
                ConnectionService.transferHandler.sendEmptyMessage(Transfile.MSG_SENDUPDATELIST);
            }
            transviewAdapter.notifyDataSetChanged();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            transAction=null;

        }
    }

    void changeAction(int code){
        if(code==Transfile.ACTION_CANCEL){
            action_Remove.setEnabled(true);
        }
        else if(code==Transfile.ACTION_STATE){
            action_State.setEnabled(true);
        }
        else if(code==Transfile.ACTION_TOP){
            action_Top.setEnabled(true);
        }
        else if(code==Transfile.ACTION_UNCANCEL){
            action_Remove.setEnabled(false);
        }
        else if(code==Transfile.ACTION_UNSTATE){
            action_State.setEnabled(false);
        }
        else if(code==Transfile.ACTION_UNTOP){
            action_Top.setEnabled(false);
        }
    }

    void updateView(){
        transviewAdapter.update(MConnection.getTargetDevice().getTransfiles());
        transviewAdapter.notifyDataSetChanged();
    }

    void startTransAction(){
        if(transAction==null){
            transAction=this.startActionMode(new TransActionModeCallback());
        }
    }
}
