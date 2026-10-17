package hu.unideb.studyversity.ui.base

import android.view.View

/** Implemented by activities that show the yellow-on-blue Studyversity toolbar. */
interface ToolbarHost {
    fun updateTitle(title: String?)
    fun updateSubTitle(subTitle: String?)
    fun showUpButton(show: Boolean)
    val snackbarAnchor: View?
}
