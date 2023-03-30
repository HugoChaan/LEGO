package com.faceunity.app_ptag.ui.interaction.weight.tone

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemInteractionToneBinding
import com.faceunity.app_ptag.ui.interaction.network.entity.InteractionVoiceResult
import com.faceunity.app_ptag.util.FastAdapter

/**
 *
 */
class ToneView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val voiceList = mutableListOf<InteractionVoiceResult.Data.Voice>()
    var eventListener: EventListener? = null
    var selectItem: InteractionVoiceResult.Data.Voice? = null

    var clickPosition = -1
    var lastClickPosition = -1

    var currentAuditionId: String? = null

    init {
        layoutManager = GridLayoutManager(context, 2)
        adapter = FastAdapter<InteractionVoiceResult.Data.Voice>(
            voiceList,
            R.layout.item_interaction_tone
        ) { holder, bean, position ->
            val itemBinding = ItemInteractionToneBinding.bind(holder.itemView)
            itemBinding.root.setOnClickListener {
                eventListener?.onItemClick(bean)
                lastClickPosition = clickPosition
                clickPosition = position
                adapter?.notifyItemChanged(clickPosition)
                adapter?.notifyItemChanged(lastClickPosition)
            }
            itemBinding.auditionBtn.setOnClickListener {
                eventListener?.onAudition(bean)
            }
            itemBinding.name.text = bean.name
            if (bean == selectItem) {
                itemBinding.root.isSelected = true
            } else {
                itemBinding.root.isSelected = false
            }
            if (currentAuditionId == bean.id) {
                Glide.with(context)
                    .asGif()
                    .load(R.drawable.audition_play)
                    .into(itemBinding.auditionBtn)
            } else {
                Glide.with(context)
                    .load(R.drawable.icon_drawer_audition)
                    .into(itemBinding.auditionBtn)
            }
        }
    }

    fun fillData(
        voiceList: List<InteractionVoiceResult.Data.Voice>
    ) {
        this.voiceList.apply {
            clear()
            addAll(voiceList)
        }
        adapter?.notifyDataSetChanged()
    }

    fun setDefaultItem(voice: String) {
        val position = voiceList.indexOfFirst { it.id == voice }
        selectItem = voiceList.getOrNull(position) ?: return
        lastClickPosition = clickPosition
        clickPosition = position
        adapter?.notifyItemChanged(clickPosition)
        adapter?.notifyItemChanged(lastClickPosition)
    }

    fun startAudition(id: String) {
        currentAuditionId = id
        adapter?.notifyDataSetChanged()
    }

    fun endAudition() {
        currentAuditionId = null
        adapter?.notifyDataSetChanged()
    }

    interface EventListener {
        fun onItemClick(voice: InteractionVoiceResult.Data.Voice)

        fun onAudition(voice: InteractionVoiceResult.Data.Voice)
    }
}