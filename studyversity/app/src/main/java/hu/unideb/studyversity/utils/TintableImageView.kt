package hu.unideb.studyversity.utils

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import hu.unideb.studyversity.R

/** ImageView whose colour filter follows a state list (pressed / selected ...). */
class TintableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var tint: ColorStateList? = null

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.TintableImageView, defStyleAttr, 0)
        tint = a.getColorStateList(R.styleable.TintableImageView_tintColorStateList)
        a.recycle()
        updateTintColor()
    }

    private fun updateTintColor() {
        tint?.let { setColorFilter(it.getColorForState(drawableState, 0)) }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (tint?.isStateful == true) updateTintColor()
    }
}
