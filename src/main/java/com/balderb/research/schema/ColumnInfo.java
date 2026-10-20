package com.balderb.research.schema;

/**
 * One column of a database table.
 *
 * @param name     column name as stored in the database
 * @param typeName database type name, e.g. INTEGER or CHARACTER VARYING
 * @param numeric  true when the column holds numbers (so means/standard deviations make sense)
 * @param nullable true when the column may contain missing values
 */
public record ColumnInfo(String name, String typeName, boolean numeric, boolean nullable) {
}
