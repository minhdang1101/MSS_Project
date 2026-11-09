package fu.dlms.member.service;

import fu.dlms.member.dto.MemberImportResult;
import fu.dlms.member.dto.MemberRequest;
import fu.dlms.member.dto.MemberResponse;
import fu.dlms.member.entity.MemberStatus;

import java.io.InputStream;
import java.util.List;

public interface MemberService {

    List<MemberResponse> findAll();

    MemberResponse findById(Long id);

    /** Issues a card and a login password; the answer carries that password, once ({@code initialPassword}). */
    MemberResponse create(MemberRequest request);

    MemberResponse update(Long id, MemberRequest request);

    MemberResponse changeStatus(Long id, MemberStatus status);

    /** Sets a new random password (the old one stops working); the answer carries it, once. */
    MemberResponse resetPassword(Long id);

    /** Imports the valid rows of an .xlsx file; invalid rows are reported, not fatal. Passwords come back once. */
    MemberImportResult importFromExcel(InputStream xlsx);
}
