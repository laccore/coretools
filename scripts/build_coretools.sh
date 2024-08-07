# July 22 2024
# Build coretools libraries. Should be run from root coretools directory.

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

# Gradle 6.5.1 requires Java 8
export JAVA_HOME="/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home"

./gradlew clean
./gradlew build
