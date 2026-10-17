package hu.unideb.studyversity.ui.university_matters.sport

import android.os.Bundle
import android.view.View
import hu.unideb.studyversity.R
import hu.unideb.studyversity.utils.ListEntry
import hu.unideb.studyversity.utils.SimpleListFragment

/** Sport organisations of the university; each opens its website. */
class SportSubMenuFragment : SimpleListFragment() {
    override val iconBackground = R.drawable.selector_viewgroup_red_inverse
    override val itemBackground = R.drawable.selector_viewgroup_red
    override val iconTint = R.drawable.icon_selector_red
    override val items by lazy {
        listOf(
            ListEntry(getString(R.string.sport_sportsci), "ic_running"),
            ListEntry(getString(R.string.tourism_and_sports_management), "ic_university_1"),
            ListEntry(getString(R.string.set_center), "ic_building"),
            ListEntry(getString(R.string.deac), "ic_shield")
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.sports_organizations)
    }

    override fun onItemClicked(position: Int, item: ListEntry) {
        val url = when (item.name) {
            getString(R.string.sport_sportsci) -> getString(R.string.sportInstituteURL)
            getString(R.string.tourism_and_sports_management) -> getString(R.string.tourism_and_sports_management_url)
            getString(R.string.set_center) -> getString(R.string.setCenterURL)
            getString(R.string.deac) -> getString(R.string.deacURL)
            else -> return
        }
        openWebView(url, item.name)
    }
}
