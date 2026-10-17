package hu.unideb.studyversity.ui.happy_challenge.mood_selection.choosePackage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.QuoteItem
import hu.unideb.studyversity.databinding.ChoosePackageFragmentBinding
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import hu.unideb.studyversity.ui.happy_challenge.HappyChallengeActivity
import hu.unideb.studyversity.ui.happy_challenge.Mood
import hu.unideb.studyversity.utils.GridSpacingItemDecoration
import hu.unideb.studyversity.utils.dialog.CustomViewDialog

/** Fifteen gift boxes; opening one reveals a random quote or a video that should cheer the student up. */
class ChoosePackageFragment : Fragment() {
    private var _binding: ChoosePackageFragmentBinding? = null
    private val binding get() = _binding!!
    private val host get() = requireActivity() as HappyChallengeActivity
    private val vm get() = host.viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ChoosePackageFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mood = vm.userMood.value
        binding.moodImage.setImageResource(when (mood) {
            Mood.BAD -> R.drawable.ic_hc_mood_bad_circle
            Mood.GREAT -> R.drawable.ic_hc_mood_great_circle
            else -> R.drawable.ic_hc_mood_normal_circle
        })
        binding.moodText.text = getString(when (mood) {
            Mood.BAD -> R.string.hc_bad_mood_label
            Mood.GREAT -> R.string.hc_great_mood_label
            else -> R.string.hc_normal_mood_label
        })
        binding.skipText.setOnClickListener { host.closeOk() }

        val grid = singleLayoutAdapter<Int>(R.layout.happy_challenge_grid_item) { v, _, _ -> v.setOnClickListener { openPackage() } }
        binding.hcGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.hcGrid.adapter = grid
        binding.hcGrid.addItemDecoration(GridSpacingItemDecoration(3, resources.getDimensionPixelSize(R.dimen.recycler_view_item_width), true))
        grid.submit((0 until 15).toList())
    }

    private fun openPackage() {
        vm.randomQuote(::showQuote) { e ->
            _binding?.let { Snackbar.make(it.root, e.localizedMessage ?: getString(R.string.error), Snackbar.LENGTH_LONG).show() }
        }
    }

    private fun showQuote(q: QuoteItem) {
        pendingVideo = q.video
        CustomViewDialog(
            title = getString(if (q.quote != null) R.string.quote else R.string.video),
            message = q.quote ?: getString(R.string.please_wathc_this_video_to_changyour_mood),
            secondLabel = q.author ?: q.video,
            mainIcon = R.drawable.ic_gift,
            positiveIcon = R.drawable.ic_arrow_right,
            // A video is opened when the dialog is confirmed; the game state is refreshed afterwards either way.
            onPositive = { finishPackage() }
        ).show(parentFragmentManager, "quote")
    }

    private var pendingVideo: String? = null

    private fun finishPackage() {
        pendingVideo?.let { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        vm.resolveNextScreen({ host.closeWith(it) }, { host.closeOk() })
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
