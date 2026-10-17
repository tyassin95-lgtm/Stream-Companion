package hu.unideb.studyversity.ui.happy_challenge.history.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.Formats
import hu.unideb.studyversity.databinding.SummaryFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.happy_challenge.Mood
import java.util.Calendar
import java.util.Date

enum class Range(val days: Int) { ONE_WEEK(7), TWO_WEEKS(14), ONE_MONTH(30), TWO_MONTHS(60) }

/** History tab: how many great / normal / bad days were logged in the last 7 / 14 / 30 / 60 days. */
class SummaryFragment : BaseFragment() {
    private var _binding: SummaryFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var buttons: List<LinearLayout>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SummaryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buttons = listOf(binding.oneWeek, binding.twoWeek, binding.thirtyDays, binding.sixtyDays)
        val ranges = mapOf(binding.oneWeek to Range.ONE_WEEK, binding.twoWeek to Range.TWO_WEEKS,
            binding.thirtyDays to Range.ONE_MONTH, binding.sixtyDays to Range.TWO_MONTHS)
        buttons.forEach { b -> b.setOnClickListener { select(b, ranges.getValue(b)) } }
        binding.restartedText.visibility = View.GONE
        binding.oneWeek.postDelayed({ _binding?.let { select(it.oneWeek, Range.ONE_WEEK) } }, 150)
    }

    private fun select(button: LinearLayout, range: Range) {
        buttons.forEach { it.isSelected = false }
        button.isSelected = true
        load(retry = { select(button, range) }) {
            val to = Date()
            val from = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -range.days) }.time
            val counts = backend.moodSummary(from, to).associate { it.mood to (it.count ?: "0") }
            binding.headerText.text = android.text.format.DateUtils.formatDateRange(
                requireContext(), from.time, to.time, android.text.format.DateUtils.FORMAT_SHOW_DATE or android.text.format.DateUtils.FORMAT_ABBREV_ALL
            )
            binding.greatCount.text = counts[Mood.GREAT.name] ?: "0"
            binding.normalCount.text = counts[Mood.NORMAL.name] ?: "0"
            binding.badCount.text = counts[Mood.BAD.name] ?: "0"
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
