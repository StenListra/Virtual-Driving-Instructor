package com.example.meelis.virtualdrivinginstructor;

import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SLIST on 10-Jan-16.
 */
public class LessonMapFragment extends Fragment {
    private static GoogleMap mGoogleMap;

    public List<Location> mLocationList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.map_fragment, container, false);
    }

    public void setUpMap() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mGoogleMap == null) {
            MapFragment fragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            fragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if (googleMap != null){
                        mGoogleMap = googleMap;
                        googleMap.getUiSettings().setAllGesturesEnabled(true);
                        if (mLocationList.size() > 0){
                            Location defaultLocation = mLocationList.get(0);
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(defaultLocation.getLatitude(), defaultLocation.getLongitude())).zoom(15.0f).build();
                            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
                            googleMap.moveCamera(cameraUpdate);
                            setUpMarkers();
                        }
                    }
                }
            });
        }
    }

    private void setUpMarkers(){
        List<LatLng> markerList = new ArrayList<>();
        for (Location location : mLocationList){
            LatLng marker = new LatLng(location.getLatitude(), location.getLongitude());
            markerList.add(marker);
        }
        if (markerList.size() > 1) {
            PolylineOptions lineOptions = new PolylineOptions().addAll(markerList);
            mGoogleMap.addMarker(new MarkerOptions().position(markerList.get(0)));
            mGoogleMap.addMarker(new MarkerOptions().position(markerList.get(markerList.size() - 1)));
            mGoogleMap.addPolyline(lineOptions);
        }
    }

    public String getDistance() {
        if (mLocationList.size() > 1) {
            Location startLocation = mLocationList.get(0);
            Location endLocation  = mLocationList.get(mLocationList.size() - 1);
            MapDistanceRunnable distanceRunnable = new MapDistanceRunnable(startLocation.getLatitude(), startLocation.getLongitude(),
                    endLocation.getLatitude(), endLocation.getLongitude());
            Thread thread = new Thread(distanceRunnable);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return distanceRunnable.mParsedDistance;
        }
        else
        {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mGoogleMap != null) {
            getChildFragmentManager().beginTransaction()
                    .remove(getChildFragmentManager().findFragmentById(R.id.map)).commit();
            mGoogleMap = null;
        }
    }

}
