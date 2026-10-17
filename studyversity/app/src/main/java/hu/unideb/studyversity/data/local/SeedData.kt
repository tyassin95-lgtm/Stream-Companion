package hu.unideb.studyversity.data.local

import hu.unideb.studyversity.data.model.*
import java.util.Calendar
import java.util.Date

/**
 * Content served by [LocalBackend] until a real server exists: university information, the phone book, places,
 * library, messages, mood game and community. The student's own records live in [DentistryProgram] / [StudentRecord].
 * Dates of the news and messages are generated relative to [now].
 *
 * Texts come in English and Hungarian; [hu] selects the language.
 */
class SeedData(private val hu: Boolean, private val now: Date = Date()) {

    private fun l(en: String, hu: String) = if (this.hu) hu else en

    // ---- helpers ---------------------------------------------------------------------------------------------
    private fun cal(base: Date = now) = Calendar.getInstance().apply { time = base }

    private fun at(base: Date, dayOffset: Int, hour: Int, minute: Int = 0): Date = cal(base).apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time

    // ---- people ----------------------------------------------------------------------------------------------
    private val trainingName get() = l("Dentistry", "Fogorvos")
    private val facultyName get() = l("Faculty of Dentistry", "Fogorvostudományi Kar")

    /** The student the record describes; changes made by the instructor show up here. */
    fun newStudentRecord() = StudentRecord(
        id = 1, firstName = "Máté", lastName = "Szilágyi", neptun = "DZ7K4M", email = "mate.szilagyi@students.example.org", balance = 0,
        trainingPlanEnd = DentistryProgram.plannedEnd(), exams = DentistryProgram.exams().toMutableList(),
        classes = DentistryProgram.classes(INSTRUCTOR_NAME).toMutableList()
    )

    fun studentUser(r: StudentRecord) = User(
        id = r.id, name = r.fullName, email = r.email, profile = "STUDENT", neptunCode = r.neptun, balance = r.balance,
        studentTrainingNameList = trainingName, facultyNameList = facultyName, lastName = r.lastName, firstName = r.firstName
    )

    fun instructorUser() = User(
        id = 2, name = INSTRUCTOR_NAME, firstName = "Zoltán", lastName = "Kelemen", email = "zoltan.kelemen@staff.example.org",
        profile = "INSTRUCTOR", role = ROLE_INSTRUCTOR,
        facultyNameList = l("Faculty of Medicine – Department of Internal Medicine", "Általános Orvostudományi Kar – Belgyógyászati Klinika")
    )

    fun staffStudent(r: StudentRecord) = StaffStudent(
        id = r.id, firstName = r.firstName, lastName = r.lastName, neptunCode = r.neptun, email = r.email, balance = r.balance,
        trainingName = trainingName, facultyName = facultyName, trainingStart = DentistryProgram.trainingStart(), trainingPlanEnd = r.trainingPlanEnd
    )

    // ---- student data ----------------------------------------------------------------------------------------
    fun trainings(r: StudentRecord): List<StudentTrainingItem> = listOf(
        StudentTrainingItem(
            id = 1, neptunCode = r.neptun, moduleName = trainingName, moduleCode = "DDS-EN",
            facultyName = facultyName, facultyCode = "FOK",
            moduleLevel = l("Undivided master's programme", "Osztatlan mesterképzés"), trainingMode = l("Full-time", "Nappali"),
            financialStatus = l("Self-funded", "Önköltséges"), moduleStatus = l("Active", "Aktív"),
            activeSemesters = "12", passiveSemesters = "1",
            moduleValidFrom = at(DentistryProgram.trainingStart(), -6, 0), moduleValidTo = at(DentistryProgram.plannedEnd(), 62, 0),
            trainingStart = DentistryProgram.trainingStart(), trainingPlanEnd = r.trainingPlanEnd
        )
    )

    fun modules(r: StudentRecord): List<StudentModuleItem> = listOf(StudentModuleItem(id = 1, moduleCode = "DDS-EN", moduleName = DentistryProgram.MODULE, userId = r.id))

    fun courseTypeLabel(code: String?) = when (code) { "L" -> l("Lecture", "Előadás"); "P" -> l("Practice", "Gyakorlat"); else -> code }
    fun examTypeLabel(code: String?) = when (code) {
        "EXAM" -> l("Exam", "Vizsga"); "MARK" -> l("Practical mark", "Gyakorlati jegy"); "FINAL" -> l("Final examination", "Záróvizsga"); else -> code
    }

    fun courses(): List<StudentCourseItem> = DentistryProgram.courses().map { it.copy(courseType = courseTypeLabel(it.courseType)) }

    /** Opened or graded exams only: what the instructor has not opened is invisible to the student. */
    fun exams(r: StudentRecord): List<StudentExamItem> = r.exams.filter { it.opened || it.grade != null }.map { e ->
        StudentExamItem(
            id = e.id, subjectName = e.subjectName, courseCode = e.courseCode, courseType = courseTypeLabel(e.courseType),
            examType = examTypeLabel(e.examType), credit = e.credit?.toString(), semester = e.semester, moduleName = DentistryProgram.MODULE,
            moduleValidFrom = DentistryProgram.trainingStart(), moduleValidTo = r.trainingPlanEnd,
            examDate = e.examDate, examRecordDate = e.recordDate, examValue = e.grade?.toString(),
            examRecordType = e.grade?.let { if (e.examType == "MARK") l("Practical grade", "Gyakorlati jegy") else l("Exam grade", "Vizsgajegy") },
            subjectRequirement = examTypeLabel(e.examType)
        )
    }

