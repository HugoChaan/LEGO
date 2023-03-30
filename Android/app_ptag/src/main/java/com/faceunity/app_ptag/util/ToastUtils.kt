package com.faceunity.app_ptag.util

import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.faceunity.app_ptag.R
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.toolbox.utils.FUDensityUtils
import java.lang.ref.WeakReference

/**
 * Toast 工具类
 */
object ToastUtils {
    private var successToast: WeakReference<Toast>? = null
    private var failureToast: WeakReference<Toast>? = null

    fun showSuccessToast(context: Context, msg: String) {
        try {
            successToast?.get()?.cancel()
            val toast = Toast(context)
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = layoutInflater.inflate(R.layout.toast_custom, null)
            view.findViewById<TextView>(R.id.toast_text).text = msg
            toast.view = view
            toast.setGravity(Gravity.TOP, 0, FUDensityUtils.dp2px(70f))
            toast.duration = Toast.LENGTH_SHORT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                toast.addCallback(object : Toast.Callback() {
                    override fun onToastHidden() {
                        successToast = null
                    }
                })
            }
            toast.show()
            successToast = WeakReference(toast)
            FuLog.info("showSuccessToast: $msg")
        } catch (ex: Throwable) {
            FuLog.warn("showSuccessToast error: $ex")
        }
    }

    fun showFailureToast(context: Context, msg: String) {
        try {
            failureToast?.get()?.cancel()
            val toast = Toast(context)
            val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = layoutInflater.inflate(R.layout.toast_custom, null)
            view.findViewById<ImageView>(R.id.toast_icon).setImageResource(R.drawable.icon_toast_failure)
            view.findViewById<TextView>(R.id.toast_text).text = msg
            toast.view = view
            toast.setGravity(Gravity.TOP, 0, FUDensityUtils.dp2px(70f))
            toast.duration = Toast.LENGTH_SHORT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                toast.addCallback(object : Toast.Callback() {
                    override fun onToastHidden() {
                        failureToast = null
                    }
                })
            }
            toast.show()
            failureToast = WeakReference(toast)
            FuLog.info("showFailureToast: $msg")
        } catch (ex: Throwable) {
            FuLog.warn("showFailureToast error: $ex")
        }

    }
}