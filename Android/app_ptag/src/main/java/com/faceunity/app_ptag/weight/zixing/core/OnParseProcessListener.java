package com.faceunity.app_ptag.weight.zixing.core;

import android.graphics.Bitmap;

public interface OnParseProcessListener {
    void onPostParseData(ScanResult scanResult);
    void onPostParseBitmapOrPicture(ScanResult scanResult);
    ScanResult processBitmapData(Bitmap bitmap);
    ScanResult processData(byte[] data, int width, int height, boolean isRetry);
}
