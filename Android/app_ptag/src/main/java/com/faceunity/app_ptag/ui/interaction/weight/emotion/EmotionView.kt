package com.faceunity.app_ptag.ui.interaction.weight.emotion

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemEmotionBinding
import com.faceunity.app_ptag.databinding.ViewInteractionAnimationBinding
import com.faceunity.app_ptag.ui.edit.weight.control.adapter.StyleRecordInterface
import com.faceunity.app_ptag.ui.interaction.entity.EmotionConfig
import com.faceunity.app_ptag.util.FastAdapter
import com.faceunity.editor_ptag.util.gone
import com.faceunity.editor_ptag.util.visible
import com.faceunity.editor_ptag.util.visibleOrInvisible

/**
 *
 */
class EmotionView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding: ViewInteractionAnimationBinding
    private val list = mutableListOf<EmotionConfig.Item>()
    private var clickListener: (item: EmotionConfig.Item) -> Unit = {}

    private var cacheUiState: EmotionUiState = EmotionUiState.default

    init {
        binding = ViewInteractionAnimationBinding.inflate(LayoutInflater.from(context), this, true)
        initView()
    }

    private fun initView() {
        binding.animationRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 5)
            adapter = FastAdapter(list, R.layout.item_emotion) { holder, bean, position ->
                val itemBinding = ItemEmotionBinding.bind(holder.itemView)
                itemBinding.name.text = bean.name
                itemBinding.selectView.visibleOrInvisible(cacheUiState.selectItem == bean)
                when(cacheUiState.downloadStateMap[bean.path]) {
                    StyleRecordInterface.DownloadStyle.Normal -> {
                        itemBinding.downloadImageView.gone()
                    }
                    StyleRecordInterface.DownloadStyle.NeedDownload -> {
                        itemBinding.downloadImageView.apply {
                            visible()
                            setImageResource(R.drawable.icon_tab2_download)
                            animation = null
                        }

                    }
                    StyleRecordInterface.DownloadStyle.Downloading -> {
                        itemBinding.downloadImageView.apply {
                            visible()
                            setImageResource(R.drawable.icon_tab2_loading)
                            val anim = RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,0.5f,
                                Animation.RELATIVE_TO_SELF,0.5f).apply {
                                repeatCount = -1
                                duration = 1000
                                interpolator = LinearInterpolator()
                            }
                            animation = anim
                        }
                    }
                    StyleRecordInterface.DownloadStyle.Error -> {
                        itemBinding.downloadImageView.apply {
                            visible()
                            setImageResource(R.drawable.icon_tab2_download)
                            animation = null
                        }
                    }
                    else -> {
                        itemBinding.downloadImageView.gone()
                    }
                }
                itemBinding.root.setOnClickListener {
                    clickListener(bean)
                }
            }
        }
    }

    fun collectUiState(expressionUiState: EmotionUiState) {
        //TODO 此处的标准做法是进行数据比较，计算出要更新哪几个 Item，然后 notifyItemChanged。目前暂时全量刷新。
        cacheUiState = expressionUiState
        cacheUiState.list.let {
            list.clear()
            list.addAll(it)
            binding.animationRecyclerView.adapter?.notifyDataSetChanged()
        }
    }


    fun setClickListener(block : (item: EmotionConfig.Item) -> Unit) {
        clickListener = block
    }
}