package fu.dlms.fine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** A day the library is closed (holiday, stocktaking...). Closed days are not counted as late. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "closed_day")
public class ClosedDay {

    @Id
    @Column(name = "closed_on")
    private LocalDate date;

    @Column(length = 200)
    private String reason;
}
