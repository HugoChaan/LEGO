package com.faceunity.app_ptag.view_model.helper

import androidx.lifecycle.LiveData
import com.faceunity.app_ptag.ui.home.entity.state.ExceptionEvent
import com.faceunity.app_ptag.ui.home.entity.state.LoadingState
import com.faceunity.editor_ptag.use_case.cloud_platform.DownloadBundleUseCase
import kotlinx.coroutines.flow.Flow

/**
 *
 */
interface FuDownloadUiImpl {

    fun getLoadingState(): LiveData<LoadingState>

    fun notifyLoadingState(loadingState: LoadingState)

    fun getExceptionEvent(): LiveData<ExceptionEvent>

    fun notifyExceptionEvent(exceptionEvent: ExceptionEvent)

    fun finishExceptionEvent()

    fun parseCatchTip(throwable: Throwable): String

    suspend fun parseDownloadFlow(
        downloadFlow: Flow<DownloadBundleUseCase.State>,
        styleConfig: FuDownloadUIHelper.StyleConfig = FuDownloadUIHelper.StyleConfig(),
        retryBlock: Block
    ): DownloadBundleUseCase.State
}