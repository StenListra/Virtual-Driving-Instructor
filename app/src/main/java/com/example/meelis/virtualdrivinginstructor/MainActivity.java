package com.example.meelis.virtualdrivinginstructor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private static final int mRequestPermissions = 1;
    private static String[] mPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyPermissions();
    }

    private void verifyPermissions(){
        List<String> requestedPermissions = new ArrayList<>();
        for (String permission : mPermissions){
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                requestedPermissions.add(permission);
            }
        }
        ActivityCompat.requestPermissions(this, requestedPermissions.toArray(new String[requestedPermissions.size()]), 1);
    }

    public void startNewLesson(View view)
    {
        Intent intent = new Intent(this, LessonActivity.class);
        startActivity(intent);
    }

    public void seeLessons(View view)
    {
        Intent intent = new Intent(this, WebViewActivity.class);
        startActivity(intent);
    }
}