    fun timetable(r: StudentRecord): List<TimeTableItem> = r.classes.map { it.copy(courseType = courseTypeLabel(it.courseType)) }

    // ---- university content ----------------------------------------------------------------------------------
    /** The academic calendar is published per faculty; the Faculty of Dentistry's is bundled with the app. */
    fun academicYears(): List<AcademicYearItem> = listOf(
        AcademicYearItem(1, l("Faculty of Dentistry", "Fogorvostudományi Kar"), "file:///android_asset/calendar/dentistry_2026_27.html"),
        AcademicYearItem(2, l("Faculty of Informatics", "Informatikai Kar"), SITE),
        AcademicYearItem(3, l("Faculty of Economics and Business", "Gazdaságtudományi Kar"), SITE),
        AcademicYearItem(4, l("Faculty of Engineering", "Műszaki Kar"), SITE),
        AcademicYearItem(5, l("Faculty of Humanities", "Bölcsészettudományi Kar"), SITE),
        AcademicYearItem(6, l("Faculty of Medicine", "Általános Orvostudományi Kar"), SITE)
    )

    fun scholarships(): List<StudentScholarShipItem> = listOf(
        StudentScholarShipItem(1, l("Study scholarship – application guide", "Tanulmányi ösztöndíj – pályázati útmutató"), SITE),
        StudentScholarShipItem(2, l("Social scholarship – application guide", "Szociális ösztöndíj – pályázati útmutató"), SITE),
        StudentScholarShipItem(3, l("Sports scholarship", "Sportösztöndíj"), SITE)
    )

    private class News(val en: String, val hu: String, val bodyEn: String, val bodyHu: String)

    private val generalNews = listOf(
        News("Open day at the Faculty of Informatics", "Nyílt nap az Informatikai Karon", "Visit the labs, meet lecturers and try out the robots.", "Látogasson el a laborokba, ismerkedjen meg az oktatókkal és próbálja ki a robotokat."),
        News("New opening hours of the library", "A könyvtár új nyitvatartása", "From Monday the reading rooms stay open until 10 pm during the exam period.", "Hétfőtől a vizsgaidőszakban az olvasótermek este 10 óráig tartanak nyitva."),
        News("Registration for the autumn semester", "Regisztráció az őszi félévre", "Registration is open in the Neptun system until the end of next week.", "A regisztráció a Neptun rendszerben a jövő hét végéig lehetséges."),
        News("Career fair on campus", "Állásbörze a campuson", "More than forty companies present their internship programmes.", "Több mint negyven cég mutatja be gyakornoki programjait."),
        News("Erasmus+ application deadline", "Erasmus+ jelentkezési határidő", "Students can apply for study visits abroad until the 15th.", "A külföldi tanulmányutakra 15-ig lehet jelentkezni."),
        News("Student government elections", "Hallgatói önkormányzati választások", "Candidates introduce themselves on the faculty websites.", "A jelöltek a kari weboldalakon mutatkoznak be.")
    )
    private val eventNews = listOf(
        News("Welcome week concert", "Gólyahét koncert", "Live music on the main square, free entrance for students.", "Élő zene a főtéren, hallgatóknak ingyenes belépés."),
        News("Science night", "Tudomány éjszakája", "Experiments, lectures and guided tours across the campus.", "Kísérletek, előadások és tárlatvezetések a campus területén."),
        News("Film club: season opening", "Filmklub: évadnyitó", "Screening and discussion with the director.", "Vetítés és beszélgetés a rendezővel."),
        News("Startup pitch evening", "Startup pitch est", "Student teams present their ideas to local investors.", "Hallgatói csapatok mutatják be ötleteiket helyi befektetőknek.")
    )
    private val sportNews = listOf(
        News("University league: football results", "Egyetemi bajnokság: labdarúgó eredmények", "The faculty teams played the third round last weekend.", "A kari csapatok a múlt hétvégén játszották a harmadik fordulót."),
        News("New fitness classes", "Új fitneszórák", "Yoga and functional training start next Monday.", "A jóga- és funkcionális edzések jövő hétfőn indulnak."),
        News("Swimming pool maintenance", "Uszoda karbantartás", "The pool is closed for two days.", "Az uszoda két napig zárva tart.")
    )
    private val sportEvents = listOf(
        News("Campus run", "Campus futás", "A 5 km fun run, registration at the sports office.", "5 km-es szórakoztató futás, regisztráció a sportirodán."),
        News("Basketball tournament", "Kosárlabda torna", "Teams of five, matches in the evening.", "Ötfős csapatok, esti mérkőzések."),
        News("Climbing wall open evening", "Falmászó nyitott est", "Equipment provided, beginners welcome.", "Felszerelést biztosítunk, kezdőket is várunk.")
    )

    fun rss(channel: NewsChannel): List<RssFeedItem> {
        val list = when (channel) {
            NewsChannel.unidebGeneralNews -> generalNews
            NewsChannel.unidebEvents -> eventNews
            NewsChannel.sportNews -> sportNews
            NewsChannel.sportEvents -> sportEvents
        }
        return list.mapIndexed { i, n ->
            RssFeedItem(
                id = channel.value * 100L + i, title = l(n.en, n.hu), description = "<p>" + l(n.bodyEn, n.bodyHu) + "</p>",
                link = SITE, pubDate = at(now, -(i * 3 + 1), 9), channelIDs = channel.value.toString()
            )
        }
    }

