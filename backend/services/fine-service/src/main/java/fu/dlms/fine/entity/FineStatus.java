package fu.dlms.fine.entity;

public enum FineStatus {
    /** The loan is overdue and the book is still out: the amount grows every day until it comes back. */
    ACCRUING,
    UNPAID,
    PAID,
    WAIVED
}
