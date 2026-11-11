package com.whatsappconnect.database;

import java.nio.file.Files;
import java.nio.file.Path;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterSchemaIT {
    @Test void manualFreshInstallWorksAndRequiresExplicitFlywayAdoption() throws Exception {
        try(var fixture=new PostgresFixture()) {
            String sql=Files.readString(Path.of("../database/schema.sql"));
            try(var db=fixture.connect("wc_owner");var statement=db.createStatement()) {
                statement.execute(sql);
                try(var rows=statement.executeQuery("SELECT count(*) FROM pg_tables WHERE schemaname='public'")){rows.next();assertThat(rows.getInt(1)).isEqualTo(37);}
            }
            var flyway=Flyway.configure().dataSource(fixture.url,"wc_owner",fixture.password).locations("classpath:db/migration").load();
            assertThatThrownBy(flyway::migrate).hasMessageContaining("non-empty schema");
            // Only an explicit baseline on a known fresh/reference installation.
            var adoption=Flyway.configure().dataSource(fixture.url,"wc_owner",fixture.password).locations("classpath:db/migration").baselineVersion("3").load();
            adoption.baseline();assertThat(adoption.migrate().migrationsExecuted).isZero();
        }
    }
}
