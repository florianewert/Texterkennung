package com.example.texterkennung;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import 	android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

 EditText log;

 String logStr;
    SurfaceView surfaceView;
    TextView textView;
Button anwenden;
    CameraSource cameraSource;
private static final int PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        startCamera();
    }

    private void init(){
        surfaceView = findViewById(R.id.surfaceView);
        textView = findViewById(R.id.textView);
        log = findViewById(R.id.etLogic);
        anwenden = findViewById(R.id.button2);
        logStr = "\\d+";
        anwenden.setOnClickListener(v -> {
            logStr = log.getText().toString();
            hideKeyboard(this);
            Snackbar.make(findViewById(R.id.surfaceView), "Logik angewendet!", Snackbar.LENGTH_LONG).show();
        });
    }

    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = activity.getCurrentFocus();
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }
    public void startCamera(){

        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if(!textRecognizer.isOperational()){
            Log.w("Texterkennung", "Dependencies wurden nicht geladen");
        }else{
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK).setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true).setRequestedFps(2.0f).build();

            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                    try {
                        if(ActivityCompat.checkSelfPermission(getApplicationContext(), permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission.CAMERA},
                                    PERMISSION);
                            return;
                        }
                        cameraSource.start(surfaceView.getHolder());
                    }catch (IOException ex){
                        ex.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }


            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if(items.size() != 0){
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i=0; i< items.size();i++){
                                    TextBlock item = items.valueAt(i);
                                    if(check(item.getValue())){
                                        stringBuilder.append(item.getValue());
                                        stringBuilder.append("\n");
                                    }
                                }
                                    textView.setText(stringBuilder.toString());
                            }
                        });
                    }
                }
            });
        }

    }

    public boolean check(String str) {
        Pattern pattern = Pattern.compile(logStr);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }
}