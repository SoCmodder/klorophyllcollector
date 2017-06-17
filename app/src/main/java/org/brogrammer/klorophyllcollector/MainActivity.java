package org.brogrammer.klorophyllcollector;

import android.graphics.Bitmap;
import android.icu.text.DateFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import org.brogrammer.klorophyllcollector.util.CameraHandler;
import org.brogrammer.klorophyllcollector.util.ImagePreprocessor;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener
{
    private final static int INTERVAL = 1000 * 60 * 15; // 15 Minutes

    private CameraHandler cameraHandler;
    private Handler backgroundHandler;
    private Handler takePictureHandler;
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

        setup();
    }

    private void setup()
    {
        preprocessor = new ImagePreprocessor(CameraHandler.IMAGE_WIDTH, CameraHandler.IMAGE_HEIGHT, 240);
        backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        backgroundHandler.post(initializeOnBackground);
        takePictureHandler = new Handler();
    }

    private Runnable initializeOnBackground = new Runnable()
    {
        @Override
        public void run()
        {
            cameraHandler = CameraHandler.getInstance();
            cameraHandler.initializeCamera(MainActivity.this, backgroundHandler, MainActivity.this);
        }
    };

    private void startCameraTask()
    {
        cameraTask.run();
    }

    private void stopCameraTask()
    {
        takePictureHandler.removeCallbacks(cameraTask);
    }

    private Runnable cameraTask = new Runnable()
    {
        @Override
        public void run()
        {
            cameraHandler.takePicture();
            takePictureHandler.postDelayed(cameraTask, INTERVAL);
        }
    };

    @Override
    public void onImageAvailable(ImageReader imageReader)
    {
        final Bitmap bitmap;
        try (Image image = imageReader.acquireNextImage())
        {
            bitmap = preprocessor.preprocessImage(image);
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                imageView.setImageBitmap(bitmap);
                textView.setText(DateFormat.getDateTimeInstance().format(new Date()));
            }
        });
    }
}
