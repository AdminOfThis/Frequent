# Frequent
[![Latest Version](https://img.shields.io/github/v/release/AdminOfThis/Frequent?include_prereleases)](https://github.com/AdminOfThis/Frequent/releases)
[![CodeFactor](https://www.codefactor.io/repository/github/adminofthis/frequent/badge)](https://www.codefactor.io/repository/github/adminofthis/frequent) [![Build Status](https://github.com/AdminOfThis/Frequent/workflows/Build/badge.svg)](https://github.com/AdminOfThis/Frequent/actions) [![Release Status](https://github.com/AdminOfThis/Frequent/workflows/Release/badge.svg)](https://github.com/AdminOfThis/Frequent/releases) [![Coverage](https://codecov.io/gh/AdminOfThis/Frequent/branch/master/graph/badge.svg)](https://codecov.io/gh/AdminOfThis/Frequent) [![License: MIT](https://img.shields.io/github/license/AdminOfThis/Frequent)](https://opensource.org/licenses/MIT) [![Known Vulnerabilities](https://snyk.io/test/github/AdminOfThis/Frequent/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/AdminOfThis/Frequent?targetFile=pom.xml)

## Description

This is a lightweight audio analyzer, using the Steinberg ASIO protocol for spectrum analysis of a multichannel audio-stream.
The intended use-case of this application is to use in a live audio engineering setup, and therefore focuses on a lightweight installation, quick setup and operation, stability, as well as a great usability.


## Code
### JRE
This project was originally developed with and for an Oracle Java *JRE8* installation.
It was then upgraded to *Java 13*, using OpenJDK.

Since the project mainly depends on the *jasiohost* library, which in turn uses its precompiled *.dll* files as library, the application is currently (and for the foreseeable future) only able to run on Windows machines.
### Dependencies
 - **JasioHost** library from [mhroth](https://github.com/mhroth) as the underlying ASIO library
 https://github.com/mhroth/jasiohost
	 - Fork from [wind-season](https://github.com/wind-season), which fixes a bug with DANTE Via and MADI-Connections
	 https://github.com/wind-season/jasiohost
 - **OpenJFX** The open source variant of JavaFX as the GUI library, displaying all data
 https://openjfx.io/
 - **Log4j2** for handling logging to console and to file
 https://logging.apache.org/log4j/2.x/
 - **JUnit5** for testing the application, especially during the build
 https://junit.org/junit5/
 - **Apache Commons Collections** for some minor List partitioning https://commons.apache.org/proper/commons-collections/
 - **Rollbar** for realtime Exception and Error logging, helps for debugging https://rollbar.com

### Build
The whole project is built with Maven. It currently contains two submodules, and the main project. The submodules are:

 - **Util** A generic library which contains convenience methods for writing/loading properties, handling JavaFX problems, initializing Logging and much more.
 - **JavaVersionChecker** A simple mini project which checks the installed Java Version against a minimum requirement, to see if the programm can even start. Sits in an extra project, because it gets compiled for Java 1.7 to show an Error Message if neccessary.
 
After the main project is checked out, it is required to initialize and update the submodules with `git submodule --init` and `git submodule update --remote`.

Since the root project contains most of the source code, but root maven projects must be packaged as `pom`, it requires another set of commands to compile it into a `jar` with the help of several Maven plugins.

The main project can be built using `mvn clean compiler:compile compiler:testCompile validate surefire:test package`
It produces a "fat" *jar*, with all dependencies extracted into the jar, as well as a *.zip* file, which contains everything (except a Java13 runtime) required to run the program.
Those artifacts can be found in the `./target` subdirectory.

## Usage
Start the program with the *start.bat*, or the *start_with_console.bat* script in the root directory of the *.zip* file.
The third script, *start_without_checks.bat* starts the main project directly instead of checking for a *JRE* beforehand. If Java8 is installed, this works perfectly fine, but it won't display an error dialog otherwise.
### Input Chooser
You'll be required to choose the correct ASIO driver before the main application launches. 
![IO Chooser Window](https://i.postimg.cc/FzZXxKwc/grafik.png) 
### Main-Window
The main Application window has 3 logical regions:
 1.  **Channellist** 
 2.  **Menubar**
 3. **Module View**
![Main Window](https://i.postimg.cc/HkyRbskB/grafik.png)
### Channellist
The channellist on the left of the screen is used to display and select the input channels of the ASIO driver. It also shows a quick overview over the levels by acting as a vertical VU-meter.
The channels can be renamed, linked into stereo-pairs and added/removed from groups via the context-menu accessible by right clicking any channel in the list.

By default there is also a live preview of the raw data of the selected channel on the bottom of the channel list. This can be paused for a closer look by clicking on the chart, swapped with a waveform-preview by double clicking or using the buttons under the chart, or completely removed by toggling the button above the channellist *Wave*.

### Modules
The content of the Module View can be chosen with the buttons on the top-right.
Currently there are 6 Modules to choose from:

| Module-Name | Description |
| --- | --- |
|*Overview*| A basic view with a VU-Meter for every ASIO-Input. Can be used to have a quick overview on all the input channels without prior configuration or setup|
|*Spectrum* | The most used view, displays the frequency spectrum of the selected channel, using fourier transformation, in the range from *10Hz* to *20kHz* |
|*RTA* | Shows the frequency spectrum over time, by coloring more active frequency in a color-shade from yellow to red |
|*Groups* | Displays all the groups and their corresponding channels in one big overview|
|*Phase*|Shows the phase alignment of two channels, commonly used to see if stereo pairs, such as keyboards and guitars are out of phase |.
|*Bleed*| Shows the percentage of noise bleed of a selectable master signal into other channels signal. Can be used to check how much of the PA is audible in vocal or other mics. |
### Modules in Development
There are other modules, that can be accessed by starting the program with the flag `-debug`, but those are currently not considered useful.
