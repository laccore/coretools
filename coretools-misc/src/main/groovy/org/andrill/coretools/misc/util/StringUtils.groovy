package org.andrill.coretools.misc.util

public class StringUtils {
    // Remove spaces and camel-case input String, making first character of first word lowercase.
	public static String camel(String str) {
		def camel = str.split(" ").collect { it[0].toUpperCase() + it[1..-1] }.join("")
		return camel[0].toLowerCase() + camel[1..-1]
	}

    // Remove spaces and camel-case input String, making first character of first word uppercase.
    public static String camelcap(String str) {
        return str.split(" ").collect { it[0].toUpperCase() + it[1..-1] }.join("")
    }
	
	// Given camel-cased input String, add spaces and make first character of first word uppercase.
	public static String uncamel(String str) {
		StringBuilder b = new StringBuilder()
		str.eachWithIndex { c, index -> 
            // never prepend string with a space
			if (index > 0 && c == c.toUpperCase()) {
				b.append(" ")
			}
			b.append(c)
		}
		def uncamel = b.toString()
		return uncamel[0].toUpperCase() + uncamel[1..-1]
	}

	// Convert tech-y GeologyModel class name to human-friendly name, dropping "Interval" as well, e.g.
	// "UnitInterval" -> "Unit"
	// "GrainSizeInterval" -> "Grain Size".
	// "Feature" -> "Feature" (no change)
	public static String humanizeModelName(String modelName) {
		return uncamel(modelName).replace(" Interval", "")
	}
}
