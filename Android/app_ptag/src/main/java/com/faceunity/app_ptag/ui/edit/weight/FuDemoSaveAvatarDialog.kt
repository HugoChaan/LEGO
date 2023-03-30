package com.faceunity.app_ptag.ui.edit.weight

import android.content.Context
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.DialogFuDemoCommonBinding
import com.faceunity.app_ptag.weight.FuBaseDialog

/**
 *
 */
class FuDemoSaveAvatarDialog(ctx: Context) : FuBaseDialog(ctx) {

    var onCancel: () -> Unit = {}
    var onFinish: () -> Unit = {}

    override fun getLayoutId() = R.layout.dialog_fu_demo_common

    override fun afterCreate() {
        val binding = DialogFuDemoCommonBinding.bind(mRootView)

        binding.apply {
            icon.setImageResource(R.drawable.icon_popup_modelfailed)
            title.text = "当前形象尚未保存"
            cancelBtn.text = "确认离开"
            finishBtn.text = "保存并返回"

            cancelBtn.setOnClickListener {
                onCancel()
            }
            finishBtn.setOnClickListener {
                onFinish()
            }
        }
    }
}