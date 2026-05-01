package com.appointmentscheduler.application;

import com.appointmentscheduler.domain.policy.Policy;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized policy enforcement layer.
 * Validates all business operations through registered policies.
 * Policies are extensible without modifying existing services.
 */
public class PolicyEngine {

    private final List<Policy<?>> policies = new ArrayList<>();

    /**
     * Registers a policy. Policies are evaluated in registration order.
     */
    public <T> void registerPolicy(Policy<T> policy) {
        if (policy != null) {
            policies.add(policy);
        }
    }

    /**
     * Evaluates all policies that apply to the given context.
     *
     * @param context the operation context
     * @return the first failing result, or success if all pass
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Policy.PolicyResult evaluate(Object context) {
        for (Policy policy : policies) {
            try {
                Policy.PolicyResult result = policy.evaluate(context);
                if (!result.isPassed()) {
                    return result;
                }
            } catch (ClassCastException ignored) {
                continue;
            }
        }
        return Policy.PolicyResult.allow();
    }
}
