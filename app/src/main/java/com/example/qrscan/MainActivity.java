package com.example.qrscan;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.Arrays;

import dev.keiji.util.Base32;

public class MainActivity extends AppCompatActivity {

    public Button button;
    public RadioGroup radioGroup;
    boolean isChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioGroup = findViewById(R.id.radioGroupBeep);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
                    case R.id.beepOff:
                        isChecked = false;
                        Toast.makeText(MainActivity.this, "Beep is turned off", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.beepOn:
                        isChecked = true;
                        Toast.makeText(MainActivity.this, "Beep is turned on", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        button = findViewById(R.id.buttonScan);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanOptions scanOptions = new ScanOptions();
                scanOptions.setPrompt("Volume up -> flash on");
                scanOptions.setOrientationLocked(true);
                scanOptions.setBeepEnabled(isChecked);
                scanOptions.setCaptureActivity(CaptureAct.class);
                barLauncher.launch(scanOptions);
            }
        });
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
       if(result.getContents() != null) {
           AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
           builder.setTitle("Scanned");
           //String s = Arrays.toString(Base32.decode(result.getContents()));
           //builder.setMessage(Arrays.toString(Base32.decodeExtendedHex(result.getContents())));
           builder.setMessage(result.getContents());
           builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                   dialog.dismiss();
               }
           }).show();
       }
    });
}