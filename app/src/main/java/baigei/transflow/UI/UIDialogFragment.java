package baigei.transflow.UI;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import baigei.transflow.core.NsdMgr;

public class UIDialogFragment extends DialogFragment {
    String message;
    String pos;
    String neg;
    DialogInterface.OnClickListener posclick;
    DialogInterface.OnClickListener negclick;
    public void setDialog(String message, String pos, String neg, DialogInterface.OnClickListener posclick, DialogInterface.OnClickListener negclick){
        this.message=message;
        this.pos=pos;
        this.neg=neg;
        this.posclick=posclick;
        this.negclick=negclick;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder=new AlertDialog.Builder(NsdMgr.foregroundActivity);
        builder.setMessage(message).setPositiveButton(pos,posclick).setNegativeButton(neg,negclick);
        return builder.create();
    }
}
