package com.faceunity.app_ptag.util.expand

import android.content.Context
import android.content.res.Resources
import android.text.Spannable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

/**
 * dp to px
 */
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

/**
 * 根据[digits]返回指定小数点位数的字符串。会四舍五入
 */
fun Double.format(digits: Int) = "%.${digits}f".format(this)

val View.marginParams: ViewGroup.MarginLayoutParams
    get() = (this.layoutParams as ViewGroup.MarginLayoutParams)

@ColorInt
fun Context.themeColor(@AttrRes attrRes: Int): Int = TypedValue()
    .apply { theme.resolveAttribute (attrRes, this, true) }
    .data


fun Spannable.customStyle(spannable: Spannable, content: String, start: Int, styleText: String, styleBlock: () -> Any) {
    if (content.isBlank()) return
    val startPos: Int = start + content.substring(start).indexOf(styleText)
    if (startPos > -1) {
        val endPos: Int = startPos + styleText.length
        if (content.substring(endPos).contains(styleText)) {
            customStyle(this, content, endPos, styleText, styleBlock)
        }
        spannable.setSpan(styleBlock(), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

inline fun <R, T> Result<T>.merge(transform: (value: T) -> Result<R>): Result<R> {
    return try {
        fold({
            transform(it)
        }, {
            Result.failure(it)
        })
    } catch (ex: Throwable) {
        Result.failure(ex)
    }

}