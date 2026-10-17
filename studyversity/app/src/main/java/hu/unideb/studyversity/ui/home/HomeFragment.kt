package hu.unideb.studyversity.ui.home

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.MainMenuSubItemLayoutBinding
import hu.unideb.studyversity.databinding.SlideUpListItemBinding
import hu.unideb.studyversity.databinding.HomeFragmentBinding
import hu.unideb.studyversity.data.model.NewsChannel
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.community.CommunityActivity
import hu.unideb.studyversity.ui.instructor.InstructorActivity
import hu.unideb.studyversity.ui.mood.ActivityMoodHolder
import hu.unideb.studyversity.ui.university_matters.university_locale.InstitutionListActivity
import hu.unideb.studyversity.ui.webview.WebViewFragment
import hu.unideb.studyversity.utils.Animations
import kotlinx.coroutines.launch

/** The start screen: an accordion with the five menu sections. */
class HomeFragment : BaseFragment() {
    private val viewModel: HomeViewModel by viewModels()
    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!
    private var detailHeight = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbarHost()?.apply {
            updateTitle(getString(R.string.app_name))
            updateSubTitle("")
            showUpButton(false)
        }
        session.user.observe(viewLifecycleOwner) { initMenu() }
        // The expanded section gets all the height that the collapsed headers leave over.
        binding.mainMenuHolder.post {
            val headerH = resources.getDimension(R.dimen.manu_list_header_default_height).toInt()
            detailHeight = (binding.mainMenuHolder.height - viewModel.mainMenuItems.size * headerH).coerceAtLeast(0)
            initMenu()
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch { session.refresh() }
    }

