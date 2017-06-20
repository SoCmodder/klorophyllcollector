package org.brogrammer.klorophyllcollector;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.icu.text.DateFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.brogrammer.klorophyllcollector.util.CameraHandler;
import org.brogrammer.klorophyllcollector.util.ImagePreprocessor;
import org.brogrammer.klorophyllcollector.util.ImageUtils;

import java.util.Date;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener
{
    private final static int INTERVAL = 1000 * 60 * 1; // 15 Minutes

    private static final int PERMISSIONS_REQUEST = 1;

    private CameraHandler cameraHandler;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private ImagePreprocessor preprocessor;

    private ImageView imageView;
    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView) findViewById(R.id.imageview);
        textView = (TextView) findViewById(R.id.timestamp);

        if (hasPermission())
        {
            if (savedInstanceState == null)
            {
                init();
            }
        }
        else
        {
            requestPermission();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        startCameraTask();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        stopCameraTask();
    }

    // Permission-related methods. This is not needed for Android Things, where permissions are
    // automatically granted. However, it is kept here in case the developer needs to test on a
    // regular Android device.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    requestPermission();
                }
            }
        }
    }

    private void init() {
        backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        cameraHandler = CameraHandler.getInstance();
        backgroundHandler.post(initializeOnBackground);
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(CAMERA) ||
                    shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Camera AND storage permission are required for this to run",
                        Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{CAMERA, WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    private Runnable initializeOnBackground = new Runnable()
    {
        @Override
        public void run()
        {
            preprocessor = new ImagePreprocessor(CameraHandler.IMAGE_WIDTH, CameraHandler.IMAGE_HEIGHT, 480);
            cameraHandler.initializeCamera(MainActivity.this, backgroundHandler, MainActivity.this);
        }
    };

    private void startCameraTask()
    {
        cameraTask.run();
    }

    private void stopCameraTask()
    {
        backgroundHandler.removeCallbacks(cameraTask);
    }

    private Runnable cameraTask = new Runnable()
    {
        @Override
        public void run()
        {
            if (cameraHandler != null)
            {
                cameraHandler.takePicture();
                backgroundHandler.postDelayed(cameraTask, INTERVAL);
            }
            else
            {
                textView.setText("Camera Handler is null.");
            }
        }
    };

    @Override
    public void onImageAvailable(ImageReader imageReader)
    {
        final Bitmap bitmap;
        try (Image image = imageReader.acquireNextImage())
        {
            bitmap = preprocessor.preprocessImage(image);
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    ImageUtils.saveBitmap(bitmap);
                    imageView.setImageBitmap(bitmap);
                    textView.setText(DateFormat.getDateTimeInstance().format(new Date()));
                }
            });
        } catch (Exception e)
        {
            imageView.setBackgroundColor(getColor(android.R.color.holo_red_dark));
            textView.setText(e.getMessage());
        }
    }
}
