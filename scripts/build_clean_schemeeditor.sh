# August 7 2024 
# Build SchemeEditor from scratch.
#
# Should be run from root coretools directory.

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


export GRIFFON_HOME="/Applications/griffon-0.2"
export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/sbin:$GRIFFON_HOME/bin

cd tools/SchemeEditor

# Griffon build env requires Java 6
export JAVA_HOME="/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home"

# Ensure all SchemeEditor classes are rebuilt from scratch
griffon clean

# Gradle 6.5.1 requires Java 8, isn't this ridiculous?
export JAVA_HOME="/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home"

# Copy coretools library dependencies (which `griffon clean` blew away)
../../gradlew copyDependencies

# Yup, back to Java 6 for another Griffon command!
export JAVA_HOME="/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home"

# Build SchemeEditor griffon app
griffon compile

# Return to root coretools dir
cd ../..
