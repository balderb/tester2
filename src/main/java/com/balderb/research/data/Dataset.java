package com.balderb.research.data;

import java.nio.file.Path;
import java.util.List;

/**
 * Description of a dataset: where it comes from and which columns it has.
 * It does not hold the values themselves; the R script reads those.
 *
 * @param name      short name, used for the variable in the R script
 * @param file      the file the data was read from
 * @param delimiter character between the values, e.g. ',' or ';'
 * @param rowCount  number of data rows, not counting the header
 * @param columns   the columns in file order
 */
public record Dataset(String name, Path file, char delimiter, int rowCount, List<ColumnInfo> columns) {

    public Dataset {
        columns = List.copyOf(columns);
    }
}
