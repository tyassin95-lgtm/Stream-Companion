package hu.unideb.studyversity.utils

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

object Animations {
    /** Animates the height of [view] from [from] to [to] pixels (used by the accordion menu). */
    fun slideView(view: View, from: Int, to: Int, durationMs: Long = 300) {
        ValueAnimator.ofInt(from, to).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                view.layoutParams = view.layoutParams.also { lp -> lp.height = it.animatedValue as Int }
            }
            start()
        }
    }
}
