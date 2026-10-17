package hu.unideb.studyversity.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import hu.unideb.studyversity.R
import hu.unideb.studyversity.utils.image.ImageShowActivity

object UtilActivityOpener {
    @JvmStatic
    fun zoomImage(context: Context, itemUrl: String) {
        context.startActivity(Intent(context, ImageShowActivity::class.java).putExtra("imageResource", itemUrl))
        (context as? Activity)?.overridePendingTransition(0, R.anim.fade_out)
    }
}
