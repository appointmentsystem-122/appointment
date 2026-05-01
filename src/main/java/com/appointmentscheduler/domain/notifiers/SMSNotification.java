package com.appointmentscheduler.domain.notifiers;

import com.appointmentscheduler.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMS reminder channel (adapter stub): no SMS gateway; logs at debug for traceability.
 */
public class SMSNotification implements Observer {

    private static final Logger log = LoggerFactory.getLogger(SMSNotification.class);

    @Override
    public void notify(User user, String message) {
        if (user == null) {
            log.warn("SMS reminder skipped: user is null");
            return;
        }
        log.debug("SMS reminder (stub): userId={}, messageLength={}", user.getId(), message != null ? message.length() : 0);
    }
}
