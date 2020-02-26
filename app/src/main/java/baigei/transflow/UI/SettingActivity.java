package baigei.transflow.UI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import baigei.example.transflow.R;
import baigei.transflow.core.NsdMgr;

public class SettingActivity extends MbaseActivity {
    EditText nameText;
    Button namebtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        nameText=findViewById(R.id.settingname_Text);
        namebtn=findViewById(R.id.savename_Button);
        namebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NsdMgr.getInstance().setServiceName(nameText.getText().toString());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        NsdMgr.foregroundActivity=this;
    }



}
