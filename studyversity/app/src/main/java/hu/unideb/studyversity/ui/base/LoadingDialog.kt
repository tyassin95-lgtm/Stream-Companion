package hu.unideb.studyversity.ui.base

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import hu.unideb.studyversity.R

/** The "please wait" overlay (layout simple_loading_layout) shown while data loads. */
class LoadingDialog(context: Context) : Dialog(context, R.style.Theme_Transparent) {
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.simple_loading_layout)
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}
