package com.example.meelis.virtualdrivinginstructor;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class LessonEndActivity extends AppCompatActivity {

    public static FragmentManager mFragmentManager;
    private long mDuration;
    private LessonMapFragment mMapFragment;
    private TextView mDistanceTextView, mDurationTextView, mTxtPercentage;
    private ProgressBar mProgressBar;
    private File mVideoFile = new File(Environment.getExternalStorageDirectory() + "/video.mp4");
    private Button mBtnUpload;
    private long mTotalSize = 0;
    private boolean mSuccessful;
    private JSONArray mLessonJSONArray = new JSONArray();
    private JSONObject mLessonJSON = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_end);

        mDistanceTextView = (TextView) findViewById(R.id.distanceTextView);
        mDurationTextView = (TextView) findViewById(R.id.durationTextView);
        mTxtPercentage = (TextView) findViewById(R.id.percentageView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mBtnUpload = (Button) findViewById(R.id.uploadButton);

        mBtnUpload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View w) {
                new FileUploader().execute();
            }
        });

        mFragmentManager = getFragmentManager();
        mMapFragment = (LessonMapFragment) mFragmentManager.findFragmentById(R.id.mapFragment);
        mMapFragment.mLocationList = (ArrayList<Location>) getIntent().getSerializableExtra("LocationList");
        mMapFragment.setUpMap();

        mDuration = (long) getIntent().getSerializableExtra("Duration");

        try {
            JSONArray locationJSONArray = new JSONArray((String) getIntent().getSerializableExtra("LocationJSON"));
            JSONArray sensorJSONArray = new JSONArray((String) getIntent().getSerializableExtra("SensorJSON"));
            JSONObject locationJSON = new JSONObject();
            JSONObject sensorJSON = new JSONObject();

            locationJSON.put("locations", locationJSONArray);
            sensorJSON.put("sensors", sensorJSONArray);
            mLessonJSONArray.put(locationJSON);
            mLessonJSONArray.put(sensorJSON);
            mLessonJSON.put("lesson", mLessonJSONArray);
        } catch (JSONException e) {
            Log.e("LessonEndActivity", "Exception when creating JSON array: " + e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String distance = mMapFragment.getDistance();
        if (distance != null) {
            mDistanceTextView.setText(String.format(getResources().getString(R.string.distance_0_00_km), distance));
        } else {
            mDistanceTextView.setText(String.format(getResources().getString(R.string.distance_0_00_km), "0 m"));
        }
        mDurationTextView.setText(String.format(getResources().getString(R.string.duration_s), mDuration / 60000));
    }

    private class FileUploader extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressBar.setProgress(0);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgressBar.setVisibility(View.VISIBLE);
            mTxtPercentage.setVisibility(View.VISIBLE);

            mProgressBar.setProgress(progress[0]);

            mTxtPercentage.setText(String.format(getResources().getString(R.string.upload_progress), progress[0]));
        }

        @Override
        protected String doInBackground(Void... params) {
            return uploadFile();
        }

        @SuppressWarnings("deprecation")
        private String uploadFile() {
            String responseString;

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(Constants.SERVER_URL + "upload");

            try {
                ProgressMultiPartEntity entity = new ProgressMultiPartEntity(
                        new ProgressMultiPartEntity.ProgressListener() {

                            @Override
                            public void transferred(long num) {
                                publishProgress((int) ((num / (float) mTotalSize) * 100));
                            }
                        });

                File sourceFile = mVideoFile;

                entity.addPart("video", new FileBody(sourceFile));
                entity.addPart("JSON", new StringBody(mLessonJSON.toString(1),ContentType.APPLICATION_JSON));

                mTotalSize = entity.getContentLength();
                httppost.setEntity(entity);

                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    mSuccessful = true;
                    responseString = EntityUtils.toString(r_entity);
                } else {
                    responseString = "Error occurred! Http Status Code: "
                            + statusCode;
                }

            } catch (Exception e) {
                responseString = e.toString();
            }

            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            if (mSuccessful) {
                Log.i("LessonEndActivity", "Response from server: " + result);
            }
            else{
                Log.e("LessonEndActivity", "Response from server: " + result);
            }
            showAlert(result);

            super.onPostExecute(result);
        }
    }

    private void showAlert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message).setTitle("Response from Server")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mSuccessful){
                            mBtnUpload.setVisibility(View.INVISIBLE);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            mTxtPercentage.setVisibility(View.INVISIBLE);
                            if (mVideoFile.delete()){
                                Log.i("LessonEndActivity", "Video file successfully deleted");
                            }
                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }
}
