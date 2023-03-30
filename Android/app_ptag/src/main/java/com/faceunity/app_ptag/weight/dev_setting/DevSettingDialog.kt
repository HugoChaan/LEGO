package com.faceunity.app_ptag.weight.dev_setting

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.FuDevSettingBottomsheetBinding
import com.faceunity.app_ptag.databinding.ItemDevSettingBinding
import com.faceunity.app_ptag.weight.dev_setting.entity.DevAction
import com.faceunity.app_ptag.weight.dev_setting.entity.DevSetting
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 *
 */
class DevSettingDialog(context: Context) : BottomSheetDialog(context) {
    private lateinit var binding: FuDevSettingBottomsheetBinding
    private val settingList = mutableListOf<DevSetting>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FuDevSettingBottomsheetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dismissWithAnimation = true
        initView()
    }

    private fun initView() {
        settingList.forEach { setting ->
            val itemBinding = ItemDevSettingBinding.inflate(layoutInflater)
            itemBinding.apply {
                title.text = setting.title
                when(setting.action) {
                    is DevAction.Click -> {
                        root.setOnClickListener {
                            setting.action.action()
                        }
                    }
                    is DevAction.Switch -> {
                        switchBtn.visible()
                        desc.gone()
                        arrowIcon.gone()
                        switchImageBtn(switchBtn, setting.action.nowState)
                        switchBtn.setOnClickListener {
                            setting.action.nowState = !setting.action.nowState
                            setting.action.action(setting.action.nowState)
                            switchImageBtn(switchBtn, setting.action.nowState)
                        }
                    }
                    is DevAction.List -> {
                        desc.text = setting.action.nowText
                        val list = setting.action.list
                        val popupMenu = PopupMenu(context, desc, Gravity.END)
                        list.forEach {
                            popupMenu.menu.add(it.text)
                        }
                        popupMenu.setOnMenuItemClickListener { menuItem ->
                            list.firstOrNull { it.text == menuItem.title  }?.action?.invoke()
                            desc.text = menuItem.title
                            true
                        }
                        root.setOnClickListener {
                            popupMenu.show()
                        }
                    }
                }
            }
            binding.root.addView(itemBinding.root)
        }
    }

    private fun switchImageBtn(imageView: ImageView, state: Boolean) {
        if (state) {
            imageView.setImageResource(R.drawable.btn_switch_on)
        } else {
            imageView.setImageResource(R.drawable.btn_switch_off)
        }
    }

    fun setSetting(list: List<DevSetting>) {
        settingList.clear()
        settingList.addAll(list)
    }
}