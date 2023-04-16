package com.faceunity.app_ptag.ui.interaction.weight.lego

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.*
import androidx.annotation.RequiresApi
import com.faceunity.app_ptag.R

@SuppressLint("ServiceCast")
@RequiresApi(Build.VERSION_CODES.M)
class LegoView (context: Context, attrs: AttributeSet? = null) : ScrollView(context, attrs) {
    init {

        // 加载布局文件
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.lego_enable_view, this, true)

        // 获取按钮控件
        val joinBtn = findViewById<Button>(R.id.joinChannel_button)
        val leaveBtn = findViewById<Button>(R.id.leavechannel_button)
        //val localViewBtn = findViewById<Button>(R.id.local_toggle_button)
        val remoteViewBtn = findViewById<Button>(R.id.remote_toggle_button)
        val sttButton = findViewById<Button>(R.id.stt_button)
        val avatarButton = findViewById<Button>(R.id.avatar_button)
        val ttsButton = findViewById<Button>(R.id.tts_button)
        val gptButton = findViewById<Button>(R.id.gpt_button)
        val systemBtn = findViewById<Button>(R.id.system_button)

        // 设置按钮点击事件
        joinBtn.setOnClickListener {
            onLegoViewListener?.onJoinChannelButtonClick(findViewById<EditText>(R.id.channel_tx).text.toString())
            leaveBtn.isEnabled = true
            joinBtn.isEnabled = false
            showToast("加入频道")
        }
        leaveBtn.setOnClickListener {
            onLegoViewListener?.onLeaveChannelButtonClick()
            joinBtn.isEnabled = true
            leaveBtn.isEnabled = false
            showToast("退出频道")
        }
        leaveBtn.isEnabled = false
//        localViewBtn.setOnClickListener {
//            if ((it as ToggleButton).isChecked) {
//                remoteViewBtn.isEnabled = false
//                showToast("本地视频 开")
//                onLegoViewListener?.onLocalChatMode(true)
//            } else {
//                remoteViewBtn.isEnabled = true
//                showToast("本地视频 关")
//                onLegoViewListener?.onLocalChatMode(false)
//            }
//        }
        gptButton.setOnClickListener {
            if ((it as ToggleButton).isChecked) {
                showToast("GPT 开")
                onLegoViewListener?.onGPTMode(true)
            } else {
                showToast("GPT 关")
                onLegoViewListener?.onGPTMode(false)
            }
        }
        remoteViewBtn.setOnClickListener {
            if ((it as ToggleButton).isChecked) {
               // localViewBtn.isEnabled = false
                onLegoViewListener?.onRemoteChatMode(true)
            } else {
               // localViewBtn.isEnabled = true
                onLegoViewListener?.onRemoteChatMode(false)
            }
        }
        sttButton.setOnClickListener {
            if ((it as ToggleButton).isChecked) {
                showToast("语音转文字 开")
                onLegoViewListener?.onSttMode(true)
            } else {
                showToast("语音转文字 关")
                onLegoViewListener?.onSttMode(false)
            }
        }
        avatarButton.setOnClickListener {
            if ((it as ToggleButton).isChecked) {
                showToast("虚拟人 开")
                onLegoViewListener?.onAvatarShow(true)
            } else {
                showToast("虚拟人 关")
                onLegoViewListener?.onAvatarShow(false)
            }
        }
        ttsButton.setOnClickListener {
            if ((it as ToggleButton).isChecked) {
                showToast("文字转语音 开")
                onLegoViewListener?.onTTsMode(true)
            } else {
                showToast("文字转语音 关")
                onLegoViewListener?.onTTsMode(false)
            }
        }
        systemBtn.setOnClickListener {
            onLegoViewListener?.onSystemSetting(findViewById<EditText>(R.id.system_tx).text.toString())
        }
    }

    private var onLegoViewListener: OnLegoViewListener? = null
    fun setLegoViewListener(listener: OnLegoViewListener) {
        this.onLegoViewListener = listener
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    interface OnLegoViewListener {
        fun onJoinChannelButtonClick(channel: String)
        fun onLeaveChannelButtonClick()
        fun onLocalChatMode(enable: Boolean)
        fun onRemoteChatMode(enable: Boolean)
        fun onSttMode(enable: Boolean)
        fun onAvatarShow(enable: Boolean)
        fun onTTsMode(enable: Boolean)
        fun onGPTMode(enable: Boolean)
        fun onSystemSetting(system: String)
    }
}