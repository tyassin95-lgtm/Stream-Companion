package hu.unideb.studyversity.data.local

import hu.unideb.studyversity.data.model.StaffExam
import hu.unideb.studyversity.data.model.TimeTableItem
import java.util.Calendar
import java.util.Date

/**
 * The student's curriculum: Dentistry (English programme), started in autumn 2020. Subject names of the first
 * semester of each year follow the 2026/27 timetable of the Faculty of Dentistry; the credits, the spring semesters
 * and the grades are filled in to complete the record. One course is left: Internal Medicine, taken in the autumn
 * semester of 2026/27 (Wednesday lecture, Thursday practice, weeks 1-14 of the 2026/27 calendar).
 *
 * The spring semesters follow the 2025/26 spring timetable (Internal Medicine, Monday, was taken in 4th-year spring too):
 * the student was passive in autumn 2025, took Internal Medicine in spring 2026 without finishing it and repeats it now.
 *
 * Course and exam types are stored as codes (`L`/`P`, `EXAM`/`MARK`/`FINAL`) so they can be shown in either language.
 */
object DentistryProgram {
    const val CURRENT_SEMESTER = "2026/27/1"
    const val INTERNAL_MEDICINE = "DEN4105"
    const val FINAL_EXAM = "DENFINAL"


    private class Subject(val name: String, val credit: Int, val practical: Boolean = false)
    private class Term(val year: Int, val sem: Int, val subjects: List<Subject>) {
        /** "2020/21/1": year 1 started in 2020 */
        val label get() = "${START_YEAR + year - 1}/${(START_YEAR + year) % 100}/$sem"
        fun code(i: Int) = "DEN$year$sem" + (i + 1).toString().padStart(2, '0')
    }

    private const val START_YEAR = 2020

    private fun s(name: String, credit: Int, practical: Boolean = false) = Subject(name, credit, practical)

    private val terms = listOf(
        Term(1, 1, listOf(s("Anatomy I", 6), s("Histology and Embryology I", 4), s("Medical Chemistry", 5), s("Biophysics", 4), s("Biostatistics", 3),
            s("Odontology I", 3, true), s("Hungarian Language I", 2, true), s("Latin Language", 2, true), s("Communication Skills", 2, true),
            s("Library Skills", 1, true), s("Basics of Behavioural Sciences", 2))),
        Term(1, 2, listOf(s("Cell Biology", 4), s("Medical Chemistry II", 5), s("Anatomy II", 6), s("Histology and Embryology II", 4),
            s("Hungarian Language II", 2, true), s("Latin Language II", 2, true), s("Computer Science", 2, true), s("Introduction to Dentistry", 2),
            s("First Aid and Reanimation", 2, true), s("Preventive Dentistry I", 2))),
        Term(2, 1, listOf(s("Physiology I", 6), s("Biochemistry I", 6), s("Anatomy III (Neuroanatomy)", 4), s("Dental Materials I", 3),
            s("Hungarian Language III", 2, true))),
        Term(2, 2, listOf(s("Biochemistry II", 5), s("Dental Physiology", 5), s("Basic in Dental Propedeutics", 4, true),
            s("Introduction to the Fixed Prosthodontics", 3, true), s("Neurobiological Anatomy", 3), s("Hungarian Language IV", 2, true))),
        Term(3, 1, listOf(s("Clinical Biochemistry I", 3), s("Propedeutics and Technology of Removable Dentures", 5, true), s("Odontotechnology I", 4, true),
            s("Pathology I", 5), s("Immunology", 3), s("Cariology Propedeutics", 4, true), s("Medical Hungarian I", 2, true), s("Microbiology I", 4),
            s("Periodontology Propedeutics", 3, true), s("Oral Biology", 3), s("Dosimetry and Radiation Health Effects", 2), s("Medical Anthropology", 2))),
        Term(3, 2, listOf(s("Oral Surgery Propedeutics", 3), s("Introduction to Prosthodontics IV (Propedeutics of Fixed Prosthodontics)", 4, true),
            s("Restorative Dentistry Propedeutics II (Endodontics)", 4, true), s("Odontotechnology II", 4, true), s("Clinical Biochemistry II", 2),
            s("Pathology II", 5), s("Histopathology", 2, true), s("Radiology", 2), s("Medical Psychology", 2), s("Medical Sociology", 2),
            s("Bioethics", 2), s("Basic Surgical Techniques", 3, true), s("Periodontology Propedeutics II", 3, true), s("Oral Pathology", 3),
            s("Dental Radiology", 3), s("Medical Hungarian II", 2, true), s("Clinicopathologic and Oralpathologic Cases", 2))),
        Term(4, 1, listOf(s("Surgery", 4), s("Otolaryngology", 3), s("Complex Dentistry I", 4), s("Pharmacology I", 4), s("Internal Medicine", 6),
            s("Preventive Medicine", 3), s("Preventive Dentistry II", 3), s("Oral Surgery I", 3), s("Periodontology I", 3), s("Prosthetic Dentistry I", 4, true),
            s("Cariology I", 4, true), s("Orthodontics I", 3), s("Behavioural Medicine", 2), s("Dermatology", 3), s("Dental Practice I", 4, true),
            s("FOK Elective Course", 2))),
        Term(4, 2, listOf(s("Emergency Medicine", 3), s("Oral Surgery II", 3), s("Prosthetic Dentistry II", 4), s("Restorative Dentistry II (Endodontics)", 4),
            s("Periodontology II", 3), s("Orthodontics II", 3), s("Complex Dentistry II", 4), s("Pharmacology II", 4),
            s("Pediatric Dentistry Propedeutics", 3), s("Digital Dentistry", 3, true), s("Dental Practice II", 4, true))),
        Term(5, 1, listOf(s("Dental Practice III", 8, true), s("Oral Surgery III", 3), s("Prosthetic Dentistry III", 3), s("Pediatric Dentistry I", 3),
            s("Restorative Dentistry III (Cariology and Endodontics)", 3), s("Oral Medicine", 3), s("Pediatrics", 3), s("Forensic Medicine", 3),
            s("Neurology", 3), s("Complex Dentistry III", 3), s("Psychiatry", 3))),
        Term(5, 2, listOf(s("Oral Surgery IV", 3), s("Prosthetic Dentistry IV", 3), s("Pediatric Dentistry II", 3),
            s("Restorative Dentistry IV (Cariology and Endodontics)", 3), s("Periodontology III", 3), s("Praxis Management", 2),
            s("Complex Dentistry IV", 4), s("Dental Elective Courses", 2), s("Dental Practice IV", 8, true)))
    )

