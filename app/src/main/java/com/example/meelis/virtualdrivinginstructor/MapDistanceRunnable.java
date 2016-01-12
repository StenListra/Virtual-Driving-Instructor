package com.example.meelis.virtualdrivinginstructor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by SLIST on 12-Jan-16.
 */
public class MapDistanceRunnable implements Runnable {

    public String mParsedDistance;
    private double mlat1, mlon1, mlat2, mlon2;

    public MapDistanceRunnable(double lat1, double lon1, double lat2, double lon2){
        mlat1 = lat1;
        mlat2 = lat2;
        mlon1 = lon1;
        mlon2 = lon2;
    }

    public void run() {
        try {
            String response;

            URL url = new URL("http://maps.googleapis.com/maps/api/directions/json?origin=" + mlat1 + "," +
                    mlon1 + "&destination=" + mlat2 + "," + mlon2 + "&sensor=false&units=metric&mode=driving");
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            InputStream in = new BufferedInputStream(conn.getInputStream());
            response = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

            JSONObject jsonObject = new JSONObject(response);
            JSONArray array = jsonObject.getJSONArray("routes");
            JSONObject routes = array.getJSONObject(0);
            JSONArray legs = routes.getJSONArray("legs");
            JSONObject steps = legs.getJSONObject(0);
            JSONObject distance = steps.getJSONObject("distance");
            mParsedDistance = distance.getString("text");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
