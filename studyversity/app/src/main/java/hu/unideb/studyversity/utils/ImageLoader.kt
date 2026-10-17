package hu.unideb.studyversity.utils

import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import hu.unideb.studyversity.R

/**
 * Loads profile / institution pictures. Besides normal http(s) and file urls the local backend uses
 * `res://avatar_3` for the bundled default avatars.
 */
object ImageLoader {
    const val RES_SCHEME = "res://"

    fun load(view: ImageView, url: String?, @DrawableRes placeholder: Int = R.drawable.ic_avatar_silhuette) {
        if (url.isNullOrBlank()) { view.setImageResource(placeholder); return }
        if (url.startsWith(RES_SCHEME)) {
            val id = view.context.resources.getIdentifier(url.removePrefix(RES_SCHEME), "drawable", view.context.packageName)
            view.setImageResource(if (id != 0) id else placeholder)
            return
        }
        Glide.with(view).load(url).placeholder(placeholder).error(placeholder).centerCrop().into(view)
    }

    /** The bundled default avatars (`res://avatar_1` … `res://avatar_8`). */
    fun defaultAvatars(context: Context): List<String> = (1..8).map { "$RES_SCHEME" + "avatar_$it" }
}
