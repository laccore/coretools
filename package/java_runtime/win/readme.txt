October 28 2021
coretools/package/java_runtime/win

The packageWin Gradle tasks in tools/PSICAT/build.gradle and tools/SchemeEditor/build.gradle
require a user-provided Windows Java 8 runtime in this directory.

Then, in coretools/build.gradle, set the project.ext.javaRuntimeFile var to the name of your
runtime file/dir, e.g.

project.ext.javaRuntimeFile = 'jre8'