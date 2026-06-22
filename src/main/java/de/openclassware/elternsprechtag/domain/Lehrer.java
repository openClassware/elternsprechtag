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
@Table(name = "lehrer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lehrer {

    @Id
    @Column(name = "id", nullable = false, updatable = false,  unique = true)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vorname", nullable = false)
    private String vorname;

    @Column(name = "nachname", nullable = false)
    private String nachname;

    @Column(name = "kuerzel", nullable = false)
    private String kuerzel;

    @OneToMany(mappedBy = "lehrer")
    private List<Termin> termine  = new ArrayList<>();

    @OneToMany(mappedBy = "lehrer")
    private List<Lehrauftrag> lehrauftraege = new ArrayList<>();

}
