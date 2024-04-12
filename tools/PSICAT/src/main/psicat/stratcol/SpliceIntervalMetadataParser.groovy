package psicat.stratcol

import java.util.Arrays
import java.util.List

import au.com.bytecode.opencsv.CSVReader

import psicat.stratcol.SpliceIntervalMetadata

// SpliceIntervalMetadataParser simplifies access to tabular Splice Interval
// data, parsing each row of splice interval metadata into a map keyed on column
// names.
class SpliceIntervalMetadataParser {
	def headers = []
	def rows = []
	def colMap = [:]
	
	public SpliceIntervalMetadataParser(List<String[]> _rows) {
		parse(_rows);
	}
	
	public getRows() { return rows; }
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
		this.headers.eachWithIndex { col, index ->
			map.putAt(col, row[index])
		}
		return map
	}

	private void parse(List<String[]> _rows) {
		_rows.eachWithIndex { row, rowIndex ->
			if (rowIndex == 0) {
				this.headers = Arrays.asList(row)
			} else {
				rows << makeMap(row)
			}
		}
	}
}