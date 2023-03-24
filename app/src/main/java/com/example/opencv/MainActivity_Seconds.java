package com.example.opencv;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;

public class MainActivity_Seconds extends AppCompatActivity {
    JavaCameraView cameraBridgeViewBase;
    Button Inicio;
    String base64Image2;
    boolean captured = false;
    private long startTime = System.currentTimeMillis();
     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_seconds);
        getPermission();
        cameraBridgeViewBase = findViewById(R.id.camera_view2);
        cameraBridgeViewBase.setCameraPermissionGranted();
        Inicio = (Button) findViewById(R.id.Inicio);


         Inicio.setOnClickListener(new View.OnClickListener() {
             String Imagen1= getIntent().getStringExtra("ParteDelantera");
             @Override
             public void onClick(View v) {
                 if (captured) {
                     ImageView imageView = findViewById(R.id.imageView2);
                     imageView.setVisibility(View.GONE);
                     Intent intent = new Intent(MainActivity_Seconds.this,init.class);
                     intent.putExtra("ParteDelantera", Imagen1);
                     intent.putExtra("ParteTrasera", base64Image2);
                     startActivity(intent);
                 } else {
                     Toast.makeText(MainActivity_Seconds.this, "Por favor, tome una foto primero", Toast.LENGTH_SHORT).show();
                 }
             }
         });


         cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {



            public void onCameraViewStarted(int width, int height) {
                startTime = System.currentTimeMillis();
            }

            @Override
            public void onCameraViewStopped() {

            }

             private void saveCapturedImage(Mat mat) {
                 Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                 Utils.matToBitmap(mat, bmp);

                  Matrix matrix = new Matrix();
                 matrix.postRotate(90);
                 Bitmap rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

                 base64Image2 = convertBitmapToBase64(rotatedBmp);
                 System.out.println(base64Image2);
                 runOnUiThread(() -> {
                     ImageView imageView = findViewById(R.id.imageView2);
                     imageView.setImageBitmap(rotatedBmp);
                 });
                 runOnUiThread(() -> {
                     cameraBridgeViewBase.disableView();
                 });
                 captured = true;
                 runOnUiThread(() -> {
                     Inicio.setEnabled(true);
                 });
             }


             private String convertBitmapToBase64(Bitmap bitmap) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                byte[] byteArray = outputStream.toByteArray();
                return Base64.encodeToString(byteArray, Base64.DEFAULT);
            }

            private Mat rotateImage(Mat image) {
                Mat rotated = new Mat();
                 Core.transpose(image, rotated);
                 Core.flip(rotated, rotated, 1);
                return rotated;
            }

            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                if (System.currentTimeMillis() - startTime > 5000) {
                    rotateImage(inputFrame.rgba());
                    saveCapturedImage(inputFrame.rgba());


                }
                return inputFrame.rgba();
            }
        });

        if (OpenCVLoader.initDebug()) {
            cameraBridgeViewBase.enableView();
            cameraBridgeViewBase.enableFpsMeter();
            //cameraBridgeViewBase.setMaxFrameSize(640,360);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraBridgeViewBase.enableView();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraBridgeViewBase.disableView();

    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();


    }



    //Validar Permisos Cada vez que se Inicie la app

    private void getPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity_Seconds.this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity_Seconds.this, "No se concedieron los permisos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}