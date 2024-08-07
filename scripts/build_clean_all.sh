# August 7 2024
# Build coretools, PSICAT and SchemeEditor from scratch. A very good thing to do
# before packaging binaries for release with 
#
# ./gradlew packagePSICATMac packagePSICATWin
#
# command, to ensure no old .class files in the ~/.griffon dir find their way into
# those binaries. `griffon clean` is the simplest way to blow those .class files
# away.
#
# This script should be run from the root coretools directory.

# TODO: I lack the bash skills/patience to refactor this check into its own script
# so we don't duplicate it across all the build scripts.
if [[ "$PWD" != */coretools ]]
then
echo ""
echo "Error: this script must be run from the root coretools directory."
echo "The current directory is [$PWD]".
echo ""
exit 1
fi

scripts/build_coretools.sh
scripts/build_clean_psicat.sh
scripts/build_clean_schemeeditor.sh