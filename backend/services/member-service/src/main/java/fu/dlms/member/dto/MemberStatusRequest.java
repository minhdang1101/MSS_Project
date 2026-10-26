package fu.dlms.member.dto;

import fu.dlms.member.entity.MemberStatus;
import jakarta.validation.constraints.NotNull;

public record MemberStatusRequest(@NotNull MemberStatus status) {
}
