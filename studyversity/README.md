# Studyversity (offline rebuild)

A rebuild of the Android app **UD Studyversity** (`hu.unideb.studyversity`, v1.3.3, build 70, University of Debrecen)
that runs **without a backend**. All server data comes from an on-device stand-in (`LocalBackend`) with demo content,
behind a single `Backend` interface, so the real server can be plugged in later without touching the UI.

* Ready-made APK: [`dist/Studyversity-offline-1.3.3.apk`](dist/Studyversity-offline-1.3.3.apk)
  (package `hu.unideb.studyversity.reborn`, so it installs next to the original; signed with the Android **debug** key – re-sign it for distribution)
* Screenshots: [`docs/screenshots`](docs/screenshots) (rendered by the test suite, see *Testing*)

> **Rights.** The layouts, icons, illustrations, strings and the app name were recovered from the original APK and belong to the
> University of Debrecen / the original developers. Confirm that you may reuse them before publishing anything built from this project.
> No credentials of the original were taken over: the OAuth client, the Firebase configuration and the Google Maps key were left out.

## What the original app is

| Aspect | Finding |
|---|---|
| Language / UI | Kotlin, XML layouts with Data Binding, Navigation component (single `MainActivity` + fragments), Material Components, Paris, Glide |
| Architecture | MVVM; Dagger 2 DI; RxJava 2 (`Single`/`Observable`) in repositories; Room cache; Retrofit 2 + Moshi |
| Platform libs | Firebase Messaging/Analytics (push), AppAuth (OpenID Connect against a Hydra server), Google Maps, Dexter, WorkManager |
| Backend | "Abydos" REST platform, 13 service interfaces, every answer wrapped in `RestResponse{data,startRow,endRow,totalRows,status,errorCode,errorMessage}`; list endpoints take Abydos *AdvancedCriteria* |
| Features | Neptun student data (profile, time table, courses, exams), university info (academic calendar, scholarships, news/events RSS, phone book, places + map, library loans & catalogue, sport, DEXPO), messages/push, settings, contact, **Happy Challenge** mood game, **UD Community** |

## Architecture of the rebuild

```
app/src/main/java/hu/unideb/studyversity
├── StudyversityApp.kt            ServiceLocator + osmdroid setup
├── core/                         ServiceLocator (manual DI), AppPreferences, SessionManager, Formats
├── data/
│   ├── Backend.kt                THE seam: every server call the UI needs (suspend functions)
│   ├── model/Models.kt           value objects, field names = wire names of the original API
│   ├── local/                    LocalBackend (+ persisted state) and SeedData (demo content)
│   └── remote/                   ApiService (Retrofit contract), RestResponse + request bodies, RemoteBackend, BackendConfig
├── ui/
│   ├── base/                     BaseFragment/BaseActivity (loading, errors, login redirect), ListItemAdapter, PagedSearch
│   ├── home, main, splash, user/login
│   ├── studyversity_profile/     profile, major details, time table, courses, exams
│   ├── university_matters/       HAK, news/events, phone book, places (list/detail/map), library, sport, DEXPO
│   ├── messages, contact, settings, webview
│   ├── happy_challenge/          "How is your day?" sheet, package, questionnaire; MoodCommons = rules of the game
│   ├── mood/                     statistics / rewards / history tabs, level-up, game ended, form
│   └── community/                member list, member detail, self profile, registration wizard, chip selector
└── utils/                        dialogs, image loading, calendar export, custom views
```

* **Resources are the original ones**: ~130 layouts, drawables, fonts, menus, colours, styles and all string translations were recovered with
  apktool, tree-shaken to the app-owned set and reused as they are, so screens look like the original.
* **Data Binding was replaced by View Binding.** The binding expressions only exist as compiled code in the APK; they were re-implemented in Kotlin screen by screen.
* **Dagger/Rx/Room were replaced** by `ServiceLocator`, coroutines and (for the local backend) SharedPreferences/JSON.
* `BaseFragment.load { }` runs a call, shows the loading overlay, turns `AuthRequiredException` into the login screen and other errors into a snackbar with *Retry*.

### The local backend

`LocalBackend` implements `Backend` completely:

* **Content** (`SeedData`): demo student "Minta Anna" with two trainings, a time table relative to today, 5 semesters of courses and exams, news/event/sport feeds,
  30 phone-book entries, 16 places around Debrecen (approximate coordinates), library catalogue and loans, messages, quotes, community categories and 18 students.
  Everything is invented sample data; texts exist in English and Hungarian.
* **Behaviour** (state persisted as JSON in SharedPreferences):
  * personal data needs a login (`AuthRequiredException` otherwise); **any non-empty Neptun code + password is accepted** (offline demo)
  * one mood per day; the mood game has 5 levels of 73 reported days (365 days) and a 6th "game ended" stage; finished levels offer 3 rewards
    (choosing one is an addition – the original did it on a web page)
  * community suggestions rank students by shared interests (×2), faculties and languages; shared categories are highlighted
  * messages become read when opened; profile pictures are stored in app storage
* The demo student starts with 20 reported days in level 2, no community profile (so the wizard shows up) and unread messages.

### Rules ported from the original (`MoodCommons`, `MoodFlow`)
Game over = stage 6 with no next stage; level-up when the stage number rose since the last stored game; questionnaire once after 10 identical moods in a row;
the "add mood" sheet is shown unless skipped today or already answered; the logout item is visible only when signed in, the mood item only when Happy Challenge is on.

