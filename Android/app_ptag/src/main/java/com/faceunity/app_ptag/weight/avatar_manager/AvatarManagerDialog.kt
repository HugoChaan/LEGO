package com.faceunity.app_ptag.weight.avatar_manager

import android.content.Context
import android.os.Bundle
import com.faceunity.app_ptag.databinding.FuRecyclerViewBottomsheetBinding
import com.faceunity.app_ptag.weight.avatar_manager.entity.FuAvatarContainer
import com.faceunity.app_ptag.weight.avatar_manager.entity.FuAvatarWrapper
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Created on 2021/12/13 0013 19:40.
 */
class AvatarManagerDialog(context: Context, val onItemClick: (item: FuAvatarWrapper) -> Unit, val onItemDelete: (item: FuAvatarWrapper) -> Unit) : BottomSheetDialog(context) {
    private lateinit var binding: FuRecyclerViewBottomsheetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FuRecyclerViewBottomsheetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.title.text = "形象管理"
        binding.avatarManagerView.onItemClick = onItemClick
        binding.avatarManagerView.onItemDelete = onItemDelete
    }

    fun syncAvatarContainer(fuAvatarContainer: FuAvatarContainer) {
        binding.avatarManagerView.syncAvatarContainer(fuAvatarContainer)
    }


}