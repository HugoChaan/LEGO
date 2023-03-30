package com.faceunity.app_ptag.util

import android.util.Log

/**
 *
 */
class FPSChecker(val tag: String = "FPSChecker") {

    var enable = false

    /**
     * 每多少帧进行一次打印。
     */
    var MaxFrameCnt = 10
    private var mCurrentFrameCnt = 0
    private var mLastOneHundredFrameTimeStamp: Long = 0
    private var mOneHundredFrameFUTime: Long = 0
    private var mFuCallStartTime: Long = 0 //渲染前时间锚点（用于计算渲染市场）

    /**
     * 相关结果回调
     */
    var print: (fps: Double, renderTime: Double) -> Unit = {  fps: Double, renderTime: Double ->
        Log.w(tag, "fps:$fps    renderTime:$renderTime")
    }

    /**
     * 进行一次 FPS 统计。在 Renderer 的 [onRenderBefore] 开始处调用。
     */
    fun benchmarkFPS() {
        if (!enable) return
        mOneHundredFrameFUTime += System.nanoTime() - mFuCallStartTime
        if (++mCurrentFrameCnt == MaxFrameCnt) {
            mCurrentFrameCnt = 0
            val fps: Double = MaxFrameCnt.toDouble() * 1000000000L / (System.nanoTime() - mLastOneHundredFrameTimeStamp)
            val renderTime: Double = mOneHundredFrameFUTime.toDouble() / MaxFrameCnt / 1000000L
            mLastOneHundredFrameTimeStamp = System.nanoTime()
            mOneHundredFrameFUTime = 0
            onBenchmarkFPSChanged(fps, renderTime)
        }
    }

    /**
     * 进行一次数据记录，在 Renderer 的 [onDrawFrameAfter] 开始处调用。
     */
    fun markRenderBefore() {
        if (!enable) return
        mFuCallStartTime = System.nanoTime()
    }

    private fun onBenchmarkFPSChanged(fps: Double, renderTime: Double) {
        print(fps, renderTime)
    }

}