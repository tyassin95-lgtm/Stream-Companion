package hu.unideb.studyversity.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import hu.unideb.studyversity.R

class MaxHeightRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var maxHeightPx = 0

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightScrollView)
        maxHeightPx = a.getLayoutDimension(R.styleable.MaxHeightScrollView_maxHeight, maxHeightPx)
        a.recycle()
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val spec = if (maxHeightPx > 0) View.MeasureSpec.makeMeasureSpec(maxHeightPx, View.MeasureSpec.AT_MOST) else heightSpec
        super.onMeasure(widthSpec, spec)
    }
}