    // ---- phone book -----------------------------------------------------------------------------------------
    private class Person(val name: String, val job: String, val dept: String, val deptEn: String, val room: String)

    private val people = listOf(
        Person("Dr. Kovács Béla", "egyetemi docens", "Informatikai Kar<br/>Alkalmazott Matematika és Valószínűségszámítás Tanszék", "Faculty of Informatics<br/>Department of Applied Mathematics and Probability Theory", "IK 3.12"),
        Person("Szabó Réka", "tanársegéd", "Informatikai Kar<br/>Adattudományi és Vizualizációs Tanszék", "Faculty of Informatics<br/>Department of Data Science and Visualization", "IK 2.04"),
        Person("Dr. Nagy Gergely", "egyetemi tanár", "Informatikai Kar<br/>Számítógéptudományi Tanszék", "Faculty of Informatics<br/>Department of Computer Science", "IK 3.01"),
        Person("Dr. Tóth Katalin", "egyetemi adjunktus", "Informatikai Kar<br/>Információs Technológia Tanszék", "Faculty of Informatics<br/>Department of Information Technology", "IK 2.18"),
        Person("Dr. Horváth Péter", "egyetemi docens", "Informatikai Kar<br/>Számítógéptudományi Tanszék", "Faculty of Informatics<br/>Department of Computer Science", "IK 3.07"),
        Person("Dr. Varga Zsolt", "egyetemi docens", "Informatikai Kar<br/>Információs Technológia Tanszék", "Faculty of Informatics<br/>Department of Information Technology", "IK 2.22"),
        Person("Farkas Judit", "tanulmányi ügyintéző", "Informatikai Kar<br/>Dékáni Hivatal", "Faculty of Informatics<br/>Dean's Office", "IK 0.05"),
        Person("Papp Dániel", "tanulmányi ügyintéző", "Informatikai Kar<br/>Dékáni Hivatal", "Faculty of Informatics<br/>Dean's Office", "IK 0.06"),
        Person("Dr. Molnár Ilona", "dékán", "Informatikai Kar<br/>Dékáni Hivatal", "Faculty of Informatics<br/>Dean's Office", "IK 0.01"),
        Person("Balogh Tamás", "rendszergazda", "Informatikai Kar<br/>Informatikai Központ", "Faculty of Informatics<br/>IT Centre", "IK 1.03"),
        Person("Németh Eszter", "könyvtáros", "Egyetemi és Nemzeti Könyvtár<br/>Olvasószolgálat", "University and National Library<br/>Reader Services", "ENK 1.10"),
        Person("Simon Gábor", "könyvtáros", "Egyetemi és Nemzeti Könyvtár<br/>Katalógusosztály", "University and National Library<br/>Cataloguing Department", "ENK 2.14"),
        Person("Lukács Andrea", "osztályvezető", "Tanulmányi és Vizsgaügyi Osztály", "Study and Examination Office", "Főép. 112"),
        Person("Takács Márk", "ügyintéző", "Tanulmányi és Vizsgaügyi Osztály", "Study and Examination Office", "Főép. 114"),
        Person("Juhász Orsolya", "ügyintéző", "Nemzetközi Kapcsolatok Osztálya<br/>Erasmus Iroda", "International Relations Office<br/>Erasmus Office", "Főép. 208"),
        Person("Oláh Bence", "koordinátor", "Nemzetközi Kapcsolatok Osztálya<br/>Erasmus Iroda", "International Relations Office<br/>Erasmus Office", "Főép. 209"),
        Person("Fehér Nóra", "pályaorientációs tanácsadó", "Karrier Iroda", "Career Office", "Főép. 021"),
        Person("Máté Szilárd", "sportszervező", "Egyetemi Sportközpont", "University Sports Centre", "SK 1.02"),
        Person("Rácz Vivien", "edző", "Egyetemi Sportközpont", "University Sports Centre", "SK 1.08"),
        Person("Dr. Antal Mónika", "egyetemi docens", "Bölcsészettudományi Kar<br/>Angol Nyelvi Tanszék", "Faculty of Humanities<br/>Department of English Linguistics", "BTK 4.11"),
        Person("Dr. Szűcs Attila", "egyetemi tanár", "Természettudományi és Technológiai Kar<br/>Fizikai Intézet", "Faculty of Science and Technology<br/>Institute of Physics", "TTK 2.30"),
        Person("Budai Kinga", "tanársegéd", "Természettudományi és Technológiai Kar<br/>Matematikai Intézet", "Faculty of Science and Technology<br/>Institute of Mathematics", "TTK 3.15"),
        Person("Dr. Kiss Lajos", "egyetemi adjunktus", "Gazdaságtudományi Kar<br/>Vállalkozásfejlesztési Intézet", "Faculty of Economics and Business<br/>Institute of Enterprise Development", "GTK 1.20"),
        Person("Orbán Hanna", "oktatási referens", "Gazdaságtudományi Kar<br/>Dékáni Hivatal", "Faculty of Economics and Business<br/>Dean's Office", "GTK 0.12"),
        Person("Bogdán Ferenc", "gondnok", "Üzemeltetési Igazgatóság", "Facilities Directorate", "Főép. 003"),
        Person("Pap Lilla", "kollégiumi titkár", "Kollégiumi Iroda", "Dormitory Office", "Kollégium 1"),
        Person("Gál Krisztián", "hallgatói önkormányzati elnök", "Hallgatói Önkormányzat", "Student Government", "HÖK iroda"),
        Person("Illés Boglárka", "pszichológus", "Mentálhigiénés Központ", "Mental Health Centre", "MK 2"),
        Person("Vincze Roland", "informatikus", "Informatikai Igazgatóság<br/>Hallgatói Támogatás", "IT Directorate<br/>Student Support", "IG 1.04"),
        Person("Sipos Beáta", "recepciós", "Rektori Hivatal", "Rector's Office", "Főép. 001"),
        Person(INSTRUCTOR_NAME, "egyetemi docens", "Általános Orvostudományi Kar<br/>Belgyógyászati Klinika", "Faculty of Medicine<br/>Department of Internal Medicine", "2 MC")
    )

