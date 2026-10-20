package com.balderb.research.data;

/**
 * One column (variable) of a dataset.
 *
 * @param name         column name as written in the header of the file
 * @param numeric      true when every non-empty value is a number (so means/standard deviations make sense)
 * @param missingCount number of rows where the value is empty
 */
public record ColumnInfo(String name, boolean numeric, int missingCount) {
}
