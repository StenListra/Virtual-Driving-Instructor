package com.example.meelis.virtualdrivinginstructor;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.File;
import java.io.Serializable;
import java.security.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by SLIST on 23-Nov-15.
 */
public class LocationHandler implements android.location.LocationListener {
    public LocationManager mLocationManager;
    public String mProvider;
    final private long mStartTime = System.currentTimeMillis();
    private File mDataFile;
    private FileUtility mFileUtility;
    public List<Location> mLocations;

    public LocationHandler(Context context){
        String fileLocation = "/locData.txt";
        mDataFile = new File(Environment.getExternalStorageDirectory() + fileLocation);
        mFileUtility = new FileUtility(mDataFile, fileLocation);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mLocations = new ArrayList<>();
    }

    public void registerListener(){
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        try{
            mProvider = mLocationManager.getBestProvider(criteria, true);
            Location defaultLocation = mLocationManager.getLastKnownLocation(mProvider);
            if (defaultLocation != null){
                writeLocationToFile(defaultLocation);
            }
            mLocationManager.requestLocationUpdates(mProvider, 0, 0, this);
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
        writeLocationToFile(location);
    }

    private void writeLocationToFile(Location location){
        mLocations.add(location);
        Double longitude = location.getLongitude();
        Double latitude = location.getLatitude();
        Float speed = location.getSpeed();
        Long currTime = System.currentTimeMillis();
        String jsonString = "{\"longitude\":\"" + longitude + "\",\"latitude\":\"" + latitude + "\",\"speed\":\"" + speed + "\",\"time\":\"" + (currTime - mStartTime) + "\"}";
        mFileUtility.writeToFile(jsonString,mDataFile);
    }
}
