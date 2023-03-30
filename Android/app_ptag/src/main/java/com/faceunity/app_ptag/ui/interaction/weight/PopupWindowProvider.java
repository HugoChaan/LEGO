package com.faceunity.app_ptag.ui.interaction.weight;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.faceunity.app_ptag.R;
import com.faceunity.toolbox.utils.FUDensityUtils;


/**
 * @author Richie on 2019.03.27
 */
public class PopupWindowProvider {
    private Context mContext;
    private PopupWindow mPopupWindowRecord;
    private PopupWindow mPopupWindowCancel;
    private PopupWindow mPopupWindowWarn;
    private PopupWindow mPopupWindowTips;
    private TextView mTvRecordDuration;

    private TextView warnTextView;
    private View mParent;
    private int yOffset;
    private int mPopupWindowHeight, mPopupWindowWidth;

    public PopupWindowProvider(Context context, View parent) {
        mContext = context;
        mParent = parent;
        yOffset = 0;
        mPopupWindowHeight = FUDensityUtils.dp2px(140);
        mPopupWindowWidth = FUDensityUtils.dp2px(180);
    }

    public void showRecordPopupWindow(String duration) {
        if (mPopupWindowCancel != null && mPopupWindowCancel.isShowing()) {
            return;
        }
        if (mPopupWindowRecord == null) {
            mPopupWindowRecord = new PopupWindow(mContext);
            mPopupWindowRecord.setBackgroundDrawable(null);
            View contentView = LayoutInflater.from(mContext).inflate(R.layout.popup_recoding, null);
            mTvRecordDuration = contentView.findViewById(R.id.duration);
            mPopupWindowRecord.setContentView(contentView);
            mPopupWindowRecord.setWidth(mPopupWindowWidth);
            mPopupWindowRecord.setHeight(mPopupWindowHeight);
        }
        if (!mPopupWindowRecord.isShowing()) {
            dismissAllPopupWindow();
            mPopupWindowRecord.showAtLocation(mParent, Gravity.CENTER_HORIZONTAL, 0, yOffset);
        }

        if (!TextUtils.isEmpty(duration)) {
            mTvRecordDuration.setText(duration);
        }
    }

    public void refreshRecordDuration(String duration) {
        if (mPopupWindowRecord.isShowing() && mTvRecordDuration != null) {
            mTvRecordDuration.setText(duration);
        }
    }

    public void showCancelPopupWindow() {
        if (mPopupWindowCancel == null) {
            mPopupWindowCancel = new PopupWindow(mContext);
            mPopupWindowCancel.setBackgroundDrawable(null);
            View contentView = LayoutInflater.from(mContext).inflate(R.layout.popup_cancel, null);
            mPopupWindowCancel.setContentView(contentView);
            mPopupWindowCancel.setWidth(mPopupWindowWidth);
            mPopupWindowCancel.setHeight(mPopupWindowHeight);
        }
        if (!mPopupWindowCancel.isShowing()) {
            dismissAllPopupWindow();
            mPopupWindowCancel.showAtLocation(mParent, Gravity.CENTER_HORIZONTAL, 0, yOffset);
        }
    }

    public void showWarnPopupWindow(String tipText) {
        if (mPopupWindowWarn == null) {
            mPopupWindowWarn = new PopupWindow(mContext);
            mPopupWindowWarn.setBackgroundDrawable(null);
            View contentView = LayoutInflater.from(mContext).inflate(R.layout.popup_warning, null);
            warnTextView = contentView.findViewById(R.id.title);
            mPopupWindowWarn.setContentView(contentView);
            mPopupWindowWarn.setWidth(mPopupWindowWidth);
            mPopupWindowWarn.setHeight(mPopupWindowHeight);
        }
        if (!mPopupWindowWarn.isShowing()) {
            dismissAllPopupWindow();
            mPopupWindowWarn.showAtLocation(mParent, Gravity.CENTER_HORIZONTAL, 0, yOffset);
        }
        if (!TextUtils.isEmpty(tipText)) {
            warnTextView.setText(tipText);
        }
    }


    public void dismissAllPopupWindow() {
        dismissRecordPopupWindow();
        dismissCancelPopupWindow();
        dismissWarnPopupWindow();
        dismissTipsPopupWindow();
    }

    public void dismissRecordPopupWindow() {
        if (mPopupWindowRecord != null && mPopupWindowRecord.isShowing()) {
            mPopupWindowRecord.dismiss();
        }
    }

    public void dismissWarnPopupWindow() {
        if (mPopupWindowWarn != null && mPopupWindowWarn.isShowing()) {
            mPopupWindowWarn.dismiss();
        }
    }

    public void dismissCancelPopupWindow() {
        if (mPopupWindowCancel != null && mPopupWindowCancel.isShowing()) {
            mPopupWindowCancel.dismiss();
        }
    }

    public void dismissTipsPopupWindow() {
        if (mPopupWindowTips != null && mPopupWindowTips.isShowing()) {
            mPopupWindowTips.dismiss();
            mPopupWindowTips = null;
        }
    }
}
