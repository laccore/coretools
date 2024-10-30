import java.util.Random

import org.andrill.coretools.Platform
import org.andrill.coretools.geology.models.SchemeRef
import org.andrill.coretools.model.scheme.*

class DebugUtils {
    static schemeManager = Platform.getService(SchemeManager.class)

    public static SchemeRef randomSchemeEntry(schemeType) {
        def rand = new Random()
        def schemes = schemeManager.getSchemes(schemeType) as List
        def entries = schemes[0].entries as List
        def randomEntry = entries[rand.nextInt(entries.size())]
        return new SchemeRef(scheme:schemes[0].id, code:randomEntry.code)
    }
}