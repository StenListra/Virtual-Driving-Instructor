package com.example.meelis.virtualdrivinginstructor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Environment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

/**
 * Created by SLIST on 18-Nov-15.
 */
public class AccelerometerTextView extends LinearLayout implements android.hardware.SensorEventListener2 {
    private SensorManager mManager;
    private Sensor mAccelerometer;
    private FileUtility mFileUtility;
    private long mLastUpdate = 0;
    final private long mStartTime = System.currentTimeMillis();
    private File mDataFile;
    private boolean mIsCalibrated = false;
    private float[] mCalibrationValues = new float[3];
    private View mRootView;
    private TextView mTopTextView, mMiddleTextView, mBottomTextView;

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

    public AccelerometerTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        super(context,attrs,defStyleAttr,defStyleRes);
        init(context);
    }

    private void init(Context context){
        setupViews(context);
        String FileLocation = "/accData.txt";
        mManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mDataFile = new File(Environment.getExternalStorageDirectory() + FileLocation);
        mFileUtility = new FileUtility(mDataFile, FileLocation);
    }

    private void setupViews(Context context){
        mRootView = inflate(context, R.layout.accelerometer_text_view, this);
        mTopTextView = (TextView) mRootView.findViewById(R.id.topTextView);
        mMiddleTextView = (TextView) mRootView.findViewById(R.id.midTextView);
        mBottomTextView = (TextView) mRootView.findViewById(R.id.botTextView);
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
                mFileUtility.writeToFile(jsonString, mDataFile);
            }
        }
    }

    public void onFlushCompleted (Sensor sensor){

    }

    public void onAccuracyChanged (Sensor sensor, int accuracy){

    }

}
