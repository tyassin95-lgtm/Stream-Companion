package hu.unideb.studyversity.ui.university_matters.hak.academicyear

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.AcademicYearItem
import hu.unideb.studyversity.databinding.AcademicYearFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment

/** Academic year schedule per faculty: pick a faculty, the document is shown below. */
class AcademicYearFragment : BaseFragment() {
    private var _binding: AcademicYearFragmentBinding? = null
    private val binding get() = _binding!!
    private var items: List<AcademicYearItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = AcademicYearFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.academicyear)
        binding.webView.settings.apply { javaScriptEnabled = true; allowFileAccess = true }
        loadYears()
    }

    private fun loadYears() = load(retry = ::loadYears) {
        items = backend.academicYears()
        if (items.isEmpty()) {
            binding.emptyLayout.root.visibility = View.VISIBLE
            binding.academicLayout.visibility = View.GONE
            return@load
        }
        binding.emptyLayout.root.visibility = View.GONE
        binding.academicLayout.visibility = View.VISIBLE
        val names = items.mapNotNull { it.name }
        val dropdown = binding.mySpinnerDropdown
        dropdown.setDropDownBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.autocomplete_dropdown))
        dropdown.setAdapter(ArrayAdapter(requireContext(), R.layout.dropdown_list_item, names))
        dropdown.setOnItemClickListener { _, _, pos, _ -> show(items[pos]) }

        // Preselect the student's own faculty when signed in.
        val faculties = session.refresh()?.facultyNameList?.split(",", ";")?.map { it.trim() }.orEmpty()
        val own = items.firstOrNull { y -> faculties.any { f -> f.isNotEmpty() && (y.name?.contains(f, true) == true || f.contains(y.name ?: "\u0000", true)) } }
        own?.let { dropdown.setText(it.name, false); show(it) }
    }

    private fun show(item: AcademicYearItem) {
        val url = item.url
        if (url.isNullOrBlank()) { binding.webView.visibility = View.GONE; return }
        binding.webView.visibility = View.VISIBLE
        binding.webView.loadUrl(url)
    }

    override fun onDestroyView() { _binding?.webView?.destroy(); _binding = null; super.onDestroyView() }
}
