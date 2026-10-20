package com.balderb.research.data;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads a CSV file with a header row and works out, per column, whether it is numeric and how many
 * values are missing.
 *
 * <p>Handles the common variants: comma, semicolon or tab between values; a decimal comma when values
 * are separated by semicolons (as Excel does with Dutch settings); quoted values; and the invisible
 * byte order mark that Excel puts at the start of "CSV UTF-8" files.
 */
@Service
public class CsvDatasetProvider implements DatasetProvider {

    private static final char BYTE_ORDER_MARK = '﻿';
    private static final char[] DELIMITERS = {',', ';', '\t'};

    private Dataset current;

    /** Loads the configured file if it exists; otherwise the app starts empty and waits for 'load'. */
    public CsvDatasetProvider(@Value("${research.data-file:}") String dataFile) {
        if (!dataFile.isBlank() && Files.isRegularFile(Path.of(dataFile))) {
            this.current = read(Path.of(dataFile));
        }
    }

    @Override
    public Dataset current() {
        if (current == null) {
            throw new IllegalStateException("No CSV file loaded yet. Use: load <path-to-file.csv>");
        }
        return current;
    }

    @Override
    public Dataset load(Path file) {
        current = read(file);
        return current;
    }

    static Dataset read(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            throw new IllegalArgumentException("CSV file not found: " + absolute);
        }
        try {
            char delimiter = detectDelimiter(firstLine(absolute));
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .get();
            try (Reader reader = Files.newBufferedReader(absolute, StandardCharsets.UTF_8);
                    CSVParser parser = CSVParser.parse(skipByteOrderMark(reader), format)) {
                List<String> headers = parser.getHeaderNames();
                int[] missing = new int[headers.size()];
                boolean[] numeric = new boolean[headers.size()];
                Arrays.fill(numeric, true);
                int rows = 0;
                for (CSVRecord record : parser) {
                    rows++;
                    for (int i = 0; i < headers.size(); i++) {
                        String value = i < record.size() ? record.get(i) : "";
                        if (value.isEmpty()) {
                            missing[i]++;
                        }
                        else if (numeric[i] && !isNumber(value, delimiter)) {
                            numeric[i] = false;
                        }
                    }
                }
                List<ColumnInfo> columns = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    // A completely empty column has no numbers to summarise
                    boolean isNumeric = numeric[i] && missing[i] < rows;
                    columns.add(new ColumnInfo(headers.get(i), isNumeric, missing[i]));
                }
                return new Dataset(datasetName(absolute), absolute, delimiter, rows, columns);
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException("Could not read " + absolute, e);
        }
    }

    /** Picks the delimiter that occurs most often in the header line. */
    static char detectDelimiter(String headerLine) {
        char best = ',';
        long bestCount = 0;
        for (char candidate : DELIMITERS) {
            long count = headerLine.chars().filter(c -> c == candidate).count();
            if (count > bestCount) {
                best = candidate;
                bestCount = count;
            }
        }
        return best;
    }

    private static boolean isNumber(String value, char delimiter) {
        // With ';' between values, a comma is the decimal separator: "3,5" means 3.5
        String normalized = delimiter == ';' ? value.replace(',', '.') : value;
        try {
            Double.parseDouble(normalized);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private static String firstLine(Path file) throws IOException {
        try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.findFirst().orElse("");
        }
    }

    private static Reader skipByteOrderMark(Reader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != BYTE_ORDER_MARK) {
            reader.reset();
        }
        return reader;
    }

    /** "Survey 2024.csv" becomes "survey_2024", a valid R variable name. */
    private static String datasetName(Path file) {
        String name = file.getFileName().toString().replaceFirst("\\.[^.]*$", "");
        name = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return name.isEmpty() || Character.isDigit(name.charAt(0)) ? "data_" + name : name;
    }
}
