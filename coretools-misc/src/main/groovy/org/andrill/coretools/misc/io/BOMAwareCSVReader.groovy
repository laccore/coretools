package org.andrill.coretools.misc.io

import java.io.Reader
import java.util.Arrays

import au.com.bytecode.opencsv.CSVReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Adapt CSVReader to remove UTF-8 BOM

public class BOMAwareCSVReader {
    private CSVReader reader
    private Logger logger = LoggerFactory.getLogger(BOMAwareCSVReader.class)

    public BOMAwareCSVReader(CSVReader reader) {
        this.reader = reader;
    }  

    public List<String[]> readAll() {
        def rows = null
        try {
            rows = reader.readAll()
            if (rows.size() > 0) {
                if (rows.get(0).length > 0) {
                    rows.get(0)[0] = removeBOM(rows.get(0)[0])
                }
            }
        } catch (Exception e) {
            logger.ERROR("${e.message}")
        }
        return rows
    }

    public String[] readNext() {
        String[] row = reader.readNext()
        if (row != null && row.length > 0) {
            row[0] = removeBOM(row[0])
        }
        return row
    }

    public void close() { reader.close() }

    private String removeBOM(String entry) {
        def bytes = entry.getBytes() as byte[]

        // 4/11/2024 brg: must specify UTF-8 encoding in getBytes() due to our
        // unfortunate dependence on Java 1.6.
        String bomFreeEntry = entry
        if (Arrays.equals("\uFEFF".getBytes("UTF-8"), Arrays.copyOfRange(bytes, 0, 3))) {
            bomFreeEntry = new String(Arrays.copyOfRange(bytes, 3, bytes.length), "UTF-8")
        }
        return bomFreeEntry
    }
}