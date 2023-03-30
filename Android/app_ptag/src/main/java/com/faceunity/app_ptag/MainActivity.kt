package com.faceunity.app_ptag

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.faceunity.app_ptag.util.ToastUtils
import com.faceunity.editor_ptag.util.getColorCompat
import com.faceunity.editor_ptag.util.tintBarColor

class MainActivity : AppCompatActivity() {
    private val needPermission = mutableSetOf(
        Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO
    )

    var avatarId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        avatarId = getLoadAvatarId()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                checkPermissions(needPermission) -> {
                    intoEditPage()
                }
                else -> {
                    requestPermissions(needPermission.toTypedArray(), 10086)
                }
            }
        }
        tintBarColor(getColorCompat(R.color.scene_bg))
    }

    private fun checkPermissions(permissions: Collection<String>) = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLoadAvatarId(): String? {
        val data = intent.data ?: return null
        val query = data.queryParameterNames.firstOrNull { it.lowercase() == "avatarid" }
        val avatarId = data.getQueryParameter(query)
        return avatarId
    }

    fun intoEditPage() {
        if (avatarId == null) {
            FuEditActivity.startActivity(this)
        } else {
            FuEditActivity.startActivity(this, avatarId)
        }
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            10086 -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults.all { it == PackageManager.PERMISSION_GRANTED } )) {
                    intoEditPage()
                } else {
                    ToastUtils.showFailureToast(baseContext, "请进入设置页赋予需要的权限。")
                }
                return
            }

        }
    }
}