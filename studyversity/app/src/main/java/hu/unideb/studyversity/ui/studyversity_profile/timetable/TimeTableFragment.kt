package hu.unideb.studyversity.ui.studyversity_profile.timetable

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.Formats
import hu.unideb.studyversity.data.AuthRequiredException
import hu.unideb.studyversity.data.model.TimeTableItem
import hu.unideb.studyversity.databinding.TimeTableHeaderItemBinding
import hu.unideb.studyversity.databinding.TimeTableListItemBinding
import hu.unideb.studyversity.databinding.TimetableFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.utils.CalendarEvent
import hu.unideb.studyversity.utils.CalendarService
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** Day-by-day class schedule with "show on map" and "add to calendar" actions. */
class TimeTableFragment : BaseFragment() {
    private var _binding: TimetableFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var layoutManager: LinearLayoutManager
    private var raw: List<TimeTableItem> = emptyList()
    private var pendingCalendarAction: (() -> Unit)? = null

    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) pendingCalendarAction?.invoke() else snack(R.string.permission_denied_calendar)
        pendingCalendarAction = null
    }

    private val adapter = ListItemAdapter<TimeTableItem>(
        layoutFor = { item, _ -> if (item.isHeader) R.layout.time_table_header_item else R.layout.time_table_list_item }
    ) { layout, view, item, _ ->
        if (layout == R.layout.time_table_header_item) {
            TimeTableHeaderItemBinding.bind(view).apply {
                dateText.text = Formats.date(item.classStart)
                dayText.text = item.dayFromClassStart
            }
        } else {
            TimeTableListItemBinding.bind(view).apply {
                timeText.text = item.classStartAndEnd
                subjectText.text = item.subjectName
                roomText.text = getString(R.string.class_room, item.roomName)
                markerImageView.visibility = if (item.location == null) View.INVISIBLE else View.VISIBLE
                addToTimeTableImageView.setOnClickListener { withCalendarPermission { saveEventToCalendar(item) } }
                markerImageView.setOnClickListener { openMap(item.location) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TimetableFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_time_table)
        layoutManager = LinearLayoutManager(requireContext())
        binding.trainingList.layoutManager = layoutManager
        binding.trainingList.setHasFixedSize(true)
        binding.trainingList.adapter = adapter
        binding.addToCalendarButton.setOnClickListener { withCalendarPermission { saveAllEventsToCalendar() } }
        loadTimeTable()
    }

    private fun loadTimeTable() = load(retry = ::loadTimeTable) {
        val items = try { backend.timetable() } catch (e: AuthRequiredException) { throw e }
        raw = items
        if (items.isEmpty()) {
            binding.emptyLayout.root.visibility = View.VISIBLE
            binding.trainingList.visibility = View.GONE
            binding.addToCalendarButton.visibility = View.GONE
            return@load
        }
        binding.emptyLayout.root.visibility = View.GONE
        binding.trainingList.visibility = View.VISIBLE
        binding.addToCalendarButton.visibility = View.VISIBLE
        val withHeaders = withDayHeaders(items)
        adapter.submit(withHeaders)
        val target = closestPositionToToday(withHeaders)
        binding.trainingList.postDelayed({ _binding?.let { layoutManager.scrollToPositionWithOffset(target, 0) } }, 150)
    }

    /** One header row (id -1) before the first class of every day, classes sorted by start. */
    private fun withDayHeaders(items: List<TimeTableItem>): List<TimeTableItem> {
        val out = ArrayList<TimeTableItem>()
        var lastDay: String? = null
        items.filter { it.classStart != null }.sortedBy { it.classStart }.forEach { c ->
            val day = Formats.date(c.classStart)
            if (day != lastDay) { out.add(TimeTableItem(id = -1L, classStart = c.classStart)); lastDay = day }
            out.add(c)
        }
        return out
    }

    /** Position of the day header closest before now (the list opens on "today"). */
    private fun closestPositionToToday(list: List<TimeTableItem>): Int {
        if (list.isEmpty()) return 0
        val now = Date().time
        var best = 0
        var bestDiff = 10000000000000L
        list.filter { it.isHeader }.forEach { h ->
            val diff = now - h.classStart!!.time
            if (diff in 1 until bestDiff) best = list.indexOf(h)
            bestDiff = diff
        }
        return best
    }

    private fun withCalendarPermission(action: () -> Unit) {
        val perms = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        if (perms.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }) action()
        else { pendingCalendarAction = action; calendarPermission.launch(perms) }
    }

    private fun saveAllEventsToCalendar() = load {
        val ctx = requireContext()
        val service = CalendarService(ctx)
        val name = "DE " + (raw.firstOrNull()?.semester ?: "")
        service.getCalendars().firstOrNull { it.displayName == name }?.let { service.deleteCalendar(it.id) }
        service.addCalendar(name)
        raw.forEach { c ->
            val start = c.classStart ?: return@forEach
            service.addEvent(CalendarEvent(
                start, c.classEnd ?: start,
                ctx.getString(R.string.formatted_subject_name_for_calendar, c.subjectName, c.subjectCode),
                ctx.getString(R.string.formatted_description_for_calendar, c.instructorName, c.roomName),
                c.roomName
            ))
        }
        prefs.allEventsSavedToCalendar = true
        snack(R.string.timetable_saved_to_calendar)
    }

    private fun saveEventToCalendar(item: TimeTableItem) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, item.classStart?.time)
            put(CalendarContract.Events.DTEND, item.classEnd?.time)
            put(CalendarContract.Events.TITLE, getString(R.string.formatted_subject_name, item.subjectCode, item.subjectName, item.courseType))
            put(CalendarContract.Events.DESCRIPTION, getString(R.string.formatted_description_for_calendar, item.instructorName, item.roomName))
            put(CalendarContract.Events.CALENDAR_ID, 1L)
            put(CalendarContract.Events.EVENT_TIMEZONE, "Europe/Budapest")
            put(CalendarContract.Events.EVENT_LOCATION, item.roomName)
        }
        runCatching { requireContext().contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) }
            .onSuccess { snack(R.string.event_saved_to_calendar) }
            .onFailure { snack(R.string.unknown_error) }
    }

    private fun openMap(location: Pair<Double, Double>?) {
        if (location == null) {
            Toast.makeText(requireContext(), R.string.item_has_no_location_data, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${location.first},${location.second}"))
        try { startActivity(intent) } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.not_avalible_navigation_software_error_msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
