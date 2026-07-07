package de.openclassware.elternsprechtag.domain;

import java.util.EnumSet;
import java.util.Set;

public enum SprechtagStatusEnum {
    ENTWURF, VEROEFFENTLICHT, ABGESAGT, ABGESCHLOSSEN;

    /** Erlaubte Folgestatus für diesen Status. Endzustände liefern eine leere Menge. */
    public Set<SprechtagStatusEnum> allowedTransitions() {
        return switch (this) {
            case ENTWURF -> EnumSet.of(VEROEFFENTLICHT);
            case VEROEFFENTLICHT -> EnumSet.of(ABGESCHLOSSEN, ABGESAGT);
            case ABGESAGT, ABGESCHLOSSEN -> EnumSet.noneOf(SprechtagStatusEnum.class);
        };
    }
}
