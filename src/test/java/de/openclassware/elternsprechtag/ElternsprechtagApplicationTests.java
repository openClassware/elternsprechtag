package de.openclassware.elternsprechtag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Gegen die Test-Datenbank, nicht gegen die Entwicklungsdatenbank: Dieser Test fährt den echten
// Context hoch und lässt dabei die volle Migrationskette laufen.
//
// Er ist der einzige Test, der die gesamte Verdrahtung sieht — alle Views, Presenter, Use Cases
// und Adapter in einem Kontext. Genau deshalb steht er hier: Ein Bean, das sich nur im
// Zusammenspiel nicht auflösen lässt, fällt in keinem Slice-Test auf, sondern erst hier.
@SpringBootTest
@ActiveProfiles("test")
class ElternsprechtagApplicationTests {

	@Test
	void contextLoads() {
	}

}
