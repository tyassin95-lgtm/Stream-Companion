package hu.unideb.studyversity.ui.university_matters.phonebook.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.EmployeeDetailItem
import hu.unideb.studyversity.databinding.EmployeeDetailFragmentBinding
import hu.unideb.studyversity.databinding.EmployeeDetailHeaderItemBinding
import hu.unideb.studyversity.databinding.EmployeeDetailItemBinding
import hu.unideb.studyversity.databinding.EmployeeDetailProfileButtonsItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter

/** One employee: name + job title header, contact card, and profile link buttons. */
class EmployeeDetailFragment : BaseFragment() {
    private var _binding: EmployeeDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private val adapter = ListItemAdapter<EmployeeDetailItem>(
        layoutFor = { item, _ ->
            when {
                item.id == -1L -> R.layout.employee_detail_header_item
                item.id == -100L -> R.layout.employee_detail_profile_buttons_item
                else -> R.layout.employee_detail_item
            }
        }
    ) { layout, view, item, _ ->
        when (layout) {
            R.layout.employee_detail_header_item -> EmployeeDetailHeaderItemBinding.bind(view).jobPositionText.text = item.jobPosition
            R.layout.employee_detail_profile_buttons_item -> bindButtons(EmployeeDetailProfileButtonsItemBinding.bind(view), item)
            else -> bindDetails(EmployeeDetailItemBinding.bind(view), item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = EmployeeDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_phone_book)
        binding.employeeDetails.layoutManager = LinearLayoutManager(requireContext())
        binding.employeeDetails.adapter = adapter
        loadEmployee()
    }

    private fun loadEmployee() = load(retry = ::loadEmployee) {
        val employee = backend.employeeDetail(requireArguments().getLong("phoneBookId")) ?: return@load
        binding.nameTv.text = employee.name
        adapter.submit(listOf(
            EmployeeDetailItem(id = -1, clientContactId = -1, name = employee.name, jobPosition = employee.jobPosition),
            employee,
            EmployeeDetailItem(
                id = -100, clientContactId = -100, tudosterUrl = employee.tudosterUrl, odtProfilUrl = employee.odtProfilUrl,
                website = employee.website, ownWebUrl = employee.ownWebUrl
            )
        ))
    }

    private fun html(s: String?) = Html.fromHtml(s ?: "", Html.FROM_HTML_MODE_LEGACY)

    private fun bindDetails(b: EmployeeDetailItemBinding, e: EmployeeDetailItem) {
        b.departmentValue.text = e.departmentName
        fun row(row: View, value: CharSequence?, text: (CharSequence) -> Unit) {
            row.visibility = if (value.isNullOrBlank()) View.GONE else View.VISIBLE
            if (!value.isNullOrBlank()) text(value)
        }
        row(b.phoneRow, e.centralPhoneWithExtension) { b.phoneValue.text = html(e.centralPhoneWithExtension) }
        row(b.emailRow, e.email) { b.emailValue.text = e.email }
        row(b.addressRow, e.address) {
            b.addressValue.text = e.address
            // Tapping the address opens it in a maps app.
            b.addressValue.setOnClickListener {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(e.address)))) }
            }
        }
        row(b.buildingRow, e.addressInBuilding) { b.buildingValue.text = html(e.addressInBuilding) }
    }

    private fun bindButtons(b: EmployeeDetailProfileButtonsItemBinding, e: EmployeeDetailItem) {
        fun link(button: View, url: String?) {
            button.visibility = if (url.isNullOrBlank()) View.GONE else View.VISIBLE
            button.setOnClickListener { url?.let { openWebPage(it) } }
        }
        link(b.tudosterButton, e.tudosterUrl)
        link(b.odtButton, e.odtProfilUrl)
        link(b.websiteButton, e.website)
        link(b.ownWebButton, e.ownWebUrl)
    }

    private fun openWebPage(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
