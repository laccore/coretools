coretools/scripts/readme.md
August 7 2024

This dir contains Bash scripts to simplify the messy business of building and running the coretools
libraries, PSICAT, and SchemeEditor, which requires switching between Java 6 (required by Griffon 0.2)
and Java 8 (required by Gradle 6.5.1 wrapper coretools/gradlew).

All scripts should be run from the root coretools directory e.g.

scripts/build_clean_all.sh

. There is a very primitive check for this at launch, easily foiled if you happen to be in
another dir named coretools. So don't do that! ;)