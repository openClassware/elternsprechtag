package de.openclassware.elternsprechtag.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "buchungen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Buchung {

    @Id
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @Column(name = "status", nullable = false)
    private BuchungStatusEnum status;

    @Column(name = "schueler_name", nullable = false)
    private String schuelerName;

    @Column(name = "eltern_name", nullable = false)
    private String elternName;

    @Column(name = "notiz")
    private String notiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lehrauftrag_id")
    private Lehrauftrag lehrauftrag;

}
