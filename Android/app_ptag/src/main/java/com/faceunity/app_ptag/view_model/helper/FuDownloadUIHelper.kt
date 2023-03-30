package com.faceunity.app_ptag.view_model.helper

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.faceunity.app_ptag.ui.home.entity.state.ExceptionEvent
import com.faceunity.app_ptag.ui.home.entity.state.LoadingState
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadBundleUseCase
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.safeCollect
import com.faceunity.fupta.cloud_download.FormatFailure
import com.faceunity.fupta.cloud_download.RequestFailure
import com.faceunity.fupta.cloud_download.ResponseCodeThrowable
import com.faceunity.fupta.cloud_download.ServiceCodeThrowable
import kotlinx.coroutines.flow.*

/**
 * 下载功能与 UI 相关的辅助类。
 */
class FuDownloadUIHelper: FuDownloadUiImpl {
    val _loadingState = MutableStateFlow<LoadingState>(LoadingState.UnDo)

    /**
     * 下载进度状态。
     */
    private val loadingState = _loadingState.asLiveData()
    override fun getLoadingState(): LiveData<LoadingState> = loadingState

    override fun notifyLoadingState(loadingState: LoadingState) {
        _loadingState.value = loadingState
    }

    private val _exceptionEvent = MutableStateFlow<ExceptionEvent>(ExceptionEvent.Nothing)

    /**
     * 下载异常事件。
     */
    private val exceptionEvent = _exceptionEvent.asLiveData()
    override fun getExceptionEvent(): LiveData<ExceptionEvent> = exceptionEvent

    override fun notifyExceptionEvent(exceptionEvent: ExceptionEvent) {
        _exceptionEvent.value = exceptionEvent
    }

    /**
     * 消费了该事件。
     * 为了解决 LiveData 重放的问题，故事件需要手动申明消费。
     */
    override fun finishExceptionEvent() {
        _exceptionEvent.value = ExceptionEvent.Nothing
    }

    /**
     * 解析这个网络操作的异常的显示文案。
     */
    override fun parseCatchTip(throwable: Throwable) : String {
        val tipContent = when(throwable) {
            is ResponseCodeThrowable -> "请求网络异常：${throwable.code}"
            is RequestFailure -> "请求操作异常：${throwable.ex.message}"
            is FormatFailure -> "数据解析异常：${throwable.desc} ${throwable.originContent.run { if (length <= 50) this else substring(0, 49) }}"
            is ServiceCodeThrowable.TokenThrowable -> "Token 过期"
            is ServiceCodeThrowable.NormalThrowable -> "服务器异常，错误码：${throwable.code}，错误信息：${throwable.message}"
            else -> throwable.toString()
        }
        FuLog.warn("parseCatch failed:$throwable")
        return tipContent
    }

    suspend fun parseDownloadFlow(downloadFlow: Flow<DownloadBundleUseCase.State>, retryBlock: Block) =
        parseDownloadFlow(downloadFlow, StyleConfig(), retryBlock)

    /**
     * 解析一次下载事件，并更新相关 UI 状态。
     * @param retryBlock 重试按钮需要进行的操作
     * @return 该下载事件是否成功完成
     */
    override suspend fun parseDownloadFlow(downloadFlow: Flow<DownloadBundleUseCase.State>, styleConfig: StyleConfig, retryBlock: Block): DownloadBundleUseCase.State {
        var state: DownloadBundleUseCase.State? = null
        downloadFlow.onStart {
            notifyLoadingState(LoadingState.ShowWithTip(styleConfig.tipContent))
        }.onCompletion {
            if (styleConfig.isDismissOnCompletion) {
                notifyLoadingState(LoadingState.Hidden)
            }
        }.onEach {
            FuLog.info("downloadBundleUseCase:$it")
            when (it) {
                is DownloadBundleUseCase.State.Finish, DownloadBundleUseCase.State.Skip -> {
                    state = it
                }
                is DownloadBundleUseCase.State.DownloadProgress -> {
                    notifyLoadingState(LoadingState.ShowWithTip(styleConfig.tipContent, "${it.downloadCount}/${it.totalCount}"))
                }
                is DownloadBundleUseCase.State.DownloadFailed -> {
                    state = it
                    notifyExceptionEvent(ExceptionEvent.FailedToast("${it.errorResource.map { it.split("/").last() }}下载失败，请重试"))
                }
                is DownloadBundleUseCase.State.StartFailed -> {
                    state = it
                    FuLog.warn("${it.lostFileIdList}下载失败")
                    notifyExceptionEvent(ExceptionEvent.FailedToast("素材已更新"))
                }
            }
        }.catch {
            notifyExceptionEvent(ExceptionEvent.RetryDialog(parseCatchTip(it), { retryBlock() }))
        }.safeCollect {}

        return state!!
    }

    data class StyleConfig(
        val tipContent: String = "正在下载道具资源",
        val isDismissOnCompletion: Boolean = true
    )
}

typealias Block = () -> Unit