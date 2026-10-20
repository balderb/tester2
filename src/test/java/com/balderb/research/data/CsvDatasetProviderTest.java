package com.balderb.research.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvDatasetProviderTest {

    @TempDir
    Path folder;

    @Test
    void readsColumnsTypesAndMissingValues() throws IOException {
        Path file = write("survey.csv", """
                id,country,score
                1,Netherlands,7.5
                2,"Belgium, Flanders",
                3,Germany,6
                """);

        Dataset dataset = CsvDatasetProvider.read(file);

        assertThat(dataset.name()).isEqualTo("survey");
        assertThat(dataset.delimiter()).isEqualTo(',');
        assertThat(dataset.rowCount()).isEqualTo(3);
        assertThat(dataset.columns()).containsExactly(
                new ColumnInfo("id", true, 0),
                new ColumnInfo("country", false, 0),
                new ColumnInfo("score", true, 1));
    }

    @Test
    void understandsSemicolonsAndDecimalCommasFromDutchExcel() throws IOException {
        Path file = write("Enquete 2024.csv", "﻿leeftijd;tevredenheid\n34;7,5\n51;6\n");

        Dataset dataset = CsvDatasetProvider.read(file);

        assertThat(dataset.name()).isEqualTo("enquete_2024");
        assertThat(dataset.delimiter()).isEqualTo(';');
        assertThat(dataset.columns()).containsExactly(
                new ColumnInfo("leeftijd", true, 0),
                new ColumnInfo("tevredenheid", true, 0));
    }

    @Test
    void loadSwitchesToAnotherFile() throws IOException {
        CsvDatasetProvider provider = new CsvDatasetProvider(write("first.csv", "a\n1\n").toString());

        provider.load(write("second.csv", "b,c\n1,2\n"));

        assertThat(provider.current().name()).isEqualTo("second");
    }

    @Test
    void startsEmptyWhenTheConfiguredFileDoesNotExist() {
        CsvDatasetProvider provider = new CsvDatasetProvider(folder.resolve("missing.csv").toString());

        assertThatThrownBy(provider::current).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("load");
        assertThatThrownBy(() -> provider.load(folder.resolve("missing.csv")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Path write(String name, String content) throws IOException {
        return Files.writeString(folder.resolve(name), content);
    }
}
