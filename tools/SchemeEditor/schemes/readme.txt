October 28 2021
coretools/tools/SchemeEditor/schemes

Summary: schemes found in coretools/schemes and those in coretools/tools/SchemeEditor/schemes
(SE_...) schemes are *not* interchangeable!

SchemeEditor depends on the schemes in this directory to provide default
symbol and lithology imagery for new scheme entries.

These schemes differ slightly from the schemes at [higher level], where scheme.xml
lives at the root of the JAR hierarchy. In these schemes, scheme.xml lives at:
- org/psicat/resources/lithologies/scheme.xml (SE_lithologies.jar)
- org/psicat/resources/symbols/scheme.xml (SE_symbols.jar)

SchemeEditor depends on scheme.xml in those locations. Conversely, PSICAT depends
on scheme.xml at the root of the JAR hierarchy! So the coretools/schemes schemes and
SchemeEditor schemes are *not* interchangeable.