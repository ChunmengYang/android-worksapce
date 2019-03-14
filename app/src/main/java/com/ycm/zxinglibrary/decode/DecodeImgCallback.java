package com.ycm.zxinglibrary.decode;

import com.google.zxing.Result;

public interface DecodeImgCallback {
    public void onImageDecodeSuccess(Result result);

    public void onImageDecodeFailed();
}
