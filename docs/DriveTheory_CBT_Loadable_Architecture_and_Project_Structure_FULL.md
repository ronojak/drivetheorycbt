# DriveTheory CBT  
## Loadable Architecture, Full Development Roadmap & Android Project Structure

This document is intended to be **dropped into a repo** (e.g., `/docs/`) and used for:
- Developer onboarding
- Architecture reference
- Implementation planning
- QA and release planning

---

# 1. Product Summary

## 1.1 Product Name
**DriveTheory CBT**

## 1.2 Platform
- **Android** (offline-first)

## 1.3 Purpose
DriveTheory CBT is an Android Computer-Based Testing (CBT) application for running **randomized driving theory exams**. It starts with an initial driving school theory bank and scales to hold **many more questions** across topics, schools, and versions.

---

# 2. Core Product Capabilities

- Run **timed** multiple-choice exams (MCQ)
- Randomize:
  - Question order
  - Answer option order
  - Question subset per attempt
- Offline-first:
  - Works fully offline
  - Autosaves in-progress exams
- Scoring:
  - Instant results
  - Pass/fail
  - Review mode (optional)
- Progress tracking:
  - Attempt history
  - Best score
  - Average score
- Security (phase-based):
  - Screenshot blocking during exam mode
  - Encrypted local DB (optional)
- Expansion-ready:
  - Cloud sync + admin portal (future phase)

---

# 3. Full Development Roadmap (12-Week Plan)

## Phase 0 — Discovery & Planning (Week 1)
**Objectives**
- Confirm exam rules (time limit, pass mark, question count)
- Validate initial question bank format and structure

**Deliverables**
- Approved PRD
- Seed question data standard (JSON or prepackaged DB)
- Final roadmap sign-off

**Exit Criteria**
- No unresolved scope blockers

---

## Phase 1 — UX/UI Design (Week 2)
**Objectives**
- Exam-focused, low-distraction UI
- Works well on low-end devices

**Deliverables**
- Wireframes:
  - Splash
  - Home
  - Exam Setup
  - Exam (timer + navigation)
  - Results
  - Review Answers
  - History/Progress
  - Settings
- Component inventory (buttons, cards, timer chip)
- Accessibility plan (font size, contrast, screen reader)

**Exit Criteria**
- UX approved for implementation

---

## Phase 2 — Database & Data Layer (Week 3)
**Objectives**
- Offline-first scalable storage using Room/SQLite

**Deliverables**
- ER model + schema
- Room entities + DAOs
- Seed import pipeline (JSON → Room) OR prepackaged DB

**Exit Criteria**
- Database tested with 1,000+ questions and fast query performance

---

## Phase 3 — Core Exam Engine (Weeks 4–5)
**Objectives**
- Implement CBT engine with robust randomization and autosave

**Deliverables**
- Exam session orchestration
- Randomization (Fisher–Yates)
- Anti-repeat policy
- Timer controller (lifecycle-safe)
- Scoring rules and attempt persistence
- Unit tests (engine + randomization + scoring)

**Exit Criteria**
- Full exam lifecycle works without UI

---

## Phase 4 — Android App Development (Weeks 6–7)
**Objectives**
- Build Android app using Kotlin + Compose + MVVM

**Deliverables**
- Navigation graph
- Screens + ViewModels
- Data bindings and state handling (StateFlow)
- Offline exam flow end-to-end

**Exit Criteria**
- App functional on Android 8+ with stable performance

---

## Phase 5 — Scoring, Analytics & Progress (Week 8)
**Objectives**
- Insightful performance stats and review UX

**Deliverables**
- Attempt history
- Trend metrics (avg, best, recent)
- Optional: weak-topic detection by category

**Exit Criteria**
- Analytics computed correctly from attempts

---

## Phase 6 — Security & Exam Integrity (Week 9)
**Objectives**
- Reduce cheating and protect question bank

**Deliverables**
- Screenshot blocking in exam mode
- Back-navigation and interruption handling
- Optional: encrypted DB using SQLCipher

**Exit Criteria**
- Exam cannot be trivially harvested

---

## Phase 7 — Testing & QA (Weeks 10–11)
**Objectives**
- Production stability

**Deliverables**
- Unit + integration + UI tests
- Stress tests (large question bank)
- Offline/online switching tests
- Accessibility tests

**Exit Criteria**
- No critical/high severity bugs remaining

---

## Phase 8 — Deployment & Release (Week 12)
**Objectives**
- Release to Play Store

**Deliverables**
- Signed AAB
- Store listing assets
- Privacy policy + basic support email
- Release notes

**Exit Criteria**
- App available to users and installs cleanly

---

## Post-Launch (Ongoing)
- Patch releases, crash fixes, performance tuning
- Feedback-driven iteration

---

# 4. Android Project Folder Structure (Kotlin + Compose + MVVM + Room)

> **Package name example:** `com.drivetheory.cbt`  
> Structure separates **engine** and **domain** so core logic is testable without UI.

