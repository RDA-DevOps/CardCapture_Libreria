package com.example.opencv;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.opencv.android.CameraBridgeViewBase;
 import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
 import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;


public class MainActivity_Seconds extends AppCompatActivity  {
    private JavaCameraView cameraBridgeViewBase;
    private Mat grayscaleImage;
    private Mat thresholdImage;
    private int framesCount = 120;
    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_seconds);
        getPermission();
        getPermissionexternal();
        cameraBridgeViewBase = findViewById(R.id.camera_view2);
         cameraBridgeViewBase.setCameraPermissionGranted();

        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC1);
        thresholdImage = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        grayscaleImage.release();
        thresholdImage.release();
    }

            private void saveCapturedImage(Mat mat) {
                Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bmp);

                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DocumentoCaratrasera.jpg";
                File file = new File(filePath);
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    String base64Image = convertBitmapToBase64(bmp);
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private String convertBitmapToBase64(Bitmap bitmap) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                byte[] byteArray = outputStream.toByteArray();
                return Base64.encodeToString(byteArray, Base64.DEFAULT);
            }

            private Mat rotateImage(Mat mat) {
                Mat rotated = new Mat();
                Core.transpose(mat, rotated);
                Core.flip(rotated, rotated, 1);
                return rotated;
            }
            private Rect detectBarcode(Mat inputImage) {
                Bitmap bitmap = Bitmap.createBitmap(inputImage.width(), inputImage.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(inputImage, bitmap);

                BarcodeDetector detector = new BarcodeDetector.Builder(getApplicationContext())
                        .setBarcodeFormats(Barcode.PDF417)
                        .build();

                if (!detector.isOperational()) {
                     return null;
                }

                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Barcode> barcodes = detector.detect(frame);

                for (int i = 0; i < barcodes.size(); i++) {
                    Barcode barcode = barcodes.valueAt(i);
                    if (barcode.format == Barcode.PDF417) {
                        android.graphics.Rect rect = barcode.getBoundingBox();
                        return new Rect(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
                    }
                }

                return null;
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                if (framesCount == 120) {
                    framesCount = 0;
                    Mat inputImage = inputFrame.rgba();
                    Mat grayscaleImage = new Mat();
                    Mat thresholdImage = new Mat();
                    Imgproc.cvtColor(inputImage, grayscaleImage, Imgproc.COLOR_RGBA2GRAY);
                    Imgproc.threshold(grayscaleImage, thresholdImage, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
                    Rect barcodeRect = detectBarcode(thresholdImage);
                    if (barcodeRect != null) {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Código de barras detectado", Toast.LENGTH_SHORT).show());
                        Mat rotated = rotateImage(grayscaleImage);
                        saveCapturedImage(rotated);
                        rotated.release();
                         Imgproc.rectangle(inputImage, barcodeRect.tl(), barcodeRect.br(), new Scalar(0, 255, 0), 4);
                        return inputImage;
                    } else {
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Código de barras no detectado", Toast.LENGTH_SHORT).show());
                    }
                }
                framesCount++;
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

    private void getPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
        }
    }

    private void getPermissionexternal() {
        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity_Seconds.this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity_Seconds.this,"No se concedieron los permisos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
