# Appointment Booking System

A **general-purpose** appointment scheduling application. Use it for any business: clinics, salons, consultants, tutors, or any service that takes bookings. JavaFX desktop app with configurable branding, appointment types, business hours, and optional H2 database persistence.

---

## Architecture

### Layered Architecture (N-Tier)

```
┌─────────────────────────────────────────────────────────────┐
│  PRESENTATION (JavaFX)                                       │
│  Controllers, FXML, CSS, Toast, Dialogs, Calendar            │
├─────────────────────────────────────────────────────────────┤
│  APPLICATION (Services)                                      │
│  AuthService, BookingService, ScheduleService,               │
│  PermissionService, PolicyEngine, ReportingService           │
├─────────────────────────────────────────────────────────────┤
│  DOMAIN (Entities, Rules, Events, Policies)                  │
│  Appointment, User, TimeSlot, BookingRuleStrategy,           │
│  AppointmentEventPublisher                                   │
├─────────────────────────────────────────────────────────────┤
│  PERSISTENCE (Repositories)                                  │
│  UserRepository, AppointmentRepository (In-Memory impl.)     │
└─────────────────────────────────────────────────────────────┘
```

### UML Class Diagram (Simplified)

```
┌──────────────────┐     ┌─────────────────────┐     ┌────────────────────┐
│      User        │     │    Appointment      │     │     TimeSlot       │
├──────────────────┤     ├─────────────────────┤     ├────────────────────┤
│ id, name, email  │     │ id, patient, slot   │     │ startTime, endTime │
│ password         │     │ status, deleted     │     │ overlapsWith()     │
│ isAdmin()        │     │ markDeleted()       │     └────────────────────┘
└────────┬─────────┘     └──────────┬──────────┘              ▲
         │                          │                         │
         │         ┌────────────────┼────────────────┐        │
         │         │                │                │        │
         ▼         ▼                ▼                ▼        │
┌──────────────────┐  ┌────────────────────┐  ┌──────────────────────┐
│  Administrator   │  │ IndividualAppt     │  │ RecurringAppointment │
│  extends User    │  │ FollowUpAppt       │  │ VirtualAppt          │
└──────────────────┘  │ AssessmentAppt     │  │ InPersonAppt         │
                      │ UrgentAppt, etc.   │  └──────────────────────┘
                      └────────────────────┘

┌─────────────────────┐     ┌──────────────────────┐     ┌────────────────────┐
│   BookingService    │────▶│  PermissionService   │     │   PolicyEngine     │
├─────────────────────┤     ├──────────────────────┤     ├────────────────────┤
│ bookAppointment()   │     │ hasPermission()      │     │ registerPolicy()   │
│ modifyAppointment() │     │ requirePermission()  │     │ evaluate()         │
│ cancelAppointment() │     └──────────────────────┘     └────────────────────┘
└──────────┬──────────┘
           │
           ├──▶ AppointmentEventPublisher ──▶ NotificationService
           ├──▶ ScheduleService (getAvailableSlots)
           ├──▶ ReportingService (per-type, cancellation, peak)
           └──▶ AppointmentExpirationService
```

### Design Patterns

| Pattern | Usage |
|--------|-------|
| **Strategy** | `BookingRuleStrategy` — duration, capacity, working hours, cutoff, follow-up dependency |
| **Observer** | `AppointmentEventPublisher` / `AppointmentEventListener` — event-driven notifications |
| **Policy** | `Policy<T>`, `PolicyEngine` — extensible business rule validation |
| **Repository** | `UserRepository`, `AppointmentRepository` — abstraction over storage |
| **Dependency Injection** | Constructor-based DI in services; `ApplicationContext` holds service/repository references for controllers |
| **Central error handling** | `ErrorHandler` — log + user-facing dialog; I18n for titles/messages |
| **Screen constants** | `ScreenConstants` — FXML names and window titles in one place |

---

## System Features

### Advanced Logic