```text
DriveTheoryCbt/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ gradle/
├─ README.md
├─ docs/
│  ├─ PRD.md
│  ├─ ROADMAP.md
│  ├─ ERD.png
│  ├─ API_OPENAPI.yaml
│  └─ WIREFRAMES/
│
├─ app/
│  ├─ build.gradle.kts
│  ├─ proguard-rules.pro
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml
│     │  ├─ assets/
│     │  │  ├─ db/
│     │  │  │  └─ drivetheory_seed.db         # optional prepackaged db
│     │  │  └─ seed/
│     │  │     ├─ questions_seed.json         # alternative seeding method
│     │  │     └─ exams_seed.json
│     │  │
│     │  ├─ res/
│     │  │  ├─ drawable/
│     │  │  ├─ mipmap-*/
│     │  │  ├─ values/
│     │  │  ├─ values-night/
│     │  │  └─ xml/
│     │  │
│     │  └─ java/com/drivetheory/cbt/
│     │     ├─ DriveTheoryApp.kt               # Application class
│     │     ├─ di/                             # Dependency Injection (Hilt/Koin)
│     │     │  ├─ AppModule.kt
│     │     │  ├─ DatabaseModule.kt
│     │     │  ├─ NetworkModule.kt             # future
│     │     │  └─ RepositoryModule.kt
│     │     │
│     │     ├─ core/
│     │     │  ├─ constants/
│     │     │  │  ├─ AppConstants.kt
│     │     │  │  └─ DbConstants.kt
│     │     │  ├─ common/
│     │     │  │  ├─ Result.kt                 # Success/Error wrapper
│     │     │  │  ├─ UiState.kt                # Loading/Success/Error
│     │     │  │  └─ Extensions.kt
│     │     │  ├─ security/
│     │     │  │  ├─ DbEncryption.kt           # SQLCipher hooks (optional)
│     │     │  │  └─ SecurePrefs.kt
│     │     │  ├─ utils/
│     │     │  │  ├─ DateTimeUtils.kt
│     │     │  │  ├─ RandomUtils.kt
│     │     │  │  ├─ NetworkMonitor.kt
│     │     │  │  └─ Logger.kt
│     │     │  └─ validators/
│     │     │     ├─ InputValidators.kt
│     │     │     └─ ExamRulesValidator.kt
│     │     │
│     │     ├─ data/
│     │     │  ├─ local/
│     │     │  │  ├─ db/
│     │     │  │  │  ├─ AppDatabase.kt
│     │     │  │  │  ├─ migrations/
│     │     │  │  │  │  ├─ Migration_1_2.kt
│     │     │  │  │  │  └─ Migration_2_3.kt
│     │     │  │  │  └─ typeconverters/
│     │     │  │  │     └─ Converters.kt
│     │     │  │  ├─ dao/
│     │     │  │  │  ├─ QuestionDao.kt
│     │     │  │  │  ├─ AnswerDao.kt
│     │     │  │  │  ├─ ExamDao.kt
│     │     │  │  │  ├─ AttemptDao.kt
│     │     │  │  │  └─ AttemptAnswerDao.kt
│     │     │  │  ├─ entities/
│     │     │  │  │  ├─ QuestionEntity.kt
│     │     │  │  │  ├─ AnswerEntity.kt
│     │     │  │  │  ├─ ExamEntity.kt
│     │     │  │  │  ├─ ExamAttemptEntity.kt
│     │     │  │  │  └─ AttemptAnswerEntity.kt
│     │     │  │  └─ seed/
│     │     │  │     ├─ SeedImporter.kt        # import JSON → Room
│     │     │  │     └─ SeedValidator.kt
│     │     │  ├─ remote/                      # Phase 2+ cloud sync
│     │     │  │  ├─ api/
│     │     │  │  │  ├─ ApiService.kt
│     │     │  │  │  └─ Endpoints.kt
│     │     │  │  ├─ dto/
│     │     │  │  │  ├─ QuestionDto.kt
│     │     │  │  │  ├─ ExamDto.kt
│     │     │  │  │  └─ SyncPayloadDto.kt
│     │     │  │  └─ sync/
│     │     │  │     ├─ SyncManager.kt
│     │     │  │     └─ ConflictResolver.kt
│     │     │  ├─ mappers/
│     │     │  │  ├─ QuestionMapper.kt         # Entity ↔ Domain ↔ DTO
│     │     │  │  ├─ ExamMapper.kt
│     │     │  │  └─ AttemptMapper.kt
│     │     │  └─ repository/
│     │     │     ├─ QuestionRepositoryImpl.kt
│     │     │     ├─ ExamRepositoryImpl.kt
│     │     │     └─ AttemptRepositoryImpl.kt
│     │     │
│     │     ├─ domain/
│     │     │  ├─ model/
│     │     │  │  ├─ Question.kt
│     │     │  │  ├─ Answer.kt
│     │     │  │  ├─ Exam.kt
│     │     │  │  ├─ ExamAttempt.kt
│     │     │  │  └─ AttemptAnswer.kt
│     │     │  ├─ repository/
│     │     │  │  ├─ QuestionRepository.kt
│     │     │  │  ├─ ExamRepository.kt
│     │     │  │  └─ AttemptRepository.kt
│     │     │  └─ usecase/
│     │     │     ├─ GenerateExamUseCase.kt
│     │     │     ├─ GetExamConfigUseCase.kt
│     │     │     ├─ SubmitExamUseCase.kt
│     │     │     ├─ GetProgressStatsUseCase.kt
│     │     │     ├─ GetAttemptHistoryUseCase.kt
│     │     │     └─ ImportSeedDataUseCase.kt
│     │     │
│     │     ├─ engine/                          # Exam engine (core logic)
│     │     │  ├─ ExamEngine.kt                 # orchestrates session
│     │     │  ├─ ExamSession.kt                # state model
│     │     │  ├─ Randomizer.kt                 # question + answer shuffle
│     │     │  ├─ TimerController.kt            # count down + lifecycle safe
│     │     │  ├─ Scoring.kt                    # scoring rules
│     │     │  └─ AntiRepeatPolicy.kt           # prevents immediate repeats
│     │     │
│     │     ├─ presentation/
│     │     │  ├─ navigation/
│     │     │  │  ├─ Routes.kt
│     │     │  │  ├─ NavGraph.kt
│     │     │  │  └─ DeepLinks.kt
│     │     │  ├─ theme/
│     │     │  │  ├─ Color.kt
│     │     │  │  ├─ Type.kt
│     │     │  │  ├─ Shape.kt
│     │     │  │  └─ Theme.kt
│     │     │  ├─ components/
│     │     │  │  ├─ AppTopBar.kt
│     │     │  │  ├─ PrimaryButton.kt
│     │     │  │  ├─ LoadingView.kt
│     │     │  │  ├─ ErrorView.kt
│     │     │  │  ├─ TimerChip.kt
│     │     │  │  └─ QuestionCard.kt
│     │     │  ├─ screens/
│     │     │  │  ├─ splash/   (SplashScreen.kt, SplashViewModel.kt)
│     │     │  │  ├─ home/     (HomeScreen.kt, HomeViewModel.kt)
│     │     │  │  ├─ exam_setup/ (ExamSetupScreen.kt, ExamSetupViewModel.kt)
│     │     │  │  ├─ exam/     (ExamScreen.kt, ExamViewModel.kt)
│     │     │  │  ├─ results/  (ResultsScreen.kt, ResultsViewModel.kt)
│     │     │  │  ├─ review/   (ReviewScreen.kt, ReviewViewModel.kt)
│     │     │  │  ├─ history/  (HistoryScreen.kt, HistoryViewModel.kt)
│     │     │  │  └─ settings/ (SettingsScreen.kt, SettingsViewModel.kt)
│     │     │  └─ state/
│     │     │     ├─ ExamUiState.kt
│     │     │     ├─ HomeUiState.kt
│     │     │     └─ ProgressUiState.kt
│     │     │
│     │     ├─ workers/                         # background tasks
│     │     │  ├─ SeedImportWorker.kt            # first-run import
│     │     │  ├─ SyncWorker.kt                  # future cloud sync
│     │     │  └─ CleanupWorker.kt               # prune old attempts
│     │     │
│     │     └─ testing/
│     │        ├─ fakes/
│     │        └─ testdata/
│     │
│     ├─ androidTest/
│     └─ test/
│
└─ buildSrc/                                     # optional: central deps
   ├─ Versions.kt
   └─ Dependencies.kt
```

