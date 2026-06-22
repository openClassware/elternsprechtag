package de.openclassware.elternsprechtag.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sprechtage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sprechtag {

    @Id
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "titel", nullable = false)
    private String titel;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_in_minutes", nullable = false)
    private Integer slotInMinutes;

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @Column(name = "location")
    private String location;

    @Column(name = "description")
    private String description;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SprechtagStatusEnum status;

    @ManyToMany
    @JoinTable(
            name = "sprechtage_klassen",
            joinColumns = @JoinColumn(name = "sprechtag_id"),
            inverseJoinColumns = @JoinColumn(name = "klasse_íd")
    )
    private List<Klasse> klassen = new ArrayList<>();

}
