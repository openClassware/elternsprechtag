package de.openclassware.elternsprechtag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Gegen die Test-Datenbank, nicht gegen die Entwicklungsdatenbank: Dieser Test fährt den echten
// Context hoch und fasst damit das Schema an (ddl-auto). Ab Flyway wird er zum Migrationstest —
// dann muss er ohnehin auf einer Datenbank laufen, die niemand nebenher benutzt.
@SpringBootTest
@ActiveProfiles("test")
class ElternsprechtagApplicationTests {

	@Test
	void contextLoads() {
	}

}
