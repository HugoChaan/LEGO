package com.faceunity.app_ptag.weight.zixing.core;

import android.graphics.Bitmap;
import android.graphics.PointF;

import java.util.Arrays;

/**
 * 作者:王浩
 * 创建时间:2018/6/15
 * 描述:
 */
public class ScanResult {
    String result;//识别结果
    PointF[] resultPoints;
    byte[] data;//摄像头数据
    Bitmap bitmap;//图片

    public ScanResult(String result) {
        this.result = result;
    }
    public ScanResult(String result, Bitmap bitmap) {
        this.result = result;
        this.bitmap = bitmap;
    }
    public ScanResult(String result, byte[] data){
        this.result = result;
        this.data = data;
    }
    public ScanResult(String result, PointF[] resultPoints) {
        this.result = result;
        this.resultPoints = resultPoints;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public byte[] getData() {
        return data;
    }

    public PointF[] getResultPoints() {
        return resultPoints;
    }

    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "result='" + result + '\'' +
                ", resultPoints=" + Arrays.toString(resultPoints) +
                ", data=" + Arrays.toString(data) +
                ", bitmap=" + bitmap +
                '}';
    }
}
