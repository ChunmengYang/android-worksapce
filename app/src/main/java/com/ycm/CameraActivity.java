package com.ycm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.RectF;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.ycm.demo.R;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity {
    private RectF mScreenRect = null;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;

    private String mCameraId;
    private ImageReader mImageReader;

    private HandlerThread mHandlerThread;
    private Handler mChildHandler;
    private Handler mMainHandler;

    private SurfaceView mSurfaceView;
    private Surface mSurface;
    private ImageView mImageView;

    private CaptureRequest.Builder mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;

    private Timer timer;
    private Handler taskHandler;
    private TimerTask timerTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mScreenRect = getDisplayPixes(this);

        mImageView = findViewById(R.id.camera_imageview);
        mSurfaceView = findViewById(R.id.camera_surfaceview);
        mSurfaceView.getLayoutParams().width = (int)(mScreenRect.width() / 3);
        mSurfaceView.getLayoutParams().height = (int)(mScreenRect.height() / 4);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurface = holder.getSurface();

                initCamera();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }
        });

        timer = new Timer();
        taskHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // TODO Auto-generated method stub
                // 要做的事情
                takePicture();
                super.handleMessage(msg);
            }
        };
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Message message = new Message();
                message.what = 1;
                taskHandler.sendMessage(message);
            }
        };
    }


    // 初始化相机并打开摄像头
    private void initCamera() {
        initImageReader();

        // 用来处理摄像头的线程与线程Handler
        mHandlerThread = new HandlerThread("Camera2 Handler Thread");
        mHandlerThread.start();
        mChildHandler = new Handler(mHandlerThread.getLooper());

        mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //按理说这里应该有一个申请权限的过程，但为了使程序尽可能最简化，所以先不添加
            return;
        }
        try {
            mCameraId = "" + CameraCharacteristics.LENS_FACING_FRONT;
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    mCameraDevice = camera;

                    initPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    finish();
                    Toast.makeText(CameraActivity.this, "打开摄像头失败", Toast.LENGTH_SHORT).show();
                }
            }, mChildHandler);

        } catch (CameraAccessException e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void initImageReader() {
        // 用来处理UI线程的Handler
        mMainHandler = new Handler(getMainLooper());
        mImageReader = ImageReader.newInstance((int) mScreenRect.width(), (int) mScreenRect.height(), ImageFormat.JPEG,2);
        // 这里必须传入mainHandler，因为涉及到了UI操作
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                //进行相片存储
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    mImageView.setImageBitmap(bitmap);
                }
                image.close();
            }
        }, mMainHandler);
    }

    public void initPreview() {
        try {
            mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequest.addTarget(mSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    /**
                     * 设置你需要配置的参数
                     */
                    //自动对焦
                    mCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    //打开闪光灯
                    mCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                    startPreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    finish();
                    Toast.makeText(CameraActivity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            }, mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void startPreview(){
        if (mCameraCaptureSession == null)
            return;

        try {
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest.build(), null, mChildHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (mCameraCaptureSession == null)
            return;

        try {
            mCameraCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void takePicture() {
        try {
            // 用来设置拍照请求的request
            CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            // 使图片做顺时针旋转
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(cameraCharacteristics, rotation));
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(mCaptureRequest, null, mChildHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        startPreview();
        timer.schedule(timerTask, 1000, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopPreview();
        timer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (mCaptureRequest != null){
            mCaptureRequest.removeTarget(mSurface);
            mCaptureRequest = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

        if (mChildHandler != null) {
            mChildHandler.removeCallbacksAndMessages(null);
            mChildHandler = null;
        }
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
    }

    // 获取图片应该旋转的角度，使图片竖直
    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // LENS_FACING相对于设备屏幕的方向,LENS_FACING_FRONT相机设备面向与设备屏幕相同的方向
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }

    public static RectF getDisplayPixes(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        return new RectF(0, 0, metrics.widthPixels, metrics.heightPixels);
    }
}
