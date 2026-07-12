package de.openclassware.elternsprechtag.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "termin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Termin {

    @Id
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "startzeit", nullable = false)
    private LocalDateTime startzeit;

    @Column(name = "endzeit", nullable = false)
    private LocalDateTime endzeit;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TerminStatusEnum status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lehrer_id")
    private Lehrer lehrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprechtag_id", nullable = false)
    private Sprechtag sprechtag;

}
