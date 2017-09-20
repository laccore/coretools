package psicat.stratcol

import java.util.Arrays
import java.util.List

import au.com.bytecode.opencsv.CSVReader

// SpliceIntervalReader simplifies access of tabular Splice Interval
// data, parsing each CSVReader row into map keyed on column names
// derived from header.
class SpliceIntervalReader {
	def headers = []
	def rows = []
	def colMap = [:]
	
	public SpliceIntervalReader(CSVReader reader) {
		parse(reader);
	}
	
	public readAll() { return rows; }
	public hasColumn(String col) { return col in headers }
	public hasColumns(def cols) {
		def hasAll = true;
		for (col in cols) {
			if (!hasColumn(col)) {
				hasAll = false
				break
			}
		}
		return hasAll
	}

	private makeMap(row) {
		def map = [:]
		//for (int i = 0; i < headers.length; i++) {
		this.headers.eachWithIndex { col, index ->
			map.putAt(col, row[index])
		}
		return map
	}

	private void parse(CSVReader reader) {
		reader.readAll().eachWithIndex { row, rowIndex ->
			if (rowIndex == 0) {
				this.headers = Arrays.asList(row)
			} else {
				rows << makeMap(row)
			}
		}
	}
}