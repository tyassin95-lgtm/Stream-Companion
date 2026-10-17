package hu.unideb.studyversity.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.cardview.widget.CardView

/** A card that is always as high as it is wide (used by the home menu grid). */
class SquareCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        val w = View.MeasureSpec.getSize(widthMeasureSpec)
        val h = View.MeasureSpec.getSize(heightMeasureSpec)
        val side = if (h in 1 until w) h else w
        setMeasuredDimension(side, side)
    }
}
