package fu.dlms.notification.entity;

public enum NotificationType {
    DUE_SOON,
    OVERDUE;

    /**
     * Business key used for duplicate checks: "DUE_SOON:15".
     * Uses loanId instead of the event's eventId because loan-service sends a NEW event (new eventId)
     * every day the loan is still due soon / overdue; we still want only one reminder per loan and type.
     */
    public String idempotencyKey(Long loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("loanId is required to build an idempotency key");
        }
        return name() + ":" + loanId;
    }
}
