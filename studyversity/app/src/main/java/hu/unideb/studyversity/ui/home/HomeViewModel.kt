package hu.unideb.studyversity.ui.home

import androidx.lifecycle.ViewModel
import hu.unideb.studyversity.R
import java.util.Locale

/** Builds the five accordion sections of the home screen (same structure and order as the original). */
class HomeViewModel : ViewModel() {

    val mainMenuItems = ArrayList<SlideUpMenuItem>()

    private val studentMatters = arrayListOf(
        item(R.color.teal, R.string.menu_profile, R.drawable.ic_user, R.id.menu_profile),
        item(R.color.teal, R.string.menu_time_table, R.drawable.ic_time_table, R.id.menu_time_table),
        item(R.color.teal, R.string.menu_course_informations, R.drawable.ic_book, R.id.menu_course_informations),
        item(R.color.teal, R.string.menu_taken_exams, R.drawable.ic_exams, R.id.menu_taken_exams),
        item(R.color.teal, R.string.menu_my_mood, R.drawable.ic_happy_challenge, R.id.menu_my_mood, SlideUpMenuItem.ONLY_WHEN_HAPPY_CHALLENGE_ON),
        item(R.color.teal, R.string.ud_community, R.drawable.ic_community, R.id.menu_ud_community),
        SlideUpMenuItem(-1, R.string.menu_logout, R.drawable.ic_logout, R.id.menu_logout, SlideUpMenuItem.ONLY_WHEN_LOGGED_IN)
    )

    private val universityMatters = arrayListOf(
        item(R.color.yellowishOrange, R.string.menu_hak, R.drawable.ic_users, R.id.menu_hak),
        item(R.color.yellowishOrange, R.string.menu_news, R.drawable.ic_newspaper, R.id.menu_news),
        item(R.color.yellowishOrange, R.string.menu_events, R.drawable.ic_events, R.id.menu_events),
        item(R.color.yellowishOrange, R.string.menu_phone_book, R.drawable.ic_phone_book, R.id.menu_phone_book),
        item(R.color.yellowishOrange, R.string.menu_uni_places, R.drawable.ic_university_type_2, R.id.menu_uni_places),
        item(R.color.yellowishOrange, R.string.menu_beyond_training, R.drawable.ic_map_signs, R.id.menu_beyond_training)
    )

    private val sportMatters = arrayListOf(
        item(R.color.sport_red, R.string.sport_places, R.drawable.ic_poi, R.id.menu_sport_places),
        item(R.color.sport_red, R.string.sport_news, R.drawable.ic_newspaper, R.id.menu_sport_news),
        item(R.color.sport_red, R.string.sport_events, R.drawable.ic_calendar, R.id.menu_sport_events),
        item(R.color.sport_red, R.string.sports_organizations, R.drawable.ic_medal, R.id.menu_sports_organizations),
        item(R.color.sport_red, R.string.questionnaires, R.drawable.ic_poll, R.id.menu_sport_questionaries)
    )

    private val informationMatters = arrayListOf(
        item(R.color.clearBlue, R.string.menu_impressum, R.drawable.ic_user_circle, R.id.menu_impressum),
        item(R.color.clearBlue, R.string.menu_privacy, R.drawable.ic_user_lock, R.id.menu_privacy),
        item(R.color.clearBlue, R.string.menu_terms_of_use, R.drawable.ic_shield_alt, R.id.menu_terms_of_use),
        item(R.color.clearBlue, R.string.menu_contact, R.drawable.ic_envelope, R.id.menu_contact),
        item(R.color.clearBlue, R.string.menu_settings, R.drawable.ic_settings, R.id.menu_settings),
        item(R.color.clearBlue, R.string.menu_my_messages, R.drawable.ic_envelope, R.id.menu_my_messages)
    )

    /** The DEXPO section is shorter in English. */
    private fun dexpoMatters(): ArrayList<SlideUpMenuItem> {
        val c = R.color.dexpo_pink
        val intro = item(c, R.string.sub_menu_dexpo_introduction, R.drawable.ic_user_circle, R.id.menu_dexpo_introduction)
        val faculties = item(c, R.string.sub_menu_dexpo_faculty_information, R.drawable.ic_chalkboard_teacher, R.id.menu_dexpo_faculties)
        val social = item(c, R.string.sub_menu_dexpo_social_media, R.drawable.ic_hashtag_solid, R.id.menu_dexpo_social_media)
        return if (Locale.getDefault().language == "en") arrayListOf(intro, faculties, social) else arrayListOf(
            intro, faculties,
            item(c, R.string.sub_menu_dexpo_programmes, R.drawable.ic_events, R.id.menu_dexpo_programmes),
            item(c, R.string.sub_menu_dexpo_on_site_information, R.drawable.ic_map_marker, R.id.menu_dexpo_on_site_information),
            item(c, R.string.sub_menu_dexpo_find_your_favourite_major, R.drawable.ic_bookmark, R.id.menu_dexpo_find_your_favourite_major),
            item(c, R.string.sub_menu_dexpo_keep, R.drawable.ic_random, R.id.menu_dexpo_menu_dexpo_keep),
            item(c, R.string.sub_menu_dexpo_for_parents, R.drawable.ic_user_friends, R.id.menu_dexpo_for_parents),
            item(c, R.string.sub_menu_dexpo_questionnaries, R.drawable.ic_chart_pie, R.id.menu_dexpo_questionnaries),
            item(c, R.string.sub_menu_dexpo_get_certificate, R.drawable.ic_file_invoice, R.id.menu_dexpo_get_certificate),
            social
        )
    }

    init {
        mainMenuItems += section(R.string.menu_minDEnt_rolam, R.drawable.ic_user_graduate, R.color.teal, R.drawable.top_rounded_corners_green, studentMatters, expanded = true)
        mainMenuItems += section(R.string.menu_university_label, R.drawable.ic_university, R.color.yellowishOrange, R.drawable.top_rounded_corners_orange, universityMatters)
        mainMenuItems += section(R.string.menu_dexpo, R.drawable.ic_calendar_check, R.color.dexpo_pink, R.drawable.top_rounded_corners_pink, dexpoMatters(), titleColor = R.color.white)
        mainMenuItems += section(R.string.menu_sport, R.drawable.ic_sport_menu_icon, R.color.sport_red, R.drawable.top_rounded_corners_sport, sportMatters)
        mainMenuItems += section(R.string.menu_informations, R.drawable.ic_info_circle, R.color.clearBlue, R.drawable.top_rounded_corners_purple, informationMatters)
    }

    private fun item(color: Int, title: Int, icon: Int, id: Int, parent: Int? = null) = SlideUpMenuItem(color, title, icon, id, parent)

    private fun section(title: Int, icon: Int, bgColor: Int, bgDrawable: Int, subs: ArrayList<SlideUpMenuItem>, expanded: Boolean = false, titleColor: Int? = null) =
        SlideUpMenuItem(null, title, icon).apply {
            backgroundDrawable = bgDrawable
            backgroundColor = bgColor
            isExpanded = expanded
            subMenuItems = subs
            titleColor?.let { this.titleColor = it }
        }

    /** Opens [item] and collapses all others. Returns false when it was already open. */
    fun flipExpanded(item: SlideUpMenuItem): Boolean {
        if (item.isExpanded) return false
        mainMenuItems.forEach { it.isExpanded = it === item }
        return true
    }
}
