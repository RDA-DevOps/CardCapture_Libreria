package com.example.opencv;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


public class init extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.init);
        getPermission();
        getPermissionexternal();
         Button New;
        New=(Button) findViewById(R.id.New);

        String base64Image= getIntent().getStringExtra("ParteDelantera");
        String base64Image2= getIntent().getStringExtra("ParteTrasera");

        byte[] decodedImage = Base64.decode(base64Image, Base64.DEFAULT);
        byte[] decodedImage2 = Base64.decode(base64Image2, Base64.DEFAULT);

        Bitmap bitmap1 = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);
        Bitmap bitmap2 = BitmapFactory.decodeByteArray(decodedImage2, 0, decodedImage2.length);

        ImageView imageView1 = findViewById(R.id.imageView1);
        ImageView imageView2 = findViewById(R.id.imageView2);

        imageView1.setImageBitmap(bitmap1);
        imageView2.setImageBitmap(bitmap2);
        imageView1.setRotation(360);
        imageView2.setRotation(180);


        New.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 ImageView imageView = findViewById(R.id.imageView);
                imageView.setVisibility(View.GONE); // ocultar ImageView
                Intent intent = new Intent(init.this, MainActivity_Seconds.class);
                startActivity(intent);
                onResume();
             }
        });


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
                Toast.makeText(init.this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(init.this, "No se concedieron los permisos", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
