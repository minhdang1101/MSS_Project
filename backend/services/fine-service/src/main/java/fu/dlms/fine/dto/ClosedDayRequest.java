package fu.dlms.fine.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClosedDayRequest(
        @NotNull(message = "Chọn ngày nghỉ")
        LocalDate date,

        @Size(max = 200, message = "Lý do tối đa 200 ký tự")
        String reason
) {
}
