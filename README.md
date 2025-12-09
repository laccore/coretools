PSICAT
======

![PSICAT mascot](https://github.com/laccore/coretools/blob/master/tools/PSICAT/img/psicat.gif)

PSICAT (Paleontological Stratigraphic Interval Construction and Analysis Tool) is a desktop application for capturing Initial Core Description (ICD) data.

Captured description data can be exported in a variety of forms:
- tabular data (Excel)
- diagrams in common vector (PDF, SVG) and raster formats (BMP, JPG, PNG)
- stratigraphic column diagram comprising a user-defined set of core sections

PSICAT is available for Windows, Macintosh, and Linux.

Pre-built Windows (.exe) and Mac (.app) applications are provided. See the [releases](https://github.com/laccore/coretools/releases) page for downloads.

On Linux systems, PSICAT can be built and run from source code using the "For Developers" instructions below. If you'd rather not build from scratch, the Windows build runs on Linux under [WINE](https://www.winehq.org/).

These [video tutorials](https://www.youtube.com/playlist?list=PLLHxfH9IrTIMit13zSZs91_IJBMfNEUYi) describe basic and more advanced features of PSICAT. **Please note:** As of version 1.2.0, PSICAT has changed sigificantly. Most notably, several new column types and corresponding schemes have been added. Much of the video tutorial content remains relevant, but portions may be out of date. New video tutorials will be produced when PSICAT 1.2.* features are refined and become more settled. You can always [contact the CSD Facility](https://cse.umn.edu/csd/about-us) if you need support using PSICAT.

If you're having trouble running PSICAT on a Mac, check [this troubleshooting page](https://github.com/laccore/coretools/wiki/Using-PSICAT-on-a-Mac-(macOS-or-OSX)).

If you're still having trouble, think you've found a bug, or have other questions/comments, please use the [feedback form](https://docs.google.com/forms/d/e/1FAIpQLSdKJB-ayDo4btwBa-By4Cd4cL5_MxcE7vcu90K_CfYx03HwuA/viewform) and we'll respond as soon as possible.

PSICAT was originally developed by Josh Reed, in partnership with CHRONOS and ANDRILL.

Since 2014, the [CSD Facility](https://cse.umn.edu/csd) has continued maintenance and development of PSICAT.

![PSICAT_screenshot](https://github.com/user-attachments/assets/889bc6d0-3aa8-415d-9a03-0756d632d79d)


For Developers: Building PSICAT and SchemeEditor
================================================
Getting PSICAT up and running from source code is a little fussy, but it can be done!
Please [contact the CSD Facility](https://cse.umn.edu/csd/about-us) if you need assistance.

The following instructions are for a macOS or Linux environment. Binaries for both Mac and Windows can be built on a Mac or Linux machine.

### Requirements

#### Griffon 0.2
PSICAT is based on version 0.2 of the [Griffon Framework](http://new.griffon-framework.org/index.html), which is really, really old. Because it's impossible to find in the wild as of October 2021, a complete Griffon 0.2 package has been included in the `bootstrap` dir.

In order to run under merely-decrepit Java 8 (vs. truly ancient Java 6), make the following tweaks to the scripts in `griffon-0.2/scripts`:  

**_GriffonSettings.groovy, lines 51 and 52**  
Replace `.toBoolean()` with ` as Boolean`, resulting in:
```
enableJndi = getPropertyValue("enable.jndi", false) as Boolean
enableProfile = getPropertyValue("griffon.script.profile", false) as Boolean
```

**_GriffonCompile.groovy, line 51**  
Replace `target: '1.5'` with `target: '1.8'`

Now you can build and run against Java 8, hooray!

#### Java 8
PSICAT and SchemeEditor are developed in Java 8. Mac (.app) and Windows (.exe) binaries run against a bundled Java 8 runtime. A Mac/Windows Java 8 runtime must be provided by the user. We use the [Temurin](https://adoptium.net/) JDK for development and bundle the JRE with releases.

#### Gradle 6.5.1+
A Gradle 6.5.1 wrapper is included (`gradlew`) in the root `coretools` directory.

### Preparation
#### Griffon
Open a Terminal window.

Add the Griffon 0.2 `bin` directory to your path:  
`export PATH=$PATH:/griffon-0.2/bin`

Set the `JAVA_HOME` environment variable to your JDK 8 `Contents/Home` directory, adjusting for your JDK 8 path if needed:

`export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home`

Point Gradle to your JDK 8 path:
In `build.gradle`, update the `javaHome` and `javaExecutablesPath` vars to reflect your JDK 8 installation.

#### Java 8 Runtime
Required to build standalone applications. Copy your Java 8 Runtime into `package/java_runtime/mac` and/or `package/java_runtime/win` as appropriate for your target platform(s).

### Build and Run Locally
In the root directory, `./gradlew build` to build supporting coretools Java and Groovy libraries.

Copy built coretools libraries and other dependencies to PSICAT dir:  
`./gradlew tools:PSICAT:copyDeps`  

Do the same for SchemeEditor:  
`./gradlew tools:SchemeEditor:copyDeps`

Move to the PSICAT directory: `cd tools/PSICAT`

Build and launch the PSICAT application: `griffon run-app`

Similarly, for SchemeEditor: `cd tools/SchemeEditor`

Build and launch the SchemeEditor application: `griffon run-app`


### Create Standalone Mac and Windows Applications
#### Mac
In `build.gradle`, set the `project.ext.javaRuntimeFile` var to your Java 8 runtime's root directory name e.g. for `package/java_runtime/mac/jdk8u472-b08-jre`, set  

`project.ext.javaRuntimeFile = 'jdk8u472-b08-jre'`

We typically update the verbose Temurin 8 runtime root dir name to `'jre8'`. For those building both Mac and Windows applications, we recommend using the same runtime root directory name for both platforms. Otherwise, the name will need to be updated when building for a different platform.

Update the `project.ext.macAppSigningCertificate` with your App Distribution cert's full name, or `-` for ad-hoc signing.

Create a distribution-ready package comprising PSICAT, SchemeEditor, and CSD Facility default Lithology, Grain Size, Bedding, Texture, and Feature schemes:  
`./gradlew packagePSICATMac`

The resulting package is created in `dist/mac`.

#### Windows
Follow the steps in Mac, replacing `Mac` with `Win` e.g. `./gradlew packagePSICATWin`. The generated PSICAT.exe and SchemeEditor.exe will be found in their respective `/dist/win` directories.
