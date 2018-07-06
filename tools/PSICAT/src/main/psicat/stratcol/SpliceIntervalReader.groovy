package psicat.stratcol

import java.util.Arrays
import java.util.List

import au.com.bytecode.opencsv.CSVReader

import psicat.stratcol.SpliceIntervalMetadata

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
	public hasColumn(String col, def alts) {
		def has = col in headers
		if (!has) {
			if (alts.containsKey(col)) {
				has = alts[col] in headers
			}
		}
		return has
	}

	// alts: map with elements of form expected name:alternate name
	public hasColumns(def cols, def alts) {
		def hasAll = true;
		for (col in cols) {
			if (!hasColumn(col, alts)) {
				hasAll = false
				break
			}
		}
		return hasAll
	}

	public String getToolHeaderName() { 
		if (SpliceIntervalMetadata.CoreType in this.headers) {
			return SpliceIntervalMetadata.CoreType
		} else if (SpliceIntervalMetadata.Tool in this.headers) {
			return SpliceIntervalMetadata.Tool
		} else {
			return null
		}
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