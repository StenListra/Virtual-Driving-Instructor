package com.example.meelis.virtualdrivinginstructor;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by SLIST on 23-Nov-15.
 */
public class LocationHandler implements android.location.LocationListener {
    private LocationManager mLocationManager;
    final private long mStartTime = System.currentTimeMillis();
    private File mDataFile;
    private FileUtility mFileUtility;
    private String mFileLocation = "/locData.txt";

    public LocationHandler(Context context){
        mDataFile = new File(Environment.getExternalStorageDirectory() + mFileLocation);
        mFileUtility = new FileUtility(mDataFile, mFileLocation);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void registerListener(){
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        try{
            mLocationManager.requestLocationUpdates(mLocationManager.getBestProvider(criteria, true), 100, 0, this);
        }
        catch (SecurityException e){
            Log.e("LocationHandler", "Missing permissions for location updates");
        }
    }

    public void unRegisterListener(){
        try {
            mLocationManager.removeUpdates(this);
        }
        catch (SecurityException e){
            Log.e("LocationHandler", "Missing permissions for location updates");
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras){

    }

    public void onProviderEnabled(String provider){

    }

    public void onProviderDisabled(String provider){

    }

    public void onLocationChanged(Location location){
        Double longitude = location.getLongitude();
        Double latitude = location.getLatitude();
        Float speed = location.getSpeed();
        Long currTime = System.currentTimeMillis();
        String jsonString = "{\"longitude\":\"" + longitude + "\",\"latitude\":\"" + latitude + "\",\"speed\":\"" + speed + "\",\"time\":\"" + (currTime - mStartTime) + "\"}";
        mFileUtility.writeToFile(jsonString,mDataFile);
    }
}
