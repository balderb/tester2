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
    void treatsNaAsMissingLikeR() throws IOException {
        Dataset dataset = CsvDatasetProvider.read(write("na.csv", "score\nNA\n7\n"));

        assertThat(dataset.columns()).containsExactly(new ColumnInfo("score", true, 1));
    }

    @Test
    void onlyCountsValuesAsNumbersWhenRDoes() throws IOException {
        Dataset dataset = CsvDatasetProvider.read(write("numbers.csv", "a,b,c\n1.5e3,1d,-Inf\n-.5,2f,NaN\n"));

        assertThat(dataset.columns()).extracting(ColumnInfo::numeric).containsExactly(true, false, true);
    }

    @Test
    void refusesRowsWithTheWrongNumberOfValues() throws IOException {
        Path file = write("ragged.csv", "a,b,c\n1,2,3\n4,5,6,7\n");

        assertThatThrownBy(() -> CsvDatasetProvider.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data row 2 has 4 values, but the header has 3 columns");
    }

    @Test
    void refusesDuplicateColumnNames() throws IOException {
        Path file = write("duplicate.csv", "a,a\n1,2\n");

        assertThatThrownBy(() -> CsvDatasetProvider.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"a\" appears more than once");
    }

    @Test
    void refusesAnEmptyFile() throws IOException {
        Path file = write("empty.csv", "");

        assertThatThrownBy(() -> CsvDatasetProvider.read(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
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

    @Test
    void startsEmptyAndExplainsWhyWhenTheConfiguredFileIsInvalid() throws IOException {
        CsvDatasetProvider provider = new CsvDatasetProvider(write("bad.csv", "a,b\n1\n").toString());

        assertThatThrownBy(provider::current).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Data row 1 has 1 values")
                .hasMessageContaining("load");
    }

    private Path write(String name, String content) throws IOException {
        return Files.writeString(folder.resolve(name), content);
    }
}
