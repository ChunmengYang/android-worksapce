package com.ycm.zxinglibrary.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;
import com.ycm.demo.R;
import com.ycm.zxinglibrary.bean.ZxingConfig;
import com.ycm.zxinglibrary.camera.CameraManager;
import com.ycm.zxinglibrary.common.Constant;
import com.ycm.zxinglibrary.decode.DecodeImgCallback;
import com.ycm.zxinglibrary.decode.DecodeImgThread;
import com.ycm.zxinglibrary.decode.ImageUtil;
import com.ycm.zxinglibrary.view.ViewfinderView;

import java.io.IOException;

/**
 * @author: ChunmengYang
 * @date: 2018/8/22 10:07
 * @declare: Scanner
 */

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    public  ZxingConfig config;
    private SurfaceView previewView;
    private ViewfinderView viewfinderView;
    private AppCompatImageView flashLightIv;
    private TextView flashLightTv;
    private AppCompatImageView backIv;
    private LinearLayoutCompat flashLightLayout;
    private LinearLayoutCompat albumLayout;
    private LinearLayoutCompat bottomLayout;
    private boolean hasSurface;
    private com.ycm.zxinglibrary.android.InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private CameraManager cameraManager;
    private com.ycm.zxinglibrary.android.CaptureActivityHandler handler;
    private SurfaceHolder surfaceHolder;


    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }


    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.BLACK);
        }

        try {
            config = (ZxingConfig) getIntent().getExtras().get(Constant.INTENT_ZXING_CONFIG);
        } catch (Exception e) {

            Log.i("config", e.toString());
        }

        if (config == null) {
            config = new ZxingConfig();
        }
        

        setContentView(R.layout.qrcode_activity_capture);


        initView();

        hasSurface = false;

        inactivityTimer = new com.ycm.zxinglibrary.android.InactivityTimer(this);
        beepManager = new BeepManager(this);
        beepManager.setPlayBeep(config.isPlayBeep());
        beepManager.setVibrate(config.isShake());
    }


    private void initView() {
        previewView = (SurfaceView)findViewById(R.id.qrcode_preview_view);
        previewView.setOnClickListener(this);

        viewfinderView = (ViewfinderView)findViewById(R.id.qrcode_viewfinder_view);
        viewfinderView.setZxingConfig(config);

        backIv = (AppCompatImageView)findViewById(R.id.qrcode_backIv);
        backIv.setOnClickListener(this);

        flashLightIv = (AppCompatImageView)findViewById(R.id.qrcode_flashLightIv);
        flashLightTv = (TextView)findViewById(R.id.qrcode_flashLightTv);

        flashLightLayout = (LinearLayoutCompat)findViewById(R.id.qrcode_flashLightLayout);
        flashLightLayout.setOnClickListener(this);
        albumLayout = (LinearLayoutCompat)findViewById(R.id.qrcode_albumLayout);
        albumLayout.setOnClickListener(this);
        bottomLayout = (LinearLayoutCompat)findViewById(R.id.qrcode_bottomLayout);

        switchVisibility(bottomLayout, config.isShowbottomLayout());
        switchVisibility(flashLightLayout, config.isShowFlashLight());
        switchVisibility(albumLayout, config.isShowAlbum());

        if (isSupportCameraLedFlash(getPackageManager())) {
            flashLightLayout.setVisibility(View.VISIBLE);
        } else {
            flashLightLayout.setVisibility(View.GONE);
        }
    }

    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f != null && PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void switchFlashImg(int flashState) {

        if (flashState == Constant.FLASH_OPEN) {
            flashLightIv.setImageResource(R.drawable.qrcode_ic_open);
            flashLightTv.setText(R.string.qrcode_close_flashlight);
        } else {
            flashLightIv.setImageResource(R.drawable.qrcode_ic_close);
            flashLightTv.setText(R.string.qrcode_open_flashlight);
        }

    }

    /**
     * @param rawResult
     */
    public void handleDecode(Result rawResult) {

        inactivityTimer.onActivity();

        beepManager.playBeepSoundAndVibrate();

        Intent intent = getIntent();
        intent.putExtra(Constant.CODED_CONTENT, rawResult.getText());
        setResult(RESULT_OK, intent);
        this.finish();
    }


    private void switchVisibility(View view, boolean b) {
        if (b) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        cameraManager = new CameraManager(getApplication(),config);

        viewfinderView.setCameraManager(cameraManager);
        handler = null;

        surfaceHolder = previewView.getHolder();
        if (hasSurface) {

            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }

        beepManager.updatePrefs();
        inactivityTimer.onResume();

    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new com.ycm.zxinglibrary.android.CaptureActivityHandler(this, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.qrcode_scan);
        builder.setMessage(getString(R.string.qrcode_msg_camera_framework_bug));
        builder.setPositiveButton(R.string.qrcode_button_ok, new com.ycm.zxinglibrary.android.FinishListener(this));
        builder.setOnCancelListener(new com.ycm.zxinglibrary.android.FinishListener(this));
        builder.show();
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();

        if (!hasSurface) {
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.qrcode_flashLightLayout) {
            cameraManager.switchFlashLight(handler);
        } else if (id == R.id.qrcode_albumLayout) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, Constant.REQUEST_IMAGE);
        } else if (id == R.id.qrcode_backIv) {
            finish();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constant.REQUEST_IMAGE && resultCode == RESULT_OK) {
            String path = ImageUtil.getImageAbsolutePath(this, data.getData());

            new DecodeImgThread(path, new DecodeImgCallback() {
                @Override
                public void onImageDecodeSuccess(Result result) {
                    handleDecode(result);
                }

                @Override
                public void onImageDecodeFailed() {
                    Toast.makeText(CaptureActivity.this, R.string.qrcode_parsing_failed, Toast.LENGTH_SHORT).show();
                }
            }).run();
        }
    }
}
