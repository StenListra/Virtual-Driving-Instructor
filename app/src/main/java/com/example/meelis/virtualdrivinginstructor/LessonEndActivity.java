package com.example.meelis.virtualdrivinginstructor;

import android.app.FragmentManager;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.ArrayList;

public class LessonEndActivity extends AppCompatActivity{

    public static FragmentManager mFragmentManager;
    private long mDuration;
    private LessonMapFragment mMapFragment;
    private TextView mDistanceTextView, mDurationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_end);

        mDistanceTextView = (TextView) findViewById(R.id.distanceTextView);
        mDurationTextView = (TextView) findViewById(R.id.durationTextView);

        mFragmentManager = getFragmentManager();
        mMapFragment = (LessonMapFragment) mFragmentManager.findFragmentById(R.id.mapFragment);
        mMapFragment.mLocationList = (ArrayList<Location>) getIntent().getSerializableExtra("LocationList");
        mMapFragment.setUpMap();

        mDuration = (long) getIntent().getSerializableExtra("Duration");
    }

    @Override
    protected void onResume(){
        super.onResume();
        String distance = mMapFragment.getDistance();
        if (distance != null){
            mDistanceTextView.setText(String.format(getResources().getString(R.string.distance_0_00_km), distance));
        }
        else{
            mDistanceTextView.setText(String.format(getResources().getString(R.string.distance_0_00_km), "0 m"));
        }
        mDurationTextView.setText(String.format(getResources().getString(R.string.duration_s), mDuration/60000));
    }
}
