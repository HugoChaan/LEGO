package com.faceunity.app_ptag.ui.interaction.weight.lego

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.faceunity.app_ptag.R

@SuppressLint("ServiceCast")
@RequiresApi(Build.VERSION_CODES.M)
class ModeView (context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL

        // 加载布局文件
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.lego_mode_switcher_view, this, true)

        // 获取按钮控件
        val button1 = findViewById<Button>(R.id.button1)
        val button2 = findViewById<Button>(R.id.button2)
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)
        val button5 = findViewById<Button>(R.id.button5)

        // 设置按钮点击事件
        button1.setOnClickListener {
            onLegoViewListener?.onButton1Click()
            button1.isEnabled = false
            button2.isEnabled = true
            button3.isEnabled = true
            button4.isEnabled = true
            button5.isEnabled = true
            showToast("普通模式")
        }
        button2.setOnClickListener {
            onLegoViewListener?.onButton2Click()
            button2.isEnabled = false
            button1.isEnabled = true
            button3.isEnabled = true
            button4.isEnabled = true
            button5.isEnabled = true
            showToast("直播带货模式开启")
        }
        button3.setOnClickListener {
            onLegoViewListener?.onButton3Click()
            button3.isEnabled = false
            button2.isEnabled = true
            button1.isEnabled = true
            button4.isEnabled = true
            button5.isEnabled = true
            showToast("娱乐表演模式开启")
        }
        button4.setOnClickListener {
            onLegoViewListener?.onButton4Click()
            button4.isEnabled = false
            button2.isEnabled = true
            button3.isEnabled = true
            button1.isEnabled = true
            button5.isEnabled = true
            showToast("在线教育模式开启")
        }
        button5.setOnClickListener {
            onLegoViewListener?.onButton5Click()
            button5.isEnabled = false
            button2.isEnabled = true
            button3.isEnabled = true
            button4.isEnabled = true
            button1.isEnabled = true
            showToast("会议助手模式开启")
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
        fun onButton1Click()
        fun onButton2Click()
        fun onButton3Click()
        fun onButton4Click()
        fun onButton5Click()
    }
}