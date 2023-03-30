package com.faceunity.app_ptag.ui.drive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceunity.app_ptag.FuDevInitializeWrapper
import com.faceunity.app_ptag.ui.drive.entity.BodyFollowMode
import com.faceunity.app_ptag.ui.drive.entity.BodyTrackMode
import com.faceunity.app_ptag.ui.drive.entity.DrivePage
import com.faceunity.app_ptag.use_case.FuUseCaseFactory
import com.faceunity.core.avatar.model.Scene
import com.faceunity.editor_ptag.store.DevAIRepository
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevDrawRepository
import com.faceunity.editor_ptag.store.DevSceneManagerRepository
import com.faceunity.editor_ptag.util.FuElapsedTimeChecker
import com.faceunity.editor_ptag.util.FuLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FuDemoDriveViewModel : ViewModel() {

    private var drivePage: DrivePage = DrivePage.Text
    private val _drivePageLiveData = MutableLiveData<DrivePage>()
    val drivePageLiveData: LiveData<DrivePage> get() = _drivePageLiveData

    var isFaceTrack: Boolean = true
    var bodyTrackMode: BodyTrackMode = BodyTrackMode.Full
    var bodyFollowMode: BodyFollowMode = BodyFollowMode.Stage

    private var driveScene: Scene? = null

    /**
     * 按照页面的实际需求，构造一个对应的 Scene 和 Avatar。
     */
    fun drawAvatar() {
        viewModelScope.launch {
            FuElapsedTimeChecker.start("initDriveAvatarConfig")
            //创建一个用于驱动页面的 Scene，此处可自定义一些额外配置
            val scene = FuUseCaseFactory.smartCreateSceneUseCase()
            if (scene == null) return@launch
            scene.apply {
                FuDevInitializeWrapper.sceneCustom(this)
                DevAIRepository.openArMode(this)
                rendererConfig.setPostProcessMirrorParam(null)
            }
            val currentAvatarInfo = DevAvatarManagerRepository.getCurrentAvatarInfo()
            if (currentAvatarInfo == null) return@launch
            //根据当前 AvatarInfo 的 avatar.json，重建一个用于驱动页面的 Avatar。
            FuUseCaseFactory.rebuildAvatarUseCase(currentAvatarInfo)
            val sceneInfo = DevSceneManagerRepository.filterGenderDefaultSceneFirst(currentAvatarInfo.gender())
            if (sceneInfo == null) return@launch
            //根据驱动页面的需求，给该 Avatar 绑定一个呼吸动画
            DevSceneManagerRepository.setAvatarDefaultAnimation(currentAvatarInfo.avatar, sceneInfo.sceneResource)
            //应用该 Avatar 到 Scene 上。
            DevDrawRepository.replaceCurrentAvatar(currentAvatarInfo.avatar, scene)
            DevDrawRepository.setUseScene(scene) {
                FuElapsedTimeChecker.end("initDriveAvatarConfig")
            }
            driveScene = scene
            withContext(Dispatchers.Main) {
                init()
            }
        }
    }

    fun init() {
        _drivePageLiveData.value = drivePage
    }

    fun checkoutArPage() {
        if (drivePage == DrivePage.Ar) {
            FuLog.warn("checkoutArPage: already in ArPage, break.")
            return
        }
        drivePage = DrivePage.Ar
        _drivePageLiveData.value = drivePage
    }

    fun checkoutTrackPage() {
        if (drivePage == DrivePage.Track) {
            FuLog.warn("checkoutTrackPage: already in TrackPage, break.")
            return
        }
        drivePage = DrivePage.Track
        _drivePageLiveData.value = drivePage
    }

    fun checkoutTextPage() {
        if (drivePage == DrivePage.Text) {
            FuLog.warn("checkoutTextPage: already in TextPage, break.")
            return
        }
        drivePage = DrivePage.Text
        _drivePageLiveData.value = drivePage
    }

    fun release() {
        driveScene?.let {
            DevAIRepository.release(it)
        }
    }
}