package com.faceunity.app_ptag.ui.scan_code

import android.app.Activity
import android.content.Intent
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.faceunity.app_ptag.databinding.FuDemoScanCodeFragmentBinding
import com.faceunity.app_ptag.util.ToastUtils
import com.faceunity.app_ptag.weight.DownloadingDialog
import com.faceunity.app_ptag.weight.zixing.core.QRCodeView
import com.faceunity.app_ptag.weight.zixing.core.ScanResult
import com.faceunity.editor_ptag.store.DevAvatarManagerRepository
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.toolbox.file.FUFileUtils
import kotlinx.coroutines.launch
import java.io.File

class FuScanCodeFragment : Fragment(), QRCodeView.Delegate {
    private var _binding: FuDemoScanCodeFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<FuScanCodeViewModel>()

    private val downloadingDialog: DownloadingDialog by lazy {
        DownloadingDialog(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FuDemoScanCodeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView() {
        binding.zxingview.setDelegate(this)

        binding.flBack.setOnClickListener {
            Navigation.findNavController(it).popBackStack() //关闭页面
        }
        binding.flSelectPhoto.setOnClickListener {
            openSelectPhotoPage()
        }
        // 本来就用到 QRCodeView 时可直接调 QRCodeView 的方法，走通用的回调
        //        mZXingView.startCamera(Camera.CameraInfo.CAMERA_FACING_FRONT); // 打开前置摄像头开始预览，但是并未开始识别
        binding.zxingview.getScanBoxView().setOnlyDecodeScanBoxArea(false) // 仅识别扫描框中的码

//        zXingView.changeToScanBarcodeStyle(); // 切换成扫描条码样式
//        zXingView.setType(BarcodeType.CUSTOM, ALL_HINT_MAP); // 自定义识别类型
        //        zXingView.changeToScanBarcodeStyle(); // 切换成扫描条码样式
//        zXingView.setType(BarcodeType.CUSTOM, ALL_HINT_MAP); // 自定义识别类型
    }

    override fun onCameraPreViewFrame(data: ByteArray?, camera: Camera?) {
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.zxingview.onDestroy() // 销毁二维码扫描控件

    }

    override fun onResume() {
        super.onResume()
        binding.zxingview.startCamera() // 打开后置摄像头开始预览，但是并未开始识别
        binding.zxingview.startSpotAndShowRect() // 显示扫描框，并开始识别
    }

    override fun onStop() {
        super.onStop()
        binding.zxingview.stopCamera() // 关闭摄像头预览，并且隐藏扫描框

    }

    /**
     * 接收扫码结果
     */
    override fun onScanQRCodeSuccess(result: ScanResult?) {
        FuLog.info("onScanQRCodeSuccess: " + result)
        if (result != null && !TextUtils.isEmpty(result.getResult())) {
            val isParse = parseContent(result.result)
            if (isParse) {
                binding.zxingview.stopSpot() //暂停识别
            } else {
                binding.zxingview.startSpot() // 开始识别
            }
        } else {
            ToastUtils.showFailureToast(requireContext(), "识别不到二维码，请重试。")
            binding.zxingview.startSpot() // 开始识别
        }
    }

    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {
    }

    override fun onScanQRCodeOpenCameraError() {
    }

    /**
     * 处理该链接。
     */
    private fun parseContent(content: String): Boolean {
        try {
            val uri = Uri.parse(content)
            val avatarId = uri.getQueryParameter("avatarid") ?: return false
//            ToastUtils.showSuccessToast(requireContext(), "识别形象二维码成功，准备加载")
            downloadAvatarInfo(avatarId)
            return true
        } catch (ex: Throwable) {
            ToastUtils.showFailureToast(requireContext(), "解析失败：$ex")
            FuLog.warn(ex.toString())
        }
        return false
    }

    private fun downloadAvatarInfo(avatarId: String) {
        lifecycleScope.launch {
            downloadingDialog.show()
            val isExist = viewModel.avatarIdIsExist(avatarId)
            if (isExist) {
                ToastUtils.showSuccessToast(requireContext(), "形象已存在")
                DevAvatarManagerRepository.switchAvatar(avatarId)
                downloadingDialog.dismiss()
                findNavController().popBackStack()
            } else {
                ToastUtils.showSuccessToast(requireContext(), "识别形象二维码成功，正在加载")
                viewModel.downloadAvatarConfig(avatarId).onSuccess {
                    findNavController().popBackStack()
                }.onFailure {
                    ToastUtils.showFailureToast(requireContext(), "获取形象信息失败：${it.message}，请重试。")
                    binding.zxingview.startSpot() // 开始识别
                }
                downloadingDialog.dismiss()
            }
        }
    }

    //region 选择图片
    private val IMAGE_REQUEST_CODE = 0x102
    private fun openSelectPhotoPage() {
        val intent2 = Intent()
        intent2.addCategory(Intent.CATEGORY_OPENABLE)
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        intent2.type = "image/*"
        intent2.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
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
            binding.zxingview.startSpotAndShowRect() // 显示扫描框，并开始识别

            val filePath = FUFileUtils.getAbsolutePathByUri(data.data!!)
            if (filePath == null) {
                ToastUtils.showFailureToast(requireContext(), "所选图片文件不存在。")
                return
            }
            val file = File(filePath)
            if (file.exists()) {
                binding.zxingview.decodeQRCode(filePath)
            } else {
                ToastUtils.showFailureToast(requireContext(), "所选图片文件不存在。")
            }
        }
    }
}