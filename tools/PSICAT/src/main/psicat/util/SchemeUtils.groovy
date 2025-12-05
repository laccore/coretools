package psicat.util

import org.andrill.coretools.Platform
import org.andrill.coretools.model.scheme.Scheme
import org.andrill.coretools.model.scheme.SchemeManager

class SchemeUtils {
    static getGrainSizeSchemeEntries() {
		def sm = Platform.getService(SchemeManager.class)
        def entries = []
        sm.schemes.each {
            if (it.type == "grainsize") {
                it.entries.each { e ->
                    entries << [name: e.name, width: e.getProperty('width', '0')]
                }
            }
        }
        return entries
	}
}