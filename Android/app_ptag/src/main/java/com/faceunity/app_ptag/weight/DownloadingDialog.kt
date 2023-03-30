package com.faceunity.app_ptag.weight

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.faceunity.app_ptag.R

/**
 *
 */
class DownloadingDialog(ctx: Context) : FuBaseDialog(ctx) {
    private var titleTextView: TextView? = null
    private var processTextView: TextView? = null
    private var loadingTitle: String? = null
    private var loadingProcessText: String? = null

    override fun getLayoutId() = R.layout.dialog_downloading

    override fun afterCreate() {
        setCanceledOnTouchOutside(false)
        titleTextView = mRootView.findViewById<TextView>(R.id.title)
        processTextView = mRootView.findViewById<TextView>(R.id.loading_text)
        if (loadingTitle != null) {
            titleTextView?.text = loadingTitle + "\n请耐心等待…"
        }
        if (loadingProcessText != null) {
            processTextView?.text = loadingProcessText
        } else {
            processTextView?.text = ""
        }
    }

    fun updateText(title: String, processText: String? = null) {
        loadingTitle = title
        loadingProcessText = processText
        titleTextView?.text = loadingTitle + "\n请耐心等待…"
        if (loadingProcessText != null) {
            processTextView?.text = loadingProcessText
        } else {
            processTextView?.text = ""
        }
    }

    override fun resetHeight(): Int {
        return ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun onBackPressed() {

    }
}