package com.faceunity.app_ptag.ui.build_avatar

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.FuDemoBuildFragmentBinding
import com.faceunity.app_ptag.ui.build_avatar.widget.FuDemoBuildFinishDialog
import com.faceunity.app_ptag.ui.build_avatar.widget.FuDemoPTALoadingDialog
import com.faceunity.app_ptag.use_case.renderer.BindRendererListenerUseCase
import com.faceunity.app_ptag.use_case.renderer.CreateRendererUseCase
import com.faceunity.app_ptag.use_case.renderer.RendererBindLifecycleUseCase
import com.faceunity.app_ptag.util.ToastUtils
import com.faceunity.app_ptag.view_model.FuBuildAvatarViewModel
import com.faceunity.core.entity.FURenderInputData
import com.faceunity.core.media.photo.FUPhotoRecordHelper
import com.faceunity.editor_ptag.store.DevAIRepository
import com.faceunity.editor_ptag.util.GL
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible
import com.faceunity.toolbox.file.FUFileUtils
import com.faceunity.toolbox.media.FUMediaUtils
import com.faceunity.toolbox.utils.FUDensityUtils
import com.faceunity.toolbox.utils.FUGLUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FuDemoBuildFragment : Fragment() {

    companion object {
        fun newInstance() = FuDemoBuildFragment()
        const val IMAGE_REQUEST_CODE = 0x102
    }

    private lateinit var binding: FuDemoBuildFragmentBinding

    private lateinit var viewModel: FuDemoBuildViewModel
    private lateinit var fuBuildAvatarViewModel: FuBuildAvatarViewModel

    private val ptaRenderer by lazy {
        CreateRendererUseCase.buildCamera().apply {
            RendererBindLifecycleUseCase(this).execute(lifecycle)
        }
    }

    private val loadingDialog by lazy {
        FuDemoPTALoadingDialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FuDemoBuildFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(FuDemoBuildViewModel::class.java)
        fuBuildAvatarViewModel = ViewModelProvider(this).get(FuBuildAvatarViewModel::class.java)

        initView()
        initSelectGenderLayout()
        initRenderer()
        inject()

    }

    private fun requestRender() {
        fuBuildAvatarViewModel.initPTAResource()
        DevAIRepository.aiOpenFaceTrack()
    }

    private fun initView() {
        binding.takePhotoBack.setOnClickListener {
            Navigation.findNavController(it).popBackStack() //关闭页面
        }
        binding.takePhotoChangeCamera.setOnClickListener {
            ptaRenderer.switchCamera() //切换摄像头
        }
        binding.takePhotoSelect.setOnClickListener {
            openSelectPhotoPage() //打开选择图片页面，完成后会调用 [onActivityResult]
        }
        binding.takePhotoBtn.setOnClickListener {
            if (viewModel.isCheckFacePassLiveData.value != true) {
                ToastUtils.showFailureToast(requireContext(), "未识别到人脸")
                return@setOnClickListener
            }
            takeSnapSotToPTA() //用 PTARenderer 拍照，成功后会调用 [onSaveSnapShotSuccess]
        }
    }

    private fun initSelectGenderLayout() {
        binding.createDialogBack.setOnClickListener {
            fuBuildAvatarViewModel.clearCachePhoto() //清空之前选择的图片。只是清空状态，并不会删除文件。
            binding.selectGenderLayout.gone()
        }
        binding.createDialogLeft.setOnClickListener {
            buildAvatar(true) //申请生成男性 Avatar，需提前调用 [cachePhoto] 设置图片
            loadingDialog.show()
        }
        binding.createDialogRight.setOnClickListener {
            buildAvatar(false) //申请生成女性 Avatar，需提前调用 [cachePhoto] 设置图片
            loadingDialog.show()
        }
    }

    private fun buildAvatar(isMale: Boolean) {
        lifecycleScope.launch {
            fuBuildAvatarViewModel.buildAndLoadAvatarThenSwitch(isMale).onSuccess {
                loadingDialog.dismiss()
                FuDemoBuildFinishDialog(requireContext()).apply {
                    callback = object : FuDemoBuildFinishDialog.Callback {
                        override fun onCancel() {
                            findNavController().popBackStack()
                            dismiss()
                        }

                        override fun onFinish() {
                            findNavController().popBackStack()
                            findNavController().navigate(R.id.editFragment)
                            dismiss()
                        }
                    }
                    create()
                    show()
                }
            }.onFailure {
                loadingDialog.dismiss()
                ToastUtils.showFailureToast(requireContext(), "生成失败，请重新拍照尝试。\n${it}")
            }
        }
    }

    private fun initRenderer() {
        ptaRenderer.apply {
//            setRenderKitSwitch(false)
            bindGLTextureView(binding.glTextureView)
            BindRendererListenerUseCase(ptaRenderer, listener = object : BindRendererListenerUseCase.Listener {
                override fun surfaceState(isAlive: Boolean) {
                    if (isAlive) requestRender()
                    if (isAlive) {
                        Dispatchers.GL.init(binding.glTextureView)
                    } else {
                        Dispatchers.GL.release()
                    }
                }

                override fun onRenderDataPrepared(inputData: FURenderInputData) {
                    viewModel.onRenderPrepare(inputData)
                    if (isNeedTakePic) {
                        val texture = inputData.texture
                        if (texture != null) {
                            val texMatrix = ptaRenderer.getRendererTexMatrix(inputData.renderConfig.cameraFacing)
                            val recordData = FUPhotoRecordHelper.RecordData(texture.texId, texMatrix, FUGLUtils.IDENTITY_MATRIX, texture.height, texture.width)
                            recordData.isOES = true
                            photoRecordHelper.sendRecordingData(recordData)
                        } else {
                            onSaveSnapShotError("outputData texture is null")
                        }
                        isNeedTakePic = false
                    }
                }
            })
        }
    }

    private fun inject() {
        fuBuildAvatarViewModel.photoPathLiveData.observe(viewLifecycleOwner) {
            val bitmap = FUMediaUtils.loadBitmap(it, FUDensityUtils.getScreenWidth(), FUDensityUtils.getScreenHeight())
            binding.takePhotoPic.setImageBitmap(bitmap)
            binding.selectGenderLayout.visible() //显示后续步骤 UI
        }

        viewModel.tipMsgLiveData.observe(viewLifecycleOwner) {
            binding.takePhotoPoint.text = it
        }
    }

    private val photoRecordHelper by lazy {
        val helper = FUPhotoRecordHelper()
        helper.bindListener(object : FUPhotoRecordHelper.OnPhotoRecordingListener {
            override fun onRecordSuccess(bitmap: Bitmap?, tag: String) {
                if (bitmap != null) {
                    onSaveSnapShotSuccess(bitmap)
                } else {
                    onSaveSnapShotError("record bitmap is null")
                }
            }
        })
        helper
    }

    @Volatile
    private var isNeedTakePic = false


    private fun takeSnapSotToPTA() {
        isNeedTakePic = true
    }

    private fun onSaveSnapShotSuccess(bitmap: Bitmap) {
        val cacheImageFile = File(requireContext().externalCacheDir, "cacheImage${System.currentTimeMillis()}.jpg")
        FUMediaUtils.addBitmapToExternal(cacheImageFile.path, bitmap, true)
        fuBuildAvatarViewModel.cachePhoto(cacheImageFile) //将用于形象生成的图片文件传递给 FUBuildAvatarViewModel，之后根据 photoPathLiveData 执行后续逻辑
    }

    private fun onSaveSnapShotError(errorMsg: String) {
        ToastUtils.showFailureToast(requireContext(), errorMsg)
    }

    private fun openSelectPhotoPage() {
        val intent2 = Intent()
        intent2.addCategory(Intent.CATEGORY_OPENABLE)
        intent2.type = "image/*"
        if (Build.VERSION.SDK_INT < 19) {
            intent2.action = Intent.ACTION_GET_CONTENT
        } else {
            intent2.action = Intent.ACTION_OPEN_DOCUMENT
        }
        startActivityForResult(
            intent2,
            IMAGE_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val filePath = FUFileUtils.getAbsolutePathByUri(data.data!!)
            if (filePath == null) {
                ToastUtils.showFailureToast(requireContext(), "所选图片文件不存在。")
                return
            }
            val file = File(filePath)
            if (file.exists()) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeFile(filePath, this)
                }
                val cacheFile: File? = if (options.outWidth >= 2160 || options.outHeight >= 2160) { //如果选中的图片大于 2160，则压缩图片
                    val bitmap = FUMediaUtils.loadBitmap(filePath, 2160, 2160)
                    if (bitmap != null) {
                        val cacheImageFile = File(requireContext().externalCacheDir, "cacheImage${System.currentTimeMillis()}.jpg")
                        FUMediaUtils.addBitmapToExternal(cacheImageFile.path, bitmap, true)
                        cacheImageFile
                    } else {
                        null
                    }
                } else {
                    null
                }
                fuBuildAvatarViewModel.cachePhoto(cacheFile ?: file) //将用于形象生成的图片文件传递给 FUBuildAvatarViewModel，之后根据 photoPathLiveData 执行后续逻辑

            } else {
                ToastUtils.showFailureToast(requireContext(), "所选图片文件不存在。")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fuBuildAvatarViewModel.releasePTAResource() //释放资源
        ptaRenderer.release()
        DevAIRepository.aiReleaseFaceTrack()
    }
}