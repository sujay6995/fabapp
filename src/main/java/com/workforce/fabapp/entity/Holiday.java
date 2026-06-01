package com.workforce.fabapp.entity;

import com.workforce.fabapp.enums.HolidayScope;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "holidays",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_holiday_observed_date", columnNames = {"observed_date"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(name = "observed_date", nullable = false)
    private LocalDate observedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private HolidayScope scope;

    @Column(name = "paid_hours", precision = 10, scale = 2)
    private BigDecimal paidHours;

    @Column(nullable = false)
    private Boolean active;
}