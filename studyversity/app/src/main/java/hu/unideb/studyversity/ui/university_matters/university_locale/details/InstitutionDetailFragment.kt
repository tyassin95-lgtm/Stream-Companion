package hu.unideb.studyversity.ui.university_matters.university_locale.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.Institution
import hu.unideb.studyversity.databinding.EconomicListItemBinding
import hu.unideb.studyversity.databinding.InstitutionBusinessHoursItemBinding
import hu.unideb.studyversity.databinding.InstitutionDetailFragmentBinding
import hu.unideb.studyversity.databinding.IntitutionImageSliderItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import hu.unideb.studyversity.utils.image.ImageSlideShowActivity

/** One place: header picture, contact rows, opening hours, gallery and organisations in the building. */
class InstitutionDetailFragment : BaseFragment() {
    private var _binding: InstitutionDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private val images: ListItemAdapter<String> = singleLayoutAdapter<String>(R.layout.intitution_image_slider_item) { v, url, position ->
        val b = IntitutionImageSliderItemBinding.bind(v)
        Glide.with(v).load(url + IMAGE_RESIZE).centerCrop().into(b.sliderImage)
        b.sliderImage.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        v.setOnClickListener {
            startActivity(Intent(requireContext(), ImageSlideShowActivity::class.java)
                .putExtra("imageUrls", images.items.map { it + IMAGE_RESIZE }.toTypedArray())
                .putExtra("position", position))
        }
    }
    private val hours = singleLayoutAdapter<hu.unideb.studyversity.data.model.IdCodeName>(R.layout.institution_business_hours_item) { v, item, _ ->
        val b = InstitutionBusinessHoursItemBinding.bind(v)
        b.dayText.text = item.name
        b.hoursText.text = item.value
    }
    private val units = singleLayoutAdapter<hu.unideb.studyversity.data.model.EconomicUnit>(R.layout.economic_list_item) { v, item, _ ->
        val b = EconomicListItemBinding.bind(v)
        b.unitTitle.text = item.title
        b.unitItems.text = item.formattedSubItems
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InstitutionDetailFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imageRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.imageRv.adapter = images
        binding.businessHoursRv.layoutManager = LinearLayoutManager(requireContext())
        binding.businessHoursRv.adapter = hours
        binding.econmicUnitsRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.econmicUnitsRv.adapter = units
        loadInstitution()
    }

    private fun loadInstitution() = load(retry = ::loadInstitution) {
        val inst = backend.institution(requireArguments().getLong("poiId"))
        if (inst == null || inst.isEmpty) {
            binding.emptyLayout.root.visibility = View.VISIBLE
            binding.instituteDetailLayout.visibility = View.GONE
            return@load
        }
        show(inst.copy(name = requireArguments().getString("localizedName")?.takeIf { it.isNotBlank() } ?: inst.name))
    }

    private fun row(row: View, value: View?, text: String?, set: (String) -> Unit) {
        row.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        if (!text.isNullOrBlank()) set(text)
    }

    private fun show(inst: Institution) {
        binding.emptyLayout.root.visibility = View.GONE
        binding.instituteDetailLayout.visibility = View.VISIBLE
        setScreenTitle(inst.name)
        binding.nameL.text = inst.name

        row(binding.campusRow, binding.campusValue, inst.campus) { binding.campusValue.text = it }
        row(binding.addressRow, binding.addressValue, inst.address) { addr ->
            binding.addressValue.text = addr
            binding.addressValue.setOnClickListener {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(addr)))) }
            }
        }
        row(binding.directPhoneRow, binding.directPhoneValue, inst.directPhone) { binding.directPhoneValue.text = it }
        row(binding.centralPhoneRow, binding.centralPhoneValue, inst.centralDispatcherPhone) { binding.centralPhoneValue.text = it }
        row(binding.websiteRow, binding.websiteValue, inst.webPage) { binding.websiteValue.text = it }

        binding.hoursGroup.visibility = if (inst.businessHoursIsEmpty) View.GONE else View.VISIBLE
        binding.hoursTitle.text = inst.businessHoursTitle
        hours.submit(inst.managedBusinessHours)

        val economic = inst.economicUnits
        binding.economicUnitsHolder.visibility = if (economic.isEmpty()) View.GONE else View.VISIBLE
        units.submit(economic)

        val urls = inst.imageUrls.ifEmpty { inst.imageIds.map { DOCUMENT_URL + it } }
        images.submit(urls)
        binding.imageRv.visibility = if (urls.isEmpty()) View.GONE else View.VISIBLE
        val header = urls.firstOrNull()
        if (header != null) Glide.with(this).load(header + IMAGE_RESIZE).centerCrop().into(binding.headerImage)
        else binding.headerImage.setImageResource(R.drawable.institution_search_logo)
        binding.headerImage.scaleType = if (header != null) android.widget.ImageView.ScaleType.CENTER_CROP else android.widget.ImageView.ScaleType.CENTER_INSIDE

        // The name sits over the lower edge of the picture, the gallery overlaps the picture as well.
        binding.nameL.post {
            _binding?.let { b ->
                var top = -b.nameL.height
                if (urls.isNotEmpty()) top = -(b.nameL.height + (b.imageRv.height / 2 - 50))
                b.instituteDetailLayout.layoutParams = LinearLayout.LayoutParams(-1, -1).also { it.setMargins(0, top, 0, 0) }
            }
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    private companion object {
        const val IMAGE_RESIZE = "?width=600"
        const val DOCUMENT_URL = "https://example.invalid/portal/displayDocument/id/"
    }
}
