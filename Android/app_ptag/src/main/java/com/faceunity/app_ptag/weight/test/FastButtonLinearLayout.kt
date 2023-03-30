package com.faceunity.app_ptag.weight.test

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout

/**
 *
 */
class FastButtonLinearLayout@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    fun putButton(vararg params: Pair<String, () -> Unit>) {
        putButton(params.toMap())
    }

    fun putButton(map: Map<String, () -> Unit>) {
        map.forEach { item ->
            val button = Button(context)
            button.text = item.key
            button.setOnClickListener {
                item.value.invoke()
            }
            addView(button)
        }
    }
}