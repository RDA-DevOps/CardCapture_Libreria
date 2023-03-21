package com.example.opencv;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private JavaCameraView cameraBridgeViewBase;
    private CascadeClassifier cascadeClassifier;
    private Mat rgb, gray;
    private MatOfRect rects;
    private long timeStart;
    private boolean isCounting = false;
    private Button btnNuevaFoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initDebug();
        getPermission();
        getPermissionexternal();
        cameraBridgeViewBase = findViewById(R.id.camera_view);
        cameraBridgeViewBase.setCameraPermissionGranted();
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {

            @Override
            public void onCameraViewStarted(int width, int height) {
                rgb = new Mat();
                gray = new Mat();
                rects = new MatOfRect();
                timeStart = System.currentTimeMillis();
                 /// Aplicar archivo Cascade que detecte los bordes de la cedula:: Pendiente!!!!
                // carga el archivo cascade para deteccion de caras frontales
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

                    cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
                    cascadeDir.delete();
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

            private boolean isFrontalFace(Mat mat) {
                MatOfRect faces = new MatOfRect();
                Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
                cascadeClassifier.detectMultiScale(gray, faces, 1.1, 2, 0, new Size(30, 30));

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


             private boolean tomadoFoto = false;

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

                // Si se ha tomado una foto, devolver una matriz vacía sin procesar
                if (tomadoFoto) {
                    return new Mat();
                }

                btnNuevaFoto = findViewById(R.id.btnNewPhoto);
                btnNuevaFoto.setOnClickListener(v -> {
                    // Reinicia la cámara para tomar una nueva foto
                    cameraBridgeViewBase.enableView();
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageDrawable(null);
                    btnNuevaFoto.setEnabled(false);

                    // Establece tomadoFoto en true para detener la recepción de fotogramas de la cámara
                    tomadoFoto = true;
                });

                Mat rgb = inputFrame.rgba();
                Mat gray = inputFrame.gray();

                cascadeClassifier.detectMultiScale(gray, rects, 1.1, 2);

                boolean frontalFaceDetected = false;

                for (Rect rect : rects.toList()) {
                    Mat submat = rgb.submat(rect);
                    if (isFrontalFace(submat)) {
                        frontalFaceDetected = true;
                        Imgproc.rectangle(rgb, rect, new Scalar(0, 144, 0), 5);
                        if (!isCounting) {
                            timeStart = System.currentTimeMillis();
                            isCounting = true;
                        }
                        break;
                    }
                    submat.release();
                }
                     //Aplicar que se cambie la ayuda visual dependiendo el rostro:
                     //Agregar condicion para que en la segunda foto se tome la captura de la parte trasera de la cedula : Sugerencia: trabajar ya sea con la deteccio OCR O deteccion de la huella
                if (frontalFaceDetected && System.currentTimeMillis() - timeStart >= 10000) {
                    JavaCameraView cameraView = findViewById(R.id.camera_view);
                    cameraView.disableView();

                    runOnUiThread(() -> {
                        byte[] imageBytes = new byte[rgb.rows() * rgb.cols() * rgb.channels()];
                        rgb.get(0, 0, imageBytes);
                        //Guarda la imagen y la convierte en base64:
                        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                        String imageString = "data:image/jpeg;base64," + base64Image;
                        Log.d("Base64 Image", imageString);// ¿Buscar que hacer con un base 64 es necesario para posprocesamiento y verificar que no afecte rendimiento?
                        ImageView imageView = findViewById(R.id.imageView);
                        Glide.with(getApplicationContext()).load(imageString).into(imageView);
                        btnNuevaFoto.setEnabled(true);
                         tomadoFoto = false;
                        //Deteccion De OCR //Aun sin hacer pruebas de esta parte:
                        Context context = getApplicationContext();
                        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
                        if (!textRecognizer.isOperational()) {
                         }
                        Frame frame = new Frame.Builder().setBitmap(imageView.getDrawingCache()).build();
                        SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);
                        String nombre = "";
                        String apellido = "";
                        String numero = "";
                        for (int i = 0; i < textBlocks.size(); i++) {
                            TextBlock textBlock = textBlocks.valueAt(i);
                            String texto = textBlock.getValue();
                            if (texto.matches("^[A-Z][a-z]+")) {
                                nombre = texto;
                            } else if (texto.matches("^[A-Z][a-z]+ [A-Z][a-z]+")) {
                                apellido = texto.split(" ")[1];
                            } else if (texto.matches("^\\d{10}$")) {
                                numero = texto;
                            }
                        }
                        if (!nombre.isEmpty() && !apellido.isEmpty() && !numero.isEmpty()) {
                         }
                    });
                 } else {
                    if (System.currentTimeMillis() - timeStart >= 30000) {
                        timeStart = System.currentTimeMillis();
                        isCounting = false;
                        runOnUiThread(() -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Alerta");
                            builder.setMessage("No se ha detectado ningún documento");
                            builder.setPositiveButton("OK", null);
                            builder.show();
                        });
                    }
                }

                return rgb;
            }
        });
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
                Toast.makeText(MainActivity.this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "No se concedieron los permisos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
