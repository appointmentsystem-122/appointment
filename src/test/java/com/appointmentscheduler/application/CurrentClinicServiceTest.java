package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.persistence.InMemoryClinicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentClinicServiceTest {

    @BeforeEach
    @AfterEach
    void clearPref() {
        Preferences p = Preferences.userNodeForPackage(CurrentClinicService.class);
        p.remove("current.clinicId");
        try {
            p.flush();
        } catch (java.util.prefs.BackingStoreException ignored) {
            // test VM may not persist prefs
        }
    }

    @Test
    void getSetAndResolveClinic() {
        InMemoryClinicRepository repo = new InMemoryClinicRepository();
        repo.save(new Clinic("c1", "Main", "Addr", "UTC"));
        CurrentClinicService svc = new CurrentClinicService(repo);
        assertThat(svc.getCurrentClinicId()).isNull();
        svc.setCurrentClinicId("c1");
        assertThat(svc.getCurrentClinic()).isNotNull();
        svc.setCurrentClinicId(null);
        assertThat(svc.getCurrentClinicId()).isNull();
    }
}
