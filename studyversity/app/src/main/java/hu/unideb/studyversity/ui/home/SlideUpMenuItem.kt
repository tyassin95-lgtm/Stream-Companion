package hu.unideb.studyversity.ui.home

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import hu.unideb.studyversity.R

/**
 * Entry of the home accordion. Top level items have [color] == null and carry [subMenuItems];
 * leaf items carry the [id] (an `R.id.menu_*`) that HomeFragment routes on.
 * [parentId] -1 means "only visible when signed in", -100 "only when the Happy Challenge is switched on".
 */
class SlideUpMenuItem(
    @ColorRes val color: Int?,
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    @IdRes val id: Int? = null,
    val parentId: Int? = null
) {
    @ColorRes var backgroundColor: Int? = null
    @DrawableRes var backgroundDrawable: Int? = null
    @ColorRes var titleColor: Int = R.color.white
    var isExpanded = false
    var subMenuItems: ArrayList<SlideUpMenuItem> = arrayListOf()
    val isHeader get() = color == null

    companion object {
        const val ONLY_WHEN_LOGGED_IN = -1
        const val ONLY_WHEN_HAPPY_CHALLENGE_ON = -100
    }
}
