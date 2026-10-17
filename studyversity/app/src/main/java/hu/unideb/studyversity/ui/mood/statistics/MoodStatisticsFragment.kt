package hu.unideb.studyversity.ui.mood.statistics

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.FragmentMoodStatisticsBinding
import hu.unideb.studyversity.databinding.MoodFaqItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import hu.unideb.studyversity.ui.happy_challenge.MoodFlow
import hu.unideb.studyversity.ui.happy_challenge.MoodScreen
import hu.unideb.studyversity.ui.mood.MoodHolderFragment
import hu.unideb.studyversity.ui.mood.MoodViewModel
import hu.unideb.studyversity.ui.webview.WebViewFragment

/** Level, points and progress of the running mood game, with shortcuts to the explanation pages. */
class MoodStatisticsFragment : BaseFragment() {
    private var _binding: FragmentMoodStatisticsBinding? = null
    private val binding get() = _binding!!
    private val vm: MoodViewModel by activityViewModels()
    private val flow = MoodFlow(this) { vm.loadGameData() }

    private data class FaqItem(val title: Int, val image: Int, val url: String)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.userNameText.text = session.currentUser?.name
        binding.simpleImageButton.setOnClickListener { openWeb(getString(R.string.mood_game_url)) }

        val faq = singleLayoutAdapter<FaqItem>(R.layout.mood_faq_item) { v, item, _ ->
            val b = MoodFaqItemBinding.bind(v)
            b.faqImage.setImageResource(item.image)
            b.textView11.setText(item.title)
            v.setOnClickListener { openWeb(item.url, getString(item.title)) }
        }
        binding.faqHolderRv.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.faqHolderRv.setHasFixedSize(true)
        binding.faqHolderRv.adapter = faq
        val game = getString(R.string.mood_game_url)
        faq.submit(listOf(
            FaqItem(R.string.mood_faq_1, R.drawable.mood_level_4, "$game?page=0"),
            FaqItem(R.string.mood_faq_2, R.drawable.mood_level_2, "$game?page=1"),
            FaqItem(R.string.mood_faq_3, R.drawable.mood_level_3, getString(R.string.mood_game_url_how_to_collect_reward_url)),
            FaqItem(R.string.mood_faq_4, R.drawable.mood_level_1, "$game?page=3")
        ))

        vm.progressText.observe(viewLifecycleOwner) { binding.pointsTitle.text = it }
        vm.moodData.observe(viewLifecycleOwner) { d ->
            binding.chip.text = d.levelLabel
            binding.levelImage.background = vm.levelImage(requireContext(), d.currentStage)
            binding.progressRightText.text = d.nextLevelLabel
            binding.progressLeftText.text = "0"
            val max = d.currentLevelMaxPoints ?: 100
            binding.progressBar.max = max
            countUp(d.progress ?: 0) { value ->
                _binding?.let { b -> b.pointsTv.text = value.toString(); b.progressBar.progress = value }
            }
        }
        load()
    }

    private fun load() {
        vm.loadGameData(
            onGame = { game ->
                val screen = vm.commons.processStudentMoodGame(game)
                when {
                    screen != MoodScreen.NONE -> present(screen)
                    vm.commons.shouldShowMoodAddView() -> flow.openAdd()
                }
            },
            onNoGame = { flow.openRegistration() },
            onError = { handleError(it) { load() } }
        )
    }

    private fun present(screen: MoodScreen) = when (screen) {
        MoodScreen.LEVEL_UPGRADE -> flow.openLevelUpgrade()
        MoodScreen.GAME_ENDED -> flow.openGameEnded()
        MoodScreen.FORM -> flow.openForm()
        MoodScreen.ADD -> flow.openAdd()
        else -> Unit
    }

    private fun countUp(target: Int, onValue: (Int) -> Unit) {
        ValueAnimator.ofInt(0, target).apply {
            duration = 1000
            addUpdateListener { onValue(it.animatedValue as Int) }
            start()
        }
    }

    private fun openWeb(url: String, title: String? = null) {
        findNavController().navigate(R.id.webViewFragment2, bundleOf("fileName" to url, "fileMode" to WebViewFragment.MODE_URL, "title" to title))
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
