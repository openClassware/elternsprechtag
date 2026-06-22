package de.openclassware.elternsprechtag.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "faecher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Fach {

    @Id
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "short_name", nullable = false, unique = true)
    private String shortName;

    @OneToMany(mappedBy = "fach")
    private List<Lehrauftrag> lehrauftraege;

}
