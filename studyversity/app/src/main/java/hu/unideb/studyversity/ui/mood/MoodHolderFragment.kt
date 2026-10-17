package hu.unideb.studyversity.ui.mood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.FragmentMoodHolderBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.happy_challenge.MoodFlow
import hu.unideb.studyversity.ui.happy_challenge.history.summary.SummaryFragment
import hu.unideb.studyversity.ui.mood.rewards.MoodRewardsFragment
import hu.unideb.studyversity.ui.mood.statistics.MoodStatisticsFragment

/** Tab host: statistics, my rewards, history – plus the "Add mood" button. */
class MoodHolderFragment : BaseFragment() {
    private var _binding: FragmentMoodHolderBinding? = null
    private val binding get() = _binding!!
    private val vm: MoodViewModel by activityViewModels()
    private val flow = MoodFlow(this) { vm.loadGameData() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodHolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_my_mood)
        val pages = listOf<() -> Fragment>({ MoodStatisticsFragment() }, { MoodRewardsFragment() }, { SummaryFragment() })
        binding.fragmentPager.adapter = object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
            override fun getItemCount() = pages.size
            override fun createFragment(position: Int) = pages[position]()
        }
        val titles = intArrayOf(R.string.mood_statistics, R.string.mood_my_rewards, R.string.mood_history)
        TabLayoutMediator(binding.tabLayout, binding.fragmentPager) { tab, i -> tab.setText(titles[i]) }.attach()
        navigateToTab(requireActivity().intent.getIntExtra("moodTabNumber", 0))

        binding.floatingActionButton.setOnClickListener { flow.resetHandled(); flow.openAdd() }
        vm.moodData.observe(viewLifecycleOwner) { updateAddButton() }
        updateAddButton()
    }

    private fun updateAddButton() {
        (binding.floatingActionButton.parent as View).visibility = if (vm.showAddMoodButton()) View.VISIBLE else View.GONE
    }

    fun navigateToTab(index: Int) {
        binding.fragmentPager.postDelayed({ _binding?.fragmentPager?.setCurrentItem(index, true) }, 600)
    }

    override fun onResume() { super.onResume(); if (_binding != null) updateAddButton() }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
