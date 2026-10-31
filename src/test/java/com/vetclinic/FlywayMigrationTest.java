package com.vetclinic;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The test database starts empty, so this checks that the migrations run cleanly on a fresh
 * database, that no seed data is applied in the test profile, and that the entities match the
 * schema (the context only starts if ddl-auto=validate passes).
 */
class FlywayMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void schemaMigrationsAreAppliedAndSeedIsNot() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(info -> assertThat(info.getState().isFailed()).isFalse());
        assertThat(Arrays.stream(applied).map(info -> info.getVersion().getVersion()))
                .contains("1")
                .noneMatch(version -> Integer.parseInt(version) >= 1000);
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void appliedMigrationsMatchTheFilesOnTheClasspath() {
        assertThatCode(() -> flyway.validate()).doesNotThrowAnyException();
    }

    @Test
    void hibernateSchemaValidationPassed() {
        assertThat(entityManagerFactory.isOpen()).isTrue();
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(8);
    }
}
