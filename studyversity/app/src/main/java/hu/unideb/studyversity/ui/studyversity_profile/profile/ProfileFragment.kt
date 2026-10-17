package hu.unideb.studyversity.ui.studyversity_profile.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.StudentTrainingItem
import hu.unideb.studyversity.data.model.User
import hu.unideb.studyversity.databinding.ProfileFragmentBinding
import hu.unideb.studyversity.databinding.ProfileListHeaderBinding
import hu.unideb.studyversity.databinding.StudentTrainingListItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.InfoRows
import hu.unideb.studyversity.ui.base.ListItemAdapter

/** Name, Neptun code, balance and the list of the student's trainings. */
class ProfileFragment : BaseFragment() {
    private var _binding: ProfileFragmentBinding? = null
    private val binding get() = _binding!!

    private val adapter = ListItemAdapter<Any>(
        layoutFor = { item, _ -> if (item is User) R.layout.profile_list_header else R.layout.student_training_list_item }
    ) { layout, view, item, _ ->
        if (layout == R.layout.profile_list_header) {
            val user = item as User
            ProfileListHeaderBinding.bind(view).apply {
                logout.text = user.name
                neptunCode.text = user.neptunCode
                trainingNames.text = user.studentTrainingNameList
                balance.text = user.formattedBalance(view.context)
            }
        } else {
            val t = item as StudentTrainingItem
            val c = view.context
            StudentTrainingListItemBinding.bind(view).title.text = t.moduleName
            InfoRows.fill(view, listOf(
                R.string.training_start to t.trainingStart(c),
                R.string.training_mode to t.trainingMode,
                R.string.module_level to t.moduleLevel,
                R.string.module_status to t.moduleStatus,
                R.string.faculty_name to t.facultyName,
                R.string.active_semesters to t.activeSemesters,
                R.string.passive_semesters to t.passiveSemesters,
                R.string.module_valid_from to t.moduleValidFrom(c),
                R.string.training_plan_end to t.trainingPlanEnd(c),
                R.string.financial_status to t.financialStatus
            ))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ProfileFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_profile)
        binding.trainingList.layoutManager = LinearLayoutManager(requireContext())
        binding.trainingList.setHasFixedSize(true)
        binding.trainingList.adapter = adapter
        loadProfile()
    }

    private fun loadProfile() = load(retry = ::loadProfile) {
        val user = session.refresh() ?: throw hu.unideb.studyversity.data.AuthRequiredException()
        val trainings = backend.trainings()
        binding.emptyLayout.root.visibility = if (trainings.isEmpty()) View.VISIBLE else View.GONE
        binding.trainingList.visibility = if (trainings.isEmpty()) View.GONE else View.VISIBLE
        adapter.submit(listOf<Any>(user) + trainings)
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
