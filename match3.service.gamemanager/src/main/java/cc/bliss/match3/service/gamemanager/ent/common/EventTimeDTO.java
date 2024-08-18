package cc.bliss.match3.service.gamemanager.ent.common;

import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EventTimeDTO {
    private int startDay;
    private int startHour;
    private int startMinutes;
    private int endDay;
    private int endHour;
    private int endMinutes;

    public LocalDateTime getStartDateTime(LocalDate referenceDate) {
        DayOfWeek targetDayOfWeek = DayOfWeek.of(startDay);
        return referenceDate.with(targetDayOfWeek).atTime(startHour, startMinutes);
    }

    public LocalDateTime getEndDateTime(LocalDate referenceDate) {
        DayOfWeek targetDayOfWeek = DayOfWeek.of(endDay);
        return referenceDate.with(targetDayOfWeek).atTime(endHour, endMinutes, 59);
    }
}
