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

    MemberResponse create(MemberRequest request);

    MemberResponse update(Long id, MemberRequest request);

    MemberResponse changeStatus(Long id, MemberStatus status);

    /** Imports the valid rows of an .xlsx file; invalid rows are reported, not fatal. */
    MemberImportResult importFromExcel(InputStream xlsx);
}