    private fun emailOf(p: Person) = p.name.removePrefix("Dr. ").lowercase()
        .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ö', 'o').replace('ő', 'o')
        .replace('ú', 'u').replace('ü', 'u').replace('ű', 'u').replace(' ', '.') + "@example.com"

    fun phoneBook(): List<PhoneBookSearchItem> = people.mapIndexed { i, p ->
        val dept = l(p.deptEn, p.dept)
        PhoneBookSearchItem(
            id = i + 1L, name = p.name, formattedDepartmentName = dept,
            contactWithDepartmentName = p.name + ", " + dept.replace("<br/>", ", ")
        )
    }

    fun employee(id: Long): EmployeeDetailItem? {
        val p = people.getOrNull((id - 1).toInt()) ?: return null
        return EmployeeDetailItem(
            id = id, clientContactId = id, name = p.name, departmentName = l(p.deptEn, p.dept).replace("<br/>", " – "),
            jobPosition = p.job, centralPhoneWithExtension = "+36 52 000 000 / <b>${1000 + id}</b>", email = emailOf(p),
            address = "4032 Debrecen, Egyetem tér 1.", addressInBuilding = p.room,
            tudosterUrl = if (id % 3 == 0L) null else SITE, odtProfilUrl = if (id % 4 == 0L) SITE else null,
            website = SITE, ownWebUrl = null
        )
    }

    // ---- places ------------------------------------------------------------------------------------------------
    fun mapCategories(): List<MapCategoryItem> = listOf(
        MapCategoryItem(1, "academic_classes", l("Lecture halls and classrooms", "Előadótermek, tantermek")),
        MapCategoryItem(2, "menza_restaurant", l("Canteens and restaurants", "Menzák, éttermek")),
        MapCategoryItem(3, "university_clubs_bars", l("Clubs and bars", "Klubok, bárok")),
        MapCategoryItem(4, "sport", l("Sports facilities", "Sportlétesítmények")),
        MapCategoryItem(5, "library", l("Libraries", "Könyvtárak")),
        MapCategoryItem(6, "dormitory", l("Dormitories", "Kollégiumok")),
        MapCategoryItem(7, "student_services", l("Student services", "Hallgatói szolgáltatások"))
    )

    private class Place(
        val id: Long, val en: String, val hu: String, val category: String, val address: String,
        val lat: String, val lon: String, val campus: String, val hours: String?, val phone: String
    )

    // coordinates are approximate
    private val places = listOf(
        Place(1, "Main Building", "Főépület", "academic_classes", "4032 Debrecen, Egyetem tér 1.", "47.5472", "21.6240", "Main Campus", "08:00 - 18:00", "+36 52 000 001"),
        Place(2, "Faculty of Informatics", "Informatikai Kar", "academic_classes", "4028 Debrecen, Kassai út 26.", "47.5589", "21.6262", "Kassai Campus", "07:00 - 20:00", "+36 52 000 002"),
        Place(3, "Lecture Hall Block A", "A épület előadói", "academic_classes", "4032 Debrecen, Egyetem tér 1.", "47.5478", "21.6228", "Main Campus", "08:00 - 20:00", "+36 52 000 003"),
        Place(4, "University and National Library", "Egyetemi és Nemzeti Könyvtár", "library", "4032 Debrecen, Egyetem tér 1.", "47.5461", "21.6236", "Main Campus", "08:00 - 22:00", "+36 52 000 004"),
        Place(5, "Science Library", "Természettudományi Könyvtár", "library", "4032 Debrecen, Egyetem tér 1.", "47.5483", "21.6247", "Main Campus", "09:00 - 18:00", "+36 52 000 005"),
        Place(6, "Main Canteen", "Központi Menza", "menza_restaurant", "4032 Debrecen, Egyetem tér 3.", "47.5465", "21.6252", "Main Campus", "11:00 - 15:00", "+36 52 000 006"),
        Place(7, "Kassai Canteen", "Kassai Menza", "menza_restaurant", "4028 Debrecen, Kassai út 24.", "47.5593", "21.6255", "Kassai Campus", "11:00 - 14:30", "+36 52 000 007"),
        Place(8, "Campus Café", "Campus Kávézó", "menza_restaurant", "4032 Debrecen, Egyetem tér 1.", "47.5470", "21.6231", "Main Campus", "08:00 - 17:00", "+36 52 000 008"),
        Place(9, "Student Club Aurora", "Aurora Klub", "university_clubs_bars", "4032 Debrecen, Egyetem sugárút 9.", "47.5509", "21.6212", "Main Campus", "18:00 - 02:00", "+36 52 000 009"),
        Place(10, "Garden Bar", "Kerti Bár", "university_clubs_bars", "4032 Debrecen, Egyetem sugárút 12.", "47.5517", "21.6205", "Main Campus", "16:00 - 00:00", "+36 52 000 010"),
        Place(11, "University Sports Centre", "Egyetemi Sportközpont", "sport", "4032 Debrecen, Nagyerdei krt. 98.", "47.5559", "21.6103", "Great Forest Campus", "06:00 - 22:00", "+36 52 000 011"),
        Place(12, "Swimming Pool", "Uszoda", "sport", "4032 Debrecen, Nagyerdei krt. 100.", "47.5566", "21.6095", "Great Forest Campus", "06:00 - 21:00", "+36 52 000 012"),
        Place(13, "Climbing Wall", "Falmászó", "sport", "4032 Debrecen, Nagyerdei krt. 98.", "47.5554", "21.6110", "Great Forest Campus", "16:00 - 21:00", "+36 52 000 013"),
        Place(14, "Dormitory Campus Hall", "Campus Hall Kollégium", "dormitory", "4032 Debrecen, Kassai út 30.", "47.5611", "21.6280", "Kassai Campus", null, "+36 52 000 014"),
        Place(15, "Student Services Centre", "Hallgatói Szolgáltató Központ", "student_services", "4032 Debrecen, Egyetem tér 1.", "47.5474", "21.6235", "Main Campus", "09:00 - 15:00", "+36 52 000 015"),
        Place(17, "Faculty of Dentistry", "Fogorvostudományi Kar", "academic_classes", "4032 Debrecen, Nagyerdei krt. 98.", "47.5566", "21.6097", "Great Forest Campus", "07:00 - 20:00", "+36 52 000 017"),
        Place(18, "Department of Internal Medicine (2 MC)", "Belgyógyászati Klinika (2 MC)", "academic_classes", "4032 Debrecen, Nagyerdei krt. 98.", DentistryProgram.LAT, DentistryProgram.LON, "Great Forest Campus", "07:00 - 18:00", "+36 52 000 018"),
        Place(16, "Career Office", "Karrier Iroda", "student_services", "4032 Debrecen, Egyetem tér 1.", "47.5473", "21.6238", "Main Campus", "09:00 - 16:00", "+36 52 000 016")
    )

