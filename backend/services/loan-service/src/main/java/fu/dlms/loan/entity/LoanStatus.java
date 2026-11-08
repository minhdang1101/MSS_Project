package fu.dlms.loan.entity;

/** Only moves forward: BORROWED -> OVERDUE -> RETURNED, or BORROWED -> RETURNED. */
public enum LoanStatus {
    BORROWED,
    OVERDUE,
    RETURNED;

    public boolean isOpen() {
        return this != RETURNED;
    }
}
