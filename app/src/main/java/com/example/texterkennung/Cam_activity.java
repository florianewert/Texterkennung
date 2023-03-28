package com.example.texterkennung;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.views.GestureImageView;
import com.alexvasilkov.gestures.views.interfaces.GestureView;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class Cam_activity extends AppCompatActivity {
    CameraSource cameraSource;
    SurfaceView surfaceView;
    ImageButton scan, cancel;
    private static final int PERMISSION = 100;
    Bitmap bitmap;
    SurfaceView imageView;
    HashMap<Rect, String> wordRectMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cam_layout);
        init();
        startCamera();
        btnAction();
    }

    public void btnAction(){
        scan.setOnClickListener(v -> {
            cameraSource.takePicture(null, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes) {
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    bitmap = rotateBitmap(bitmap, 90); // rotate the bitmap by 90 degrees
                    imageView.setVisibility(View.VISIBLE);
                    drawBitmapOnSurfaceView(detectAndMarkText(bitmap), imageView);

                    imageView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            Log.i("Event", isTouchInsideRectHashMap(wordRectMap, motionEvent) + "");
                        return false;
                        }
                    });

                }
            });
        });

        cancel.setOnClickListener(v ->{
            if(imageView.getVisibility() == View.INVISIBLE){
                finish();
            }else{
                imageView.setVisibility(View.INVISIBLE);
            }
        });
    }
    public Bitmap rotateBitmap(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
    public void init(){
        surfaceView = findViewById(R.id.surfaceView);
        scan = findViewById(R.id.btnScan);
        cancel = findViewById(R.id.btnCancel);
        imageView = findViewById(R.id.surfaceView2);
        imageView.setVisibility(View.INVISIBLE);
    }

    public void startCamera(){
        TextRecognizer textRecognizer;

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {

            }
        });


        cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setAutoFocusEnabled(true)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(Cam_activity.this, new String[]{Manifest.permission.CAMERA},
                                PERMISSION);
                        return;
                    }
                    cameraSource.start(surfaceView.getHolder());
                } catch (IOException ex) {

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
    }
    public Bitmap detectAndMarkText(Bitmap bitmap) {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if(!textRecognizer.isOperational()){
            Log.w("Texterkennung", "Dependencies wurden nicht geladen");
            return bitmap;
        }else{
            Frame imageFrame = new Frame.Builder()
                    .setBitmap(bitmap)
                    .build();

            SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

            Bitmap markedBitmap = bitmap.copy(bitmap.getConfig(), true);

            Canvas canvas = new Canvas(markedBitmap);

            Paint paint = new Paint();
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4.0f);


            wordRectMap = new HashMap<>();

            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                for (Text textLine : textBlock.getComponents()) {
                    for (Text element : textLine.getComponents()) {
                        Rect rect = new Rect(element.getBoundingBox());
                        String word = element.getValue();
                        rect.inset(-5, -5); // Add padding of 5 pixels to all sides
                        wordRectMap.put(rect, word);
                        canvas.drawRect(rect, paint);
                    }
                }
            }

            return markedBitmap;
        }
    }

    public boolean isTouchInsideRectHashMap(HashMap<Rect, String> rectHashMap, MotionEvent event) {

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        int surfaceViewWidth = surfaceView.getWidth();
        int surfaceViewHeight = surfaceView.getHeight();

        float widthRatio = (float) surfaceViewWidth / (float) bitmapWidth;
        float heightRatio = (float) surfaceViewHeight / (float) bitmapHeight;

        int x = (int)Math.round( event.getX());
        int y = (int) Math.round(event.getY());

        for (Rect rect : rectHashMap.keySet()) {
            if (rect.left * widthRatio <= x && x <= rect.right * widthRatio && rect.top * heightRatio<= y && y <= rect.bottom* heightRatio) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("result", rectHashMap.get(rect));
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                return true;
            }
        }
        return false;
    }
    public void drawBitmapOnSurfaceView(Bitmap bitmap, SurfaceView surfaceView) {

        Canvas canvas = surfaceView.getHolder().lockCanvas();

        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawBitmap(bitmap, null, new RectF(0, 0, canvas.getWidth(), canvas.getHeight()), null);
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
        }
    }

}
