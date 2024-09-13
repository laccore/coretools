package org.andrill.coretools.misc.util

public class StringUtils {
    // lowercase first word
	public static String camel(String str) {
		def camel = str.split(" ").collect { it[0].toUpperCase() + it[1..-1] }.join("")
		return camel[0].toLowerCase() + camel[1..-1]
	}

    // capitalize first word
    public static String camelcap(String str) {
        return str.split(" ").collect { it[0].toUpperCase() + it[1..-1] }.join("")
    }
	
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

	public static String uncamelReplace(String str, String search, String replace) {
		return uncamel(str).replace(search, replace)
	}
}
