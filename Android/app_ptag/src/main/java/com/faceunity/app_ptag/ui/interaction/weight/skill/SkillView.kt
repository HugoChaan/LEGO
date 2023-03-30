package com.faceunity.app_ptag.ui.interaction.weight.skill

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.ui.interaction.network.entity.InteractionSkillResult
import com.faceunity.app_ptag.ui.interaction.weight.FlowLayout
import com.faceunity.app_ptag.util.expand.marginParams
import com.faceunity.app_ptag.util.expand.px

/**
 *
 */
class SkillView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {
    private val linearLayout: LinearLayout = LinearLayout(context)

    init {
        linearLayout.apply {
            orientation = LinearLayout.VERTICAL
        }
        addView(linearLayout)
    }

    fun fillData(skillList: List<InteractionSkillResult.Data.Skill>, onItemClick: (String) -> Unit) {
        skillList.forEach { skill ->
            val titleView = TextView(context).apply {
                text = skill.scene
                setTextColor(Color.parseColor("#27272B"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            }
            linearLayout.addView(titleView)
            titleView.apply {
                marginParams.leftMargin = 16.px
            }

            val flowLayout = FlowLayout(context).apply {

            }
            skill.list.forEach { skillItem ->
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_layout_flow_skill, flowLayout, false) as TextView
                view.text = skillItem.name
                view.setOnClickListener {
                    onItemClick(skillItem.name)
                }
                flowLayout.addView(view)
            }
            linearLayout.addView(flowLayout)
            flowLayout.apply {
                marginParams.bottomMargin = 16.px
            }
        }
    }

}