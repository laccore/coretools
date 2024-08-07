# August 7 2024
# Build coretools libraries, copy them to PSICAT dir, then build and launch PSICAT.
# Should be run from root coretools dir.

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

# BUILD-TIME ENV VARS
export JAVA_HOME="/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home"
export GRIFFON_HOME="/Applications/griffon-0.2"

#export PATH=$PATH:$GRADLE_HOME/bin:$GRIFFON_HOME/bin
export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$GRIFFON_HOME/bin

./gradlew build
cd tools/PSICAT
../../gradlew copyDependencies

# Griffon 0.2 requires Java 6
export JAVA_HOME="/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home"

# Build and run PSICAT
alias grap="griffon run-app" # Griffon shorthand
griffon run-app # builds, then runs PSICAT

# Return to root coretools dir
cd ../..
