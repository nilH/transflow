package baigei.transflow.UI;

import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import baigei.example.transflow.R;

public class MbaseActivity extends AppCompatActivity {
    Toast toast;
    static final int PERMISSION_WRITE=0x4110;
    public void showdialog(UIDialogFragment uiDialogFragment){
        FragmentManager fm=getSupportFragmentManager();
        uiDialogFragment.show(fm,"connection dialog");
    }
    public void showstates(String text){
        TextView bottomtext=findViewById(R.id.stateText);
        bottomtext.setText(text);
    }
    void showError(String error){
        toast= Toast.makeText(this,error,Toast.LENGTH_SHORT);
        toast.show();
    }
}
