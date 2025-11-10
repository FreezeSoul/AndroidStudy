package org.ninetripods.mq.study.nestedScroll.util.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import org.ninetripods.mq.study.R
import androidx.core.view.isNotEmpty

class SimpleViewPagerIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private lateinit var mTitles: Array<String?>
    private var mTabCount = 0
    private var mTranslationX = 0f
    private val mPaint = Paint()
    private var mTabWidth = 0

    init {
        mPaint.setColor(context.resources.getColor(R.color.orange_salmon))
        mPaint.strokeWidth = 9.0f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mTabWidth = w / mTabCount
    }

    fun setTitles(titles: Array<String?>) {
        mTitles = titles
        mTabCount = titles.size
        generateTitleView()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        canvas.save()
        canvas.translate(mTranslationX, (height - 2).toFloat())
        canvas.drawLine(0f, 0f, mTabWidth.toFloat(), 0f, mPaint)
        canvas.restore()
    }

    fun scroll(position: Int, offset: Float) {
        /**
         * <pre>
         * 0-1:position=0 ;1-0:postion=0;
        </pre> *
         */
        mTranslationX = width / mTabCount * (position + offset)
        invalidate()
    }

    private fun generateTitleView() {
        if (isNotEmpty()) this.removeAllViews()
        val count = mTitles.size

        weightSum = count.toFloat()
        for (i in 0 until count) {
            val lp = LayoutParams(0, LayoutParams.MATCH_PARENT).apply { weight = 1f }
            val tv = TextView(context).apply {
                setGravity(Gravity.CENTER)
                setTextColor(COLOR_TEXT_NORMAL)
                text = mTitles[i]
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setLayoutParams(lp)
                setOnClickListener { }
            }
            addView(tv)
        }
    }

    companion object {
        private const val COLOR_TEXT_NORMAL = -0x1000000
    }
}
