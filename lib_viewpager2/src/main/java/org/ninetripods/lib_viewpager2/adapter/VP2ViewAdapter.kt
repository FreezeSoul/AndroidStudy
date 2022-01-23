package org.ninetripods.lib_viewpager2.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.ninetripods.lib_viewpager2.imageLoader.ILoader
import org.ninetripods.lib_viewpager2.log

const val EXTRA_NUM = 4 //额外增加4条数据
const val SIDE_NUM = 2 //左右两侧各增加2条

class MVP2Adapter : RecyclerView.Adapter<MVP2Adapter.PageViewHolder>() {

    companion object {
        //支持异步比较数据 因为这里是比较的String 暂时没有使用
        private val ITEM_COMPARATOR = object : DiffUtil.ItemCallback<String>() {

            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem::class.java == newItem::class.java
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

        }
    }

    /**
     * use[DiffUtil] 增量更新数据
     * @param newList 新数据
     */
    fun submitList(newList: MutableList<String>) {
        val diffUtil = PageDiffUtil(mModels, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        diffResult.dispatchUpdatesTo(this)
        //NOTE:注意这里要重新设置Adapter中的数据
        setModels(newList)
    }

    private var mModels = mutableListOf<String>()
    private var mLoader: ILoader<View>? = null
    private var mItemClickListener: OnBannerClickListener? = null

    fun setModels(models: List<String>) {
        mModels.clear()
        mModels.addAll(models)
    }

    fun setImageLoader(loader: ILoader<View>?) {
        this.mLoader = loader
    }

    fun setOnItemClickListener(listener: OnBannerClickListener?) {
        this.mItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        log("onCreateViewHolder()")
        var itemShowView = mLoader?.createView(parent.context)
        if (itemShowView == null) {
            itemShowView = ImageView(parent.context)
        }
        return PageViewHolder(itemShowView).apply {
            /**
             * 必须都是MATCH_PARENT，否则报java.lang.IllegalStateException: Pages must fill the whole ViewPager2 (use match_parent)
             */
            itemShowView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            itemShowView.setOnClickListener {
                val realPosition = getRealPosition(bindingAdapterPosition)
                mItemClickListener?.onItemClick(realPosition)
            }
        }
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        log("onBindViewHolder(): pos is $position, model is ${mModels[position]}")
        val contentStr = mModels[position]
        mLoader?.display(holder.itemShowView.context, contentStr, holder.itemShowView)
    }

    override fun onBindViewHolder(
        holder: PageViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            //局部更新 partial bind
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemCount(): Int = mModels.size

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemShowView: View = itemView
    }

    /**
     * 获取数据对应的真实位置
     * @param exPosition 扩展数据中的位置
     */
    private fun getRealPosition(exPosition: Int): Int {
        if (itemCount == 1) return 0  //只有一条数据的时候直接返回0
        val realCount = itemCount - EXTRA_NUM
        var realPos = (exPosition - SIDE_NUM) % realCount
        if (realPos < 0) realPos += realCount
        return realPos
    }

}