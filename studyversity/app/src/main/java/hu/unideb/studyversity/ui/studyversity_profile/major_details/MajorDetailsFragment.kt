package hu.unideb.studyversity.ui.studyversity_profile.major_details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.unideb.studyversity.databinding.MajorDetailsFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment

/** Placeholder screen ("Educational information"): the original only shows its title. */
class MajorDetailsFragment : BaseFragment() {
    private var _binding: MajorDetailsFragmentBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = MajorDetailsFragmentBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
