package com.faceunity.app_ptag.ui.interaction.weight.background

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.faceunity.app_ptag.R
import com.faceunity.app_ptag.databinding.ItemInteractionBackgroundBinding
import com.faceunity.app_ptag.databinding.ViewBackgroundBinding
import com.faceunity.app_ptag.ui.interaction.weight.background.entity.BackgroundSource
import com.faceunity.app_ptag.util.FastAdapter
import com.faceunity.editor_ptag.util.invisible
import com.faceunity.editor_ptag.util.visible
import com.faceunity.editor_ptag.util.visibleOrInvisible
import java.util.*

/**
 *
 */
class BackgroundView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding: ViewBackgroundBinding
    private val imagePathList = mutableListOf<BackgroundSource>()
    var eventListener: EventListener? = null
    var selectItem: BackgroundSource? = null

//    var clickPosition = -1
//    var lastClickPosition = -1

    init {
        binding = ViewBackgroundBinding.inflate(LayoutInflater.from(context), this, true)
        initView()
    }

    fun initView() {
        binding.backgroundRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = FastAdapter(imagePathList, R.layout.item_interaction_background) { holder, bean, position ->
                val itemBinding = ItemInteractionBackgroundBinding.bind(holder.itemView)

                when (bean) {
                    is BackgroundSource.Add -> {
                        itemBinding.previewImageView.setImageResource(bean.drawableId)
                        itemBinding.root.setOnClickListener {
                            eventListener?.onAddItemClick()
                        }
                    }
                    is BackgroundSource.User -> {
                        Glide.with(this)
                            .load(bean.path)
                            .into(itemBinding.previewImageView)
                        itemBinding.root.setOnClickListener {
                            eventListener?.onItemClick(bean)
//                            lastClickPosition = clickPosition
//                            clickPosition = position
//                            adapter?.notifyItemChanged(clickPosition)
//                            adapter?.notifyItemChanged(lastClickPosition)
                            binding.backgroundRecyclerView.adapter?.notifyDataSetChanged()
                        }
                    }
                    is BackgroundSource.Default -> {
                        itemBinding.previewImageView.setImageResource(bean.drawableId)
                        itemBinding.root.setOnClickListener {
                            eventListener?.onItemClick(bean)
//                            lastClickPosition = clickPosition
//                            clickPosition = position
//                            adapter?.notifyItemChanged(clickPosition)
//                            adapter?.notifyItemChanged(lastClickPosition)
                            binding.backgroundRecyclerView.adapter?.notifyDataSetChanged()
                        }
                    }
                }
                itemBinding.selectImageView.visibleOrInvisible(selectItem === bean)
            }
        }
    }

    fun fillData(data: List<String>) {
        imagePathList.clear()
        imagePathList.add(BackgroundSource.Add(R.drawable.icon_drawer_add))
        val default = BackgroundSource.Default(R.drawable.interaction_bg1)
        imagePathList.add(default)
        imagePathList.addAll(data.map { BackgroundSource.User(it) })
        binding.backgroundRecyclerView.adapter?.notifyDataSetChanged()
    }

    fun addUserImport(path: String) {
        imagePathList.add(BackgroundSource.User(path))
        binding.backgroundRecyclerView.adapter?.notifyItemRangeInserted(imagePathList.size - 1, 1)
    }

    fun selectDefaultItem() {
        imagePathList.indexOfFirst { it is BackgroundSource.Default }.let {
            selectItem = imagePathList[it]
//            lastClickPosition = clickPosition
//            clickPosition = it
//            binding.backgroundRecyclerView.adapter?.notifyItemChanged(clickPosition)
//            binding.backgroundRecyclerView.adapter?.notifyItemChanged(lastClickPosition)
            binding.backgroundRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    fun selectUserItem(path: String) {
        imagePathList.indexOfLast { it is BackgroundSource.User && it.path == path }.let {
            selectItem = imagePathList[it]
            eventListener?.onItemClick(imagePathList[it])
//            lastClickPosition = clickPosition
//            clickPosition = it
            binding.backgroundRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    fun selectIndex(position: Int) {
        selectItem = imagePathList.getOrNull(position) ?: return
        eventListener?.onItemClick(imagePathList[position])
        binding.backgroundRecyclerView.adapter?.notifyDataSetChanged()
    }

    fun getListSize() = imagePathList.size

    //region 拖拽功能


    private val stickerTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        private var isTouchUp = false

        private var dragListener: DragListener = object : DragListener {
            override fun deleteState(delete: Boolean) {
                (binding.backgroundDeleteLayout).let {
                    if (delete) {
                        it.setBackgroundColor(Color.parseColor("#CF6034"))
                    } else {
                        it.setBackgroundColor(Color.parseColor("#F68149"))
                    }

                }
            }

            override fun dragState(start: Boolean) {
                if (start) {
                    binding.backgroundDeleteLayout.visible()
//                    VibratorUtil.vibrate(FUARVideoApp.sContext)
                } else {
                    binding.backgroundDeleteLayout.invisible()
//                    saveCustomList()
                }
            }

            override fun remove(position: Int) {
                val source = imagePathList[position]
                imagePathList.removeAt(position)
                //TODO 此处硬编码，减去了添加和默认的两个下标
                eventListener?.onItemRemove(position, position - 2, source)
                //使用 notifyItemRemoved 时会造成数据样式错乱，因为不会重新 bind holder。按理论来说加后面的 notifyItemRangeRemoved 就可以了，但是依然有问题，不管了=。=
//                binding.backgroundRecyclerView.adapter?.notifyItemRemoved(position)
//                binding.backgroundRecyclerView.adapter?.notifyItemRangeRemoved(position, binding.backgroundRecyclerView.adapter?.itemCount - position)
                binding.backgroundRecyclerView.adapter?.notifyDataSetChanged()
            }
        }


        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            var dragFlag = 0
            if (recyclerView.layoutManager is GridLayoutManager) {
                dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            } else if (recyclerView.layoutManager is LinearLayoutManager) {
                dragFlag = ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            }
            val swipeFlag = 0
            return makeMovementFlags(dragFlag, swipeFlag)
        }

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return true
            //得到当拖拽的viewHolder的Position
            val fromPosition = viewHolder.adapterPosition
            //拿到当前拖拽到的item的viewHolder
            val toPosition = target.adapterPosition
            if (toPosition == 0 || toPosition == imagePathList.size) {
                return false
            }
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(imagePathList, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(imagePathList, i, i - 1)
                }
            }
            binding.backgroundRecyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
            return true
        }

        override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
//            FULog.d("dx:$dX, dy:$dY, 列表本身高度：${recyclerView.height}, 底部：${viewHolder.itemView.bottom}")
//            FULog.d("当前操作：${viewHolder.adapterPosition}, 是否抬起手指：$isTouchUp")
            val removeBtnXY = IntArray(2)
            binding.backgroundDeleteLayout.getLocationOnScreen(removeBtnXY)
            val itemXY = IntArray(2)
            viewHolder.itemView.getLocationOnScreen(itemXY)
            if (Math.abs(itemXY[1] - removeBtnXY[1]) <= 100) {
                dragListener.deleteState(true)
                if (isTouchUp) {
//                    viewHolder.itemView.visibility = View.INVISIBLE
                    if (viewHolder.adapterPosition != -1) {
                        dragListener.remove(viewHolder.adapterPosition)
                        reset()
                        return
                    }
                }
            } else {
                dragListener.deleteState(false)
            }
            //如果不加这行，那么 onChildDraw 会一直绘制直到经过删除触发区。此处代码可以确保删除逻辑只在刚触发抬手时间时执行。
            isTouchUp = false

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            imagePathList.removeAt(direction)
            binding.backgroundRecyclerView.adapter?.notifyItemRemoved(direction)
        }

        override fun isLongPressDragEnabled(): Boolean {
            return true
        }

        override fun getAnimationDuration(recyclerView: RecyclerView, animationType: Int, animateDx: Float, animateDy: Float): Long {
            isTouchUp = true
            return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                dragListener.dragState(true)
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            reset()
        }

        private fun reset() {
            dragListener.dragState(false)
            isTouchUp = false
        }

    })

    init {
        stickerTouchHelper.attachToRecyclerView(binding.backgroundRecyclerView)
    }

    /**
     * 拖拽删除监听
     */
    interface DragListener {
        /**
         * 用户是否将 item拖动到删除处，根据状态改变颜色
         */
        fun deleteState(delete: Boolean)

        /**
         * 是否于拖拽状态
         */
        fun dragState(start: Boolean)

        /**
         * 当用户与item的交互结束并且item也完成了动画时调用
         */
        fun remove(position: Int)
    }

    //endregion

    interface EventListener {
        fun onItemClick(backgroundSource: BackgroundSource)
        fun onAddItemClick()
        fun onItemRemove(position: Int, userPosition: Int, backgroundSource: BackgroundSource)
    }
}