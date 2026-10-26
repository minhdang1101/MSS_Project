package fu.dlms.member.dto;

import java.util.List;

public record MemberImportResult(int imported, List<RowError> errors) {

    /** {@code row} is the Excel row number, so the librarian can find and fix it in the file. */
    public record RowError(int row, String message) {
    }
}
