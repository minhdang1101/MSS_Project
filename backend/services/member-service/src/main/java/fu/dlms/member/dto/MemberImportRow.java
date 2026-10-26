package fu.dlms.member.dto;

/**
 * One data row read from the Excel file. {@code rowNumber} is the row number as shown in Excel (header = 1).
 * {@code phoneIsNumeric} is true when the phone cell held a number rather than text: Excel drops leading zeros
 * from numbers, so such a phone cannot be trusted.
 */
public record MemberImportRow(int rowNumber, String fullName, String email, String phone, boolean phoneIsNumeric) {

    public MemberImportRow(int rowNumber, String fullName, String email, String phone) {
        this(rowNumber, fullName, email, phone, false);
    }
}