    private fun initMenu() {
        val holder = binding.mainMenuHolder
        holder.removeAllViews()
        val loggedIn = session.isLoggedIn
        val items = viewModel.mainMenuItems
        items.forEachIndexed { index, section ->
            val row = SlideUpListItemBinding.inflate(layoutInflater, holder, false)
            row.name.setText(section.title)
            row.name.setTextColor(ContextCompat.getColor(requireContext(), section.titleColor))
            row.headerIcon.setImageResource(section.icon)
            row.headerIcon.setColorFilter(ContextCompat.getColor(requireContext(), section.titleColor))
            section.backgroundDrawable?.let { row.slideUpHeader.setBackgroundResource(it) }
            row.slideUpHeader.setOnClickListener { flip(section) }

            // The strip behind the rounded header corners shows the colour of the section above.
            val behind = if (index > 0) {
                val prev = items[index - 1]
                if (prev.isExpanded) R.color.white else prev.backgroundColor ?: R.color.white
            } else section.backgroundColor ?: R.color.white
            row.menuItemHeader.setBackgroundColor(ContextCompat.getColor(requireContext(), behind))
            holder.addView(row.root)

            row.subMenuHolderScv.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, if (section.isExpanded) detailHeight else 0)
            section.subMenuItems.forEach { sub -> row.detailHolder.addView(subItemView(sub, row.detailHolder, loggedIn)) }
        }
    }

    private fun subItemView(sub: SlideUpMenuItem, parent: ViewGroup, loggedIn: Boolean): View {
        val b = MainMenuSubItemLayoutBinding.inflate(layoutInflater, parent, false)
        b.menuItemTitle.setText(sub.title)
        b.subMenuParent.setOnClickListener { sub.id?.let { handleMenuItemClick(it) } }

        val (imageStyleBg, tintList, itemBg) = when (sub.color) {
            R.color.teal -> Triple(R.drawable.selector_viewgroup_green_inverse, R.drawable.icon_selector_green, R.drawable.selector_viewgroup_green)
            R.color.yellowishOrange -> Triple(R.drawable.selector_viewgroup_orange_inverse, R.drawable.icon_selector_orange, R.drawable.selector_viewgroup_orange)
            R.color.dexpo_pink -> Triple(R.drawable.selector_viewgroup_pink_inverse, R.drawable.icon_selector_pink, R.drawable.selector_viewgroup_pink)
            R.color.sport_red -> Triple(R.drawable.selector_viewgroup_sport_green_inverse, R.drawable.icon_selector_sport_green, R.drawable.selector_viewgroup_sport_green)
            R.color.clearBlue -> Triple(R.drawable.selector_viewgroup_purple_inverse, R.drawable.icon_selector_purple, R.drawable.selector_viewgroup_purple)
            else -> Triple(R.drawable.selector_viewgroup_red_inverse, R.drawable.icon_selector_red, R.drawable.selector_viewgroup_red)
        }
        b.menuItemIcon.setBackgroundResource(imageStyleBg)
        b.subMenuParent.setBackgroundResource(itemBg)
        ContextCompat.getDrawable(requireContext(), sub.icon)?.let { d ->
            val wrapped = DrawableCompat.wrap(d.mutate())
            DrawableCompat.setTintList(wrapped, ContextCompat.getColorStateList(requireContext(), tintList))
            b.menuItemIcon.setImageDrawable(wrapped)
        }

        val instructor = session.currentUser?.isInstructor == true
        b.root.visibility = if (instructor && sub.id in HomeViewModel.STUDENT_ONLY) View.GONE else when (sub.parentId) {
            SlideUpMenuItem.INSTRUCTORS_ONLY -> if (instructor) View.VISIBLE else View.GONE
            SlideUpMenuItem.ONLY_WHEN_LOGGED_IN -> if (loggedIn) View.VISIBLE else View.GONE
            SlideUpMenuItem.ONLY_WHEN_HAPPY_CHALLENGE_ON -> if (prefs.happyChallengeEnabled) View.VISIBLE else View.GONE
            else -> View.VISIBLE
        }
        return b.root
    }

    private fun flip(section: SlideUpMenuItem) {
        if (!viewModel.flipExpanded(section)) return
        val holder = binding.mainMenuHolder
        for (i in 0 until holder.childCount) {
            val row = holder.getChildAt(i) as ViewGroup
            val scroll = row.getChildAt(1)
            val item = viewModel.mainMenuItems[i]
            if (scroll.height == detailHeight) Animations.slideView(scroll, detailHeight, 0)
            else if (item.isExpanded) Animations.slideView(scroll, 0, detailHeight)
            if (i > 0) {
                val prev = viewModel.mainMenuItems[i - 1]
                val color = if (prev.isExpanded) R.color.white else prev.backgroundColor ?: R.color.white
                row.findViewById<View>(R.id.menuItemHeader).setBackgroundColor(ContextCompat.getColor(requireContext(), color))
            }
        }
    }

    private fun nav(destination: Int, args: Bundle? = null) {
        runCatching { findNavController().navigate(destination, args) }
    }

    private fun openWeb(url: String, title: String? = null, mode: String = WebViewFragment.MODE_URL) =
        nav(R.id.webViewFragment, bundleOf("fileName" to url, "fileMode" to mode, "title" to title))

    private fun openDocument(title: String, url: String) = nav(
        R.id.webViewFragment, bundleOf("fileName" to url, "fileMode" to WebViewFragment.MODE_URL, "title" to title)
    )

    private fun rssList(layout: Int, title: String, channel: NewsChannel) =
        nav(R.id.rssListFragment, bundleOf("list_item_layout" to layout, "fragmentTitle" to title, "channelID" to intArrayOf(channel.value)))

    private fun dexpoSub(type: String, title: String) = nav(R.id.dexpoSubMenuFragment, bundleOf("type" to type, "title" to title))

    private fun institutionList(titleRes: Int, categories: Array<String>?, sport: Boolean) {
        startActivity(Intent(requireActivity(), InstitutionListActivity::class.java).apply {
            putExtra("fragmentTitle", getString(titleRes))
            putExtra("categorieCodes", categories)
            putExtra("autoload", false)
            putExtra("headerEnabled", true)
            putExtra("hideBottomLayout", false)
            putExtra("searchIsForSport", sport)
        })
    }

    private fun handleMenuItemClick(menuId: Int) {
        when (menuId) {
            R.id.menu_instructor_console -> startActivity(Intent(requireContext(), InstructorActivity::class.java))
            R.id.menu_profile -> nav(R.id.profileFragment)
            R.id.menu_time_table -> nav(R.id.timeTableFragment)
            R.id.menu_course_informations -> nav(R.id.coursesInformationFragment)
            R.id.menu_taken_exams -> nav(R.id.examsInformationFragment)
            R.id.menu_my_mood -> startActivity(Intent(requireContext(), ActivityMoodHolder::class.java))
            R.id.menu_ud_community -> startActivity(Intent(requireContext(), CommunityActivity::class.java))
            R.id.menu_logout -> viewLifecycleOwner.lifecycleScope.launch { session.logout() }

            R.id.menu_hak -> nav(R.id.hakFragment)
            R.id.menu_news -> rssList(R.layout.news_rss_list_item, getString(R.string.menu_news), NewsChannel.unidebGeneralNews)
            R.id.menu_events -> rssList(R.layout.events_rss_list_item, getString(R.string.menu_events), NewsChannel.unidebEvents)
            R.id.menu_phone_book -> nav(R.id.phonebookFragment)
            R.id.menu_uni_places -> institutionList(R.string.menu_uni_places, null, false)
            R.id.menu_beyond_training -> nav(R.id.overEducationFragment)
            R.id.menu_e_learning -> openWeb(getString(R.string.e_learning_url_address), getString(R.string.menu_e_learning))

            R.id.menu_dexpo_faculties -> dexpoSub("FACULTIES", getString(R.string.sub_menu_dexpo_faculty_information))
            R.id.menu_dexpo_programmes -> dexpoSub("PROGRAMS", getString(R.string.sub_menu_dexpo_programmes))
            R.id.menu_dexpo_on_site_information -> dexpoSub("LOCATION_INFORMATION", getString(R.string.sub_menu_dexpo_on_site_information))
            R.id.menu_dexpo_social_media -> dexpoSub("SOCIAL_MEDIA", getString(R.string.sub_menu_dexpo_social_media))
            R.id.menu_dexpo_find_your_favourite_major -> openWeb(getString(R.string.dexpoFavouriteMajorURL), getString(R.string.sub_menu_dexpo_find_your_favourite_major))
            R.id.menu_dexpo_for_parents -> openWeb(getString(R.string.dexpoForParentsURL), getString(R.string.sub_menu_dexpo_for_parents))
            R.id.menu_dexpo_get_certificate -> openWeb(getString(R.string.dexpoGetCertificateURL), getString(R.string.sub_menu_dexpo_get_certificate))
            R.id.menu_dexpo_introduction -> openWeb(getString(R.string.dexpoIntroductionURL), getString(R.string.sub_menu_dexpo_introduction))
            R.id.menu_dexpo_menu_dexpo_keep -> openWeb(getString(R.string.dexpoKeepURL), getString(R.string.sub_menu_dexpo_keep))
            R.id.menu_dexpo_questionnaries -> openWeb(getString(R.string.dexpoGetQuestionariesURL), getString(R.string.sub_menu_dexpo_questionnaries))

            R.id.menu_sport -> openDocument(getString(R.string.menu_sport), getString(R.string.menu_sport_link))
            R.id.menu_sport_places -> institutionList(R.string.sport_places, arrayOf("sport"), true)
            R.id.menu_sport_news -> rssList(R.layout.news_rss_list_item, getString(R.string.sport_news), NewsChannel.sportNews)
            R.id.menu_sport_events -> rssList(R.layout.news_rss_list_item, getString(R.string.sport_events), NewsChannel.sportEvents)
            R.id.menu_sports_organizations -> nav(R.id.sportSubMenuFragment)
            R.id.menu_sport_questionaries -> openDocument(getString(R.string.questionnaires), getString(R.string.sport_questionary_url))
            R.id.menu_sport_registration -> openWeb(getString(R.string.sportRegistrationURL), getString(R.string.sport_registration))
            R.id.menu_sport_sportsmate_finder -> openWeb(getString(R.string.sportMateFinderURL), getString(R.string.sportsmate_finder))

            R.id.menu_impressum -> openDocument(getString(R.string.menu_impressum), getString(R.string.impressum_url))
            R.id.menu_privacy -> openDocument(getString(R.string.menu_privacy), getString(R.string.privacy_policy_url))
            R.id.menu_terms_of_use -> openDocument(getString(R.string.menu_terms_of_use), getString(R.string.terms_of_use_url))
            R.id.menu_contact -> nav(R.id.contactFragment)
            R.id.menu_settings -> nav(R.id.settingsFragment)
            R.id.menu_my_messages -> nav(R.id.messagesFragment)
            else -> snack(R.string.no_menu_found_by_this_title)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
