package com.vetclinic;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for tests that need the database.
 * <p>
 * The PostgreSQL 16 container is a Spring bean from {@link TestcontainersConfiguration}
 * ({@code @ServiceConnection}), so it lives exactly as long as the cached application context
 * and is shared by every subclass (see docs/decisions.md, decision 10).
 * Profile "test": Flyway applies only {@code db/migration}; no seed data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
}
