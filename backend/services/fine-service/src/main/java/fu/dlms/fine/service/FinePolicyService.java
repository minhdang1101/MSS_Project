package fu.dlms.fine.service;

import fu.dlms.fine.dto.ClosedDayRequest;
import fu.dlms.fine.dto.ClosedDayResponse;
import fu.dlms.fine.dto.FinePolicyRequest;
import fu.dlms.fine.dto.FinePolicyResponse;

import java.time.LocalDate;
import java.util.List;

public interface FinePolicyService {

    List<FinePolicyResponse> findAll();

    /** The policy new fines are opened with today. */
    FinePolicyResponse findCurrent();

    FinePolicyResponse create(FinePolicyRequest request);

    /** Policies are never deleted: fines keep pointing at the version they were calculated with. */
    FinePolicyResponse deactivate(Long id);

    List<ClosedDayResponse> findClosedDays();

    ClosedDayResponse addClosedDay(ClosedDayRequest request);

    void removeClosedDay(LocalDate date);
}
