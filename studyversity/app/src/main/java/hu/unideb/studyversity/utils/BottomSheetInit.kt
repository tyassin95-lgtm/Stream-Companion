package hu.unideb.studyversity.utils

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import hu.unideb.studyversity.R

/** The tap-to-expand help strip at the bottom of search screens (`bottom_helper_view`). */
object BottomSheetInit {
    fun initBottomSheet(topHolder: LinearLayout, detailText: TextView, indicator: ImageView) {
        topHolder.setOnClickListener {
            if (detailText.visibility == View.GONE) {
                detailText.visibility = View.VISIBLE
                indicator.setImageResource(R.drawable.ic_arrow_down)
            } else {
                detailText.visibility = View.GONE
                indicator.setImageResource(R.drawable.ic_arrow_up)
            }
        }
    }
}
