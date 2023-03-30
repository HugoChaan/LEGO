package com.faceunity.app_ptag.util

import android.app.Dialog
import java.lang.ref.WeakReference
import java.util.*

/**
 * 一个确保全局只有一个 Dialog 的辅助类
 */
object DialogDisplayHelper {
    val dialogSet = Collections.synchronizedSet(mutableSetOf<WeakReference<Dialog>>())

    fun show(dialog: Dialog) {
        synchronized(dialogSet) {
            val iterator = dialogSet.iterator()
            while (iterator.hasNext()) {
                val weakReference = iterator.next()
                val d = weakReference.get()
                if (d != null && d.isShowing && d != dialog) {
                    d.dismiss()
                    iterator.remove()
                }
            }
            dialogSet.add(WeakReference(dialog))
        }
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun dismiss(dialog: Dialog) {
        synchronized(dialogSet) {
            dialogSet.removeAll {
                it.get() == dialog
            }
        }
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun release() {
        dialogSet.clear()
    }
}