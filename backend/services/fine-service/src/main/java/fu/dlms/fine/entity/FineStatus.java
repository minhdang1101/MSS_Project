package fu.dlms.fine.entity;

/**
 * UNPAID or PAID. A fine whose book is still out is UNPAID with no returnedAt: its amount grows every day and is
 * worked out on every read until the book comes back.
 */
public enum FineStatus {
    UNPAID,
    PAID
}
