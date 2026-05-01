package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Clinic;
import com.appointmentscheduler.persistence.ClinicRepository;

import java.util.prefs.Preferences;

/**
 * Holds the currently selected clinic/branch for filtering (multi-clinic).
 */
public class CurrentClinicService {

    private static final String PREF_CLINIC_ID = "current.clinicId";
    private final ClinicRepository clinicRepository;
    private final Preferences prefs = Preferences.userNodeForPackage(CurrentClinicService.class);

    public CurrentClinicService(ClinicRepository clinicRepository) {
        this.clinicRepository = clinicRepository;
    }

    public String getCurrentClinicId() {
        String v = prefs.get(PREF_CLINIC_ID, "");
        return v.isEmpty() ? null : v;
    }

    public void setCurrentClinicId(String clinicId) {
        if (clinicId == null) prefs.remove(PREF_CLINIC_ID);
        else prefs.put(PREF_CLINIC_ID, clinicId);
    }

    public Clinic getCurrentClinic() {
        String id = getCurrentClinicId();
        return id != null ? clinicRepository.findById(id).orElse(null) : null;
    }
}