1. **Authorization & Permissions** — Roles: **Administrator**, **Doctor**, **Receptionist**, **Patient**. Permissions: `BOOK_APPOINTMENT`, `MODIFY_ANY_APPOINTMENT`, `VIEW_REPORTS`, `MANAGE_DOCTORS`, `MANAGE_ROOMS`, `VIEW_ANALYTICS`, etc. `PermissionService` enforces before critical actions.
2. **Login security** — Failed attempt tracking, temporary account lockout after 5 failures (15 min), audit log for `LOGIN_SUCCESS` / `LOGIN_FAILED` / `LOGIN_BLOCKED`.
3. **Doctors & Rooms** — `Doctor` and `Room` entities; appointments can have optional `doctorId` and `roomId`. Rules: no double-booking per doctor/room, max appointments per doctor per day.
4. **Time-Aware Scheduling** — Working hours, booking cutoff, automatic expiration of past appointments.
5. **Recurring Appointments** — Weekly/monthly series, cancel single occurrence or entire series.
6. **Appointment Dependencies** — Follow-up appointments can require prior completed appointments.
7. **Reporting Engine** — Appointments per type, cancellation rate, peak booking hour (dynamic from system data).
8. **Smart Notification Triggers** — Event-driven (create, modify, cancel, reminder) via `AppointmentEventPublisher`.
9. **Slot Recommendation** — Earliest availability, lowest congestion.
10. **Soft Delete & Audit Trail** — `deleted` flag, `AuditEntry` with entity type/ID, old/new values; queryable by user/entity.
11. **Policy Enforcement Layer** — Central `PolicyEngine`, extensible policies.
12. **Business Validation** — No double booking, no modify/cancel of CANCELLED/EXPIRED, valid state transitions.  
13. **Report export** — Daily HTML report via `PdfReportService`.  
14. **i18n** — English/Arabic via `I18n` and `messages_*.properties`.  
15. **Global search** — `GlobalSearchService` over appointments and users.  
16. **App notifications** — `AppNotificationStore` for event-driven in-app notifications.

### Enterprise UI

- **Design system (Navy & Teal):** Corporate theme with navy sidebar (`#1e3a5f`), teal accent for actions and links (`#0d9488`), light content background (`#f1f5f9`), and consistent cards, buttons, and tables. Dark mode supported; high-contrast accessibility overrides available.
- Dashboard with real analytics
- Sidebar navigation, breadcrumbs
- Reports screen with charts/tables
- Audit log screen
- Calendar view (weekly, color-coded)
- Empty states, loading indicators
- Toast notifications, confirmation dialogs
- Light/Dark theme
- **Logout on every window** — Admin/Patient dashboards (sidebar), Book Appointment and Modify Appointment (top app bar). Confirmation dialog and audit log before sign-out.
- Keyboard shortcuts: **Ctrl+F** (focus search), **Ctrl+Enter** (submit booking), **Ctrl+Q** (logout on all authenticated screens)
- **Session timeout:** After inactivity, a dialog offers "Stay logged in" or "Log out". Configurable via `session.timeoutMinutes` and `session.warningMinutes`.
- **Login validation:** Email format check, field-level messages (email required, password required, invalid email). Lockout message when account is temporarily locked.
- **Tooltips** on main actions (sidebar, branch selector, export, logout). **Audit log export** to CSV from the Audit view.

---

## How to Run

### Prerequisites

- JDK 17+
- Maven 3.6+

### Build

```bash
mvn clean compile
```

### Run

```bash
mvn javafx:run
```

Or with the exec plugin:

```bash
mvn exec:java -Dexec.mainClass="com.appointmentscheduler.presentation.MainApp"
```

### Run Tests

```bash
mvn test
```

### Code Coverage (JaCoCo)

```bash
mvn test jacoco:report
```

Report: `target/site/jacoco/index.html`

---

## SonarCloud Setup

### 1) Prepare SonarCloud project

- Create a project in SonarCloud and connect it to your GitHub repository.
- Copy:
  - Organization key
  - Project key

