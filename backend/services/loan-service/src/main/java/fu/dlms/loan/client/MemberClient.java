package fu.dlms.loan.client;

import fu.dlms.loan.client.dto.MemberView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service", path = "/api/members", configuration = RemoteErrorConfig.class)
public interface MemberClient {

    /** Eligibility check before a borrow: only an ACTIVE member may borrow. */
    @GetMapping("/{id}")
    MemberView getMember(@PathVariable("id") Long id);
}
