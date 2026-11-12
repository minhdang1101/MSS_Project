package fu.dlms.member.dto.req;

import fu.dlms.member.entity.MemberStatus;
import jakarta.validation.constraints.NotNull;

public record MemberStatusRequest(@NotNull MemberStatus status) {
}
