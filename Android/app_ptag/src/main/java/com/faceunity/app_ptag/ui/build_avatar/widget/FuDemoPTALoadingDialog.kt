package com.faceunity.app_ptag.ui.build_avatar.widget

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.weight.FuBaseDialog
import org.libpag.PAGFile
import org.libpag.PAGView

/**
 *
 */
class FuDemoPTALoadingDialog(ctx: Context, val content: String? = "") : FuBaseDialog(ctx) {
    override fun getLayoutId() = R.layout.dialog_fu_demo_pta_loading

    override fun afterCreate() {
        setCanceledOnTouchOutside(false)
        findViewById<TextView>(R.id.content_text).apply {
            if (content?.isNotBlank() ?: false) {
                text = content
            }
        }
        findViewById<PAGView>(R.id.loading_image).apply {
            val startFile = PAGFile.Load(ctx.resources.openRawResource(R.raw.pta_loading_start).readBytes())
            val loopFile = PAGFile.Load(ctx.resources.openRawResource(R.raw.pta_loading_loop).readBytes())
            setRepeatCount(1)
            composition = startFile
            addListener(object : PAGView.PAGViewListener {
                override fun onAnimationEnd(view: PAGView) {
                    view.apply {
                        composition = loopFile
                        setRepeatCount(0)
                        play()
                    }
                }

                override fun onAnimationStart(view: PAGView) = Unit
                override fun onAnimationCancel(view: PAGView) = Unit
                override fun onAnimationRepeat(view: PAGView) = Unit
                override fun onAnimationUpdate(view: PAGView) = Unit
            })
            play()

        }
    }

    override fun resetHeight() = ViewGroup.LayoutParams.MATCH_PARENT
}