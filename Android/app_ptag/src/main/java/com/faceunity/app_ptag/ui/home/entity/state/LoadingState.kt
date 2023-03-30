package com.faceunity.app_ptag.ui.home.entity.state

/**
 *
 */
sealed class LoadingState {
    /**
     * 因为当前为了兼容性考虑，使用了 LiveData，导致初始化时会发送一次默认值。故先临时增加该变量，而不对其处理。
     */
    object UnDo: LoadingState()
    object Hidden : LoadingState()
    object Show : LoadingState()
    data class ShowWithTip(val content: String, val processText: String? = null) : LoadingState()
}