    // ---- derived data ------------------------------------------------------------------------------------------------
    private fun isPending(term: Term, i: Int) = term.code(i) == INTERNAL_MEDICINE

    private fun date(y: Int, m: Int, d: Int, hour: Int = 0): Date =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, hour, 0, 0) }.time

    private fun weekday(c: Calendar): Calendar {
        when (c.get(Calendar.DAY_OF_WEEK)) { Calendar.SATURDAY -> c.add(Calendar.DAY_OF_YEAR, 2); Calendar.SUNDAY -> c.add(Calendar.DAY_OF_YEAR, 1) }
        return c
    }

    /** Autumn: exam period December 14 - January 29, spring: May 24 - July 9 (same pattern as the 2026/27 calendar). */
    private fun examDate(term: Term, i: Int, hash: Int): Date {
        val y0 = START_YEAR + term.year - 1
        val start = if (term.sem == 1) date(y0, 12, 14, 9) else date(y0 + 1, 5, 24, 9)
        val c = Calendar.getInstance().apply { time = start; add(Calendar.DAY_OF_YEAR, (i * 3 + hash % 4) % 38); add(Calendar.HOUR_OF_DAY, i % 5) }
        return weekday(c).time
    }

    private fun grade(code: String): Int = when ((code.hashCode() and 0x7fffffff) % 10) { in 0..2 -> 3; in 3..6 -> 4; else -> 5 }

    /** Every graded exam of the finished semesters (grades 3-5) and the two not yet open ones. */
    fun exams(): List<StaffExam> {
        var id = 1L
        val out = ArrayList<StaffExam>()
        terms.forEach { term ->
            term.subjects.forEachIndexed { i, sub ->
                if (isPending(term, i)) return@forEachIndexed
                val code = term.code(i)
                val h = code.hashCode() and 0x7fffffff
                val d = examDate(term, i, h)
                out.add(StaffExam(
                    id = id++, courseCode = "$code-L", subjectName = sub.name, courseType = "L", semester = term.label, credit = sub.credit,
                    examType = if (sub.practical) "MARK" else "EXAM", examDate = d, grade = grade(code), recordDate = d, opened = true
                ))
            }
        }
        // Internal Medicine: the one course left; the exam is opened by the instructor
        out.add(StaffExam(id = id++, courseCode = "$INTERNAL_MEDICINE-L", subjectName = "Internal Medicine", courseType = "L", semester = CURRENT_SEMESTER, credit = 6, examType = "EXAM"))
        out.add(StaffExam(id = id, courseCode = "$FINAL_EXAM-W", subjectName = "Final Examination (written)", courseType = "L", semester = "2026/27/2", credit = 0, examType = "FINAL"))
        return out
    }

    /** Course list entries (lecture + practice per subject) of all finished semesters plus the current Internal Medicine. */
    fun courses(): List<hu.unideb.studyversity.data.model.StudentCourseItem> {
        var id = 1L
        val out = ArrayList<hu.unideb.studyversity.data.model.StudentCourseItem>()
        fun add(code: String, name: String, credit: Int, sem: String) {
            out.add(hu.unideb.studyversity.data.model.StudentCourseItem(id++, name, "$code-L", "L", credit.toString(), sem, MODULE))
            out.add(hu.unideb.studyversity.data.model.StudentCourseItem(id++, name, "$code-P", "P", "0", sem, MODULE))
        }
        terms.forEach { term -> term.subjects.forEachIndexed { i, sub -> if (!isPending(term, i)) add(term.code(i), sub.name, sub.credit, term.label) } }
        add(INTERNAL_MEDICINE, "Internal Medicine", 6, "2025/26/2")
        add(INTERNAL_MEDICINE, "Internal Medicine", 6, CURRENT_SEMESTER)
        return out
    }

    /** Name of the single module (programme) the courses belong to. */
    const val MODULE = "Dentistry"

    /**
     * The classes of the remaining course: spring 2025/26 Monday 11-13 lecture and 14-16 practice in 2 MC (from the 2025/26 spring
     * timetable; semester dates inferred: February 9 - May 22, 2026 with a spring break in the week of April 6), and autumn 2026/27
     * Wednesday 10-11 lecture and Thursday 15-16 practice in 2 MC, weeks 1-14 (September 7 - December 11, 2026).
     */
    fun classes(instructor: String): List<TimeTableItem> {
        fun monday(y: Int, m: Int, d: Int) = Calendar.getInstance().apply { clear(); set(y, m - 1, d) }
        val out = ArrayList<TimeTableItem>()
        fun add(base: Calendar, week: Int, dayOffset: Int, from: Int, to: Int, type: String, sem: String) {
            fun at(hour: Int): Date = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, week * 7 + dayOffset); set(Calendar.HOUR_OF_DAY, hour) }.time
            out.add(TimeTableItem(
                id = 0, subjectName = "Internal Medicine", subjectCode = "$INTERNAL_MEDICINE-$type", roomName = "2 MC", roomCode = "2MC", courseType = type,
                classStart = at(from), classEnd = at(to), semester = sem, instructorName = instructor, latitude = LAT, longitude = LON
            ))
        }
        val spring = monday(2026, 2, 9)
        for (w in 0 until 15) {
            if (w == 8) continue // spring break: week of April 6
            add(spring, w, 0, 11, 13, "L", "2025/26/2"); add(spring, w, 0, 14, 16, "P", "2025/26/2")
        }
        val autumn = monday(2026, 9, 7)
        for (w in 0 until 14) {
            add(autumn, w, 2, 10, 11, "L", CURRENT_SEMESTER); add(autumn, w, 3, 15, 16, "P", CURRENT_SEMESTER)
        }
        return out.sortedBy { it.classStart }.mapIndexed { i, c -> c.copy(id = i + 1L) }
    }

    // approximate position of the Clinical Centre, Nagyerdei krt. 98
    const val LAT = "47.5571"
    const val LON = "21.6104"

    /** Planned end of the training: the final examination is on June 14, 2027. */
    fun plannedEnd(): Date = date(2027, 6, 30)
    fun trainingStart(): Date = date(2020, 9, 7)
}
