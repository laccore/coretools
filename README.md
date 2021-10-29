PSICAT
======

![PSICAT mascot](https://github.com/laccore/coretools/blob/master/tools/PSICAT/img/psicat.gif)

PSICAT is a desktop application for capturing Initial Core Description (ICD) data.


Captured description data can be exported in a variety of forms:
- tabular data (Excel)
- diagrams in common vector (PDF, SVG) and raster formats (BMP, JPG, PNG)
- stratigraphic column diagram comprising a user-defined set of core sections

PSICAT is available for Windows, Macintosh, and Linux.

Pre-built Windows (.exe) and Mac (.app) applications are provided. See the [releases](https://github.com/laccore/coretools/releases) page for downloads.

On Linux systems, PSICAT can be built and run from source code using the "For Developers" instructions below. If you'd rather not build from scratch, the Windows build runs on Linux under [WINE](https://www.winehq.org/).

These [video tutorials](https://www.youtube.com/playlist?list=PLLHxfH9IrTIMit13zSZs91_IJBMfNEUYi) describe basic and more advanced features of PSICAT.

If you're having trouble running PSICAT on a Mac, check [this troubleshooting page](https://github.com/laccore/coretools/wiki/Running-PSICAT-on-a-Mac-(macOS-or-OSX)).

If you're still having trouble, think you've found a bug, or have other questions/comments, please use the [feedback form](https://docs.google.com/forms/d/e/1FAIpQLSdKJB-ayDo4btwBa-By4Cd4cL5_MxcE7vcu90K_CfYx03HwuA/viewform) and we'll respond as soon as possible.

PSICAT was originally developed by Josh Reed, in partnership with [CHRONOS](http://chronos.org/index.html) and [ANDRILL](http://www.andrill.org/static/index.html).

![PSICAT screenshot](http://www.beerolf.com/img/psicat.gif)


For Developers: Building PSICAT and SchemeEditor
================================================
Getting PSICAT up and running from source code is challenging, but it can be done!
Please [contact the CSD Facility](https://cse.umn.edu/csd/about-us) if you need assistance.

The following instructions are for a macOS or Linux environment. Binaries for both Mac and Windows can be built on a Mac or Linux machine.

### Requirements

##### Griffon 0.2
PSICAT is based on version 0.2 of the [Griffon Framework](https://griffon-framework.org), which is really, really old. Because it's impossible to find in the wild as of October 2021, a complete Griffon 0.2 package has been included in the `bootstrap` dir.

##### Java 6 Development Kit
Java 6 is required to compile source files, and for the Griffon command-line interface to function.
Linux users can download JDK 6 [here](https://www.oracle.com/java/technologies/javase-java-archive-javase6-downloads.html).

##### Java 8
The packaged Mac (.app) and Windows (.exe) binaries run against a bundled Java 8 runtime. A Mac/Windows Java 8 runtime must be provided by the user. We use the [Temurin](https://adoptium.net/) binaries (formerly AdoptOpenJDK) in official releases.

##### Gradle 6.5.1+
A Gradle 6.5.1 wrapper is included.

### Preparation
##### Griffon
Open a Terminal window for Griffon use.

Add the Griffon 0.2 `bin` directory to your path.

Set the `JAVA_HOME` environment variable to your JDK 6 `Contents/Home` directory, adjusting for your JDK 6 path if needed:

`export JAVA_HOME=/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home`

##### Gradle
Open another Terminal window for Gradle use. A separate shell is necessary because Gradle 6 requires a `JAVA_HOME` of Java 8 or greater.

Set the `JAVA_HOME` environment variable to your JDK 8+ `Contents/Home` directory.

Point Gradle to your JDK 6 path:
In `coretools/build.gradle`, update the `javaHome` and `javaExecutablesPath` vars to reflect your JDK 6 install.

##### Java 8 Runtime
Required to build standalone applications. Copy your Java 8 Runtime into `coretools/package/java_runtime/mac` and/or `coretools/package/java_runtime/win` as appropriate for your target platform(s).

### Building and Running Locally

In the root `coretools` directory, `./gradlew build` to build supporting coretools
Java and Groovy libraries.

In both the Gradle and Griffon shells, move to the PSICAT directory: `cd tools/PSICAT`

In the Gradle shell, copy coretools libaries into `lib` dir: `../../gradlew copyDependencies`

In the Griffon shell, build and launch the PSICAT application: `griffon run-app`

The resulting JARs are placed in the `staging` directory. The `packageMac` and `packageWin` Gradle tasks depend on those JARs.

Follow the same steps for SchemeEditor:
Both shells: `cd ../SchemeEditor`, then
Gradle shell: `../../gradlew copyDependencies`, then
Griffon shell: `griffon run-app`

### Creating Standalone Mac and Windows Applications
##### Mac
In `coretools/build.gradle`, set the `project.ext.javaRuntimeFile` var to the name of your
Java 8 runtime file/dir, e.g.

`project.ext.javaRuntimeFile = 'jre8'`

For those building both Mac and Windows applications, we recommend using the same runtime file name for both platforms. Otherwise, the name will need to be updated when building for a different platform.

In the Gradle/Java 8 shell, move to the `tools/PSICAT` directory.
Generate a PSICAT Mac .app bundle: `../../gradlew packageMac`

The PSICAT.app bundle is created in `tools/PSICAT/dist/mac`.

Move to the `tools/SchemeEditor` directory and generate that .app bundle: `../../gradlew packageMac`

The SchemeEditor.app bundle is created in `tools/SchemeEditor/dist/mac`

Finally, create a distribution-ready package including stock lithology and symbol schemes.
In the root `coretools` directory: `./gradlew packagePSICATMac`

The resulting package is created in `dist/mac`.

##### Windows
Follow the steps in Mac, replacing `Mac` with `Win` in Gradle tasks e.g. `packageWin` and `packagePSICATWin`. The generated PSICAT.exe and SchemeEditor.exe will be found in their respective `/dist/win` directories.
