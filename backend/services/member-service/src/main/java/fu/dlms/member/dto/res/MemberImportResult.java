package fu.dlms.member.dto.res;

import java.util.List;

/**
 * {@code credentials} lists the login password of each member created by this import. It is the only time those
 * passwords are shown (only hashes are stored), so the librarian must print or save the list from the result.
 */
public record MemberImportResult(int imported, List<RowError> errors, List<IssuedCredential> credentials) {

    /** {@code row} is the Excel row number, so the librarian can find and fix it in the file. */
    public record RowError(int row, String message) {
    }

    public record IssuedCredential(String libraryCardNo, String fullName, String initialPassword) {
    }
}
