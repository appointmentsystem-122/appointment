package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.Administrator;
import com.appointmentscheduler.domain.DoctorUser;
import com.appointmentscheduler.domain.ReceptionistUser;
import com.appointmentscheduler.domain.User;

/**
 * Role classification for messaging and access control (enterprise directory semantics).
 */
public final class UserDirectory {

    private UserDirectory() { }

    public static boolean isStaff(User u) {
        if (u == null) return false;
        return u instanceof Administrator || u instanceof ReceptionistUser || u instanceof DoctorUser;
    }

    public static boolean isPatient(User u) {
        return u != null && !isStaff(u);
    }
}
