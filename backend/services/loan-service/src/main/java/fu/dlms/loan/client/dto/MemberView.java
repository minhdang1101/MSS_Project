package fu.dlms.loan.client.dto;

/** Only the member fields loan-service needs - decoupled from member-service's full DTO. */
public record MemberView(Long id, String fullName, String email, String status) {
}
