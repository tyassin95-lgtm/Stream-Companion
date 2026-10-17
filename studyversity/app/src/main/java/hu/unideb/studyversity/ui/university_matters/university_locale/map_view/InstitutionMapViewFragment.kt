package hu.unideb.studyversity.ui.university_matters.university_locale.map_view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.InstitutionMapViewFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import kotlin.random.Random

/**
 * OpenStreetMap view (the original used Google Maps + clustering). Argument `poiId` -1 shows every place of the
 * last search, any other value shows that single place. Tapping a marker's bubble opens the detail screen.
 */
class InstitutionMapViewFragment : BaseFragment() {
    private var _binding: InstitutionMapViewFragmentBinding? = null
    private val binding get() = _binding!!
    private val poiId get() = requireArguments().getLong("poiId")
    private val single get() = poiId != -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InstitutionMapViewFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_uni_places)
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(10.0)
        binding.map.controller.setCenter(GeoPoint(47.53333, 21.63333)) // Debrecen
        binding.modeIcon.setImageResource(if (single) R.drawable.ic_compress_arrows else R.drawable.ic_list)
        binding.infoCard.visibility = if (single) View.VISIBLE else View.GONE
        binding.closeMapIcon.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        loadMarkers()
    }

    private fun loadMarkers() = load(retry = ::loadMarkers) {
        if (single) {
            val inst = backend.institution(poiId) ?: return@load
            val name = requireArguments().getString("localizedName")?.takeIf { it.isNotBlank() } ?: inst.name.orEmpty()
            setScreenTitle(name)
            binding.instituteNameTextView.text = name
            binding.textView5.text = inst.address
            inst.location?.let { (lat, lng) ->
                addMarker(GeoPoint(lat, lng), name, poiId)
                binding.map.controller.setZoom(16.0)
                binding.map.controller.setCenter(GeoPoint(lat, lng))
            }
        } else {
            val items = backend.institutions(null, null).filter { it.location != null }
            items.forEach { i ->
                val (lat, lng) = i.location!!
                // Places in the same building are nudged a few metres apart so every marker can be tapped.
                addMarker(GeoPoint(lat + jitter(), lng + jitter()), i.name.orEmpty(), i.poiId, i.categoryName)
            }
            if (items.size > 1) {
                val box = BoundingBox.fromGeoPoints(items.map { GeoPoint(it.location!!.first, it.location!!.second) })
                binding.map.post { _binding?.map?.zoomToBoundingBox(box.increaseByScale(1.3f), false) }
            }
        }
        binding.map.invalidate()
    }

    private fun jitter() = (Random.nextInt(-1, 2)) * 4.5e-5

    private fun addMarker(pos: GeoPoint, name: String, id: Long, category: String? = null) {
        val marker = Marker(binding.map).apply {
            position = pos
            title = name
            subDescription = category
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker)?.mutate()?.also {
                androidx.core.graphics.drawable.DrawableCompat.setTint(it, ContextCompat.getColor(requireContext(), R.color.yellowishOrange))
            }
            setOnMarkerClickListener { m, map ->
                if (m.isInfoWindowShown) {
                    findNavController().navigate(R.id.institutionDetailFragment, bundleOf("poiId" to id, "localizedName" to ""))
                } else m.showInfoWindow()
                true
            }
        }
        binding.map.overlays.add(marker)
    }

    override fun onResume() { super.onResume(); _binding?.map?.onResume() }
    override fun onPause() { _binding?.map?.onPause(); super.onPause() }
    override fun onDestroyView() { _binding?.map?.onDetach(); _binding = null; super.onDestroyView() }
}
