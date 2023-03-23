package com.example.opencv;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
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
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private JavaCameraView cameraBridgeViewBase;
    Mat rgb, gray;
    MatOfRect rects;
    CascadeClassifier FaceModelCascadeClasifier;
    private int framesCount = 120;
    long timeStart;
    Button btnNewPhoto;
    String base64Image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();
        cameraBridgeViewBase = findViewById(R.id.camera_view);
        cameraBridgeViewBase.setCameraPermissionGranted();
        btnNewPhoto = (Button) findViewById(R.id.btnNewPhoto);


        btnNewPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnNewPhoto.setEnabled(false);
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setVisibility(View.GONE); // ocultar ImageView
                Intent intent = new Intent(MainActivity.this, MainActivity_Seconds.class);
                intent.putExtra("base64Image", base64Image); // enviar base64Image a la siguiente
                startActivity(intent);
            }
        });

        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {

            public void onCameraViewStarted(int width, int height) {
                rgb = new Mat();
                gray = new Mat();
                rects = new MatOfRect();
                try {
                    InputStream inputStream = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                    FileOutputStream outputStream = new FileOutputStream(cascadeFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                    outputStream.close();

                    FaceModelCascadeClasifier = new CascadeClassifier(cascadeFile.getAbsolutePath());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCameraViewStopped() {
                rgb.release();
                gray.release();
                rects.release();
            }

            private void saveCapturedImage(Mat mat) {
                Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bmp);

                base64Image = convertBitmapToBase64(bmp); // Se guarda la imagen en base64 en una variable
                System.out.println(base64Image);
                runOnUiThread(() -> {
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(bmp);
                });
                runOnUiThread(() -> {
                    cameraBridgeViewBase.disableView();
                });
                runOnUiThread(() -> {
                    Button button = findViewById(R.id.btnNewPhoto);
                    button.setEnabled(true);
                });
            }

            private String convertBitmapToBase64(Bitmap bitmap) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                byte[] byteArray = outputStream.toByteArray();
                return Base64.encodeToString(byteArray, Base64.DEFAULT);
            }

            private Mat rotateImage(Mat mat) {
                Mat rotated = new Mat();
                Core.transpose(mat, rotated);
                Core.flip(rotated, rotated, 1);
                return rotated;
            }

            private boolean isFrontalFace(Mat mat) {
                MatOfRect faces = new MatOfRect();
                Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
                FaceModelCascadeClasifier.detectMultiScale(gray, faces, 1.1, 2, 0, new Size(30, 30));

                Rect[] facesArray = faces.toArray();
                if (facesArray.length == 0) {
                    return false;
                }

                Rect faceRect = facesArray[0];
                if (faceRect.width > mat.width() * 0.5 && faceRect.height > mat.height() * 0.5) {
                    return true;
                }

                return false;
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

                rgb = inputFrame.rgba();

                if(framesCount > 10) {

                    framesCount = 0;

                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            Mat grayFrame = inputFrame.gray();
                            double x = grayFrame.width();
                            double y = grayFrame.height();

                            MatOfRect faces = new MatOfRect();

                            double scaleFactor = 1.1;
                            int minNeighbors = 3;
                            Size minSize = new Size(120.0, 120.0);


                            FaceModelCascadeClasifier.detectMultiScale(grayFrame, faces, scaleFactor, minNeighbors, 0, minSize);

                            List<Rect> facesList = faces.toList();

                            for (Rect rect : facesList) {
                                //Imgproc.rectangle(rgb,rect, new Scalar(0,255,0),10);
                                if (rect.x > (x / 2)) {

                                }
                            }
                        }
                    });


                }

                framesCount++;
                return rgb;
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
                Toast.makeText(MainActivity.this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "No se concedieron los permisos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