    private val categoryNames get() = mapCategories().associate { it.code to it.name }

    fun institutions(): List<CachedInstitution> = places.map {
        CachedInstitution(
            id = it.id, poiId = it.id, name = l(it.en, it.hu), category = it.category, categoryName = categoryNames[it.category],
            address = it.address, detail = it.campus, latitude = it.lat, longitude = it.lon
        )
    }

    fun institution(poiId: Long): Institution? {
        val p = places.firstOrNull { it.id == poiId } ?: return null
        val days = listOf(
            l("Monday", "Hétfő"), l("Tuesday", "Kedd"), l("Wednesday", "Szerda"), l("Thursday", "Csütörtök"),
            l("Friday", "Péntek"), l("Saturday", "Szombat"), l("Sunday", "Vasárnap")
        )
        val hours = days.mapIndexed { i, d ->
            IdCodeName(code = "day_$i", name = d, value = when {
                p.hours == null -> "N/A"
                i < 5 -> p.hours
                i == 5 && p.category in listOf("sport", "library") -> p.hours
                else -> "N/A"
            })
        }
        val units = if (p.category == "academic_classes" && p.id <= 2) listOf(
            Slide(listOf(IdCodeName(value = l("Dean's Office", "Dékáni Hivatal")), IdCodeName(value = l("Study affairs", "Tanulmányi ügyek")))),
            Slide(listOf(IdCodeName(value = l("Dean's Office", "Dékáni Hivatal")), IdCodeName(value = l("Student services", "Hallgatói szolgáltatások")))),
            Slide(listOf(IdCodeName(value = l("IT Centre", "Informatikai Központ")), IdCodeName(value = l("Help desk", "Ügyfélszolgálat"))))
        ) else emptyList()
        return Institution(
            id = p.id, name = l(p.en, p.hu), campus = p.campus, phone = p.phone, website = SITE, latitude = p.lat, longitude = p.lon,
            categories = listOf(IdCodeName(code = p.category, name = categoryNames[p.category])),
            businessHoursTitle = l("Opening hours", "Nyitvatartás"), businessHours = hours,
            slideshowTitle = l("Organisations in the building", "Szervezeti egységek az épületben"), slideshow = units,
            preDynamicValues = listOf(IdCodeName(code = "institution_address", value = p.address)),
            postDynamicValues = listOf(
                IdCodeName(code = "direct_phone", value = p.phone),
                IdCodeName(code = "central_dispatcher_phone", value = "+36 52 000 000"),
                IdCodeName(code = "webpage", value = SITE)
            )
        )
    }

    // ---- library -----------------------------------------------------------------------------------------------
    private fun location(id: String, en: String, hu: String) = HoldingLocation(id, listOf(LanguageValue("en", en), LanguageValue("hu", hu)))

    private val mainLibrary get() = location("ENK", "University and National Library", "Egyetemi és Nemzeti Könyvtár")
    private val scienceLibrary get() = location("TTK", "Science Library", "Természettudományi Könyvtár")

    private class Book(val author: String, val title: String, val publication: String)

