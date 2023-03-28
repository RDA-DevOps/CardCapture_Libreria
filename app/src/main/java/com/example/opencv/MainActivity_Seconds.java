package com.example.opencv;

import static com.google.android.gms.vision.L.TAG;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
 import java.util.Map;

public class MainActivity_Seconds extends AppCompatActivity {

    private JavaCameraView cameraBridgeViewBase;
    private int framesCount = 120;
    private MultiFormatReader reader;
    String base64Image2;
    Button validar;
    String base64Image1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_seconds);
        getPermission();
        cameraBridgeViewBase = findViewById(R.id.camera_view2);
        cameraBridgeViewBase.setCameraPermissionGranted();
        validar = (Button) findViewById(R.id.validar);


        base64Image1 = getIntent().getStringExtra("base64Image");

        validar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validar.setEnabled(false);
                ImageView imageView = findViewById(R.id.imageView2);
                imageView.setVisibility(View.GONE);
                Intent intent = new Intent(MainActivity_Seconds.this,init.class);
                intent.putExtra("ParteDelantera", base64Image1);
                intent.putExtra("ParteTrasera", base64Image2);
                startActivity(intent);
            }
        });

        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {

            @Override
            public void onCameraViewStarted(int width, int height) {
                reader = new MultiFormatReader();
                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                reader.setHints(hints);
            }

            @Override
            public void onCameraViewStopped() {

            }

            private void saveCapturedImage(Mat mat) {
                Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mat, bmp);

                base64Image2 = convertBitmapToBase64(bmp);
                System.out.println(base64Image2);
                runOnUiThread(() -> {
                    ImageView imageView = findViewById(R.id.imageView2);
                    imageView.setImageBitmap(bmp);
                });
                runOnUiThread(() -> {
                    cameraBridgeViewBase.disableView();
                });
                runOnUiThread(() -> {
                    Button button = findViewById(R.id.validar);
                    button.setEnabled(true);
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


            private Mat processframe(Mat frame) {
                Mat grayFrame = new Mat();
                Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                Mat binaryFrame = new Mat();
                Imgproc.adaptiveThreshold(grayFrame, binaryFrame, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 4);
                return binaryFrame;
            }

            private Result decodeBarcode(Mat binaryFrame) {
                byte[] binaryData = new byte[binaryFrame.cols() * binaryFrame.rows() * (int) binaryFrame.elemSize()];
                binaryFrame.get(0, 0, binaryData);
                int width = binaryFrame.cols();
                int height = binaryFrame.rows();
                int stride = binaryFrame.cols() * binaryFrame.channels();
                PlanarYUVLuminanceSource luminanceSource = new PlanarYUVLuminanceSource(binaryData, width, height, 0, 0, width, height, false);
                Result result = null;
                try {
                    result = reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(luminanceSource)));
                } catch (NotFoundException e) {
                    e.printStackTrace();
                } finally {
                    reader.reset();
                }
                return result;
            }

            ///    private void showToast(String message) {
            ///    runOnUiThread(new Runnable() {
            ///   @Override
            ///  public void run() {
            ///    Toast.makeText(MainActivity_Seconds.this, message, Toast.LENGTH_SHORT).show();
            ///     }
            ///   });
            ///   }

            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                if (framesCount > 60) {
                    framesCount = 0;
                    Mat frame = inputFrame.rgba();
                    if (frame.empty()) {
                        return frame;
                    }


                    Mat binaryFrame = processframe(frame);
                    Result result = decodeBarcode(binaryFrame);
                    if (result != null) {
                        Mat rotatedFrame = rotateImage(frame);
                        saveCapturedImage(rotatedFrame);
                         System.out.println(result);
                        /// ///  Log.d(TAG, "PDF417: " + result.getText());
                      ///  showToast("PORFIN DETECTAMOS ALGO MKPON");
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
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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