## Connecting a real backend

1. Implement the endpoints below (or reuse the original deployment – `ApiService` mirrors it).
2. Set the URLs in `data/remote/BackendConfig.kt` (`API_BASE_URL`, `DOCUMENT_BASE_URL`, `QUOTES_URL`) and flip `USE_REMOTE = true`.
3. Replace `RemoteBackend.login` by the real authentication (the original used OIDC code flow + PKCE through AppAuth; `RemoteBackend` currently expects `POST user/login` to return a bearer token).
4. `RemoteBackend` is **not tested against a server** – it is the wiring, written from the decompiled contract. Expect to adjust field mappings and criteria.
5. Push notifications (Firebase) were not ported; `Backend.registerDevice` is in place for it.

### API contract (`data/remote/ApiService.kt`)
All paths relative to `API_BASE_URL`; answers are `RestResponse<T>` with the rows in `data`; authenticated calls send `Authorization: Bearer …`.

| Area | Endpoint | Notes |
|---|---|---|
| User | `POST user/login` (form `userName`,`password`), `GET user/me` | |
| Neptun | `GET neptun/studentFinancialAccount`, `listStudentTraining`, `listStudentTimetable`, `listStudentCourse`, `listStudentExam`, `listStudentModule` | |
| Documents | `GET srest/documentListWithTag?tag=`, `GET document/get/{id}`, `POST upload/file` (multipart) | academic calendar / scholarships by tag |
| RSS | `POST` list of channel ids (119 news, 120 events, 121 sport news, 122 sport events) | |
| Phone book | `GET phonebook/searchEmployee?startRow&endRow&name`, `GET phonebook/employeeDetail?id` | paging: first 20 rows, then 10 |
| Places | `GET category/getCategoryListWithParents?parentCodes=`, `POST map/institution/search` (AdvancedCriteria), `GET map/institution/get/{id}` | categories: `academic_classes`, `menza_restaurant`, `university_clubs_bars`, `sport`, … |
| Library | `GET corvina/loanedBooks`, `GET corvina/search?startRow&endRow&term` | |
| Messages | `POST studyversityMessage/myMessageList/`, `GET studyversityMessage/get/{id}?deviceId`, `POST studyversityMessage/registerDevice` | |
| Preferences | `POST studyversityPreferences/search`, `/add`, `/update` | snake_case flags |
| Mood | `POST mood/add?newMode`, `GET mood/list`, `GET mood/getSummaryForInterval?fromDate&toDate`, `GET mood/getPoints` | server rejects a 2nd mood per day |
| Mood game | `GET studentMoodGame/getMyCurrentGame`, `GET moodGameStage/list` | no game → empty result ⇒ `NoMoodGameException`; *added:* `POST studentMoodGame/chooseGift`, `POST studentMoodGame/register` |
| Community | `GET studyversityCommunity/getMyProfile`, `GET …/get/{id}`, `POST …/add`, `…/update/`, `…/remove/{id}`, `…/searchMyInterest`, `…/searchMySuggestions`, `…/addToMyInterest/{id}`, `…/removeFromMyInterests/{id}` | |

The models in `data/model/Models.kt` list every field the UI reads.

## Building and testing

```
./gradlew :app:assembleDebug            # app/build/outputs/apk/debug
./gradlew :app:assembleRelease          # signed with the debug key
./gradlew :app:testDebugUnitTest        # 44 tests, renders screenshots into app/build/screenshots
```
Needs JDK 17 and an Android SDK (`sdk.dir` in `local.properties`, which is not committed), compileSdk 35, minSdk 24.

**Testing limits – please read.** The build environment has no emulator or device (no KVM), so the app was **never run on a real Android system**.
What was done instead:

* `LocalBackendTest` – game rules, login gating, paging, persistence, community ranking.
* `ScreenSmokeTest` – under Robolectric every activity is started (signed in and out), every navigation destination is opened with the arguments its caller passes,
  every entry of the home menu is clicked (English and Hungarian); a screen fails when it throws or ends up showing an error snackbar.
* `FlowTest` – sign-in, the whole registration wizard (validation per page → profile saved), chip selector, mood of the day, profile edit/delete, marking students as interesting, list contents.
* `ScreenshotTest` – draws screens to PNG; the screenshots in `docs/screenshots` come from it (Robolectric's renderer: fonts and some effects differ from a phone).

Not covered: real devices/OEM quirks, animations and gestures, WebView content, the osmdroid map tiles (need network), calendar export, photo picker, orientation/process-death handling, accessibility.

## Differences from the original

* No backend, no Firebase/push, no AppAuth: sign-in is a simple form accepted offline (`LoginActivity` keeps the original `RESULT_LOGGED_IN` contract).
* Map: **OpenStreetMap via osmdroid** instead of Google Maps (no API key needed); markers get a small jitter like the original to separate stacked pins.
* Mood game registration: a native "join" dialog instead of the original web page; choosing a level reward is in the app (extension).
* Questionnaire URLs were fixed to the ones in the original strings; web content (privacy policy, DEXPO, e-learning …) still opens the original university URLs from the string resources.
* Omitted as unreachable/dead in the original: calendar and prize screens, the legacy summary pager, mood FAQ pager (the FAQ cards open the web pages as before), Dexter permission helper, crash/analytics SDKs.
* The application id is `hu.unideb.studyversity.reborn`.
