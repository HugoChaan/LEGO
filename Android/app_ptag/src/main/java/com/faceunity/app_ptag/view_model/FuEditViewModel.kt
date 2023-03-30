package com.faceunity.app_ptag.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.faceunity.app_ptag.ui.edit.entity.GenderProFilter
import com.faceunity.app_ptag.ui.edit.entity.control.*
import com.faceunity.app_ptag.ui.edit.expand.download.DownloadDispatcher
import com.faceunity.app_ptag.ui.edit.filter.FilterGroup
import com.faceunity.app_ptag.ui.edit.filter.GenderFilter
import com.faceunity.app_ptag.ui.edit.parser.ControlModelOldParser
import com.faceunity.core.avatar.model.Avatar
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.store.DevEditRepository
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.fupta.avatar_data.entity.resource.*
import com.faceunity.fupta.facepup.entity.tier.FacepupSlider
import com.faceunity.pta.pta_core.model.AvatarInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 因为旧的接口大多与 UI 模型相关，故将相关逻辑整理重构后，都移至 [FuDemoEditViewModel]
 */
class FuEditViewModel: ViewModel() {

}