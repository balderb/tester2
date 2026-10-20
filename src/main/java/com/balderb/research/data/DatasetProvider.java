package com.balderb.research.data;

import java.nio.file.Path;

/**
 * Gives access to the dataset being analysed.
 *
 * <p>{@link CsvDatasetProvider} reads CSV files. To support another source later (an Excel or SPSS
 * file, or a database), add another implementation of this interface.
 */
public interface DatasetProvider {

    /**
     * The dataset currently loaded.
     *
     * @throws IllegalStateException when no dataset has been loaded yet
     */
    Dataset current();

    /**
     * Switches to another file and returns its description.
     *
     * @throws IllegalArgumentException when the file does not exist
     * @throws java.io.UncheckedIOException when the file cannot be read
     */
    Dataset load(Path file);
}
