package fu.dlms.loan.client.dto;

/** copyId is null for a digital book: there is no physical copy to hand out. */
public record ReservationView(Long id, Long bookId, Long copyId, String status) {
}
