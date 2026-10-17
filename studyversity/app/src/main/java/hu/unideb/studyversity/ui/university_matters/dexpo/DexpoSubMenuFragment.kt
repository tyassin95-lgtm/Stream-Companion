package hu.unideb.studyversity.ui.university_matters.dexpo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import hu.unideb.studyversity.R
import hu.unideb.studyversity.utils.ListEntry
import hu.unideb.studyversity.utils.SimpleListFragment

enum class DexpoSubMenuType { FACULTIES, PROGRAMS, LOCATION_INFORMATION, SOCIAL_MEDIA }

/** The DEXPO (open day) sub menus: faculties, programmes, on-site information, social media. */
class DexpoSubMenuFragment : SimpleListFragment() {
    override val iconBackground = R.drawable.selector_viewgroup_pink_inverse
    override val itemBackground = R.drawable.selector_viewgroup_pink
    override val iconTint = R.drawable.icon_selector_pink
    override val hideSubTitle = true

    private val type get() = DexpoSubMenuType.valueOf(requireArguments().getString("type", DexpoSubMenuType.FACULTIES.name))

    private fun e(name: Int, icon: String, url: Int) = ListEntry(getString(name), icon, getString(url))

    override val items by lazy {
        when (type) {
            DexpoSubMenuType.FACULTIES -> listOf(
                e(R.string.sub_menu_dexpo_faculty_law, "ic_balance_scale", R.string.dexpoFacultyOfLawURL),
                e(R.string.sub_menu_dexpo_faculty_medicine, "ic_stethoscope", R.string.dexpoFacultyOfMedicineURL),
                e(R.string.sub_menu_dexpo_faculty_humanities, "ic_book_reader", R.string.dexpoFacultyOfHumanitiesURL),
                e(R.string.sub_menu_dexpo_faculty_health_science, "ic_heartbeat", R.string.dexpoFacultyOfHealthScienceURL),
                e(R.string.sub_menu_dexpo_faculty_denistry, "ic_tooth", R.string.dexpoFacultyOfDentistryURL),
                e(R.string.sub_menu_dexpo_faculty_econonmics_business, "ic_piggy_bank", R.string.dexpoFacultyOfEconomicsAndBusinessURL),
                e(R.string.sub_menu_dexpo_faculty_special_needs, "ic_baby", R.string.dexpoFacultyOfChildAndSpecialNeedsEducationURL),
                e(R.string.sub_menu_dexpo_faculty_pharmacy, "ic_pills", R.string.dexpoFacultyOfPharmacyURL),
                e(R.string.sub_menu_dexpo_faculty_informatics, "ic_laptop_code", R.string.dexpoFacultyOfInformaticsURL),
                e(R.string.sub_menu_dexpo_faculty_afsem, "ic_tractor", R.string.dexpoFacultyAFSEMURL),
                e(R.string.sub_menu_dexpo_faculty_engineering, "ic_cog", R.string.dexpoFacultyOfEngineeringURL),
                e(R.string.sub_menu_dexpo_faculty_science_technoligy, "ic_atom", R.string.dexpoFacultyOfScienceAndTechnologyURL),
                e(R.string.sub_menu_dexpo_faculty_music, "ic_music", R.string.dexpoFacultyOfMusicURL)
            )
            DexpoSubMenuType.PROGRAMS -> listOf(
                e(R.string.sub_menu_dexpo_informative_lectures, "ic_school", R.string.dexpoInformativeLecturesURL),
                e(R.string.sub_menu_dexpo_stands, "ic_shapes", R.string.dexpoStandsURL),
                e(R.string.sub_menu_dexpo_accompanying_programs, "ic_calendar_plus", R.string.dexpoAccompanyingProgramsURL)
            )
            DexpoSubMenuType.LOCATION_INFORMATION -> listOf(
                e(R.string.sub_menu_dexpo_map, "ic_map_marked", R.string.dexpoMapURL)
            )
            DexpoSubMenuType.SOCIAL_MEDIA -> listOf(
                e(R.string.sub_menu_dexpo_facebook, "ic_facebook_square", R.string.dexpoFacebookLink),
                e(R.string.sub_menu_dexpo_instagram, "ic_instagram", R.string.dexpoInstagramLink),
                e(R.string.sub_menu_dexpo_tiktok, "ic_logo_tiktok", R.string.dexpoTiktokLink)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(arguments?.getString("title"))
    }

    override fun onItemClicked(position: Int, item: ListEntry) {
        val url = item.value ?: return
        if (type == DexpoSubMenuType.SOCIAL_MEDIA && url.contains("facebook") && facebookInstalled()) {
            // The Facebook app opens the page directly via its own scheme.
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/100064261496872"))) }
                .onFailure { openWebView(url, item.name) }
            return
        }
        openWebView(url, item.name)
    }

    private fun facebookInstalled() = runCatching {
        requireActivity().packageManager.getApplicationInfo("com.facebook.katana", 0).enabled
    }.getOrDefault(false)
}