### 2) Update local Sonar config

- Edit `sonar-project.properties` and replace:
  - `YOUR_SONARCLOUD_ORGANIZATION`
  - `YOUR_SONARCLOUD_PROJECT_KEY`

### 3) Add repository secret

- In GitHub repository settings, add secret:
  - `SONAR_TOKEN` = token generated from SonarCloud (My Account -> Security -> Generate Tokens)

### 4) CI analysis (already configured)

- Workflow file: `.github/workflows/sonarcloud.yml`
- Runs on pushes to `main`, `master`, `develop`, and all pull requests.
- Executes tests + JaCoCo + SonarCloud analysis in one pipeline.

### 5) Run Sonar locally (optional)

```bash
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.token=YOUR_SONAR_TOKEN
```

---

## Default Credentials

Seeded when the app starts with **no users** (in-memory mode) or an **empty user table** (see `MainApp.setupDummyData`):

- **Admin:** `admin@admin.com` / `admin123`
- **Customer (patient):** `customer@example.com` / `password123`
- **Provider (doctor role):** `provider@example.com` / `doctor123`
- **Receptionist:** `staff@example.com` / `reception123`

If you imported a SQL dump manually, **logins depend on that file** — use the emails stored in `app_user`, not necessarily the list above.

---

## Configuration

See `src/main/resources/application.properties`:

- `app.name`, `app.version` — application branding
- `business.hourStart`, `business.hourEnd` — working hours (24h)
- `booking.maxDurationMinutes` — max appointment duration
- `booking.cutoffHoursBefore` — minimum hours before appointment to allow booking
- `session.timeoutMinutes`, `session.warningMinutes` — inactivity timeout and warning
- **Database (optional):** `database.enabled=true` to use H2 file persistence; `database.url`, `database.username`, `database.password`. When `false` or if DB init fails, the app falls back to in-memory storage.
- **Customization:** Change `app.name`, `app.brand.name`, `app.brand.tagline` to your business name and tagline. Set `booking.appointmentTypes` to a comma-separated list of service types (e.g. `General,Consultation,Follow-up` or `Haircut,Coloring,Styling`). Adjust `business.hourStart` / `business.hourEnd` for your opening hours.

### Database (H2 – single file)

- One database file: `./data/appointment_booking.mv.db` (created automatically when `database.enabled=true`).
- Schema: users, clinics, doctors, rooms, appointments, audit trail, system settings (legacy installs may still have unused `pending_task` / `waitlist_entry` tables from older migrations). Applied via Flyway from `persistence/database/migration/V1__enterprise_schema.sql`.
- If the database is unavailable at startup, the application falls back to in-memory storage.

---

## Admin Guide (Enterprise)

- **Branch (Clinic) selector:** Use the dropdown at the top to filter by branch. Dashboard stats and Executive KPIs respect the selected branch.
- **Executive KPIs:** On the Overview Dashboard, see Total Appointments, Today, Cancellation Rate (with WARNING/CRITICAL thresholds), and Peak Hour.
- **Backup & Export:** System Settings → Backup & Export. **Export Backup Manifest** saves a summary (counts) to a `.txt` file. **Export Appointments CSV** exports all appointments to CSV.
- **Keyboard shortcuts:** Ctrl+F (focus search), Ctrl+Q (logout).

## Package Structure

```
com.appointmentscheduler
├── application/           AppConfig, ApplicationContext, services (Auth, Booking, Schedule, etc.)
├── domain/
│   ├── authorization/     Permission, Role
│   ├── events/            AppointmentEvent, AppointmentEventPublisher
│   ├── notifiers/         Observer, EmailNotifier, CalendarNotifier
│   ├── policy/            Policy, PolicyResult
│   └── rules/             BookingRuleStrategy implementations
├── persistence/           Repository interfaces; persistence.database (Jdbc*, Flyway)
└── presentation/          MainApp, controllers, FXML, CSS, I18n, ErrorHandler, ScreenConstants
```