---

# 5. Database Model (Local – Room / SQLite)

## 5.1 Tables

### `questions`
```sql
id INTEGER PRIMARY KEY
question_text TEXT
difficulty INTEGER
category TEXT
is_active BOOLEAN
```

### `answers`
```sql
id INTEGER PRIMARY KEY
question_id INTEGER
answer_text TEXT
is_correct BOOLEAN
```

### `exams`
```sql
id INTEGER PRIMARY KEY
exam_name TEXT
time_limit INTEGER
pass_mark INTEGER
```

### `exam_attempts`
```sql
id INTEGER PRIMARY KEY
exam_id INTEGER
score INTEGER
total_questions INTEGER
timestamp DATETIME
```

### `attempt_answers`
```sql
id INTEGER PRIMARY KEY
attempt_id INTEGER
question_id INTEGER
selected_answer_id INTEGER
is_correct BOOLEAN
```

---

# 6. Randomization Strategy (Implementation Notes)

- Use **Fisher–Yates shuffle** for unbiased shuffling
- Random question subset per attempt:
  - Filter `is_active = true`
  - Optional category filter
  - Apply anti-repeat policy using recent attempts
- Shuffle answer options per question and store mapping for scoring

---

# 7. Offline-First Strategy

- Preload initial DB (prepackaged DB or JSON import on first run)
- Entire exam flow works offline
- Answers and attempts saved immediately
- Future: background sync using WorkManager

---

# 8. Security Measures (Phase-Based)

- Disable screenshots during exam (`FLAG_SECURE`)
- Optional: encrypted DB with SQLCipher
- Handle backgrounding for “official exam mode” (future)

---

# END OF DOCUMENT
