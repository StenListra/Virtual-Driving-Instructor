package com.example.meelis.virtualdrivinginstructor;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import android.annotation.SuppressLint;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;

import org.json.JSONArray;

public class LessonActivity extends AppCompatActivity
{
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private AutoFitTextureView mContentView;
    private AccelerometerTextView mAccelerometerView;
    private Button mStartButton;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;
    private LocationHandler mLocationHandler;
    private Size mPreviewSize;
    private Size mVideoSize;
    private CaptureRequest.Builder mCaptureRequest;
    private MediaRecorder mRecorder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Semaphore mCameraLock = new Semaphore(1);
    private boolean mIsRecording;
    private boolean mInRecordingFlow = false;
    private long mStartTime, mEndTime;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener()
    {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height)
        {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height)
        {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture)
        {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture)
        {
        }
    };

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback()
    {

        @Override
        public void onOpened(CameraDevice cameraDevice)
        {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraLock.release();
            if (null != mContentView)
            {
                configureTransform(mContentView.getWidth(), mContentView.getHeight());
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice)
        {
            mCameraLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error)
        {
            mCameraLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = LessonActivity.this;
            activity.finish();
        }
    };

    private static Size chooseVideoSize(Size[] choices)
    {
        for (Size size : choices)
        {
            if (size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e("Driving Instructor", "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio)
    {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices)
        {
            if (option.getWidth() >= width && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0)
        {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else
        {
            Log.e("Driving Instructor", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lesson);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        delayedHide(0);
        mContentView = (AutoFitTextureView)findViewById(R.id.fullscreen_content);
        mStartButton = (Button)findViewById(R.id.dummy_button);
        mAccelerometerView = (AccelerometerTextView)findViewById(R.id.textView2);
        mLocationHandler = new LocationHandler(this);


        mStartButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (mIsRecording)
                {
                    stopRecordingVideo();
                }
                else
                {
                    startRecordingVideo();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (mInRecordingFlow) {
            mAccelerometerView.registerListener();
            mLocationHandler.registerListener();
        }
        startBackgroundThread();
        if (mContentView.isAvailable())
        {
            openCamera(mContentView.getWidth(), mContentView.getHeight());
        } else
        {
            mContentView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause()
    {
        closeCamera();
        stopBackgroundThread();
        if (mInRecordingFlow) {
            mAccelerometerView.unRegisterListener();
            mLocationHandler.unRegisterListener();
        }
        super.onPause();
    }

    private void startBackgroundThread()
    {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread()
    {
        mBackgroundThread.quitSafely();
        try
        {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height)
    {
        final Activity activity = this;
        if (activity.isFinishing())
        {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d("Driving Instructor", "tryAcquire");
            if (!mCameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS))
            {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);

            configureTransform(width, height);
            mRecorder = new MediaRecorder();
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e)
        {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e)
        {
            throw new RuntimeException("Device does not support Camera2 API");
        } catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        } catch (SecurityException e)
        {
            throw new RuntimeException("Camera permissions not granted");
        }
    }

    private void closeCamera()
    {
        try
        {
            mCameraLock.acquire();
            if (null != mCameraDevice)
            {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mRecorder)
            {
                mRecorder.release();
                mRecorder = null;
            }
        } catch (InterruptedException e)
        {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally
        {
            mCameraLock.release();
        }
    }

    private void startPreview()
    {
        if (null == mCameraDevice || !mContentView.isAvailable() || null == mPreviewSize)
        {
            return;
        }
        try
        {
            setUpMediaRecorder();
            SurfaceTexture texture = mContentView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mCaptureRequest.addTarget(previewSurface);

            Surface recorderSurface = mRecorder.getSurface();
            surfaces.add(recorderSurface);
            mCaptureRequest.addTarget(recorderSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback()
            {

                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession)
                {
                    mSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession)
                {
                    Activity activity = LessonActivity.this;
                    Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void updatePreview()
    {
        if (null == mCameraDevice)
        {
            return;
        }
        try
        {
            setUpCaptureRequestBuilder(mCaptureRequest);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mSession.setRepeatingRequest(mCaptureRequest.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder)
    {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private void configureTransform(int viewWidth, int viewHeight)
    {
        Activity activity = this;
        if (null == mContentView || null == mPreviewSize)
        {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation)
        {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mContentView.setTransform(matrix);
    }

    private void setUpMediaRecorder() throws IOException
    {
        final Activity activity = this;
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(getVideoFile().getAbsolutePath());
        mRecorder.setVideoEncodingBitRate(10000000);
        mRecorder.setVideoFrameRate(30);
        mRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        mRecorder.setOrientationHint(orientation);
        mRecorder.prepare();
    }

    private File getVideoFile()
    {
        return new File(Environment.getExternalStorageDirectory() + "/", "video.mp4");
    }

    private void startRecordingVideo()
    {
        try
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            // UI
            mStartButton.setText("Stop");
            mIsRecording = true;

            // Start recording
            mStartTime = System.currentTimeMillis();
            mRecorder.start();
            mAccelerometerView.registerListener();
            mLocationHandler.registerListener();
            mInRecordingFlow = true;
        } catch (IllegalStateException e)
        {
            e.printStackTrace();
        }
    }

    private void stopRecordingVideo()
    {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // UI
        mIsRecording = false;
        mStartButton.setText("Record");
        // Stop recording
        try {
            // Abort all pending captures.
            mSession.abortCaptures();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mRecorder.stop();
        mEndTime = System.currentTimeMillis() - mStartTime;
        Toast.makeText(this, "Video saved: " + getVideoFile(),
                    Toast.LENGTH_LONG).show();
        endFlow();
    }

    private void endFlow(){
        mInRecordingFlow = false;
        mAccelerometerView.unRegisterListener();
        mLocationHandler.unRegisterListener();
        List<Location> locations = mLocationHandler.mLocations;
        String locationJSON = mLocationHandler.mLocationsJSON.toString();
        String sensorJSON = mAccelerometerView.mAccelerometerJSON.toString();
        Intent intent = new Intent(this, LessonEndActivity.class);
        intent.putExtra("LocationList", (ArrayList<Location>) locations);
        intent.putExtra("Duration", mEndTime);
        intent.putExtra("LocationJSON", locationJSON);
        intent.putExtra("SensorJSON", sensorJSON);
        startActivity(intent);
    }

    static class CompareSizesByArea implements Comparator<Size>
    {
        @Override
        public int compare(Size lhs, Size rhs)
        {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private void hide()
    {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            actionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable()
    {
        @SuppressLint("InlinedApi")
        @Override
        public void run()
        {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    };

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis)
    {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
