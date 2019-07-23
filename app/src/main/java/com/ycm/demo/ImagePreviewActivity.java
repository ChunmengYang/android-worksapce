package com.ycm.demo;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImagePreviewActivity extends Activity {
    public static int statusBarHeight = 0;
    private RectF mScreenRect = null;

    FrameLayout container;
    ImageView imageView;
    PhotoView photoView;
    ProgressBar progressBar;

    float scale = 1f;
    int translationX = 0;
    int translationY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        container = new FrameLayout(this);
        setContentView(container);

        mScreenRect = getDisplayPixes(this);

        Intent intent = getIntent();
        if (intent != null) {
            byte [] bis = intent.getByteArrayExtra("bitmap");
            Bitmap bitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);

            int width = intent.getIntExtra("width", 0);
            int height = intent.getIntExtra("height", 0);
            int[] locationOnScreen = intent.getIntArrayExtra("locationOnScreen");
            if (statusBarHeight == 0) {
                int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    statusBarHeight = this.getResources().getDimensionPixelSize(resourceId);
                }
            }

            Log.d("ImagePreviewActivity", "==============" + statusBarHeight);
            locationOnScreen[1] -= statusBarHeight;

            photoView = new PhotoView(this);
            photoView.setImageBitmap(bitmap);
            FrameLayout.LayoutParams photoLayoutParams = new FrameLayout.LayoutParams((int) mScreenRect.width(), (int) mScreenRect.height());
            photoView.setLayoutParams(photoLayoutParams);
            photoView.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float x, float y) {

                    playCloseAnimation();
                }

                @Override
                public void onOutsidePhotoTap() {

                }
            });
            photoView.setVisibility(View.GONE);
            container.addView(photoView);

            imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            FrameLayout.LayoutParams imageLayoutParams = new FrameLayout.LayoutParams(width, height);
            imageLayoutParams.leftMargin = locationOnScreen[0];
            imageLayoutParams.topMargin = locationOnScreen[1];
            imageView.setLayoutParams(imageLayoutParams);
            container.addView(imageView);

            progressBar = new ProgressBar(this);
            progressBar.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_small));
            FrameLayout.LayoutParams progressLayoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 150);
            progressLayoutParams.topMargin = ((int) mScreenRect.height() - 150) / 2;
            progressBar.setLayoutParams(progressLayoutParams);
            container.addView(progressBar);

            scale = mScreenRect.width() / width;
            translationX = ((int) mScreenRect.width() - width) / 2 - locationOnScreen[0];
            translationY = ((int) mScreenRect.height() - height) / 2 - locationOnScreen[1];
            playOpenAnimation();
        }

    }


    private void playOpenAnimation() {
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(imageView, "translationX", 0, translationX))
                .with(ObjectAnimator.ofFloat(imageView, "translationY", 0, translationY))
                .with(ObjectAnimator.ofFloat(imageView, "scaleX", 1, scale))
                .with(ObjectAnimator.ofFloat(imageView, "scaleY", 1, scale))
                .with(ObjectAnimator.ofArgb(container, "BackgroundColor", Color.TRANSPARENT, Color.BLACK));
        set.setDuration(300);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                photoView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.start();
    }

    private void playCloseAnimation() {
        imageView.setVisibility(View.VISIBLE);
        photoView.setVisibility(View.GONE);

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(imageView, "translationX", translationX, 0))
                .with(ObjectAnimator.ofFloat(imageView, "translationY", translationY, 0))
                .with(ObjectAnimator.ofFloat(imageView, "scaleX", scale, 1))
                .with(ObjectAnimator.ofFloat(imageView, "scaleY", scale, 1))
                .with(ObjectAnimator.ofArgb(container, "BackgroundColor", Color.BLACK, Color.TRANSPARENT));
        set.setDuration(300);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                finish();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.start();
    }

    @Override
    public void onBackPressed() {
        playCloseAnimation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    public static RectF getDisplayPixes(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        return new RectF(0, 0, metrics.widthPixels, metrics.heightPixels);
    }
}
