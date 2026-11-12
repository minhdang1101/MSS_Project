package fu.dlms.fine.service;

import fu.dlms.fine.dto.req.FinePolicyRequest;
import fu.dlms.fine.dto.res.FinePolicyResponse;

public interface FinePolicyService {

    /** The policy new fines are opened with today. */
    FinePolicyResponse findCurrent();

    /**
     * Saves a new version of the rules with its effective date. Older versions are kept, so fines already opened
     * keep the rules they started with.
     */
    FinePolicyResponse update(FinePolicyRequest request);
}
