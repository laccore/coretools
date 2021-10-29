PSICAT
======

![PSICAT mascot](https://github.com/laccore/coretools/blob/master/tools/PSICAT/img/psicat.gif)

PSICAT is a desktop application intended to capture Initial Core Description (ICD) data.

Description data can be exported in a variety of forms: as tabular data, diagrams in common vector (PDF, SVG) and raster formats (BMP, JPG, PNG), or as a stratigraphic column diagram comprising a user-defined set of core sections.

PSICAT is available for Windows and Macintosh. The Windows build runs under [WINE](https://www.winehq.org/) on Linux systems. See the [Releases](https://github.com/laccore/coretools/releases) page for downloads.

These [video tutorials](https://www.youtube.com/playlist?list=PLLHxfH9IrTIMit13zSZs91_IJBMfNEUYi) describe basic and more advanced features of PSICAT.

If you're having trouble running PSICAT on a Mac, check [this troubleshooting page](https://github.com/laccore/coretools/wiki/Running-PSICAT-on-a-Mac-(macOS-or-OSX)).

If you're still having trouble, think you've found a bug, or have other questions/comments, please use the [feedback form](https://docs.google.com/forms/d/e/1FAIpQLSdKJB-ayDo4btwBa-By4Cd4cL5_MxcE7vcu90K_CfYx03HwuA/viewform) and we'll respond as soon as possible.

PSICAT was originally developed by Josh Reed, in partnership with [CHRONOS](http://chronos.org/index.html) and [ANDRILL](http://www.andrill.org/static/index.html).

![PSICAT screenshot](http://www.beerolf.com/img/psicat.gif)


For Developers: Building PSICAT and SchemeEditor
================================================
Getting PSICAT up and running from source code is challenging, but it can be done!
Please [contact us](https://docs.google.com/forms/d/e/1FAIpQLSdKJB-ayDo4btwBa-By4Cd4cL5_MxcE7vcu90K_CfYx03HwuA/viewform) if you need assistance.

The following instructions are for a macOS environment. Binaries for both Mac and Windows
can be built on a Mac.

### Requirements

##### Griffon 0.2
PSICAT is based on version 0.2 of the [Griffon Framework](https://griffon-framework.org), which is quite old!
It can be downloaded from the [Wayback Machine](https://web.archive.org/web/20150527101132/http://griffon.codehaus.org/Download).

##### Java 6 Development Kit
Java 6 is required to compile source files, and for the Griffon command-line interface to function.

##### Java 8
The packaged Mac (.app) and Windows (.exe) binaries run against Java 8. A Mac/Windows Java 8 runtime must be provided by the user. We use the [Temurin](https://adoptium.net/) binaries (formerly AdoptOpenJDK) in official releases.

##### Gradle 6.5.1
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

##### Java 8 Runtime
Copy your Java 8 Runtime into `coretools/package/java_runtime/mac` and/or `coretools/package/java_runtime/win`.

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

### Creating Mac and Windows Applications
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
In the root `coretools` directory: `./gradlew/packagePSICATMac`

The resulting package is created in `dist/mac`.

##### Windows
Follow the steps in Mac, replacing `Mac` with `Win` in Gradle tasks e.g. `packageWin` and `packagePSICATWin`. The generated PSICAT.exe and SchemeEditor.exe will be found in their respective `/dist/win` directories.