    private val books = listOf(
        Book("Cormen, Thomas H.", "Introduction to Algorithms", "Cambridge, MIT Press, 2009"),
        Book("Knuth, Donald E.", "The Art of Computer Programming", "Boston, Addison-Wesley, 2011"),
        Book("Tanenbaum, Andrew S.", "Modern Operating Systems", "Boston, Pearson, 2015"),
        Book("Tanenbaum, Andrew S.", "Computer Networks", "Boston, Pearson, 2011"),
        Book("Silberschatz, Abraham", "Database System Concepts", "New York, McGraw-Hill, 2019"),
        Book("Russell, Stuart", "Artificial Intelligence: A Modern Approach", "Hoboken, Pearson, 2021"),
        Book("Martin, Robert C.", "Clean Code", "Upper Saddle River, Prentice Hall, 2008"),
        Book("Gamma, Erich", "Design Patterns", "Reading, Addison-Wesley, 1994"),
        Book("Sipser, Michael", "Introduction to the Theory of Computation", "Boston, Cengage, 2013"),
        Book("Strang, Gilbert", "Introduction to Linear Algebra", "Wellesley, Cambridge Press, 2016"),
        Book("Rudin, Walter", "Principles of Mathematical Analysis", "New York, McGraw-Hill, 1976"),
        Book("Feller, William", "An Introduction to Probability Theory", "New York, Wiley, 1968"),
        Book("Hennessy, John L.", "Computer Architecture", "Cambridge, Morgan Kaufmann, 2017"),
        Book("Stallings, William", "Cryptography and Network Security", "Boston, Pearson, 2016"),
        Book("Bishop, Christopher M.", "Pattern Recognition and Machine Learning", "New York, Springer, 2006"),
        Book("Goodfellow, Ian", "Deep Learning", "Cambridge, MIT Press, 2016"),
        Book("Kernighan, Brian W.", "The C Programming Language", "Englewood Cliffs, Prentice Hall, 1988"),
        Book("Bloch, Joshua", "Effective Java", "Boston, Addison-Wesley, 2018"),
        Book("Fowler, Martin", "Refactoring", "Boston, Addison-Wesley, 2018"),
        Book("Kleppmann, Martin", "Designing Data-Intensive Applications", "Sebastopol, O'Reilly, 2017")
    )

    fun librarySearchAll(): List<LibrarySearchItem> = books.mapIndexed { i, b ->
        val copies = 1 + i % 3
        val holdings = (0 until copies).map { c ->
            LibraryBookHoldings(
                uniqueID = "H${i}_$c", issueDate = "20${10 + c}", callNumber = "%03d.%d/%s".format(100 + i * 7, c + 1, b.author.take(3).uppercase()),
                location = if ((i + c) % 2 == 0) mainLibrary else scienceLibrary
            )
        }
        LibrarySearchItem(
            uniqueID = "BIB$i", author = b.author, title = b.title, publication = b.publication, catalogURL = SITE,
            coverImageURL = null, availableItemCount = (copies - (i % 2)).coerceAtLeast(0).toString(), holdings = holdings
        )
    }

    fun loans(): List<LibraryLoanedContainerItem> = listOf(0, 6, 14).mapIndexed { n, i ->
        val b = books[i]
        LibraryLoanedContainerItem(
            bibRecord = LibraryLoanedItem(uniqueID = "BIB$i", author = b.author, title = b.title, publication = b.publication, catalogURL = SITE, library = listOf(mainLibrary)),
            library = if (n % 2 == 0) mainLibrary else scienceLibrary, dueDate = at(now, 6 + n * 9, 0)
        )
    }

    // ---- messages ----------------------------------------------------------------------------------------------
    fun messages(): List<MessageDetail> = listOf(
        MessageDetail(
            id = 1, messageHeader = l("Welcome to Studyversity", "Üdvözöljük a Studyversityben"), messageDate = at(now, -1, 9), readed = false,
            senderType = "SYSTEM", status = "SENT", format = "html",
            messageBody = "<h3>" + l("Welcome!", "Üdvözöljük!") + "</h3><p>" + l(
                "Your timetable, courses, exams and the messages of your instructors are collected here.",
                "Itt találja órarendjét, kurzusait, vizsgáit és oktatói üzeneteit."
            ) + "</p>"
        ),
        MessageDetail(
            id = 2, messageHeader = l("Examination period 2026/27 autumn", "Vizsgaidőszak 2026/27 ősz"), messageDate = at(now, -3, 14), readed = false,
            senderType = "NEPTUN", status = "SENT", format = "html",
            messageBody = "<p>" + l("The examination period of the autumn semester runs from December 14, 2026 to January 29, 2027.",
                "Az őszi félév vizsgaidőszaka 2026. december 14. és 2027. január 29. között tart.") + "</p>"
        ),
        MessageDetail(
            id = 3, messageHeader = l("Library: book due soon", "Könyvtár: közelgő lejárat"), messageDate = at(now, -5, 8), readed = true,
            senderType = "LIBRARY", status = "SENT", format = "html",
            messageBody = "<p>" + l("One of your loans is due within a week.", "Egyik kölcsönzött könyve egy héten belül lejár.") + "</p>"
        ),
        MessageDetail(
            id = 4, messageHeader = l("Campus run – register now", "Campus futás – jelentkezzen most"), messageDate = at(now, -8, 11), readed = true,
            senderType = "PROMOTION", status = "SENT", format = "url", messageBody = SITE,
            attachedDocuments = listOf(DocumentVO(id = 1, name = "campus-run", url = SITE))
        ),
        MessageDetail(
            id = 5, messageHeader = l("Internal Medicine: lecture hall 2 MC", "Belgyógyászat: 2 MC előadó"), messageDate = at(now, -12, 16), readed = true,
            senderType = "NEPTUN", status = "SENT", format = "html",
            messageBody = "<p>" + l("Lectures take place on Wednesdays 10:00-11:00 and practices on Thursdays 15:00-16:00 in 2 MC.",
                "Az előadások szerdánként 10:00-11:00, a gyakorlatok csütörtökönként 15:00-16:00 között vannak a 2 MC teremben.") + "</p>"
        )
    )

