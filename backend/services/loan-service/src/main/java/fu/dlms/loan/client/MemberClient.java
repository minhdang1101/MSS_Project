package fu.dlms.loan.client;

import fu.dlms.loan.client.dto.MemberView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service", path = "/api/members")
public interface MemberClient {

    @GetMapping("/{id}")
    MemberView getMember(@PathVariable("id") Long id);
}
