package de.openclassware.elternsprechtag.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lehrauftrag")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lehrauftrag {

    @Id
    @Column(name = "id", nullable = false, updatable = false,  unique = true)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lehrer_id")
    private Lehrer lehrer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "klasse_id")
    private Klasse klasse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fach_id")
    private Fach fach;

    @OneToMany(mappedBy = "lehrauftrag")
    private List<Buchung> buchungen = new ArrayList<>();

}
