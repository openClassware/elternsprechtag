package de.openclassware.elternsprechtag.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "klassen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Klasse {

    @Id
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "klassen")
    private List<Sprechtag> sprechtage = new ArrayList<>();

    @OneToMany(mappedBy = "klasse")
    private List<Lehrauftrag> lehrauftraege = new ArrayList<>();

}
