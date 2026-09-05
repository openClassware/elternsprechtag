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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "erstellt_am", nullable = false, updatable = false)
    private LocalDateTime erstelltAm;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BuchungStatusEnum status;

    @Column(name = "schueler_name", nullable = false)
    private String schuelerName;

    @Column(name = "eltern_name", nullable = false)
    private String elternName;

    // Kontakt-E-Mail der Eltern, denormalisiert an der Buchung (es gibt bewusst keine Eltern-Entity).
    // Pflicht — dient der Benachrichtigung bei Absage des Sprechtags (ADR 0001) und der Bestätigung
    // der eigenen Buchung (ADR 0002).
    @Column(name = "eltern_email", nullable = false)
    private String elternEmail;

    // 500 Zeichen: so viel lässt die Buchungsoberfläche je Termin zu (V2-Migration).
    @Column(name = "notiz", length = 500)
    private String notiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lehrauftrag_id", nullable = false)
    private Lehrauftrag lehrauftrag;

    // ManyToOne (nicht OneToOne): pro Termin darf es höchstens eine AKTIVE Buchung geben, über die
    // Zeit aber mehrere Datensätze (Storno gibt den Termin frei, eine spätere Buchung nutzt ihn
    // erneut). Ein Unique-Constraint auf termin_id würde diese Wiederverwendung verhindern.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termin_id", nullable = false)
    private Termin termin;

}