    /** E-mails sent by an instructor, shown as messages (ids start at 1000). */
    fun mailAsMessage(m: MailItem, read: Boolean): MessageDetail = MessageDetail(
        id = m.id, messageHeader = m.subject, messageDate = m.sentAt, readed = read, senderType = "INSTRUCTOR", status = "SENT", format = "html",
        messageBody = "<p><b>" + l("From", "Feladó") + ":</b> " + escape(m.from) + "</p><p>" +
            escape(m.body).replace("\r\n", "\n").replace("\n", "<br/>") + "</p>"
    )

    private fun escape(s: String?) = android.text.TextUtils.htmlEncode(s.orEmpty())

    fun moodAlreadyAnswered() = l("You have already told us how you feel today.", "Ma már megadtad a hangulatodat.")

    // ---- mood game ---------------------------------------------------------------------------------------------
    fun quotes(): List<QuoteItem> = if (hu) listOf(
        QuoteItem(1, "A bölcsesség a boldogság kezdete.", "Szókratész"),
        QuoteItem(2, "A hosszú utazás is egyetlen lépéssel kezdődik.", "Lao-ce"),
        QuoteItem(3, "Nem az a szegény, akinek kevese van, hanem az, aki többet kíván.", "Seneca"),
        QuoteItem(4, "Mindannyian ugyanazon ég alatt élünk, de nem mindenkinek ugyanaz a horizontja.", "Konrad Adenauer"),
        QuoteItem(5, "A siker a kis erőfeszítések összessége, amelyeket nap mint nap megismétlünk.", "Robert Collier"),
        QuoteItem(6, "Az élet egyetlen valódi kudarca, ha nem tanulunk a hibáinkból.", "Ismeretlen"),
        QuoteItem(7, "Légy te magad az a változás, amelyet a világban látni szeretnél.", "Mahátma Gandhi"),
        QuoteItem(8, "Aki mer, az nyer.", "Közmondás")
    ) else listOf(
        QuoteItem(1, "Wisdom is the beginning of happiness.", "Socrates"),
        QuoteItem(2, "A journey of a thousand miles begins with a single step.", "Lao Tzu"),
        QuoteItem(3, "It is not the man who has too little, but the man who craves more, that is poor.", "Seneca"),
        QuoteItem(4, "We are all under the same sky, but not everyone has the same horizon.", "Konrad Adenauer"),
        QuoteItem(5, "Success is the sum of small efforts, repeated day in and day out.", "Robert Collier"),
        QuoteItem(6, "The only real mistake is the one from which we learn nothing.", "Unknown"),
        QuoteItem(7, "Be the change that you wish to see in the world.", "Mahatma Gandhi"),
        QuoteItem(8, "Fortune favours the bold.", "Proverb")
    )

    private fun gifts(stage: Int): List<MoodGameGift> {
        val base = stage * 10
        return listOf(
            MoodGameGift(base + 1, l("University notebook", "Egyetemi jegyzetfüzet"), null),
            MoodGameGift(base + 2, l("Canteen voucher", "Menza utalvány"), null),
            MoodGameGift(base + 3, l("Cinema ticket", "Mozijegy"), null)
        )
    }

    fun gameStages(): List<MoodGameStage> = (1..6).map { order ->
        MoodGameStage(id = order, stageOrder = order, targetDays = if (order < 6) STAGE_DAYS else 0, days = if (order < 6) STAGE_DAYS else 0, daysOfMissed = 0, gifts = if (order < 6) gifts(order) else emptyList())
    }

    // ---- community ---------------------------------------------------------------------------------------------
    private fun cat(base: Long, code: String, parentCode: String, parent: Long, vararg names: Pair<String, String>) =
        names.mapIndexed { i, (en, hu) ->
            CategoryItem(
                id = base + i, code = "${code}_${i + 1}", name = l(en, hu), parentCategory = parent, parentCategoryCode = parentCode,
                parentCategoryName = parentCode
            )
        }

    fun categories(): List<CategoryItem> =
        cat(100, "interest", "interests", 1,
            "Sports" to "Sport", "Music" to "Zene", "Programming" to "Programozás", "Reading" to "Olvasás", "Gaming" to "Videojátékok",
            "Travelling" to "Utazás", "Photography" to "Fényképezés", "Cooking" to "Főzés", "Film" to "Film", "Volunteering" to "Önkéntesség",
            "Art" to "Képzőművészet", "Dancing" to "Tánc", "Theatre" to "Színház", "Hiking" to "Túrázás", "Cycling" to "Kerékpározás",
            "Entrepreneurship" to "Vállalkozás", "Science" to "Tudomány", "Board games" to "Társasjátékok", "Fitness" to "Fitnesz", "Languages" to "Nyelvek") +
        cat(200, "faculty", "faculty_names", 2,
            "Faculty of Informatics" to "Informatikai Kar", "Faculty of Economics and Business" to "Gazdaságtudományi Kar",
            "Faculty of Engineering" to "Műszaki Kar", "Faculty of Humanities" to "Bölcsészettudományi Kar",
            "Faculty of Law" to "Állam- és Jogtudományi Kar", "Faculty of Medicine" to "Általános Orvostudományi Kar",
            "Faculty of Pharmacy" to "Gyógyszerésztudományi Kar", "Faculty of Dentistry" to "Fogorvostudományi Kar",
            "Faculty of Health" to "Egészségügyi Kar", "Faculty of Public Health" to "Népegészségügyi Kar",
            "Faculty of Science and Technology" to "Természettudományi és Technológiai Kar",
            "Faculty of Agricultural and Food Sciences and Environmental Management" to "Agrár- és Gazdálkodástudományok Kar",
            "Faculty of Music" to "Zeneművészeti Kar", "Faculty of Child and Special Needs Education" to "Gyermeknevelési és Gyógypedagógiai Kar") +
        cat(300, "language", "languages", 3,
            "English" to "Angol", "Hungarian" to "Magyar", "German" to "Német", "French" to "Francia", "Spanish" to "Spanyol",
            "Italian" to "Olasz", "Russian" to "Orosz", "Chinese" to "Kínai", "Arabic" to "Arab")

