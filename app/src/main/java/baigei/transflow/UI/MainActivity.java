package baigei.transflow.UI;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.List;

import baigei.example.transflow.R;
import baigei.transflow.adapter.FileviewAdapter;
import baigei.transflow.core.BaseTransfer;
import baigei.transflow.core.ConnectionService;
import baigei.transflow.core.FilE;
import baigei.transflow.core.NsdMgr;
import baigei.transflow.core.Transfile;
import baigei.transflow.entity.MConnection;
import baigei.transflow.entity.MDevice;

public class MainActivity extends MbaseActivity{
    boolean tosend;
    FileviewAdapter fileviewAdapter;
    MenuItem action_send;
    MenuItem action_clear;
    MenuItem action_store;
    MenuItem action_cancel;
    RecyclerView recyclerView;
    public static MainHandler mainHandler;
    ActionMode actionMode;
    ActionMode storeMode;
    Toolbar topbar;

    public static class MainHandler extends Handler{
        private WeakReference<Activity> mtarget;
        MainHandler(Activity target){
            mtarget=new WeakReference<Activity>(target);
        }
        public void setMtarget(Activity target){
            mtarget.clear();
            mtarget=new WeakReference<Activity>(target);
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivity activity=(MainActivity)mtarget.get();

            if(msg.what== NsdMgr.MSG_REGISTERED_SUCCESS){
                activity.initiate();
            }
            else if(msg.what==NsdMgr.MSG_REGISTERED_ERROR){
                activity.showError(((String) msg.obj));
            }
            else if(msg.what==FilE.MSG_FILEERROR){
                activity.showError((String)msg.obj);
            }
            else if(msg.what==FilE.MSG_STARTMULTISELECT){
                activity.startMultiactionMode();
            }
            else if(msg.what==FilE.MSG_SENDFILES){
                if(MConnection.getTargetDevice()==null){
                    activity.showError("not connected to any target device");
                }
                else {
                    activity.startTransActivity();
                }
            }
            else if(msg.what==FilE.MSG_CHANGEACTION){
                activity.changeAction();
            }
            else if(msg.what==ConnectionService.MSG_SERVICESTARTED){
                activity.onstartedService();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent=getIntent();
        if(intent==null){
            tosend=true;
        }else {
            tosend=intent.getBooleanExtra("tosend",true);
        }
        if(mainHandler==null){
            mainHandler=new MainHandler(this);
        }
        else {
            mainHandler.setMtarget(this);
        }
        if(tosend){
            FilE.currentfilePath=getExternalFilesDir("").getPath();
            NsdMgr.foregroundActivity=this;
            this.showstates("network initiating");
        }
        topbar=findViewById(R.id.topbar_main);
        setSupportActionBar(topbar);
        recyclerView=(RecyclerView)findViewById(R.id.recycler_main);
        recyclerView.setVisibility(View.INVISIBLE);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<FilE> filEList=FilE.getfilEsfrompath(FilE.currentfilePath);
        fileviewAdapter=new FileviewAdapter(filEList,tosend);
        fileviewAdapter.multiselectMode=false;
        recyclerView.setAdapter(fileviewAdapter);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},PERMISSION_WRITE);
        }
        else {
            if(tosend){
                Intent intentservice=new Intent(this, ConnectionService.class);
                startService(intentservice);
            }
            else {
                initiate();
            }
        }
    }

    void onstartedService(){
        NsdMgr.getInstance().registerService(this);
        initiate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        NsdMgr.foregroundActivity=this;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_WRITE: {
                if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    if(tosend){
                        Intent intent=new Intent(this, ConnectionService.class);
                        startService(intent);
                    }
                    else {
                        initiate();
                    }
                }
                else {
                    finish();
                }
            }

        }
    }

    void initiate(){
        if(tosend){
            this.showstates("device online");
        }else {
            startStoreActionMode();
        }
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        if (id == R.id.action_back) {
            FilE.currentfilePath= getExternalFilesDir(null).getParent();
            updatefiles();
        } else {
            if (id == R.id.action_connection) {
                intent = new Intent(this, ConnectionActivity.class);
            } else if (id == R.id.action_setting) {
                intent = new Intent(this, SettingActivity.class);
            } else {
                intent = new Intent(this, TransactionActivity.class);
            }
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    void updatefiles(){
        fileviewAdapter.updateData(FilE.getfilEsfrompath(FilE.currentfilePath));
        topbar.setTitle((new File(FilE.currentfilePath)).getName());
    }

    class ActionModeCallback implements ActionMode.Callback{
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_selection,menu);
            action_send=findViewById(R.id.selection_Send);
            action_send.setEnabled(false);
            action_clear=findViewById(R.id.selection_Clear);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode=null;
            fileviewAdapter.multiselectMode=false;
            fileviewAdapter.doclear();
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if(item.getItemId()==R.id.selection_Send){
                mode.finish();
                fileviewAdapter.sendfile();
                startTransActivity();
            }
            else if(item.getItemId()==R.id.selection_Clear){
                fileviewAdapter.doclear();
                fileviewAdapter.notifyDataSetChanged();
            }
            return true;
        }
    }

    class StoreActionModeCallback implements ActionMode.Callback{
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_store,menu);
            action_cancel=findViewById(R.id.storing_cancel);
            action_store=findViewById(R.id.storing_store);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId()==R.id.storing_cancel){
                ConnectionService.transferHandler.sendEmptyMessage(Transfile.MSG_SENDUPDATELIST);
            }else if(item.getItemId()==R.id.storing_store){
                List<Transfile> nwAllfiles=MConnection.getTargetDevice().getTransfiles();
                for(Transfile transfile: BaseTransfer.tosaveFileslist){
                    transfile.setPath(FilE.currentfilePath+transfile.getName());
                    nwAllfiles.add(transfile);
                }
                MConnection.getTargetDevice().setTransfiles(nwAllfiles);
                ConnectionService.transferHandler.sendEmptyMessage(Transfile.MSG_CHANGESTATE);
                mode.finish();
            }
            return true;
        }
    }

    void startMultiactionMode(){
        if(actionMode==null){
            actionMode=startActionMode(new ActionModeCallback());
        }
    }

    void startStoreActionMode(){
        if(storeMode==null){
            storeMode=startActionMode(new StoreActionModeCallback());
        }
    }
    void changeAction(){
        if(fileviewAdapter.filesSelected.size()==0){
            action_send.setEnabled(false);
            action_clear.setEnabled(false);
        }
        else {
            action_send.setEnabled(true);
            action_clear.setEnabled(true);
        }
    }
    void startTransActivity(){
        Intent intent=new Intent(this,TransactionActivity.class);
        startActivity(intent);
    }
}
