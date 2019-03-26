package com.ycm.demo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class ImageUploadAndDownLoadActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView imageView;
    private Button caremaBtn;
    private Button galleryBtn;

    private static final int PHOTO_REQUEST_CAREMA = 1;// 拍照
    private static final int PHOTO_REQUEST_GALLERY = 2;// 从相册中选择
    private static final int PHOTO_REQUEST_CUT = 3;// 结果


    private static final String PHOTO_FILE_NAME = "temp_photo.jpg";
    private File tempFile;

    private static final String PHOTO_CROP_FILE_NAME = "temp_crop_photo.jpg";
    private File tempCropFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_upload_and_down_load);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        imageView = findViewById(R.id.image_upload_and_download_img);
        caremaBtn = findViewById(R.id.image_upload_and_download_carema);
        galleryBtn = findViewById(R.id.image_upload_and_download_gallery);

        caremaBtn.setOnClickListener(this);
        galleryBtn.setOnClickListener(this);

        getBitmapFromSharedPreferences();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final int REQUEST_PERMISSION_CODE_CAMERA = 1;
    private static final int REQUEST_PERMISSION_CODE_WRITE_EXTERNAL_STORAGE = 2;
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.image_upload_and_download_carema:
                openCarema();
                break;
            case R.id.image_upload_and_download_gallery:
                // 激活系统图库，选择一张图片
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");

                startActivityForResult(galleryIntent, PHOTO_REQUEST_GALLERY);
                break;
        }
    }

    private void openCarema() {
        // 激活相机
        Intent caremaIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        // 判断存储卡是否可以用，可用进行存储
        if (hasSdcard()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "需要打开相机权限才可以拍照", Toast.LENGTH_LONG).show();
                        return;
                    }

                    //请求相机权限
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_PERMISSION_CODE_CAMERA);
                    return;
                }

                if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Toast.makeText(this, "需要打开存储权限才可以拍照", Toast.LENGTH_LONG).show();
                        return;
                    }

                    //请求相机权限
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_CODE_WRITE_EXTERNAL_STORAGE);
                    return;
                }

            }

            tempFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), PHOTO_FILE_NAME);//SD卡的应用关联缓存目录
            try {
                if(tempFile.exists()){
                    tempFile.delete();
                }
                tempFile.createNewFile();
            } catch (Exception e) {
                Toast.makeText(ImageUploadAndDownLoadActivity.this, "没有找到储存目录",Toast.LENGTH_LONG).show();
            }

            // 从文件中创建uri
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", tempFile);
            } else {
                uri = Uri.fromFile(tempFile);
            }
            caremaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            caremaIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            caremaIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }

        startActivityForResult(caremaIntent, PHOTO_REQUEST_CAREMA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //这里进行授权被允许的处理
                openCarema();
            } else {
                //permission denied, boo! Disable the functionality that depends on this permission.
                //这里进行权限被拒绝的处理
                Toast.makeText(this, "需要打开相机权限才可以拍照", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_PERMISSION_CODE_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //用户允许改权限，0表示允许，-1表示拒绝 PERMISSION_GRANTED = 0， PERMISSION_DENIED = -1
                //这里进行授权被允许的处理
                openCarema();
            } else {
                //permission denied, boo! Disable the functionality that depends on this permission.
                //这里进行权限被拒绝的处理
                Toast.makeText(this, "需要打开存储权限才可以拍照", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /*
     * SDcard是否被挂载
     */
    private boolean hasSdcard() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PHOTO_REQUEST_GALLERY) {
            // 从相册返回的数据
            if (data != null) {
                // 得到图片的全路径
                Uri uri = data.getData();
                crop(uri);
            }
        } else if (requestCode == PHOTO_REQUEST_CAREMA) {
            // 从相机返回的数据
            if (hasSdcard()) {
                if (tempFile.exists() && tempFile.length() > 0) {
                    Uri uri;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        uri = FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", tempFile);
                    } else {
                        uri = Uri.fromFile(tempFile);
                    }
                    crop(uri);
                }
            } else {
                Toast.makeText(this, "未找到存储卡，无法存储照片！", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PHOTO_REQUEST_CUT) {
            // 从剪切图片返回的数据
            if (tempCropFile.exists() && tempCropFile.length() > 0) {
                Bitmap bitmap = BitmapFactory.decodeFile(tempCropFile.getAbsolutePath());

                imageView.setImageBitmap(bitmap);
                //保存到SharedPreferences
                saveBitmapToSharedPreferences(bitmap);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
     * 剪切图片
     */
    private void crop(Uri uri) {
        tempCropFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), PHOTO_CROP_FILE_NAME);//SD卡的应用关联缓存目录
        try {
            if(tempCropFile.exists()){
                tempCropFile.delete();
            }
            tempCropFile.createNewFile();
        } catch (Exception e) {
            Toast.makeText(ImageUploadAndDownLoadActivity.this, "没有找到储存目录",Toast.LENGTH_LONG).show();
        }
        // 从文件中创建uri
        Uri cropUri = Uri.fromFile(tempCropFile);;

        // 获取屏幕密度
        DisplayMetrics dm = new DisplayMetrics();
        dm = getResources().getDisplayMetrics();
        float density  = dm.density;

        // 裁剪图片意图
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("circleCrop", false);
        intent.putExtra("crop", "true");

        // 裁剪框的比例，1:1
        if (android.os.Build.BRAND.contains("HUAWEI") || android.os.Build.MODEL.contains("HUAWEI")) {
            //华为特殊处理 不然会显示圆
            intent.putExtra("aspectX", 9998);
            intent.putExtra("aspectY", 9999);
        } else {
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
        }
//        intent.putExtra("aspectX", 1);
//        intent.putExtra("aspectY", 1);

        // 裁剪后输出图片的尺寸大小
        intent.putExtra("outputX", (int)(200 * density));
        intent.putExtra("outputY", (int)(200 * density));

        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());// 图片格式
        intent.putExtra("noFaceDetection", true);// 取消人脸识别
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        ComponentName componentName = intent.resolveActivity(getPackageManager());
        if (componentName != null) {
            startActivityForResult(intent, PHOTO_REQUEST_CUT);
        }
    }

    /*
     * 保存图片到SharedPreferences
     */
    private void saveBitmapToSharedPreferences(Bitmap bitmap) {
        //第一步:将Bitmap压缩至字节数组输出流ByteArrayOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);

        //第二步:利用Base64将字节数组输出流中的数据转换成字符串String
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String imageString = new String(Base64.encodeToString(byteArray, Base64.DEFAULT));

        //第三步:将String保持至SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("CURRENT_USER", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("USER_ICON", imageString);
        editor.commit();
    }

    /*
     * 从SharedPreferences获取图片
     */
    private void getBitmapFromSharedPreferences(){
        SharedPreferences sharedPreferences = getSharedPreferences("CURRENT_USER", Context.MODE_PRIVATE);
        //第一步:取出字符串形式的Bitmap
        String imageString=sharedPreferences.getString("USER_ICON", "");

        //第二步:利用Base64将字符串转换为ByteArrayInputStream
        byte[] byteArray = Base64.decode(imageString, Base64.DEFAULT);
        if (byteArray.length == 0) {
            imageView.setImageResource(R.mipmap.ic_launcher);
        } else {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

            //第三步:利用ByteArrayInputStream生成Bitmap
            Bitmap bitmap= BitmapFactory.decodeStream(byteArrayInputStream);
            imageView.setImageBitmap(bitmap);
        }

    }

}
