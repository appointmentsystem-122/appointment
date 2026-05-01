module com.appointmentscheduler {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires java.sql;
    requires javafx.graphics;
    requires org.slf4j;
    requires bcrypt;
    requires com.zaxxer.hikari;
    requires com.h2database;
    requires org.flywaydb.core;
    requires jakarta.mail;
    requires org.eclipse.angus.mail;

    /* Flyway reads SQL migrations from the module layer; without this, validate/migrate fails under JPMS (e.g. IntelliJ). */
    opens com.appointmentscheduler.persistence.database.migration to org.flywaydb.core;

    exports com.appointmentscheduler.presentation;
    opens com.appointmentscheduler.presentation to javafx.fxml;

    /* Unit tests (Mockito subclass mock maker) need reflective access to types under test */
    opens com.appointmentscheduler.persistence;
    opens com.appointmentscheduler.application;
    opens com.appointmentscheduler.domain;
    opens com.appointmentscheduler.domain.notifiers;
    opens com.appointmentscheduler.domain.rules;
    opens com.appointmentscheduler.domain.events;
    opens com.appointmentscheduler.domain.authorization;
    opens com.appointmentscheduler.domain.policy;
}
