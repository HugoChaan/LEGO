package com.faceunity.app_ptag.use_case.android

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import com.faceunity.app_ptag.FuDI
import java.text.SimpleDateFormat
import java.util.*

/**
 * 保存 Bitmap 到相册
 */
class SaveBitmapUseCase {

    companion object {
        operator fun invoke(bitmap: Bitmap, namePrefix: String = "") : Boolean = SaveBitmapUseCase().execute(bitmap, namePrefix)
    }

    operator fun invoke(bitmap: Bitmap, namePrefix: String = "") : Boolean = execute(bitmap, namePrefix)

    fun execute(bitmap: Bitmap, namePrefix: String = "") : Boolean {
        val fileName = "ptag-$namePrefix-${
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(
                GregorianCalendar().time
            )
        }.png"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DESCRIPTION, "This is a Avatar")
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.TITLE, fileName)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PTAG/")
            }
        }
        val uri = FuDI.getContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        if (uri == null) {
            return false
        }
        val openOutputStream = FuDI.getContext().contentResolver.openOutputStream(uri)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, openOutputStream)
        openOutputStream?.close()
        return true
    }
}