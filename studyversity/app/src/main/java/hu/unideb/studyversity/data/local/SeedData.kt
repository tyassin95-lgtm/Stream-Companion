package hu.unideb.studyversity.data.local

import hu.unideb.studyversity.data.model.*
import java.util.Calendar
import java.util.Date

/**
 * Demo content served by [LocalBackend] until a real server exists. Everything here is invented sample data
 * (names, phone numbers, grades, books ...); only the structure follows the original REST payloads.
 * Dates are generated relative to [now] so the timetable and exams always look current.
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

    private fun mondayOfThisWeek(): Date = cal().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        val dow = (get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
        add(Calendar.DAY_OF_YEAR, -dow)
    }.time

    /** "2025/26/1" style semester ids; [back] = 0 current semester, 1 previous ... */
    fun semester(back: Int): String {
        val c = cal()
        val month = c.get(Calendar.MONTH)
        val year = c.get(Calendar.YEAR)
        // autumn term: September - January, spring term: February - August
        var term: Int
        var startYear: Int
        when {
            month >= Calendar.SEPTEMBER -> { term = 1; startYear = year }
            month == Calendar.JANUARY -> { term = 1; startYear = year - 1 }
            else -> { term = 2; startYear = year - 1 }
        }
        repeat(back) { if (term == 2) term = 1 else { term = 2; startYear -= 1 } }
        return "$startYear/${(startYear + 1) % 100}/$term"
    }

    // ---- student ---------------------------------------------------------------------------------------------
    fun user(neptun: String) = User(
        id = 1, name = "Minta Anna", firstName = "Anna", lastName = "Minta", email = "anna.minta@example.com",
        profile = "STUDENT", neptunCode = neptun.uppercase(), balance = 25000,
        studentTrainingNameList = trainingName, facultyNameList = l("Faculty of Informatics", "Informatikai Kar")
    )

    private val trainingName get() = l("Computer Science Engineering BSc", "Mérnökinformatikus BSc")

    fun trainings(neptun: String): List<StudentTrainingItem> = listOf(
        StudentTrainingItem(
            id = 1, neptunCode = neptun.uppercase(), moduleName = trainingName, moduleCode = "BMI-BSC",
            facultyName = l("Faculty of Informatics", "Informatikai Kar"), facultyCode = "IK",
            moduleLevel = l("Bachelor (BSc)", "Alapképzés (BSc)"), trainingMode = l("Full-time", "Nappali"),
            financialStatus = l("State-funded", "Állami ösztöndíjas"), moduleStatus = l("Active", "Aktív"),
            activeSemesters = "5", passiveSemesters = "0",
            moduleValidFrom = at(now, -365 * 3, 0), moduleValidTo = at(now, 365 * 2, 0),
            trainingStart = at(now, -365 * 2 - 90, 0), trainingPlanEnd = at(now, 365 + 150, 0)
        ),
        StudentTrainingItem(
            id = 2, neptunCode = neptun.uppercase(), moduleName = l("Business Informatics MSc", "Gazdaságinformatikus MSc"), moduleCode = "GI-MSC",
            facultyName = l("Faculty of Economics and Business", "Gazdaságtudományi Kar"), facultyCode = "GTK",
            moduleLevel = l("Master (MSc)", "Mesterképzés (MSc)"), trainingMode = l("Part-time", "Levelező"),
            financialStatus = l("Self-funded", "Önköltséges"), moduleStatus = l("Passive", "Passzív"),
            activeSemesters = "1", passiveSemesters = "1",
            moduleValidFrom = at(now, -365, 0), moduleValidTo = at(now, 365 * 3, 0),
            trainingStart = at(now, -400, 0), trainingPlanEnd = at(now, 365 * 2, 0)
        )
    )

    fun modules(): List<StudentModuleItem> = listOf(
        StudentModuleItem(id = 1, moduleCode = "BMI-BSC", moduleName = trainingName, userId = 1),
        StudentModuleItem(id = 2, moduleCode = "GI-MSC", moduleName = l("Business Informatics MSc", "Gazdaságinformatikus MSc"), userId = 1)
    )

    private class Subject(val code: String, val en: String, val hu: String, val credit: Int)

    /** Subjects per semester, index 0 = first semester ... 4 = the current (fifth) one. */
    private val curriculum: List<List<Subject>> = listOf(
        listOf(
            Subject("IBN1101", "Calculus I", "Analízis I", 5), Subject("IBN1102", "Discrete Mathematics", "Diszkrét matematika", 4),
            Subject("IBN1103", "Programming Fundamentals", "Programozás alapjai", 6), Subject("IBN1104", "Computer Architecture", "Számítógép-architektúrák", 4),
            Subject("IBN1105", "Academic Skills", "Tanulásmódszertan", 2)
        ),
        listOf(
            Subject("IBN1201", "Calculus II", "Analízis II", 5), Subject("IBN1202", "Data Structures and Algorithms", "Adatszerkezetek és algoritmusok", 6),
            Subject("IBN1203", "Object-Oriented Programming", "Objektumorientált programozás", 6), Subject("IBN1204", "Linear Algebra", "Lineáris algebra", 4),
            Subject("IBN1205", "Digital Technology", "Digitális technika", 4)
        ),
        listOf(
            Subject("IBN2101", "Probability and Statistics", "Valószínűségszámítás és statisztika", 4), Subject("IBN2102", "Databases", "Adatbázisok", 6),
            Subject("IBN2103", "Operating Systems", "Operációs rendszerek", 5), Subject("IBN2104", "Computer Networks", "Számítógép-hálózatok", 5),
            Subject("IBN2105", "Software Engineering I", "Szoftvertechnológia I", 4)
        ),
        listOf(
            Subject("IBN2201", "Software Engineering II", "Szoftvertechnológia II", 4), Subject("IBN2202", "Web Technologies", "Webtechnológiák", 5),
            Subject("IBN2203", "Artificial Intelligence", "Mesterséges intelligencia", 5), Subject("IBN2204", "Formal Languages", "Formális nyelvek", 4),
            Subject("IBN2205", "Computer Graphics", "Számítógépes grafika", 4)
        ),
        listOf(
            Subject("IBN3101", "Machine Learning", "Gépi tanulás", 5), Subject("IBN3102", "Mobile Application Development", "Mobilalkalmazás-fejlesztés", 5),
            Subject("IBN3103", "Distributed Systems", "Elosztott rendszerek", 5), Subject("IBN3104", "Information Security", "Informatikai biztonság", 4),
            Subject("IBN3105", "Thesis Seminar", "Szakdolgozati szeminárium", 3)
        )
    )

    private fun semesterOfIndex(i: Int) = semester(curriculum.size - 1 - i)

    fun courses(): List<StudentCourseItem> {
        var id = 1L
        return curriculum.flatMapIndexed { si, subjects ->
            subjects.flatMap { s ->
                listOf(l("Lecture", "Előadás") to "L", l("Practice", "Gyakorlat") to "G").map { (type, suffix) ->
                    StudentCourseItem(
                        id = id++, subjectName = l(s.en, s.hu), courseCode = "${s.code}-$suffix", courseType = type,
                        credit = if (suffix == "L") s.credit.toString() else "0", semester = semesterOfIndex(si), moduleName = trainingName
                    )
                }
            }
        }
    }

    fun exams(): List<StudentExamItem> {
        val grades = listOf("5", "4", "5", "3", "4", "5", "4", "3", "5", "4")
        var id = 1L
        var g = 0
        return curriculum.flatMapIndexed { si, subjects ->
            val current = si == curriculum.size - 1
            subjects.mapIndexed { i, s ->
                val examDate = if (current) at(now, 10 + i * 3, 9 + (i % 3) * 2) else at(now, -(curriculum.size - 1 - si) * 180 + 20 + i * 2, 10)
                StudentExamItem(
                    id = id++, subjectName = l(s.en, s.hu), courseCode = "${s.code}-L", courseType = l("Lecture", "Előadás"),
                    examType = if (i == 4) l("Term mark", "Évközi jegy") else l("Written exam", "Írásbeli vizsga"),
                    credit = s.credit.toString(), semester = semesterOfIndex(si), moduleName = trainingName,
                    moduleValidFrom = at(now, -365 * 3, 0), moduleValidTo = at(now, 365 * 2, 0),
                    examDate = examDate,
                    examValue = if (current) null else grades[g++ % grades.size],
                    examRecordDate = if (current) null else examDate,
                    examRecordType = if (current) null else l("Exam grade", "Vizsgajegy"),
                    subjectRequirement = if (i == 4) l("Term mark", "Évközi jegy") else l("Exam", "Vizsga")
                )
            }
        }
    }

    // ---- timetable -------------------------------------------------------------------------------------------
    private class Slot(val day: Int, val h: Int, val m: Int, val subject: Int, val type: String, val room: String, val teacher: String, val lat: String, val lon: String)

    fun timetable(): List<TimeTableItem> {
        val current = curriculum.last()
        val ik = "47.5589" to "21.6262" // approximate positions of the sample buildings
        val main = "47.5472" to "21.6240"
        val slots = listOf(
            Slot(0, 8, 0, 0, "L", "IK 101", "Dr. Kovács Béla", ik.first, ik.second),
            Slot(0, 10, 0, 1, "G", "IK 204", "Szabó Réka", ik.first, ik.second),
            Slot(1, 12, 0, 2, "L", "IK 103", "Dr. Nagy Gergely", ik.first, ik.second),
            Slot(2, 8, 0, 3, "L", "Aula A", "Dr. Tóth Katalin", main.first, main.second),
            Slot(2, 14, 0, 4, "G", "IK 301", "Dr. Horváth Péter", ik.first, ik.second),
            Slot(3, 10, 0, 1, "L", "IK 101", "Dr. Varga Zsolt", ik.first, ik.second),
            Slot(4, 8, 0, 0, "G", "IK 208", "Szabó Réka", ik.first, ik.second)
        )
        var id = 1L
        val monday = mondayOfThisWeek()
        val sem = semester(0)
        val out = ArrayList<TimeTableItem>()
        for (week in -1..2) for (s in slots) {
            val subject = current[s.subject]
            val start = at(monday, week * 7 + s.day, s.h, s.m)
            val end = cal(start).apply { add(Calendar.MINUTE, 90) }.time
            out.add(TimeTableItem(
                id = id++, subjectName = l(subject.en, subject.hu), subjectCode = subject.code + "-" + s.type,
                roomName = s.room, roomCode = s.room.replace(" ", ""),
                courseType = if (s.type == "L") l("Lecture", "Előadás") else l("Practice", "Gyakorlat"),
                classStart = start, classEnd = end, semester = sem, instructorName = s.teacher, latitude = s.lat, longitude = s.lon
            ))
        }
        return out
    }

    // ---- university content ----------------------------------------------------------------------------------
    /** The academic calendar is published per faculty. */
    fun academicYears(): List<AcademicYearItem> = listOf(
        AcademicYearItem(1, l("Faculty of Informatics", "Informatikai Kar"), SITE),
        AcademicYearItem(2, l("Faculty of Economics and Business", "Gazdaságtudományi Kar"), SITE),
        AcademicYearItem(3, l("Faculty of Engineering", "Műszaki Kar"), SITE),
        AcademicYearItem(4, l("Faculty of Humanities", "Bölcsészettudományi Kar"), SITE),
        AcademicYearItem(5, l("Faculty of Medicine", "Általános Orvostudományi Kar"), SITE)
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
        Person("Sipos Beáta", "recepciós", "Rektori Hivatal", "Rector's Office", "Főép. 001")
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
                "This is a sample message of the offline demo. Real notifications will appear here when the server is connected.",
                "Ez egy minta üzenet az offline demóból. A valódi értesítések a szerver csatlakoztatása után jelennek meg itt."
            ) + "</p>"
        ),
        MessageDetail(
            id = 2, messageHeader = l("Exam registration reminder", "Vizsgajelentkezési emlékeztető"), messageDate = at(now, -3, 14), readed = false,
            senderType = "NEPTUN", status = "SENT", format = "html",
            messageBody = "<p>" + l("Exam registration closes in two days. Please check your exam list.", "A vizsgajelentkezés két nap múlva lezárul. Kérjük, ellenőrizze vizsgalistáját.") + "</p>"
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
            id = 5, messageHeader = l("Timetable change", "Órarendi változás"), messageDate = at(now, -12, 16), readed = true,
            senderType = "NEPTUN", status = "SENT", format = "html",
            messageBody = "<p>" + l("Thursday's lecture moves to room IK 103.", "A csütörtöki előadás az IK 103-as terembe kerül.") + "</p>"
        )
    )

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
        const val STAGE_DAYS = 73
        const val SITE = "https://www.unideb.hu/"
    }
}
