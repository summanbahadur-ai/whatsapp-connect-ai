package com.whatsappconnect.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemaSnapshotTest {
    @Test void mainSqlContainsEveryMigrationExactlyInVersionOrder() throws Exception {
        String master=Files.readString(Path.of("../database/schema.sql")).replace("\r\n","\n");
        int first=master.indexOf("\n-- BEGIN MIGRATION: ");
        assertThat(first).isPositive();
        var expected=new StringBuilder(master.substring(0,first));
        try(var paths=Files.list(Path.of("src/main/resources/db/migration"))) {
            for(var file:paths.filter(p->p.getFileName().toString().matches("V[0-9]+__.*\\.sql"))
                    .sorted(Comparator.comparingInt(p->Integer.parseInt(p.getFileName().toString().split("__")[0].substring(1)))).toList()) {
                expected.append("\n-- BEGIN MIGRATION: ").append(file.getFileName()).append('\n')
                        .append(Files.readString(file).replace("\r\n","\n").stripTrailing())
                        .append("\n-- END MIGRATION: ").append(file.getFileName()).append('\n');
            }
        }
        expected.append("\nCOMMIT;\n");
        assertThat(master).as("Update database/schema.sql whenever structure SQL changes").isEqualTo(expected.toString());
        assertThat(master).contains("Master schema requires a fresh database").contains("whatsapp_connect_ai");
    }
}
