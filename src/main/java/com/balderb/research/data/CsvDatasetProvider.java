package com.balderb.research.data;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.DuplicateHeaderMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads a CSV file with a header row and works out, per column, whether it is numeric and how many
 * values are missing.
 *
 * <p>Handles the common variants: comma, semicolon or tab between values; a decimal comma when values
 * are separated by semicolons (as Excel does with Dutch settings); quoted values; and the invisible
 * byte order mark that Excel puts at the start of "CSV UTF-8" files.
 *
 * <p>Types and missing values follow the rules R uses in {@code read.csv}, so the generated script
 * sees the same columns as this class: "NA" counts as missing, and only values R reads as numbers
 * make a column numeric. Files that R would read wrongly without an error (rows with a different
 * number of values than the header, duplicate column names) are refused.
 */
@Service
public class CsvDatasetProvider implements DatasetProvider {

    private static final char BYTE_ORDER_MARK = '﻿';
    private static final char[] DELIMITERS = {',', ';', '\t'};
    /** R's default missing value marker in read.csv. */
    private static final String R_MISSING = "NA";
    /** Values R's read.csv turns into numbers: decimals, scientific notation, Inf, NaN and hex. */
    private static final Pattern R_NUMBER = Pattern.compile(
            "[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?|[+-]?(Inf|Infinity)|NaN|[+-]?0[xX][0-9a-fA-F]+");

    private Dataset current;
    /** Why the configured file could not be loaded at startup, shown when a command needs the data. */
    private String startupProblem = "";

    /**
     * Loads the configured file if it exists and is valid. Otherwise the app still starts, without
     * data, and waits for 'load'.
     */
    public CsvDatasetProvider(@Value("${research.data-file:}") String dataFile) {
        if (!dataFile.isBlank() && Files.isRegularFile(Path.of(dataFile))) {
            try {
                this.current = read(Path.of(dataFile));
            }
            catch (IllegalArgumentException | UncheckedIOException e) {
                this.startupProblem = "Could not load " + dataFile + ": " + e.getMessage() + "\n";
            }
        }
    }

    @Override
    public Dataset current() {
        if (current == null) {
            throw new IllegalStateException(startupProblem + "No CSV file loaded yet. Use: load <path-to-file.csv>");
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
                    .setDuplicateHeaderMode(DuplicateHeaderMode.ALLOW_ALL)
                    .get();
            try (Reader reader = Files.newBufferedReader(absolute, StandardCharsets.UTF_8);
                    CSVParser parser = CSVParser.parse(skipByteOrderMark(reader), format)) {
                List<String> headers = parser.getHeaderNames();
                if (headers.isEmpty()) {
                    throw new IllegalArgumentException("The file is empty; expected a header row: " + absolute);
                }
                Set<String> seen = new HashSet<>();
                for (String header : headers) {
                    if (!seen.add(header)) {
                        throw new IllegalArgumentException("The column name \"%s\" appears more than once. Give every column a unique name: R can only reach the first one."
                                .formatted(header));
                    }
                }
                int[] missing = new int[headers.size()];
                boolean[] numeric = new boolean[headers.size()];
                Arrays.fill(numeric, true);
                int rows = 0;
                for (CSVRecord record : parser) {
                    rows++;
                    if (record.size() != headers.size()) {
                        throw new IllegalArgumentException(
                                "Data row %d has %d values, but the header has %d columns. Fix the file first: R would shift the values into the wrong columns without warning."
                                        .formatted(rows, record.size(), headers.size()));
                    }
                    for (int i = 0; i < headers.size(); i++) {
                        String value = record.get(i);
                        if (value.isEmpty() || value.equals(R_MISSING)) {
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
        return R_NUMBER.matcher(normalized).matches();
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
