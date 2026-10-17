package hu.unideb.studyversity.ui.university_matters.over_education

import android.content.Intent
import android.os.Bundle
import android.view.View
import hu.unideb.studyversity.R
import hu.unideb.studyversity.ui.university_matters.university_locale.InstitutionListActivity
import hu.unideb.studyversity.utils.ListEntry
import hu.unideb.studyversity.utils.SimpleListFragment
import hu.unideb.studyversity.utils.Utils

/** "Beyond education": campus life links (apps, transport, clubs, canteens, UniPass). */
class OverEducationFragment : SimpleListFragment() {
    override val hideSubTitle = true
    override val items by lazy {
        listOf(
            ListEntry(getString(R.string.mediversity_app), "ic_mediversity"),
            ListEntry(getString(R.string.public_transport), "ic_public_transport"),
            ListEntry(getString(R.string.public_transport_timetables), "ic_public_transport"),
            ListEntry(getString(R.string.entertainment_places), "ic_music"),
            ListEntry(getString(R.string.meal), "ic_utensils"),
            ListEntry(getString(R.string.unipass), "ic_user_circle"),
            ListEntry(getString(R.string.unipass_partners), "ic_id_card", getString(R.string.unipass_url))
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.over_education)
    }

    private fun startApp(idRes: Int, name: String) =
        Utils.startApplication(requireContext(), parentFragmentManager, getString(idRes), name)

    private fun places(titleRes: Int, category: String) = startActivity(Intent(requireActivity(), InstitutionListActivity::class.java).apply {
        putExtra("fragmentTitle", getString(titleRes))
        putExtra("categorieCodes", arrayOf(category))
        putExtra("autoload", true)
        putExtra("headerEnabled", false)
        putExtra("hideBottomLayout", true)
        putExtra("searchIsForSport", false)
    })

    override fun onItemClicked(position: Int, item: ListEntry) {
        when (item.name) {
            getString(R.string.mediversity_app) -> startApp(R.string.mediversity_id, item.name)
            getString(R.string.public_transport) -> startApp(R.string.dkv_menetrend_id, item.name)
            getString(R.string.public_transport_timetables) -> startApp(R.string.telvira_id, item.name)
            getString(R.string.entertainment_places) -> places(R.string.entertainment_places, "university_clubs_bars")
            getString(R.string.meal) -> places(R.string.meal, "menza_restaurant")
            getString(R.string.unipass_partners) -> openWebView(item.value ?: return, item.name)
        }
    }
}