    private class Seed(
        val first: String, val last: String, val modules: String, val about: String,
        val interests: List<Long>, val faculties: List<Long>, val languages: List<Long>, val avatar: Int, val links: Boolean
    )

    private val memberSeeds = listOf(
        Seed("Bence", "Kiss", "Computer Science Engineering BSc", "I like building small apps and playing football.", listOf(102, 100, 104), listOf(200), listOf(300, 301), 1, true),
        Seed("Lilla", "Szabó", "Business Informatics MSc", "Always up for a board game evening.", listOf(117, 104, 110), listOf(201, 200), listOf(300, 301, 302), 2, true),
        Seed("Máté", "Tóth", "Mechanical Engineering BSc", "Cycling around Debrecen in all weather.", listOf(114, 100, 106), listOf(202), listOf(301, 300), 3, false),
        Seed("Hanna", "Varga", "English Studies BA", "Books, theatre and travelling.", listOf(103, 112, 105), listOf(203), listOf(300, 301, 303), 4, true),
        Seed("Dániel", "Horváth", "Law", "Debating and volunteering.", listOf(109, 115), listOf(204), listOf(301, 300), 5, false),
        Seed("Nóra", "Molnár", "General Medicine", "Photography enthusiast between lectures.", listOf(106, 113, 117), listOf(205), listOf(301, 300, 304), 6, true),
        Seed("Gergő", "Farkas", "Computer Science Engineering BSc", "Gaming and programming.", listOf(104, 102, 108), listOf(200), listOf(300, 301), 7, false),
        Seed("Eszter", "Papp", "Pharmacy", "Cooking for friends is my thing.", listOf(107, 109, 110), listOf(206), listOf(301, 300, 302), 8, true),
        Seed("Marcell", "Balogh", "Business Administration BA", "Startups, running and coffee.", listOf(115, 118, 100), listOf(201), listOf(300, 301), 1, true),
        Seed("Zsófia", "Németh", "Biology BSc", "Hiking in the Great Forest every weekend.", listOf(113, 116, 106), listOf(210), listOf(301, 300, 303), 2, false),
        Seed("Péter", "Simon", "Computer Science Engineering BSc", "Music production and machine learning.", listOf(101, 102, 116), listOf(200), listOf(300, 301), 3, true),
        Seed("Réka", "Lukács", "Dentistry", "Dancing salsa and learning Spanish.", listOf(111, 119, 105), listOf(207), listOf(301, 300, 304), 4, true),
        Seed("Tamás", "Juhász", "Civil Engineering BSc", "Board games and fitness.", listOf(117, 118, 100), listOf(202), listOf(301, 300), 5, false),
        Seed("Kinga", "Oláh", "Psychology BA", "Film club regular.", listOf(108, 112, 103), listOf(203), listOf(300, 301, 305), 6, true),
        Seed("Ádám", "Fehér", "Computer Science Engineering BSc", "Open-source contributor.", listOf(102, 104, 116), listOf(200), listOf(300, 301), 7, false),
        Seed("Vivien", "Máté", "Nursing", "Volunteering at the hospital.", listOf(109, 118), listOf(208), listOf(301, 300), 8, false),
        Seed("Roland", "Rácz", "Sport Science BSc", "Climbing and cycling.", listOf(100, 114, 118), listOf(210), listOf(301, 300), 1, true),
        Seed("Anita", "Gál", "German Studies BA", "Languages and travelling.", listOf(119, 105, 103), listOf(203), listOf(302, 300, 301), 2, true)
    )

    fun members(): List<CommunityMember> {
        val all = categories().associateBy { it.id }
        return memberSeeds.mapIndexed { i, s ->
            CommunityMember(
                id = 1000L + i, userId = 2000L + i, firstName = s.first, lastName = s.last, description = s.about,
                photoUrl = "res://avatar_${s.avatar}", modules = s.modules,
                facebookLink = if (s.links) "https://www.facebook.com/" else null,
                instagramLink = if (s.links && i % 2 == 0) "https://www.instagram.com/" else null,
                linkedInLink = if (s.links && i % 3 == 0) "https://www.linkedin.com/" else null,
                tiktokLink = if (s.links && i % 4 == 0) "https://www.tiktok.com/" else null,
                interests = s.interests.mapNotNull { all[it] }, faculties = s.faculties.mapNotNull { all[it] },
                preferredLanguages = s.languages.mapNotNull { all[it] }
            )
        }
    }

    companion object {
        /** Reported days needed per mood game level (365 days over five levels, like the original). */
        const val INSTRUCTOR_NAME = "Dr. Kelemen Zoltán"
        const val STAGE_DAYS = 73
        const val SITE = "https://www.unideb.hu/"
    }
}
