package com.example.meelis.virtualdrivinginstructor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by SLIST on 18-Nov-15.
 */
public class AccelerometerTextView extends LinearLayout implements android.hardware.SensorEventListener2 {
    private SensorManager mManager;
    private Sensor mAccelerometer;
    private long mLastUpdate = 0;
    final private long mStartTime = System.currentTimeMillis();
    private boolean mIsCalibrated = false;
    private float[] mCalibrationValues = new float[3];
    private TextView mTopTextView, mMiddleTextView, mBottomTextView;
    public JSONArray mAccelerometerJSON;

    public AccelerometerTextView(Context context){
        super(context);
        init(context);
    }

    public AccelerometerTextView(Context context, AttributeSet attrs){
        super(context,attrs);
        init(context);
    }

    public AccelerometerTextView(Context context, AttributeSet attrs, int defStyleAttr){
        super(context,attrs,defStyleAttr);
        init(context);
    }

    private void init(Context context){
        setupViews(context);
        mManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccelerometerJSON = new JSONArray();
    }

    private void setupViews(Context context){
        View rootView = inflate(context, R.layout.accelerometer_text_view, this);
        mTopTextView = (TextView) rootView.findViewById(R.id.topTextView);
        mMiddleTextView = (TextView) rootView.findViewById(R.id.midTextView);
        mBottomTextView = (TextView) rootView.findViewById(R.id.botTextView);
        mTopTextView.setText(String.format(getResources().getString(R.string.x_0_00), 0.00));
        mMiddleTextView.setText(String.format(getResources().getString(R.string.y_0_00), 0.00));
        mBottomTextView.setText(String.format(getResources().getString(R.string.z_0_00), 0.00));
    }

    public void registerListener(){
        mManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unRegisterListener(){
        mManager.unregisterListener(this, mAccelerometer);
    }


    public void onSensorChanged (SensorEvent event){
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if (!mIsCalibrated){
                System.arraycopy(event.values, 0, mCalibrationValues, 0, 3);
                mIsCalibrated = true;
            }
            long currTime = System.currentTimeMillis();
            if (currTime - mLastUpdate > 100){
                mLastUpdate = currTime;
                float x = event.values[0] - mCalibrationValues[0];
                float y = event.values[1] - mCalibrationValues[1];
                float z = event.values[2] - mCalibrationValues[2];
                String jsonString = "{\"x\":\"" + x + "\",\"y\":\"" + y + "\",\"z\":\"" + z + "\",\"time\":\"" + (currTime - mStartTime) + "\"}";
                try{
                    JSONObject accelerometerData = new JSONObject(jsonString);
                    mAccelerometerJSON.put(accelerometerData);
                }
                catch (Exception e){
                    Log.e("AccelerometerTextView", "Exception when creating JSON object: " + e);
                }
                mTopTextView.setText(String.format(getResources().getString(R.string.x_0_00), x));
                mMiddleTextView.setText(String.format(getResources().getString(R.string.y_0_00), y));
                mBottomTextView.setText(String.format(getResources().getString(R.string.z_0_00), z));
            }
        }
    }

    public void onFlushCompleted (Sensor sensor){

    }

    public void onAccuracyChanged (Sensor sensor, int accuracy){

    }

}
