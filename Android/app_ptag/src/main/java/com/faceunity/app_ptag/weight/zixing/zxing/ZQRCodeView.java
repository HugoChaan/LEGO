package com.faceunity.app_ptag.weight.zixing.zxing;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

import com.faceunity.app_ptag.R;
import com.faceunity.app_ptag.weight.zixing.core.BarcodeType;
import com.faceunity.app_ptag.weight.zixing.core.OnScanBoxRectListener;
import com.faceunity.app_ptag.weight.zixing.core.ProcessDataTask;
import com.faceunity.app_ptag.weight.zixing.core.QRCodeView;
import com.faceunity.app_ptag.weight.zixing.core.ScanBoxView;
import com.faceunity.app_ptag.weight.zixing.core.ScanResult;


public class ZQRCodeView extends RelativeLayout implements Camera.PreviewCallback, OnScanBoxRectListener {
    private static final int NO_CAMERA_ID = -1;
    protected SurfaceView mCameraPreview;
    protected ScanBoxView mScanBoxView;
    protected QRCodeView.Delegate mDelegate;
    protected boolean mSpotAble = false;
    protected ProcessDataTask mProcessDataTask;
    protected int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private PointF[] mLocationPoints;
    private Paint mPaint;
    protected BarcodeType mBarcodeType = BarcodeType.HIGH_FREQUENCY;
    private long mLastPreviewFrameTime = 0;
    private ValueAnimator mAutoZoomAnimator;
    private long mLastAutoZoomTime = 0;

    // 上次环境亮度记录的时间戳
    private long mLastAmbientBrightnessRecordTime = System.currentTimeMillis();
    // 上次环境亮度记录的索引
    private int mAmbientBrightnessDarkIndex = 0;
    // 环境亮度历史记录的数组，255 是代表亮度最大值
    private static final long[] AMBIENT_BRIGHTNESS_DARK_LIST = new long[]{255, 255, 255, 255};
    // 环境亮度扫描间隔
    private static final int AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME = 150;
    // 亮度低的阀值
    private static final int AMBIENT_BRIGHTNESS_DARK = 60;
    public ZQRCodeView(Context context) {
        super(context);
    }

    public ZQRCodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    private void initView(Context context, AttributeSet attrs) {
        mCameraPreview = new SurfaceView(context);
        mScanBoxView = new ScanBoxView(context);
        mScanBoxView.init(this, attrs);
        mCameraPreview.setId(R.id.bgaqrcode_camera_preview);
        addView(mCameraPreview);
        LayoutParams layoutParams = new LayoutParams(context, attrs);
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, mCameraPreview.getId());
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mCameraPreview.getId());
        addView(mScanBoxView, layoutParams);

        mPaint = new Paint();
        mPaint.setColor(getScanBoxView().getCornerColor());
        mPaint.setStyle(Paint.Style.FILL);
    }

    private ScanBoxView getScanBoxView() {
        return mScanBoxView;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    @Override
    public void onScanBoxRectChanged(Rect rect) {

    }
    public interface Delegate {
        void onCameraPreViewFrame(byte[] data,Camera camera);
        /**
         * 处理扫描结果
         *
         * @param result 摄像头扫码时只要回调了该方法 result 就一定有值，不会为 null。解析本地图片或 Bitmap 时 result 可能为 null
         */
        void onScanQRCodeSuccess(ScanResult result);

        /**
         * 摄像头环境亮度发生变化
         *
         * @param isDark 是否变暗
         */
        void onCameraAmbientBrightnessChanged(boolean isDark);

        /**
         * 处理打开相机出错
         */
        void onScanQRCodeOpenCameraError();
    }
}
