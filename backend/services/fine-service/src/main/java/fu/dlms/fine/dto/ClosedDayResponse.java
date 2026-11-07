package fu.dlms.fine.dto;

import fu.dlms.fine.entity.ClosedDay;

import java.time.LocalDate;

public record ClosedDayResponse(LocalDate date, String reason) {

    public static ClosedDayResponse from(ClosedDay day) {
        return new ClosedDayResponse(day.getDate(), day.getReason());
    }
}
