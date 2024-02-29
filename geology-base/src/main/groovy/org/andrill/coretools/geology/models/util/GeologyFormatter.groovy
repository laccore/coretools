package org.andrill.coretools.geology.models.util

import java.text.NumberFormat

import org.andrill.coretools.geology.models.Length;

public class GeologyFormatter {
    public static format(value) {
		def formatter = NumberFormat.getInstance(Locale.ENGLISH)
		formatter.applyPattern('0.0##')
		switch (value.class) {
			case Length: return "${formatter.format(value.value)} ${value.unit}"
			case Number: return "${formatter.format(value)}"
			default: return value
		}
	}
}
