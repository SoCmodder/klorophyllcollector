package org.brogrammer.klorophyllcollector;

import android.graphics.Bitmap;
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

public class MainActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener
{
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

        setup();
    }

    private void setup()
    {
        preprocessor = new ImagePreprocessor(CameraHandler.IMAGE_WIDTH, CameraHandler.IMAGE_HEIGHT, 240);
        backgroundThread = new HandlerThread("BackgroundThread");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        backgroundHandler.post(initializeOnBackground);
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
            }
        });
    }
}
