# Project layout (`com.appointmentscheduler`)

Layered structure keeps UI, business rules, and persistence separate.

| Layer | Package | Responsibility |
|--------|---------|------------------|
| **Presentation** | `com.appointmentscheduler.presentation` | JavaFX screens (`MainApp`, `*Controller`), FXML, CSS, `I18n`, dialogs, notifications. Controllers call **ApplicationContext** for services (no direct `new` for repositories). |
| **Application** | `com.appointmentscheduler.application` | Use cases: `BookingService`, `ScheduleService`, `ReportingService`, `AuthService`, config (`AppConfig`), and the service locator `ApplicationContext` wired from `MainApp`. |
| **Domain** | `com.appointmentscheduler.domain` | Entities (`Appointment` hierarchy, `User`, `Clinic`, …), policies, rules, events. No JavaFX or JDBC. |
| **Persistence** | `com.appointmentscheduler.persistence` | Repository interfaces; `persistence.database` has JDBC implementations and Flyway migrations under `resources/.../migration/`. |

**Data flow:** Controller → service (`ApplicationContext`) → repository → DB / in-memory map.

**Adding a feature:** extend domain if needed → repository + JDBC if persisted → service method → controller + FXML.
