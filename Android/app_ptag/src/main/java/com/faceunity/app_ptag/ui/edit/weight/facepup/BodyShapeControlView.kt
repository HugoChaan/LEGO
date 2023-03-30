package com.faceunity.app_ptag.ui.edit.weight.facepup

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemControlGroupSeekBarBinding
import com.faceunity.app_ptag.databinding.ItemTierGroupBinding
import com.faceunity.app_ptag.databinding.ViewControlGroupBodyShapeBinding
import com.faceunity.app_ptag.ui.edit.entity.facepup.BodyShapeUiState
import com.faceunity.app_ptag.util.FastAdapter
import com.faceunity.editor_ptag.util.FuLog
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visibleOrInvisible
import com.faceunity.fupta.facepup.entity.bodyshape.BodyShapeItem

/**
 *
 */
class BodyShapeControlView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: ViewControlGroupBodyShapeBinding

    private var cacheUiState: BodyShapeUiState? = null

    init {
        binding = ViewControlGroupBodyShapeBinding.inflate(LayoutInflater.from(context), this, true)
        initAdapter()
        initView()
    }

    private var eventListener: EventListener = object : EventListener {

        override fun onItemClick(item: BodyShapeUiState.Group, index: Int) {
            seekBarAdapter.updateList(item.sliderList.toMutableList())
            status.clickItem(index)
            itemAdapter.notifyDataSetChanged()
        }

        override fun onSeekBarScroll(item: BodyShapeUiState.Slider, value: Float) {
            controlListener?.onSeekBarScroll(item.bodyShapeItem, value)
        }
    }
    private var controlListener: ControlListener? = null

    private lateinit var itemAdapter: FastAdapter<BodyShapeUiState.Group>
    private lateinit var seekBarAdapter: FastAdapter<BodyShapeUiState.Slider>

    private val status = Status()

    private fun initView() {

        binding.partRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = itemAdapter
        }
        binding.seekBarRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = seekBarAdapter
        }

        binding.resetBtn.setOnClickListener {
            controlListener?.onResetClick()
            itemAdapter.notifyDataSetChanged()
            seekBarAdapter.notifyDataSetChanged()
        }
        binding.finishBtn.setOnClickListener {
            controlListener?.onFinish()
        }


    }

    private fun initAdapter() {
        itemAdapter = FastAdapter(mutableListOf(), R.layout.item_tier_group) { holder, bean, position ->
            val binding = ItemTierGroupBinding.bind(holder.itemView)
            binding.name.text = bean.name
            binding.dot.visibleOrInvisible(bean.hasChanged)
            if (status.itemIndex == position) {
                binding.name.setTextColor(Color.parseColor("#27272B"))
            } else {
                binding.name.setTextColor(Color.parseColor("#A7AABF"))
            }
            binding.root.setOnClickListener {
                eventListener.onItemClick(bean, position)
            }
        }
        seekBarAdapter = FastAdapter(mutableListOf(), R.layout.item_control_group_seek_bar) { holder, bean, position ->
            val binding = ItemControlGroupSeekBarBinding.bind(holder.itemView)
            binding.name.setTextColor(Color.parseColor("#646778"))
            binding.value.setTextColor(Color.parseColor("#646778"))
            binding.minusBtn.gone()
            binding.plusBtn.gone()
            binding.name.text = bean.name
            binding.seekBar.apply {
                value = bean.value
                binding.value.text = formatValue(value)
                setCustomThumbDrawable(R.drawable.seekbar_thumb)
                clearOnChangeListeners()
                addOnChangeListener { _, value, isUser ->

                    if (isUser) {
                        updateSeekbarValue(binding, bean, value, position)
                    }
                }
            }
        }
    }

    private fun updateSeekbarValue(
        binding: ItemControlGroupSeekBarBinding,
        bean: BodyShapeUiState.Slider,
        value: Float,
        position: Int
    ) {
        eventListener.onSeekBarScroll(bean, value)
        binding.seekBar.value = value
        binding.value.text = binding.seekBar.formatValue(value)

        //及时刷新修改标识。不推荐这样做。
        itemAdapter.notifyDataSetChanged()
    }

    fun setControlListener(listener: ControlListener) {
        controlListener = listener
    }

    fun collect(bodyShapeUiState: BodyShapeUiState) {
        itemAdapter.updateList(bodyShapeUiState.groupList.toMutableList())
        when(bodyShapeUiState.eventType) {
            BodyShapeUiState.EventType.Init, BodyShapeUiState.EventType.Reset -> {
                val group = bodyShapeUiState.groupList.getOrNull(status.itemIndex)
                if (group != null) {
                    seekBarAdapter.updateList(group.sliderList.toMutableList())
                }
            }
            BodyShapeUiState.EventType.Slider -> {

            }
        }
        cacheUiState = bodyShapeUiState

    }

    fun start() {
        cacheUiState?.let { collect(it) }
        controlListener?.onStart()
    }

    data class Status(var itemIndex: Int = 0) {

        fun clickItem(index: Int) {
            itemIndex = index
        }
    }

    /**
     * 事件监听，对子 View 的事件响应进行处理
     */
    internal interface EventListener {

        fun onItemClick(item: BodyShapeUiState.Group, index: Int)

        fun onSeekBarScroll(item: BodyShapeUiState.Slider, value: Float)
    }

    /**
     * 封装给外界的接口
     */
    interface ControlListener {
        fun onSeekBarScroll(bodyShapeItem: BodyShapeItem, value: Float)

        fun onResetClick()

        fun onStart()

        fun onFinish()

    }
}