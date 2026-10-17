package hu.unideb.studyversity.ui.studyversity_profile.timetable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.unideb.studyversity.databinding.TimetableNavigationFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment

/** Placeholder ("Navigation") – reserved by the original for indoor navigation to a classroom. */
class TimeTableNavigationFragment : BaseFragment() {
    private var _binding: TimetableNavigationFragmentBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TimetableNavigationFragmentBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
