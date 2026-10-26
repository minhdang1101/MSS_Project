package fu.dlms.loan.client.dto;

public record BookView(Long id, String isbn, String title, String format, int availableCopies, String status) {
}
