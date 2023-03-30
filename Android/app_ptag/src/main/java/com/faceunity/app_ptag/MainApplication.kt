package com.faceunity.app_ptag

import android.app.Application
import com.tencent.bugly.crashreport.CrashReport

/**
 * Created on 2021/7/21 0021 11:57.
 */
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FuDevInitializeWrapper.initSDK(this)

        initSDK()
    }

    /**
     * 初始化一些与相芯功能无关的 SDK，接入者应该替换为自己的实现。
     */
    private fun initSDK() {

    }
}