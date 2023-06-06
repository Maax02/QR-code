package com.example.qrscan;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.tukaani.xz.LZMAInputStream;

public class MainActivity extends AppCompatActivity {
    //QR CODE GENERATOR
    public EditText editText;
    public Button generateButton;
    public ImageView imageView;

    //QR CODE GENERATED
    public Bitmap bitmap;
    public Uri contentUri;

    //EMAIL
    public EditText emailText;
    public Button sendButton;

    //SCANS
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

        /**--------------QR CODE GENERATOR--------------*/
        editText = findViewById(R.id.editText);
        generateButton = findViewById(R.id.generateButton);
        imageView = findViewById(R.id.imageView);

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MultiFormatWriter mFW = new MultiFormatWriter();

                try {
                    String s = editText.getText().toString();
                    if(s.equals("")) {
                        Toast.makeText(MainActivity.this, "Text needs to be at least 1 symbol long", Toast.LENGTH_SHORT).show();
                    } else {
                        BitMatrix bitMatrix = mFW.encode(editText.getText().toString(), BarcodeFormat.QR_CODE, 500, 500);

                        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                        bitmap = barcodeEncoder.createBitmap(bitMatrix);

                        imageView.setImageBitmap(bitmap);
                        saveToCache();
                    }

                } catch (WriterException e) {
                    throw new RuntimeException("Something went wrong (93)");
                }
            }
        });

        /**--------------QR SCANS--------------*/
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
                QRCodeLauncher.launch(scanOptions);
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

        /**--------------SEND EMAIL--------------*/
        emailText = findViewById(R.id.emailAddress);
        sendButton = findViewById(R.id.sendMail);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMail();
            }
        });
    }

    /**--------------SAVE--------------*/
    private void saveToCache() {
        try {
            File cachePath = new File(this.getCacheDir(), "images");
            cachePath.mkdirs();
            FileOutputStream stream = new FileOutputStream(cachePath + "/image.png");
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        File imagePath = new File(this.getCacheDir(), "images");
        File newFile = new File(imagePath, "image.png");
        contentUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", newFile);
    }

    /**--------------EMAIL--------------
     * sends a message with the QR CODE image*/
    private void sendMail() {
        String to = emailText.getText().toString();
        String[] toArray = to.split(", ");
        String subject = "QR CODE";

        if (contentUri != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_EMAIL, toArray);
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, "Send via"));
        }
    }

    private void sendMessageMail(String message, String subject) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setType("message/rfc822");
        startActivity(Intent.createChooser(intent, "Send via"));
    }

    /**--------------QRCODE--------------*/
    ActivityResultLauncher<ScanOptions> QRCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
       if(result.getContents() != null) {
           AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
           builder.setTitle("Scanned");
           String QRCodeString = result.getContents();
           builder.setMessage(QRCodeString);
           builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                   dialog.dismiss();
               }
           }).show();
           builder.setNegativeButton("Send", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                   sendMessageMail(QRCodeString, "QRCode result");
               }
           }).show();
       }
    });

    /**--------------PAY BY SQUARE--------------*/
    ActivityResultLauncher<ScanOptions> payBySquareLauncher = registerForActivityResult(new ScanContract(), result -> {
        if(result.getContents() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Scanned");
            String payBySquareResult = "";
            try {
                payBySquareResult = decodeBySquare(result.getContents());
                builder.setMessage(payBySquareResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();

            String finalPayBySquareResult = payBySquareResult;
            builder.setNegativeButton("Send", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sendMessageMail(finalPayBySquareResult, "PayBySquare result");
                    dialog.dismiss();
                }
            }).show();
        }
    });

    /**--------------DECODER--------------
     * @param encodedData data you want to decode
     * @return decoded data*/

    public static String decodeBySquare(String encodedData) throws IOException {
        initHexToBin();
        StringBuilder almostDecodedData = new StringBuilder();
        for (char c : encodedData.toCharArray()) {
            almostDecodedData.append(hexToBin.get(c));
        }

        int k = 0;
        int x = 0;
        int y = 0;
        int max = 0;
        byte[] decimalValues = new byte[1000];
        StringBuilder base32hex = new StringBuilder();
        //StringBuilder binary = new StringBuilder();
        //binary.append(almostDecodedData);

        StringBuilder header = new StringBuilder();
        for(int i = 0; i < 16; i++) {
            header.append(almostDecodedData.charAt(i));
        }

        //Skipping header (not decompressed by LZMA)
        for(int i = 16; i < almostDecodedData.length(); i++) {
            base32hex.append(almostDecodedData.charAt(i));
            if((i + 1) % 8 == 0) {
                int dec = Integer.parseInt(base32hex.toString(),2);
                if(k == 0) {
                    x = dec;
                }
                if(k == 1) {
                    y = dec;
                }
                decimalValues[k] = (byte) dec;
                k++;
                max = (i + 1) / 8;
                base32hex = new StringBuilder();
            }
        }

        byte[] bite = new byte[max + 550];
        System.arraycopy(decimalValues, 0, bite, 0, max + 550);

        byte[] newByte = new byte[550];
        for(int i = 2; i < max; i++) {
            newByte[i - 2] = bite[i];
        }

        /**--------------LZMA--------------*/
        long decompressionCount = 256L*y + x;
        LZMAInputStream lzmaInputStream = new LZMAInputStream(new ByteArrayInputStream(newByte), decompressionCount, 3, 0, 2, 131072, null);
        ByteArrayOutputStream decompressedBytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int size;
        while ((size = lzmaInputStream.read(buffer)) != -1) {
            decompressedBytes.write(buffer, 0, size);
        }
        lzmaInputStream.close();

        /**--------------PRETTIFY--------------*/
        String decodedData = decompressedBytes.toString();
        int tabCount = 0;
        int date = 0;
        String[] appendixD = {"1 InvoiceId: ", "\n2 Payments (count): ", "\n3 PaymentsOptions: ", "\n4 Amount: ",
                "\n5 CurrencyCode: ", "\n6 PaymentDueDate: ", "\n7 VariableSymbol: ", "\n8 ConstantSymbol: ",
                "\n9 SpecificSymbol:", "\n10 OriginatorsReferenceInformation: ", "\n11 PaymentNote: ",
                "\n12 BankAccounts (count): ", "\n13 Iban: ", "\n14 Swift: ", "\n15 StandingOrderExt: ", "\n16 Day: ",
                "\n17 Month: ", "\n18 Periodicity: ", "\n19 LastDate: ", "\n20 DirectDebitExt: ", "\n21 DirectDebitScheme: ",
                "\n22 DirectDebitType: ", "\n23 VariableSymbol: ", "\n24 SpecificSymbol: ", "\n25 OriginatorsReferenceInformation: ",
                "\n26 MandateID: ", "\n27 CreditorID: ", "\n28 ContractID: ", "\n29 MaxAmount: ", "\n30 ValidTillDate: ",
                "\n31 BeneficiaryName: ", "\n32 BeneficiaryAddressLine1: ", "\n33 BeneficiaryAddressLine2: "
        };

        String[] documentation = {"|Číslo faktúry, jednoznačne identifikuje faktúru v rámci účtovného systému spoločnosti|",
                "|Zoznam jednej alebo viacerých platieb v prípade hromadného príkazu. Hlavná (preferovaná) platba sa uvádza ako prvá {1, unbounded}|",
                "|Možnosti platby sa dajú kombinovať. Oddeľujú sa medzerou a treba uviesť vždy aspoň jednu z možností. paymentorder - platobný príkaz standingorder - trvalý príkaz, údaje sa vyplnia do StandingOrderExt directdebit - inkaso, údaje sa vyplnia do DirectDebitExt|",
                "|Čiastka platby. Povolené sú len kladné hodnoty. Desatinná čast je oddelená bodkou. Môže ostať nevyplnené, napríklad pre dobrovoľný príspevok (donations). Príklad: Tisíc sa uvádza ako \"1000\". Jedna celá deväťdesiatdeväť sa uvádza ako \"1.99\". Desať celých peťdesiat sa uvádza ako \"10.5\". Nula celá nula osem sa uvádza ako \"0.08\"|",
                "|Mena platby v ISO 4217 formáte (3 písmená skratka). Príklad: \"EUR\"|",
                "|Dátum splatnosti vo formáte ISO 8601 \"RRRR-MM-DD\". Nepovinný údaj. V prípade trvalého príkazu označuje dátum prvej platby|",
                "|Variabilný symbol je maximálne 10 miestne číslo. Nepovinný údaj|",
                "|Konštantný symbol je 4 miestne identifikačné číslo. Nepovinný údaj|",
                "|Špecifický symbol je maximálne 10 miestne číslo. Nepovinný údaj|",
                "|Referenčná informácia prijímateľa podľa SEPA|",
                "|Správa pre prijímateľa. Údaje o platbe, na základe ktorých príjemca bude môcť platbu identifikovať. Odporúča sa maximálne 140 Unicode znakov|",
                "|Zoznam bankových účtov {1, unbounded}|",
                "|Medzinárodné číslo bankového účtu vo formáte IBAN. Príklad: \"SK8209000000000011424060\"|",
                "|Medzinárodný bankový identifikačný kód (z ang. Bank Identification Code)|",
                "|Rozšírenie platobných údajov o údaje pre nastavenie trvalého príkazu|",
                "|Deň platby vyplývajúci z opakovania (Periodicity). Deň v mesiaci je číslo medzi 1 a 31. Deň v týždni je číslo medzi 1 a 7 (1 = pondelok, 2 =utorok, …, 7 = nedeľa)|",
                "|Medzerou oddelený zoznam mesiacov, v ktoré sa má platba uskutočniť|",
                "|Opakovanie (periodicita) trvalého príkazu|",
                "|Dátum poslednej platby v trvalom príkaze|",
                "|Rozšírenie platobných údajov o údaje pre nastavenie a identifikáciu inkasa|",
                "|Inksaná schéma. Uvádza ja jedna z možností: SEPA - Inkaso zodpovedá schéme SEPA. other - iné|",
                "|Typ inkasa. Uvádza ja jedna z možností: one-off - jednorázové inkaso recurrent - opakované inkaso|",
                "|Variabilný symbol. Vypĺňa sa len v prípade, ak sa odlišuje od variabilného symbolu v platobnom príkaze|",
                "|Špecifický symbol. Vypĺňa sa len v prípade, ak sa odlišuje od špecifického symbolu v platobnom príkaze|",
                "|Referenčná informácia. Použije sa len na prechodné obdobie z variabilného a špecifického symbolu na SEPA inkaso|",
                "|Identifikácia mandátu medzi veriteľom a dlžníkom podľa SEPA|",
                "|Identifikácia veriteľa podľa SEPA|",
                "|Identifikácia zmluvy medzi veriteľom a dlžníkom podľa SEPA|",
                "|Maximálna čiastka inkasa|",
                "|Dátum platnosti inkasa. Platnosť inkasa zaníka dňom tohto dátumu|",
                "|Rozšírenie o meno príjemcu|",
                "|Rozšírenie o adresu príjemcu|",
                "|Rozšírenie o adresu príjemcu (druhý riadok)|",
        };

        StringBuilder square = new StringBuilder();
        square.append("Header: ");
        square.append(header);
        square.append("\n");
        square.append(appendixD[tabCount]);
        int paymentCount = 0;

        //Skipping CheckSum
        for(int i = 0; i < decodedData.length(); i++) {
            char tab = decodedData.charAt(i);
            if(tab == '\t') {
                square.append("\n");
                square.append(documentation[tabCount]);
                tabCount++;
                if(tabCount == 14) {
                    paymentCount -= 1;
                    if(paymentCount > 0) {
                        tabCount = 12;
                    }
                }
                square.append(appendixD[tabCount]);
            } else {
                if(tabCount == 19) {
                    if(tab == '0') {    //from 20 to 29 are skipped
                        square.append(tab);
                        for(int t = 20; t < 30; t++) {
                            square.append(appendixD[t]);
                            square.append(documentation[t]);
                            tabCount++;
                        }
                    }
                } else if(tabCount == 14) {
                    if(tab == '0') {    //from 15 to 19 are skipped
                        square.append(tab);
                        for(int t = 15; t < 19; t++) {
                            square.append(appendixD[t]);
                            square.append(documentation[t]);
                            tabCount++;
                        }
                    }
                }

                else if(tabCount == 11) {    //Bank Accounts Count
                    paymentCount = paymentCount * 10 + (int) tab - 48;
                }

                if(tabCount != 14 && tabCount != 19) {
                    square.append(tab);
                }
                if(tabCount == 5) {
                    date++;
                    if(date == 4 || date == 6) {
                        square.append("/");
                    }

                }
            }
        }
        square.append("\n");
        square.append(documentation[32]);
        return square.toString();
    }

    /** ENCODING TABLE:
     * initialize the encoding table*/
    private static void initHexToBin() {
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