package fu.dlms.fine.dto.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** New fine rules (PUT /api/fines/policy). maxAmount may be left empty for no ceiling. */
public record FinePolicyRequest(
        @NotNull(message = "Nhập mức phạt mỗi ngày")
        @DecimalMin(value = "0", message = "Mức phạt không được âm")
        @Digits(integer = 10, fraction = 2, message = "Số tiền không hợp lệ")
        BigDecimal dailyRate,

        @NotNull(message = "Nhập số ngày ân hạn")
        @Min(value = 0, message = "Số ngày ân hạn không được âm")
        @Max(value = 30, message = "Số ngày ân hạn tối đa là 30")
        Integer graceDays,

        @DecimalMin(value = "0", message = "Mức trần không được âm")
        @Digits(integer = 10, fraction = 2, message = "Số tiền không hợp lệ")
        BigDecimal maxAmount,

        @NotNull(message = "Chọn ngày bắt đầu áp dụng")
        LocalDate effectiveFrom
) {
}
