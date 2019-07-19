package com.ycm;

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
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImagePreviewActivity extends Activity {

    private RectF mScreenRect = null;

    LinearLayout container;
    ImageView imageView;
    PhotoView photoView;

    float scale = 1f;
    int translationX = 0;
    int translationY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        container = new LinearLayout(this);
        setContentView(container);

        mScreenRect = getDisplayPixes(this);

        Intent intent = getIntent();
        if (intent != null) {
            byte [] bis = intent.getByteArrayExtra("bitmap");
            Bitmap bitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);

            int width = intent.getIntExtra("width", 0);
            int height = intent.getIntExtra("height", 0);
            int[] locationOnScreen = intent.getIntArrayExtra("locationOnScreen");
            int statusHeight = 0;
            int resourceId = this.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusHeight = this.getResources().getDimensionPixelSize(resourceId);
            }
            locationOnScreen[1] -= statusHeight;

            photoView = new PhotoView(this);
            photoView.setImageBitmap(bitmap);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            photoView.setLayoutParams(layoutParams);
            photoView.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
                @Override
                public void onPhotoTap(View view, float x, float y) {
                    playCloseAnimation();
                }

                @Override
                public void onOutsidePhotoTap() {

                }
            });

            imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            LinearLayout.LayoutParams imageLayoutParams = new LinearLayout.LayoutParams(width, height);
            imageLayoutParams.leftMargin = locationOnScreen[0];
            imageLayoutParams.topMargin = locationOnScreen[1];
            imageView.setLayoutParams(imageLayoutParams);
            container.addView(imageView);

            scale = mScreenRect.width() / width;
            translationX = ((int) mScreenRect.width() - width) / 2 - locationOnScreen[0];
            translationY = ((int) mScreenRect.height() - statusHeight - height) / 2 - locationOnScreen[1];

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
        set.setDuration(500);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                    container.addView(photoView);
                    imageView.setVisibility(View.GONE);
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
        photoView.setScale(1);
        imageView.setVisibility(View.VISIBLE);
        photoView.setVisibility(View.GONE);

        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(imageView, "translationX", translationX, 0))
                .with(ObjectAnimator.ofFloat(imageView, "translationY", translationY, 0))
                .with(ObjectAnimator.ofFloat(imageView, "scaleX", scale, 1))
                .with(ObjectAnimator.ofFloat(imageView, "scaleY", scale, 1))
                .with(ObjectAnimator.ofArgb(container, "BackgroundColor", Color.BLACK, Color.TRANSPARENT));
        set.setDuration(500);
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

    private static RectF getDisplayPixes(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        return new RectF(0, 0, metrics.widthPixels, metrics.heightPixels);
    }
}
