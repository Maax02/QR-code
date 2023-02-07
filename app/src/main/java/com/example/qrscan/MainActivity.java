package com.example.qrscan;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.tukaani.xz.LZMAInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;



public class MainActivity extends AppCompatActivity {

    public Button button;
    public Button buttonBySquare;
    public RadioGroup radioGroup;
    boolean isChecked = false;
    private static Map<Character, String> hexToBin;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioGroup = findViewById(R.id.radioGroupBeep);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("NonConstantResourceId")
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

        buttonBySquare = findViewById(R.id.buttonBySquare);
        buttonBySquare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanOptions scanOptions = new ScanOptions();
                scanOptions.setPrompt("Volume up -> flash on");
                scanOptions.setOrientationLocked(true);
                scanOptions.setBeepEnabled(isChecked);
                scanOptions.setCaptureActivity(CaptureAct.class);
                payBySquareLauncher.launch(scanOptions);
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

    ActivityResultLauncher<ScanOptions> payBySquareLauncher = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Scanned");
            try {
                builder.setMessage(decodeBySquare(result.getContents()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
        }
    });

    public static String decodeBySquare(String encodedData) throws IOException {
        initHexToBin();
        StringBuilder almostDecodedData = new StringBuilder();
        for (char c : encodedData.toCharArray()) {
            almostDecodedData.append(hexToBin.get(c));
        }

        StringBuilder hex = new StringBuilder();
        int k = 0;
        byte[] b = new byte[1000];
        int max = 0;
        for(int i = 16; i < almostDecodedData.length(); i++) {
            hex.append(almostDecodedData.charAt(i));
            if((i + 1) % 8 == 0) {
                int dec = Integer.parseInt(hex.toString(),2);
                b[k] = (byte) dec;
                k++;
                max = i / 8;
                hex = new StringBuilder();
            }
        }

        for(int i = 0; i < 7; i++) {
            b[k] = 0;
        }

        byte[] bite = new byte[max + 100];
        for(int i = 0; i < max + 100; i++) {
            bite[i] = b[i];
        }

        int a = bite[0];
        int bb = bite[1];

        long x = 256*bb + a;

        byte[] newByte = new byte[100];
        for(int i = 2; i < max; i++) {
            newByte[i - 2] = bite[i];
        }


        LZMAInputStream lzmaInputStream = new LZMAInputStream(new ByteArrayInputStream(newByte), x, 3, 0, 2, 131072, null);
        ByteArrayOutputStream decompressedBytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int size;
        while ((size = lzmaInputStream.read(buffer)) != -1) {
            decompressedBytes.write(buffer, 0, size);
        }
        lzmaInputStream.close();

        return decompressedBytes.toString();
    }

    private static void initHexToBin() {
        // encoding table
        hexToBin = new HashMap<>();
        hexToBin.put('0', "00000");
        hexToBin.put('1', "00001");
        hexToBin.put('2', "00010");
        hexToBin.put('3', "00011");
        hexToBin.put('4', "00100");
        hexToBin.put('5', "00101");
        hexToBin.put('6', "00110");
        hexToBin.put('7', "00111");
        hexToBin.put('8', "01000");
        hexToBin.put('9', "01001");
        hexToBin.put('A', "01010");
        hexToBin.put('B', "01011");
        hexToBin.put('C', "01100");
        hexToBin.put('D', "01101");
        hexToBin.put('E', "01110");
        hexToBin.put('F', "01111");
        hexToBin.put('G', "10000");
        hexToBin.put('H', "10001");
        hexToBin.put('I', "10010");
        hexToBin.put('J', "10011");
        hexToBin.put('K', "10100");
        hexToBin.put('L', "10101");
        hexToBin.put('M', "10110");
        hexToBin.put('N', "10111");
        hexToBin.put('O', "11000");
        hexToBin.put('P', "11001");
        hexToBin.put('Q', "11010");
        hexToBin.put('R', "11011");
        hexToBin.put('S', "11100");
        hexToBin.put('T', "11101");
        hexToBin.put('U', "11110");
        hexToBin.put('V', "11111");
    }
}