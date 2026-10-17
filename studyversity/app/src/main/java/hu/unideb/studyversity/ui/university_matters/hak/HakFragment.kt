package hu.unideb.studyversity.ui.university_matters.hak

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import hu.unideb.studyversity.R
import hu.unideb.studyversity.ui.university_matters.university_locale.InstitutionListActivity
import hu.unideb.studyversity.utils.ListEntry
import hu.unideb.studyversity.utils.SimpleListFragment

/** "Educational information": library, academic year, study departments, scholarships, useful links. */
class HakFragment : SimpleListFragment() {
    override val items by lazy {
        listOf(
            ListEntry(getString(R.string.library_my_loans), "ic_book_reader"),
            ListEntry(getString(R.string.library_search), "ic_book_reader"),
            ListEntry(getString(R.string.academicyear), "ic_academic_year"),
            ListEntry(getString(R.string.studydepartment), "ic_book_reader"),
            ListEntry(getString(R.string.student_scholar_ship), "ic_graduation_cap"),
            ListEntry(getString(R.string.linklibrary), "ic_links")
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_hak)
    }

    override fun onItemClicked(position: Int, item: ListEntry) {
        when (item.name) {
            getString(R.string.library_my_loans) -> findNavController().navigate(R.id.libraryFragment)
            getString(R.string.library_search) -> findNavController().navigate(R.id.librarySearchFragment)
            getString(R.string.academicyear) -> findNavController().navigate(R.id.academicYearFragment)
            getString(R.string.studydepartment) -> startActivity(Intent(requireActivity(), InstitutionListActivity::class.java).apply {
                putExtra("fragmentTitle", getString(R.string.studydepartment))
                putExtra("categorieCodes", arrayOf("academic_classes"))
                putExtra("autoload", true)
                putExtra("headerEnabled", false)
                putExtra("hideBottomLayout", false)
                putExtra("searchIsForSport", false)
            })
            getString(R.string.student_scholar_ship) -> findNavController().navigate(R.id.studentScholarShipFragment)
            getString(R.string.linklibrary) -> openWebView(getString(R.string.useful_links_url), getString(R.string.linklibrary))
        }
    }
}
