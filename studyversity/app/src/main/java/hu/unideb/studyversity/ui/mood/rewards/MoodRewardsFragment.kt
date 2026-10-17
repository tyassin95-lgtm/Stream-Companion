package hu.unideb.studyversity.ui.mood.rewards

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.MoodGameStage
import hu.unideb.studyversity.databinding.FragmentMoodRewardsBinding
import hu.unideb.studyversity.databinding.MoodRewardItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import hu.unideb.studyversity.ui.mood.MoodViewModel

/** One row per level: gifts to choose from, and – for the running level – the progress bar. */
class MoodRewardsFragment : BaseFragment() {
    private var _binding: FragmentMoodRewardsBinding? = null
    private val binding get() = _binding!!
    private val vm: MoodViewModel by activityViewModels()

    private val adapter = singleLayoutAdapter<MoodGameStage>(R.layout.mood_reward_item) { v, stage, _ ->
        val b = MoodRewardItemBinding.bind(v)
        val ctx = v.context
        val order = stage.stageOrder ?: 1
        b.levelImage.setImageDrawable(vm.levelImage(ctx, order))
        b.chip.text = ctx.getString(R.string.current_level, order.toString())
        val gifts = stage.gifts.joinToString(", ") { it.name.orEmpty() }
        v.findViewById<TextView>(R.id.giftsList).text = Html.fromHtml(ctx.getString(R.string.modd_reward_gist_list_label, gifts), Html.FROM_HTML_MODE_LEGACY)

        val current = vm.moodData.value?.currentStage ?: 1
        if (current > order) {
            // finished level: show whether the gift was taken
            b.notTakeView.visibility = View.VISIBLE
            b.progressView.visibility = View.GONE
            b.receivedGiftText.text = vm.chosenGiftByLevel(order)?.name?.let { ctx.getString(R.string.received_gift) + ": " + it } ?: ctx.getString(R.string.not_taken_over)
            b.notTakeView.setOnClickListener { if (vm.chosenGiftByLevel(order) == null) pickGift(stage) }
        } else {
            b.notTakeView.visibility = View.GONE
            b.progressView.visibility = View.VISIBLE
            val progress = if (current == order) vm.moodData.value?.progress ?: 0 else 0
            b.progressBar.max = stage.days ?: 73
            b.progressBar.progress = progress
            v.findViewById<TextView>(R.id.progressText).text = ctx.getString(R.string.mood_reward_helper_text)
            v.findViewById<TextView>(R.id.progress_text).text = "$progress/${stage.days}"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rewardsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.rewardsRv.setHasFixedSize(true)
        binding.rewardsRv.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.rewardsRv.adapter = adapter
        vm.listStages()
        vm.stages.observe(viewLifecycleOwner) { adapter.submit(it) }
        vm.completed.observe(viewLifecycleOwner) { adapter.notifyDataSetChanged() }
        vm.moodData.observe(viewLifecycleOwner) { adapter.notifyDataSetChanged() }
    }

    /** Local-engine extension: choose the reward of a finished level (the original did this on the web portal). */
    private fun pickGift(stage: MoodGameStage) {
        val gifts = stage.gifts
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.mood_pick_gift)
            .setItems(gifts.map { it.name }.toTypedArray()) { _, i ->
                val gift = gifts[i]
                vm.chooseGift(stage.id ?: return@setItems, gift.id ?: return@setItems) {
                    snack(getString(R.string.mood_gift_chosen, gift.name))
                }
            }.show()
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
