package com.balderb.research.schema;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

class SchemaServiceTest {

    private final EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("schema.sql")
            .build();

    private final SchemaService schemaService = new SchemaService(database);

    @AfterEach
    void shutDown() {
        database.shutdown();
    }

    @Test
    void listsTheSampleTables() {
        assertThat(schemaService.listTables()).extracting(TableInfo::name)
                .containsExactlyInAnyOrder("RESPONDENT", "SURVEY_RESPONSE");
    }

    @Test
    void readsColumnTypesAndNullability() {
        TableInfo respondent = schemaService.findTable("respondent").orElseThrow();

        assertThat(respondent.columns()).contains(
                new ColumnInfo("AGE", "INTEGER", true, false),
                new ColumnInfo("COUNTRY", "CHARACTER VARYING", false, true));
    }
}
