package com.cabatuan.keypoints;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.IOException;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    public static final int VIEW_MODE_GRAY = 0; // preview Y - Intensity
    public static final int VIEW_MODE_U = 1;
    public static final int VIEW_MODE_V = 2;
    public static final int VIEW_MODE_SIFT = 3;
    public static final int VIEW_MODE_SURF = 4;
    public static final int VIEW_MODE_KAZE = 5;


    private static int viewMode = VIEW_MODE_GRAY;    // default

    static {
        System.loadLibrary("ImageProcessing");
    }

    private Camera mCamera;
    private byte[] videoSource;
    private Bitmap displayImage;

    private boolean bProcessing = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Bind(R.id.texturePreview)
    TextureView tv;

    @Bind(R.id.imageView)
    ImageView displayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        tv.setSurfaceTextureListener(this);
        //displayView.setVisibility(View.VISIBLE);
        displayView.setAlpha(150.0f/256.0f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        /*********  Update this layout if you add more actions  **********/
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_rgb) {
            tv.setVisibility(View.VISIBLE);
            displayView.setVisibility(View.INVISIBLE);
        } else if (id == R.id.action_gray) {
            viewMode = VIEW_MODE_GRAY;
            tv.setVisibility(View.INVISIBLE);
            displayView.setVisibility(View.VISIBLE);
        } else if (id == R.id.action_u) {
            viewMode = VIEW_MODE_U;
            tv.setVisibility(View.INVISIBLE);
            displayView.setVisibility(View.VISIBLE);
        } else if (id == R.id.action_v) {
            viewMode = VIEW_MODE_V;
            tv.setVisibility(View.INVISIBLE);
            displayView.setVisibility(View.VISIBLE);
        } else if (id == R.id.action_sift) {
            viewMode = VIEW_MODE_SIFT;
            tv.setVisibility(View.VISIBLE);
            displayView.setVisibility(View.VISIBLE);
        } else if (id == R.id.action_surf) {
            viewMode = VIEW_MODE_SURF;
            tv.setVisibility(View.VISIBLE);
            displayView.setVisibility(View.VISIBLE);
        } else if (id == R.id.action_kaze) {
            viewMode = VIEW_MODE_KAZE;
            tv.setVisibility(View.VISIBLE);
            displayView.setVisibility(View.VISIBLE);
        }


        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mCamera != null) {
            if (!bProcessing) {
                videoSource = data;
                mHandler.post(DoImageProcessing);
            }
        }
    }

    public native void process(Bitmap pTarget, byte[] pSource, int mode);

    private Runnable DoImageProcessing = new Runnable() {
        public void run() {
            bProcessing = true;

            process(displayImage, videoSource, viewMode);
            displayView.invalidate();

            mCamera.addCallbackBuffer(videoSource);

            bProcessing = false;
        }
    };

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

        /// Use front-facing camera (if available)
        /*
        Camera.CameraInfo camInfo = new Camera.CameraInfo();

        for (int camNo = 0; camNo < Camera.getNumberOfCameras(); camNo++) {
            Camera.getCameraInfo(camNo, camInfo);

            if (camInfo.facing==(Camera.CameraInfo.CAMERA_FACING_FRONT)) {
                mCamera = Camera.open(camNo);
            }
        }
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        */
        // Open default camera
        mCamera = Camera.open();

        try {


            mCamera.setPreviewTexture(surface);
            mCamera.setPreviewCallbackWithBuffer(this);
            mCamera.setDisplayOrientation(0);

            Camera.Size size = findBestResolution(width, height);
            PixelFormat pixelFormat = new PixelFormat();
            PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), pixelFormat);
            int sourceSize = size.width * size.height * pixelFormat.bitsPerPixel / 8;

            /// Camera size and video format
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(size.width, size.height);
            parameters.setPreviewFormat(PixelFormat.YCbCr_420_SP);
            mCamera.setParameters(parameters);

            /// Video buffer and bitmap for display
            videoSource = new byte[sourceSize];
            displayImage = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888);
            displayView.setImageBitmap(displayImage);

            /// Queue video frame buffer and start camera preview
            mCamera.addCallbackBuffer(videoSource);
            mCamera.startPreview();

        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
            throw new IllegalStateException();
        }
    }


    private Camera.Size findBestResolution(int pWidth, int pHeight) {
        List<Camera.Size> sizes = mCamera.getParameters().getSupportedPreviewSizes();
        Camera.Size selectedSize = mCamera.new Size(0, 0);

        for (Camera.Size size : sizes) {
            if ((size.width <= pWidth) && (size.height <= pHeight) && (size.width >= selectedSize.width) && (size.height >= selectedSize.height)) {
                selectedSize = size;
            }
        }

        if ((selectedSize.width == 0) || (selectedSize.height == 0)) {
            selectedSize = sizes.get(0);
        }

        return selectedSize;
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

        // Release camera
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();

            mCamera = null;
            videoSource = null;

            displayImage.recycle();
            displayImage = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}